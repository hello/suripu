package com.hello.suripu.queue.cli;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.BatchResultErrorEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.api.queue.TimelineQueueProtos;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.queue.configuration.SQSConfiguration;
import com.hello.suripu.queue.configuration.SuripuQueueConfiguration;
import com.hello.suripu.queue.workers.TimelineQueueProcessor;
import com.opencsv.CSVReader;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.cli.EnvironmentCommand;
import com.yammer.dropwizard.config.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by kingshy on 2/18/16
 */
public class PopulateTimelineQueueCommand extends EnvironmentCommand<SuripuQueueConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PopulateTimelineQueueCommand.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(12);

    private static final int MAX_BATCH_SIZE = 10;

    private static class BatchResult {
        final int success;
        final List<String> failedMessages = Lists.newArrayList();

        private BatchResult(final int success, final List<String> failedMessages) {
            this.success = success;
            this.failedMessages.addAll(failedMessages);
        }
    }


    public PopulateTimelineQueueCommand(Service<SuripuQueueConfiguration> service, String name, String description) {
        super(service, name, description);
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
        // for sending messages
        subparser.addArgument("--csv")
                .nargs("?")
                .required(true)
                .help("csv file of account-ids to process");

        subparser.addArgument("--num_msg")
                .nargs("?")
                .required(true)
                .help("number of messages to send");
    }

    @Override
    protected void run(Environment environment, Namespace namespace, SuripuQueueConfiguration configuration) throws Exception {
        // read file csv file of accountIds, min Date string
        final File accountsFile = new File(namespace.getString("csv"));
        final Map<Long, DateTime> accountIds = readDataFile(accountsFile);
        LOGGER.debug("key=accounts-to-process-size value={}", accountIds.size());

        // no. of messages to add to queue per account-id
        final Integer maxMessages = (namespace.getString("num_msg") != null) ? Integer.valueOf(namespace.getString("num_msg")) : 10;

        // set up sqs client
        final AWSCredentialsProvider provider = new DefaultAWSCredentialsProviderChain();

        final SQSConfiguration sqsConfig = configuration.getSqsConfiguration();
        final int maxConnections = sqsConfig.getSqsMaxConnections();
        final AmazonSQSAsync sqsAsync = new AmazonSQSAsyncClient(provider, new ClientConfiguration()
                .withMaxConnections(maxConnections)
                .withConnectionTimeout(500));
        final AmazonSQSAsync sqs = new AmazonSQSBufferedAsyncClient(sqsAsync);

        final Region region = Region.getRegion(Regions.US_EAST_1);
        sqs.setRegion(region);

        // get queue url
        final Optional<String> optionalSqsQueueUrl = TimelineQueueProcessor.getSQSQueueURL(sqs, sqsConfig.getSqsQueueName());
        if (!optionalSqsQueueUrl.isPresent()) {
            LOGGER.error("error=no-sqs-queue-found value=queue-name-{}", sqsConfig.getSqsQueueName());
            throw new Exception("Invalid queue name");
        }

        final String sqsQueueUrl = optionalSqsQueueUrl.get();

        int totalMessages = 0;
        int totalMessagesSent = 0;
        int totalMessagesFail = 0;
        final List<SendMessageBatchRequestEntry> messages  = Lists.newArrayList();

        // process each account-id
        for (final Map.Entry<Long, DateTime> entry : accountIds.entrySet()) {

            // generate a batch of messages for account-id
            final Long accountId = entry.getKey();
            final DateTime startDate = entry.getValue();
            LOGGER.debug("key=processing value=account-{}|startdate-{}", accountId, startDate);

            final List<SendMessageBatchRequestEntry> batch = generateMessages(accountId, startDate, maxMessages);
            totalMessages += batch.size();

            LOGGER.debug("key=message-info value=days-{}|leftovers-{}", accountId, batch.size(), messages.size());

            messages.addAll(batch);

            // batch send a whole bunch of messages in multiple threads
            final BatchResult result = sendBatchMessages(sqs, sqsQueueUrl, messages);

            LOGGER.debug("key=result value=success={}|fail={}", accountId, result.success, result.failedMessages.size());
            totalMessagesSent += result.success;

            messages.clear();

            // check if any messages fail to be sent, re-add to the next batch
            if (!result.failedMessages.isEmpty()) {
                totalMessagesFail += result.failedMessages.size();
                LOGGER.debug("key=resend-message-size value={}", result.failedMessages.size());
                for (final String failedId : result.failedMessages) {
                    String [] parts = failedId.split("_");
                    final TimelineQueueProtos.Message msg = TimelineQueueProtos.Message.newBuilder()
                            .setAccountId(Long.valueOf(parts[0]))
                            .setTargetDate(parts[1])
                            .setTimestamp(DateTime.now().getMillis()).build();
                    messages.add(new SendMessageBatchRequestEntry(failedId, TimelineQueueProcessor.encodeMessage(msg)));
                }
            }

        }

        LOGGER.debug("key=summary-number-of-account-ids value={}", accountIds.size());
        LOGGER.debug("key=summary-total-messages-created value={}", totalMessages);
        LOGGER.debug("key=summary-send-success value={}", totalMessagesSent);
        LOGGER.debug("key=summary-send-failures value={}", totalMessagesFail);

        this.executor.shutdown();
    }

    private BatchResult sendBatchMessages(final AmazonSQS sqsClient,
                                          final String queueUrl,
                                          final List<SendMessageBatchRequestEntry> messages) throws ExecutionException, InterruptedException
    {
        final List<List<SendMessageBatchRequestEntry>> batches = Lists.partition(messages, MAX_BATCH_SIZE);
        final List<Future<SendMessageBatchResult>> futures = Lists.newArrayListWithCapacity(batches.size());

        LOGGER.debug("key=no-of-batches value={}", batches.size());

        for (final List<SendMessageBatchRequestEntry> batch : batches) {
            // construct a batch message request
            final Future<SendMessageBatchResult> future = executor.submit(new Callable<SendMessageBatchResult>() {
                @Override
                public SendMessageBatchResult call() throws Exception {
                    final SendMessageBatchResult result = sqsClient.sendMessageBatch(queueUrl, batch);
                    return result;
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
                    LOGGER.error("key=failed-message-id value={}|{}", failedId, entry.getCode());
                    failedAccountIdDates.add(failedId);
                }
            }
        }

        return new BatchResult(success, failedAccountIdDates);
    }

    private List<SendMessageBatchRequestEntry> generateMessages(final Long accountId, final DateTime startDate, final int maxDays) {

        final List<SendMessageBatchRequestEntry> messages = Lists.newArrayList();

        // determine max no. of msg
        final DateTime now = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
        final Days days = Days.daysBetween(startDate, now);
        final int numDays = (days.getDays() > maxDays) ? maxDays : days.getDays();

        // create messages
        final TimelineQueueProtos.Message.Builder builder = TimelineQueueProtos.Message.newBuilder();
        for (int i = 2; i <= numDays+1; i++) {
            final String targetDate = DateTimeUtil.dateToYmdString(now.minusDays(i));
            final TimelineQueueProtos.Message msg = builder
                    .setAccountId(accountId)
                    .setTargetDate(targetDate)
                    .setTimestamp(DateTime.now().getMillis()).build();
            final String messageId = String.format("%s_%s", String.valueOf(accountId), targetDate);
            messages.add(new SendMessageBatchRequestEntry(messageId, TimelineQueueProcessor.encodeMessage(msg)));
        }
        return messages;
    }

    /**
     * Read list of account-ids to process, format: account-id,y-m-d-string
     * @param filename data file
     * @return map of <account-id, date>
     * @throws IOException
     */
    private Map<Long, DateTime> readDataFile(final File filename) throws IOException {
        final Map<Long, DateTime> accountIds = Maps.newHashMap();
        try (final InputStream input = new FileInputStream(filename);
             final CSVReader reader = new CSVReader(new InputStreamReader(input), ',')) {
            for (final String[] entry : reader) {
                final DateTime date = DateTimeUtil.ymdStringToDateTime(entry[1]);
                final Long accountId = Long.valueOf(entry[0]);
                accountIds.put(accountId, date);
            }
        }
        return accountIds;
    }

}
