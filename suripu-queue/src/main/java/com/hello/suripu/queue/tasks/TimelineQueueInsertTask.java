package com.hello.suripu.queue.tasks;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.hello.suripu.api.queue.TimelineQueueProtos;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.queue.models.SenseDataDAO;
import com.hello.suripu.queue.workers.TimelineQueueProcessor;
import com.yammer.dropwizard.tasks.Task;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.List;

/**
 * Created by kingshy on 3/15/16.
 */
public class TimelineQueueInsertTask extends Task {

    private final Logger LOGGER = LoggerFactory.getLogger(TimelineQueueInsertTask.class);

    private final AmazonSQSAsync sqsClient;
    private final String sqsQueueUrl;
    private final SenseDataDAO senseDataDAO;

    public TimelineQueueInsertTask(final AmazonSQSAsync sqsClient, final SenseDataDAO senseDataDAO, final String sqsQueueUrl) {
        super("insert");
        this.sqsClient = sqsClient;
        this.senseDataDAO = senseDataDAO;
        this.sqsQueueUrl = sqsQueueUrl;
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {

        final Long accountId = 1310L;
        final DateTime now = DateTime.now(DateTimeZone.UTC).withMinuteOfHour(0);
        final String targetDate = DateTimeUtil.dateToYmdString(now);

        final TimelineQueueProtos.Message message = TimelineQueueProtos.Message.newBuilder()
                .setAccountId(accountId)
                .setTargetDate(targetDate)
                .setTimestamp(now.getMillis()).build();

        final List<SendMessageBatchRequestEntry> messages = Lists.newArrayList();
        final String messageId = String.format("%s_%s", String.valueOf(accountId), targetDate);
        messages.add(new SendMessageBatchRequestEntry(messageId, TimelineQueueProcessor.encodeMessage(message)));

        final SendMessageBatchResult result = sqsClient.sendMessageBatch(sqsQueueUrl, messages);

        LOGGER.debug("key=queue-producer send-result=done failures={}", result.getFailed().size());

        Thread.sleep(10000L);
        System.out.println(System.currentTimeMillis());

    }
}
