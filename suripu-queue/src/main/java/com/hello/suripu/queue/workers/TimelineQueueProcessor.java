package com.hello.suripu.queue.workers;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.sqs.TimelineQueue;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.queue.configuration.SQSConfiguration;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TimelineQueueProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineQueueProcessor.class);

    final private String sqsQueueUrl;

    final private AmazonSQSAsync sqsClient;

    final private int maxMessages;
    final private int visibilityTimeoutSeconds;
    final private int waitTimeSeconds;


    public static class TimelineMessage {
        public Long accountId;
        public DateTime targetDate;
        public String messageHandler;
        public String messageId;

        public TimelineMessage(final Long accountId,
                               final DateTime targetDate,
                               final String messageId,
                               final String messageHandler) {
            this.accountId = accountId;
            this.targetDate = targetDate;
            this.messageId = messageId;
            this.messageHandler = messageHandler;
        }
    }

    public TimelineQueueProcessor(final String sqsQueueUrl, final AmazonSQSAsync sqsClient, final SQSConfiguration config) {
        this.sqsQueueUrl = sqsQueueUrl;
        this.sqsClient = sqsClient;
        this.maxMessages = config.getSqsMaxMessage();
        this.visibilityTimeoutSeconds = config.getSqsVisibilityTimeoutSeconds();
        this.waitTimeSeconds = config.getSqsWaitTimeSeconds();
    }


    /**
     * Add last numdays from now to the queue
     * @param accountId user
     * @param numDays no. of days to generate
     */
    public void sendMessages(final long accountId, final int numDays) {
        final TimelineQueue.SQSMessage.Builder messageBuilder = TimelineQueue.SQSMessage.newBuilder();
        final DateTime now = DateTime.now().withTimeAtStartOfDay();

        for (int i = 2; i <= numDays; i++) {
            final DateTime targetDate = now.minusDays(i);
            final String date = DateTimeUtil.dateToYmdString(targetDate);
            LOGGER.debug("action=add-message num={} target_date={}", i, date);

            final TimelineQueue.SQSMessage SQSMessage = messageBuilder
                    .setAccountId(accountId)
                    .setTargetDate(date)
                    .setTimestamp(DateTime.now().getMillis()).build();

            final String message = encodeMessage(SQSMessage);
            sqsClient.sendMessage(new SendMessageRequest(sqsQueueUrl, message));
        }
    }

    public List<TimelineMessage> receiveMessages() {
        final ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest()
                .withQueueUrl(sqsQueueUrl)
                .withMaxNumberOfMessages(maxMessages)
                .withVisibilityTimeout(visibilityTimeoutSeconds)
                .withWaitTimeSeconds(waitTimeSeconds);

        final ReceiveMessageResult rx = sqsClient.receiveMessage(receiveMessageRequest);
        final List<Message> messages = rx.getMessages();

        final List<TimelineMessage> decodedMessages = Lists.newArrayListWithCapacity(messages.size());

        for (final Message message : messages) {
            // debugging
            final String messageId = message.getMessageId();
            final String messageReceiptHandle = message.getReceiptHandle();

            LOGGER.debug("action=get-sqs-message-info id={}", messageId);

            // decode message
            final Optional<TimelineQueue.SQSMessage> optionalMsg = decodeMessage(message.getBody());

            if (optionalMsg.isPresent()) {
                final TimelineQueue.SQSMessage sqsMsg = optionalMsg.get();
                final DateTime targetDate = DateTimeUtil.ymdStringToDateTime(sqsMsg.getTargetDate());
                final Long accountId = sqsMsg.getAccountId();
                LOGGER.debug("action=decode-protobuf-message account_id={}, date={}", accountId, targetDate);

                final TimelineMessage timelineMessage = new TimelineMessage(accountId, targetDate, messageId, messageReceiptHandle);
                decodedMessages.add(timelineMessage);
            }
        }
        return decodedMessages;
    }

    public static Optional<String> getSQSQueueURL(final AmazonSQS sqs, final String queueName) {
        try {
            final List<String> queueUrls = sqs.listQueues().getQueueUrls();

            for (final String url : queueUrls) {
                if (url.contains(queueName)) {
                    LOGGER.debug("action=found-sqs-url value={}", url);
                    return Optional.of(url);
                }
            }
        }  catch (AmazonServiceException ase) {
            LOGGER.error("error=sqs-request-rejected reason={}", ase.getMessage()) ;
        } catch (AmazonClientException ace) {
            LOGGER.error("error=amazon-client-exception-internal-problem reason={}", ace.getMessage());
        }
        return Optional.absent();
    }


    public void deleteMessages(final List<DeleteMessageBatchRequestEntry> processedMessages) {
        this.sqsClient.deleteMessageBatch(new DeleteMessageBatchRequest(sqsQueueUrl, processedMessages));
    }

    private static String encodeMessage(final TimelineQueue.SQSMessage message) {
        final byte [] bytes = message.toByteArray();
        return Base64.encodeBase64URLSafeString(bytes);
    }

    private static Optional<TimelineQueue.SQSMessage> decodeMessage(final String message) {
        final TimelineQueue.SQSMessage sqsMessage;
        try {
            sqsMessage = TimelineQueue.SQSMessage.parseFrom(Base64.decodeBase64(message));
            // LOGGER.debug("action=print-decoded-message value={}", sqsMessage.toString());
            return Optional.of(sqsMessage);
        } catch (InvalidProtocolBufferException pbe) {
            LOGGER.error(pbe.getMessage());
        }
        return Optional.absent();
    }

}
