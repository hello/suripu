package com.hello.suripu.queue.timeline;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.BatchResultErrorEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.api.queue.TimelineQueueProtos;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.queue.models.AccountData;
import com.hello.suripu.queue.models.SenseDataDAO;
import com.yammer.dropwizard.lifecycle.Managed;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by ksg on 3/16/16
 */

public class TimelineQueueProducerManager implements Managed {
    private final Logger LOGGER = LoggerFactory.getLogger(TimelineQueueProducerManager.class);

    private static final int MAX_BATCH_SIZE = 10; // max messages in each SQS batch
    private static final int HOUR_IN_MILLIS = 3600000;
    private static final int LOCAL_HOUR_TRIGGER_TIMELINE = 13; // compute timeline when local hour is >= 1pm  and < 2pm
    private static final int MAX_ATTEMPTS_LAST_BATCH = 5; // try up to 5 times to send the last batch of messages

    private final AmazonSQSAsync sqsClient;
    private final String sqsQueueUrl;
    private final SenseDataDAO senseDataDAO;
    private final ExecutorService sendMessageExecutor;
    private final int startSendingMessageSize;

    // main thread scheduler
    private final ScheduledExecutorService producerExecutor;
    private ScheduledFuture scheduledFuture; // mutable, get set each time start() is called
    private final long scheduleIntervalMinutes;

    private final ImmutableMap<Integer, List<Integer>> GMTHourToTimeZoneMap; // map GMT hour to offsetMillis of TZ at 1pm

    // results from batch-send
    private static class BatchResult {
        final int success;
        final List<String> failedMessages = Lists.newArrayList();

        private BatchResult(final int success, final List<String> failedMessages) {
            this.success = success;
            this.failedMessages.addAll(failedMessages);
        }
    }

    // metrics TODO: MOAR PLS
    private final Meter messagesCreated;
    private final Meter messagesSent;
    private final Meter sentSuccess;
    private final Meter sentFailures;
    private final Meter accountsProcessed;

    // TODO: internal metrics
    private long totalMessages = 0;
    private long totalMessagesSent = 0;
    private long totalMessagesFail = 0;

    public TimelineQueueProducerManager(final AmazonSQSAsync sqsClient,
                                        final SenseDataDAO senseDataDAO,
                                        final String sqsQueueUrl,
                                        final ScheduledExecutorService producerExecutor,
                                        final ExecutorService sendMessageExecutor,
                                        final long scheduleIntervalMinutes,
                                        final int numProducerThreads) {
        this.sqsClient = sqsClient;
        this.senseDataDAO = senseDataDAO;
        this.sqsQueueUrl = sqsQueueUrl;
        this.sendMessageExecutor = sendMessageExecutor;
        this.producerExecutor = producerExecutor;
        this.scheduleIntervalMinutes = scheduleIntervalMinutes;
        this.startSendingMessageSize = numProducerThreads * MAX_BATCH_SIZE;

        // initialize timezoneMap
        final Map<Integer, List<Integer>> tempMap = Maps.newHashMap();
        for (int hour = 1; hour <= 22; hour++) {
            final int offsetMillis = (LOCAL_HOUR_TRIGGER_TIMELINE - hour) * HOUR_IN_MILLIS;
            tempMap.put(hour, Lists.newArrayList(offsetMillis));
        }
        tempMap.put(0, Lists.newArrayList(13*HOUR_IN_MILLIS, -11*HOUR_IN_MILLIS)); // GMT+13 same day, GMT-11 yesterday
        tempMap.put(23, Lists.newArrayList(14*HOUR_IN_MILLIS, -10*HOUR_IN_MILLIS)); // GMT+14 next day, GMT-10 same day
        this.GMTHourToTimeZoneMap = ImmutableMap.copyOf(tempMap);

        // metrics
        this.messagesCreated = Metrics.defaultRegistry().newMeter(TimelineQueueProducerManager.class, "created", "messages-created", TimeUnit.SECONDS);
        this.messagesSent = Metrics.defaultRegistry().newMeter(TimelineQueueProducerManager.class, "sent", "messages-sent", TimeUnit.SECONDS);
        this.sentSuccess = Metrics.defaultRegistry().newMeter(TimelineQueueProducerManager.class, "success", "messages-sent-success", TimeUnit.SECONDS);
        this.sentFailures = Metrics.defaultRegistry().newMeter(TimelineQueueProducerManager.class, "fail", "messages-sent-fail", TimeUnit.SECONDS);
        this.accountsProcessed = Metrics.defaultRegistry().newMeter(TimelineQueueProducerManager.class, "accounts", "accounts-processed", TimeUnit.SECONDS);
    }


