package com.hello.suripu.queue.workers;

import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.yammer.dropwizard.lifecycle.Managed;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by ksg on 3/15/16
 * docs: http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/Welcome.html
 */

public class TimelineQueueConsumerManager implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineQueueConsumerManager.class);

    private static final long SLEEP_WHEN_NO_MESSAGES_MILLIS = 10000L; // 10 secs

    private final TimelineQueueProcessor queueProcessor;
    private final TimelineProcessor timelineProcessor;

    private final ExecutorService timelineExecutor;
    private final ExecutorService consumerExecutor;

    private boolean isRunning = false; // mutable

    // metrics
    private final Meter messagesProcessed;
    private final Meter messagesReceived;
    private final Meter messagesDeleted;
    private final Meter validSleepScore;
    private final Meter invalidSleepScore;
    private final Meter noTimeline;

    private long totalMessagesProcessed;
    private long totalIdleIterations;
    private long totalRunningIterations;

    public TimelineQueueConsumerManager(final TimelineQueueProcessor queueProcessor,
                                        final TimelineProcessor timelineProcessor,
                                        final ExecutorService consumerExecutor,
                                        final ExecutorService timelineExecutors) {
        this.queueProcessor = queueProcessor;
        this.timelineProcessor = timelineProcessor;
        this.timelineExecutor = timelineExecutors;
        this.consumerExecutor = consumerExecutor;

        // metrics
        this.messagesProcessed = Metrics.defaultRegistry().newMeter(TimelineQueueConsumerManager.class, "processed", "messages-processed", TimeUnit.SECONDS);
        this.messagesReceived = Metrics.defaultRegistry().newMeter(TimelineQueueConsumerManager.class, "received", "messages-received", TimeUnit.SECONDS);
        this.messagesDeleted = Metrics.defaultRegistry().newMeter(TimelineQueueConsumerManager.class, "deleted", "messages-deleted", TimeUnit.SECONDS);
        this.validSleepScore = Metrics.defaultRegistry().newMeter(TimelineQueueConsumerManager.class, "ok-sleep-score", "valid-score", TimeUnit.SECONDS);
        this.invalidSleepScore = Metrics.defaultRegistry().newMeter(TimelineQueueConsumerManager.class, "invalid-sleep-score", "invalid-score", TimeUnit.SECONDS);
        this.noTimeline = Metrics.defaultRegistry().newMeter(TimelineQueueConsumerManager.class, "timeline-fail", "fail-to-created", TimeUnit.SECONDS);

    }

    public long getProcessed() {
        return this.totalMessagesProcessed;
    }

    public long getRunningIterations() {
        return this.totalRunningIterations;
    }

    public long getIdleIterations() {
        return this.totalIdleIterations;
    }

    @Override
    public void start() throws Exception {
        consumerExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    LOGGER.debug("key=suripu-queue-consumer action=started");
                    isRunning = true;
                    processMessages();
                } catch (Exception exception) {
                    isRunning = false;
                    LOGGER.error("key=suripu-queue-consumer error=fail-to-start-consumer-thread msg={}", exception.getMessage());
                    LOGGER.error("key=suripu-queue-consumer error=dead-forcing-exit");
                    try {
                        // sleep 5 secs messages to flush
                        Thread.sleep(5000L);
                    } catch (InterruptedException ignored) {
                    }
                    System.exit(1);
                }
            }
        });

    }

    @Override
    public void stop() throws Exception {
        LOGGER.debug("key=suripu-queue-consumer action=stopped");
        isRunning = false;
    }


    private void processMessages() throws Exception {

        int numEmptyQueueIterations = 0;
        do {
            // get a bunch of messages from SQS
            final List<TimelineQueueProcessor.TimelineMessage> messages = queueProcessor.receiveMessages();

            messagesReceived.mark(messages.size());

            final List<Future<Optional<TimelineQueueProcessor.TimelineMessage>>> futures = Lists.newArrayListWithCapacity(messages.size());

            if (!messages.isEmpty()) {
                this.totalRunningIterations++;
                LOGGER.debug("key=suripu-queue-consumer action=processing size={}", messages.size());

                // generate all the timelines
                for (final TimelineQueueProcessor.TimelineMessage message : messages) {
                    final TimelineGenerator generator = new TimelineGenerator(this.timelineProcessor, message);
                    final Future<Optional<TimelineQueueProcessor.TimelineMessage>> future = timelineExecutor.submit(generator);
                    futures.add(future);
                }

                // prepare to delete processed messages
                final List<DeleteMessageBatchRequestEntry> processedHandlers = Lists.newArrayList();
                for (final Future<Optional<TimelineQueueProcessor.TimelineMessage>> future : futures) {
                    final Optional<TimelineQueueProcessor.TimelineMessage> processed = future.get();

                    if (!processed.isPresent()) {
                        noTimeline.mark();
                        continue;
                    }

                    processedHandlers.add(new DeleteMessageBatchRequestEntry(processed.get().messageId, processed.get().messageHandler));
                    if (processed.get().sleepScore > 0) {
                        validSleepScore.mark();
                    } else {
                        invalidSleepScore.mark();
                    }
                }

                // delete messages
                if (!processedHandlers.isEmpty()) {
                    LOGGER.debug("key=suripu-queue-consumer action=delete-messages num={}", processedHandlers.size());
                    final int deleted = queueProcessor.deleteMessages(processedHandlers);
                    messagesProcessed.mark(processedHandlers.size());
                    messagesDeleted.mark(deleted);
                    this.totalMessagesProcessed += processedHandlers.size();
                }

                numEmptyQueueIterations = 0;

            } else {
                numEmptyQueueIterations++;
                LOGGER.info("key=suripu-queue-consumer action=empty-iteration value={}", numEmptyQueueIterations);
                Thread.sleep(SLEEP_WHEN_NO_MESSAGES_MILLIS);
                this.totalIdleIterations++;
            }

        } while (isRunning);

        LOGGER.info("key=suripu-queue-consumer action=process-messages-done");
    }

}
