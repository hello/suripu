package com.hello.suripu.queue.workers;

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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by ksg on 3/16/16
 */

public class TimelineQueueProducerManager implements Managed {
    private final Logger LOGGER = LoggerFactory.getLogger(TimelineQueueProducerManager.class);

    private static final int MAX_BATCH_SIZE = 10;
    private static final int HOUR_IN_MILLIS = 3600000;
    private static final int LOCAL_HOUR_TRIGGER_TIMELINE = 13; // compute timeline when local hour is >= 1pm  and < 2pm

    private final AmazonSQSAsync sqsClient;
    private final String sqsQueueUrl;
    private final SenseDataDAO senseDataDAO;
    private final ExecutorService executor;
    private final int numProducerThreads;

    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture scheduledFuture;
    private final long scheduleIntervalMinutes;

    private final ImmutableMap<Integer, List<Integer>> GMTHourToTimeZoneMap;

    private static class BatchResult {
        final int success;
        final List<String> failedMessages = Lists.newArrayList();

        private BatchResult(final int success, final List<String> failedMessages) {
            this.success = success;
            this.failedMessages.addAll(failedMessages);
        }
    }

    public TimelineQueueProducerManager(final AmazonSQSAsync sqsClient,
                                        final SenseDataDAO senseDataDAO,
                                        final String sqsQueueUrl,
                                        final ExecutorService executor,
                                        final long scheduleIntervalMinutes,
                                        final int numProducerThreads) {
        this.sqsClient = sqsClient;
        this.senseDataDAO = senseDataDAO;
        this.sqsQueueUrl = sqsQueueUrl;
        this.executor = executor;
        this.scheduleIntervalMinutes = scheduleIntervalMinutes;
        this.numProducerThreads = numProducerThreads;

        // initialize timezoneMap
        final Map<Integer, List<Integer>> tempMap = Maps.newHashMap();
        for (int hour = 1; hour <= 22; hour++) {
            final int offsetMillis = (LOCAL_HOUR_TRIGGER_TIMELINE - hour) * HOUR_IN_MILLIS;
            tempMap.put(hour, Lists.newArrayList(offsetMillis));
        }
        tempMap.put(0, Lists.newArrayList(13*HOUR_IN_MILLIS, -11*HOUR_IN_MILLIS)); // GMT+13 same day, GMT-11 yesterday
        tempMap.put(23, Lists.newArrayList(14*HOUR_IN_MILLIS, -10*HOUR_IN_MILLIS)); // GMT+14 next day, GMT-10 same day
        GMTHourToTimeZoneMap = ImmutableMap.copyOf(tempMap);
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
        scheduledExecutor.shutdown();
        LOGGER.warn("key=suripu-queue-producer warning=threadPool-shutdown");

    }