    @Override
    public void start() throws Exception {
        LOGGER.info("key=suripu-queue-producer action=started");
        startProducing();
    }

    @Override
    public void stop() throws Exception {
        scheduledFuture.cancel(true);
        LOGGER.warn("key=suripu-queue-producer action=stopped");
        LOGGER.warn("key=suripu-queue-producer warning=threadPool-shutdown");

    }

    public long getTotalMessagesCreated() {
        return this.totalMessages;
    }

    public long getTotalMessagesSent() {
        return this.totalMessagesSent;
    }

    public long getTotalMessagesFailed() {
        return this.totalMessagesFail;
    }

    private void startProducing() {
        scheduledFuture = this.producerExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    LOGGER.info("key=suripu-queue-producer action=scheduled-start");
                    sendMessages();
                } catch (Exception exception) {
                    LOGGER.error("key=suripu-queue-producer action=fail-to-start-msg-production");
                    exception.printStackTrace();

                }
            }
        }, this.scheduleIntervalMinutes, this.scheduleIntervalMinutes, TimeUnit.MINUTES);
    }


    private void sendMessages() throws ExecutionException, InterruptedException {

        // get current time in UTC
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        LOGGER.debug("key=suripu-queue-producer action=send-messages-start now={}", now);

        // check if the minute of hour is right for sending queue messages, first 10 minutes of the hour
        final int nowMinutes = now.getMinuteOfHour();
        if (nowMinutes >= this.scheduleIntervalMinutes) {
            LOGGER.debug("key=suripu-queue-producer action=send-messages-skip now_minutes={} threshold={}",
                    nowMinutes, this.scheduleIntervalMinutes);
            return;
        }

        // check if we have a valid offsetMillis for this hour
        final int nowHour = now.getHourOfDay();
        if (!this.GMTHourToTimeZoneMap.containsKey(nowHour)) {
            LOGGER.error("key=suripu-queue-producer error=missing-offset-millis gmt_hour={}", nowHour);
            return;
        }

        final List<Integer> offsetMillisList = this.GMTHourToTimeZoneMap.get(nowHour);

        // get list of users with sense-data in the last 24 hours for a timezone
        final Map<Long, String> accountIds = getCurrentTimezoneAccountIds(now, offsetMillisList);

        // process account-ids
        final List<SendMessageBatchRequestEntry> messages  = Lists.newArrayList();
        final TimelineQueueProtos.Message.Builder builder = TimelineQueueProtos.Message.newBuilder();

        final long startTotal = this.totalMessages;
        final long startSent = this.totalMessagesSent;
        final long startFail = this.totalMessagesFail;

        for (final Map.Entry<Long, String> entry : accountIds.entrySet()) {

            // create message
            final Long accountId = entry.getKey();
            final String targetDate = entry.getValue();
            final String messageId = String.format("%s_%s", String.valueOf(accountId), targetDate);

            final TimelineQueueProtos.Message message = builder
                    .setAccountId(accountId)
                    .setTargetDate(targetDate)
                    .setTimestamp(DateTime.now().getMillis()).build();

            // add message to batch
            messages.add(new SendMessageBatchRequestEntry(messageId, TimelineQueueProcessor.encodeMessage(message)));

            this.totalMessages++;
            this.messagesCreated.mark();

            // not enough, keep piling
            if (messages.size() < this.startSendingMessageSize) {
                continue;
            }

            // now, send the batch
            final BatchResult result = sendBatchMessages(messages);

            totalMessagesSent += result.success;
            this.sentSuccess.mark(result.success);

            messages.clear();

            // check if any messages fail to be sent, add to the next batch
            if (!result.failedMessages.isEmpty()) {
                LOGGER.debug("key=suripu-queue-producer action=resend-failed-messages size={}", result.failedMessages.size());

                messages.addAll(processFailedMessages(result.failedMessages));
                totalMessagesFail += result.failedMessages.size();
                this.sentFailures.mark(result.failedMessages.size());
            }

        }

        // finished the last batch of messages
        int attempts = 0;
        while (!messages.isEmpty()) {
            attempts++;

            this.messagesCreated.mark(messages.size());
            LOGGER.debug("key=suripu-queue-producer action=send-last-batch attempt={} size={}", attempts, messages.size());

            final BatchResult result = sendBatchMessages(messages);
            totalMessagesSent += result.success;
            this.sentSuccess.mark(result.success);

            messages.clear();

            // check if any messages fail to be sent, add to the next batch
            if (!result.failedMessages.isEmpty()) {
                LOGGER.debug("key=suripu-queue-producer action=send-last-batch-failures size={}", result.failedMessages.size());

                messages.addAll(processFailedMessages(result.failedMessages));
                totalMessagesFail += result.failedMessages.size();
                this.sentFailures.mark(result.failedMessages.size());
            }

            if (attempts > MAX_ATTEMPTS_LAST_BATCH) {
                LOGGER.info("key=suripu-queue-producer action=send-last-batch-abort max_attempts={}", attempts);
                break;
            }
        }

        LOGGER.info("key=suripu-queue-producer action=send-messages-completed");
        LOGGER.info("key=suripu-queue-producer action=summary now={} num_timezones={} first_tz={} " +
                        "num_accounts={} total_msg_created={} send_success={} send_fail={}",
                now, offsetMillisList.size(), offsetMillisList.get(0), accountIds.size(),
                totalMessages - startTotal, totalMessagesSent - startSent, totalMessagesFail - startFail);
    }

    /**
     * Based on the current hour of GMT+0 now, select set of users where local time has passed 1pm
     * @param now now in UTC
     * @param offsetMillisList timezone offset-millis to get
     * @return Map of <account-id, target-night-date>
     */
    @VisibleForTesting
    public Map<Long, String> getCurrentTimezoneAccountIds(final DateTime now, final List<Integer> offsetMillisList) {
        final Map<Long, String>  accountIdDateMap = Maps.newHashMap();
        for (final Integer offsetMillis : offsetMillisList) {

            final String targetNight = DateTimeUtil.dateToYmdString(now.plusMillis(offsetMillis).withTimeAtStartOfDay().minusDays(1));

            LOGGER.debug("key=suripu-queue-producer now={} action=get-valid-accounts offset_millis={} target_night={}",
                    now, offsetMillis, targetNight);

            final ImmutableList<AccountData> validAccounts = this.senseDataDAO.getValidAccounts(
                    now.withTimeAtStartOfDay().minusDays(1),
                    now.withTimeAtStartOfDay(),
                    offsetMillis);

            for (final AccountData account : validAccounts) {
                accountIdDateMap.put(account.accountId, targetNight);
            }

            LOGGER.debug("key=suripu-queue-producer action=get-valid-accounts GMT_hour={} offset_millis={} accounts={}",
                    now.getHourOfDay(), offsetMillisList, validAccounts.size());
        }

        this.accountsProcessed.mark(accountIdDateMap.size());
        return accountIdDateMap;
    }

    private BatchResult sendBatchMessages(final List<SendMessageBatchRequestEntry> messages) throws ExecutionException, InterruptedException
    {
        LOGGER.debug("key=suripu-queue-producer action=send-batch size={}", messages.size());

        final List<List<SendMessageBatchRequestEntry>> batches = Lists.partition(messages, MAX_BATCH_SIZE);
        final List<Future<SendMessageBatchResult>> futures = Lists.newArrayListWithCapacity(batches.size());

        this.messagesSent.mark(messages.size());

        for (final List<SendMessageBatchRequestEntry> batch : batches) {
            // construct a batch message request
            final Future<SendMessageBatchResult> future = this.sendMessageExecutor.submit(new Callable<SendMessageBatchResult>() {
                @Override
                public SendMessageBatchResult call() throws Exception {
                    return sqsClient.sendMessageBatch(sqsQueueUrl, batch);
                }
            });
            futures.add(future);
        }

        // check results
        final List<String> failedAccountIdDates = Lists.newArrayList();
        int success = 0;
        for (final Future<SendMessageBatchResult> future : futures) {
            final SendMessageBatchResult result = future.get();
            success += result.getSuccessful().size();

            // process failures
            final List<BatchResultErrorEntry> failedEntries = result.getFailed();
            for (final BatchResultErrorEntry entry : failedEntries) {
                final String failedId = entry.getId();
                failedAccountIdDates.add(failedId);
                LOGGER.error("key=suripu-queue-producer error=failed-message msg_id={} error_code={}",
                        failedId, entry.getCode());
            }

        }

        return new BatchResult(success, failedAccountIdDates);
    }


    private List<SendMessageBatchRequestEntry> processFailedMessages(List<String> failedIds) {
        final List<SendMessageBatchRequestEntry> messages = Lists.newArrayList();

        for (final String failedId : failedIds) {
            final String [] splitId = failedId.split("_");

            if (splitId.length == 2) {
                final TimelineQueueProtos.Message failedMsg = TimelineQueueProtos.Message.newBuilder()
                        .setAccountId(Long.valueOf(splitId[0]))
                        .setTargetDate(splitId[1])
                        .setTimestamp(DateTime.now().getMillis()).build();

                messages.add(new SendMessageBatchRequestEntry(failedId, TimelineQueueProcessor.encodeMessage(failedMsg)));
            } else {
                LOGGER.error("key=suripu-queue-producer error=bogus-message-id message=id={}", failedId);
            }
        }

        return messages;
    }
}
