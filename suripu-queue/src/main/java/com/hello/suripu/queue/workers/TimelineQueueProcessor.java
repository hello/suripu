package com.hello.suripu.queue.workers;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.queue.TimelineQueueProtos;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.queue.configuration.SQSConfiguration;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TimelineQueueProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineQueueProcessor.class);

    private final String sqsQueueUrl;

    private final AmazonSQSAsync sqsClient;

    private final int maxMessages;
    private final int visibilityTimeoutSeconds;
    private final int waitTimeSeconds;


    public static class TimelineMessage {
        public static Integer DEFAULT_SLEEP_SCORE = 0;

        public Long accountId;
        public DateTime targetDate;
        public String messageHandler;
        public String messageId;
        public Integer sleepScore;

        public TimelineMessage(final Long accountId,
                               final DateTime targetDate,
                               final String messageId,
                               final String messageHandler,
                               final Integer sleepScore) {
            this.accountId = accountId;
            this.targetDate = targetDate;
            this.messageId = messageId;
            this.messageHandler = messageHandler;
            this.sleepScore = sleepScore;
        }

        public void setScore(final Integer score) { this.sleepScore = score; }
    }

    public TimelineQueueProcessor(final String sqsQueueUrl, final AmazonSQSAsync sqsClient, final SQSConfiguration config) {
        this.sqsQueueUrl = sqsQueueUrl;
        this.sqsClient = sqsClient;
        this.maxMessages = config.getSqsMaxMessage();
        this.visibilityTimeoutSeconds = config.getSqsVisibilityTimeoutSeconds();
        this.waitTimeSeconds = config.getSqsWaitTimeSeconds();
    }


    /**
     * Add last numdays from now to the queue -- used for debugging
     * @param accountId user
     * @param numDays no. of days to generate
     */
    public void sendMessages(final long accountId, final int numDays) {
        final TimelineQueueProtos.Message.Builder messageBuilder = TimelineQueueProtos.Message.newBuilder();
        final DateTime now = DateTime.now().withTimeAtStartOfDay();

        for (int i = 2; i <= numDays+1; i++) {
            final DateTime targetDate = now.minusDays(i);
            final String date = DateTimeUtil.dateToYmdString(targetDate);
            LOGGER.debug("action=add-message num={} target_date={}", i, date);
            sendMessage(accountId, date);
        }
    }

    public void sendMessage(final Long accountId, final String targetDate) {
        final TimelineQueueProtos.Message queueMessage = TimelineQueueProtos.Message.newBuilder()
                .setAccountId(accountId)
                .setTargetDate(targetDate)
                .setTimestamp(DateTime.now().getMillis()).build();

        final String message = encodeMessage(queueMessage);
        final SendMessageResult result = sqsClient.sendMessage(new SendMessageRequest(sqsQueueUrl, message));
        LOGGER.debug("Sent Message Id: {}", result.getMessageId());
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
            final Optional<TimelineQueueProtos.Message> optionalMsg = decodeMessage(message.getBody());

            if (optionalMsg.isPresent()) {
                final TimelineQueueProtos.Message msg = optionalMsg.get();
                final DateTime targetDate = DateTimeUtil.ymdStringToDateTime(msg.getTargetDate());
                final Long accountId = msg.getAccountId();
                LOGGER.debug("action=decode-protobuf-message account_id={}, date={}", accountId, targetDate);

                decodedMessages.add(new TimelineMessage(accountId,
                        targetDate,
                        messageId,
                        messageReceiptHandle,
                        TimelineMessage.DEFAULT_SLEEP_SCORE));
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


    public int deleteMessages(final List<DeleteMessageBatchRequestEntry> processedMessages) {
        final DeleteMessageBatchResult result = this.sqsClient.deleteMessageBatch(
                new DeleteMessageBatchRequest(sqsQueueUrl, processedMessages));
        return result.getSuccessful().size();
    }

    public static String encodeMessage(final TimelineQueueProtos.Message message) {
        final byte [] bytes = message.toByteArray();
        return Base64.encodeBase64URLSafeString(bytes);
    }

    private static Optional<TimelineQueueProtos.Message> decodeMessage(final String message) {
        try {
            final TimelineQueueProtos.Message decodedMessage = TimelineQueueProtos.Message.parseFrom(Base64.decodeBase64(message));
            // LOGGER.debug("action=print-decoded-message value={}", sqsMessage.toString());
            return Optional.of(decodedMessage);
        } catch (InvalidProtocolBufferException pbe) {
            LOGGER.error(pbe.getMessage());
        }
        return Optional.absent();
    }

}