    private void startProducing() {
        scheduledFuture = scheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    LOGGER.info("key=suripu-queue-producer action=scheduled-start");
                    produceMessages();
                } catch (Exception exception) {
                    LOGGER.error("key=suripu-queue-producer action=fail-to-start-msg-production");
                    exception.printStackTrace();

                }
            }
        }, scheduleIntervalMinutes, scheduleIntervalMinutes, TimeUnit.MINUTES);
    }


    private void produceMessages() throws ExecutionException, InterruptedException {

        // TODO: check if the minute of hour is right for producing

        // TODO: add metrics


        final DateTime now = DateTime.now(DateTimeZone.UTC);
        LOGGER.debug("key==suripu-queue-producer action=produce-message now={}", now);

        // check if we have the offsetMillis for this hour
        final int nowHour = now.getHourOfDay();
        if (!GMTHourToTimeZoneMap.containsKey(nowHour)) {
            LOGGER.error("key=suripu-queue-producer error=no-time-zones-to-process gmt_hour={}", nowHour);
            return;
        }

        final List<Integer> offsetMillisList = GMTHourToTimeZoneMap.get(nowHour);

        // get list of users with sense-data in the last 24 hours for a timezone
        final Map<Long, String> accountIds = getCurrentTimezoneAccountIds(now, offsetMillisList);
        final List<SendMessageBatchRequestEntry> messages  = Lists.newArrayList();
        final TimelineQueueProtos.Message.Builder builder = TimelineQueueProtos.Message.newBuilder();
        int totalMessages = 0;
        int totalMessagesSent = 0;
        int totalMessagesFail = 0;

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

            // not enough, keep piling
            if (messages.size() < numProducerThreads * MAX_BATCH_SIZE) {
                continue;
            }

            // now, send the batch
            final BatchResult result = sendBatchMessages(messages);
            totalMessagesSent += result.success;

            // check if any messages fail to be sent, add to the next batch
            if (!result.failedMessages.isEmpty()) {
                totalMessagesFail += result.failedMessages.size();
                LOGGER.debug("key=suripu-queue-producer action=resend-message size={}", result.failedMessages.size());

                for (final String failedId : result.failedMessages) {
                    String [] splitId = failedId.split("_");
                    final TimelineQueueProtos.Message failedMsg = TimelineQueueProtos.Message.newBuilder()
                            .setAccountId(Long.valueOf(splitId[0]))
                            .setTargetDate(splitId[1])
                            .setTimestamp(DateTime.now().getMillis()).build();
                    messages.add(new SendMessageBatchRequestEntry(failedId, TimelineQueueProcessor.encodeMessage(failedMsg)));
                }
            }
        }

        // summary stats
        LOGGER.debug("key=suripu-queue-producer stats=summary now={} timezones={} first={} num_accounts={}",
                now, offsetMillisList.size(), offsetMillisList.get(0), accountIds.size());
        LOGGER.debug("key=suripu-queue-producer stats=summary-number-of-account-ids value={}", accountIds.size());
        LOGGER.debug("key=suripu-queue-producer stats=summary-total-messages-created value={}", totalMessages);
        LOGGER.debug("key=suripu-queue-producer stats=summary-send-success value={}", totalMessagesSent);
        LOGGER.debug("key=suripu-queue-producer stats=summary-send-failures value={}", totalMessagesFail);
    }

    @VisibleForTesting
    protected Map<Long, String> getCurrentTimezoneAccountIds(final DateTime now, final List<Integer> offsetMillisList) {
        // TODO: test this
        final Map<Long, String>  accountIdDateMap = Maps.newHashMap();
        for (final Integer offsetMillis : offsetMillisList) {
            final DateTime targetNight = now.plusMillis(offsetMillis).withTimeAtStartOfDay().minusDays(1);
            final ImmutableList<AccountData> validAccountIds = senseDataDAO.getValidAccounts(now.withTimeAtStartOfDay().minusDays(1),
                    now.withTimeAtStartOfDay(), offsetMillis);
            for (final AccountData data : validAccountIds) {
                accountIdDateMap.put(data.accountId, DateTimeUtil.dateToYmdString(targetNight));
            }
        }
        return accountIdDateMap;
    }

    private BatchResult sendBatchMessages(final List<SendMessageBatchRequestEntry> messages) throws ExecutionException, InterruptedException
    {
        final List<List<SendMessageBatchRequestEntry>> batches = Lists.partition(messages, MAX_BATCH_SIZE);
        final List<Future<SendMessageBatchResult>> futures = Lists.newArrayListWithCapacity(batches.size());

        LOGGER.debug("key=suripu-queue-producer action=send-batch-messages size={}", batches.size());

        for (final List<SendMessageBatchRequestEntry> batch : batches) {
            // construct a batch message request
            final Future<SendMessageBatchResult> future = executor.submit(new Callable<SendMessageBatchResult>() {
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
            if (!failedEntries.isEmpty()) {
                for (final BatchResultErrorEntry entry : failedEntries) {
                    final String failedId = entry.getId();
                    LOGGER.error("key=suripu-queue-producer error=failed-message msg_id={} error_code={}", failedId, entry.getCode());
                    failedAccountIdDates.add(failedId);
                }
            }
        }

        return new BatchResult(success, failedAccountIdDates);
    }
}
