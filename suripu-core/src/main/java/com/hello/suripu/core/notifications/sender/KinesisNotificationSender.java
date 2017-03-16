package com.hello.suripu.core.notifications.sender;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.amazonaws.services.kinesis.model.PutRecordsResultEntry;
import com.google.common.collect.Lists;
import com.hello.suripu.api.notifications.PushNotification;
import com.hello.suripu.core.notifications.PushNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class KinesisNotificationSender implements NotificationSender {

    private static final int MAX_PUT_RECORDS_SIZE = 500;

    private final static Logger LOGGER = LoggerFactory.getLogger(KinesisNotificationSender.class);
    private final AmazonKinesis amazonKinesis;
    private final String streamName;

    public KinesisNotificationSender(final AmazonKinesis amazonKinesis, final String streamName) {
        this.amazonKinesis = amazonKinesis;
        this.streamName = streamName;
    }

    @Override
    public void send(final PushNotificationEvent pushNotificationEvent) {
        final PushNotification.UserPushNotification.Builder builder = PushNotification.UserPushNotification.newBuilder();
        builder.setAccountId(pushNotificationEvent.accountId);
        // add all the fields

        sendRaw(String.valueOf(pushNotificationEvent.accountId), builder.build().toByteArray());
    }

    @Override
    public void sendRaw(String partitionKey, byte[] protobuf) {
        final PutRecordRequest putRecordRequest = new PutRecordRequest()
                .withData(ByteBuffer.wrap(protobuf))
                .withStreamName(streamName)
                .withPartitionKey(partitionKey);

        try {
            amazonKinesis.putRecord(putRecordRequest);
        } catch (Exception e) {
            LOGGER.error("error={}", e.getMessage());
        }
    }

    @Override
    public List<PushNotification.UserPushNotification> sendBatch(final List<PushNotification.UserPushNotification> notifications) {
        // Can only insert a limited number of kinesis records at a time.
        final List<List<PushNotification.UserPushNotification>> partitions = Lists.partition(notifications, MAX_PUT_RECORDS_SIZE);
        final List<PushNotification.UserPushNotification> failedPuts = new ArrayList<>();
        for (final List<PushNotification.UserPushNotification> partition : partitions) {
            failedPuts.addAll(putNotificationsImpl(partition));
        }
        if(!failedPuts.isEmpty()) {
            LOGGER.warn("message=failed-put size={}", failedPuts.size());
        }

        return failedPuts;
    }


    private void validateNotification(final PushNotification.UserPushNotification userPushNotification) {
        if (!userPushNotification.hasAccountId()) {
            LOGGER.error("error=no_sense_id account_id={}", userPushNotification.getAccountId());
            throw new IllegalArgumentException();
        }
    }

    private List<PushNotification.UserPushNotification> putNotificationsImpl(final List<PushNotification.UserPushNotification> userPushNotifications) {
        final List<PutRecordsRequestEntry> entries = new ArrayList<>(userPushNotifications.size());
        for (final PushNotification.UserPushNotification notification : userPushNotifications) {
            validateNotification(notification);
            final PutRecordsRequestEntry requestEntry = new PutRecordsRequestEntry()
                    .withData(ByteBuffer.wrap(notification.toByteArray()))
                    .withPartitionKey(String.valueOf(notification.getAccountId()));
            entries.add(requestEntry);
        }

        final PutRecordsRequest request = new PutRecordsRequest().withRecords(entries).withStreamName(streamName);
        final PutRecordsResult putRecordsResult;
        try {
            putRecordsResult = amazonKinesis.putRecords(request);
        } catch (Exception e) {
            LOGGER.error("error=uncaught_kinesis_exception exception={}", e);
            // They all failed, so return them all.
            return userPushNotifications;
        }


        final List<PushNotification.UserPushNotification> failedNotifications = new ArrayList<>();
        if (putRecordsResult.getFailedRecordCount() == 0) {
            return failedNotifications;
        }

        for (int i = 0; i < putRecordsResult.getRecords().size(); i++) {
            final PutRecordsResultEntry resultEntry = putRecordsResult.getRecords().get(i);
            if (resultEntry.getErrorCode() != null) {
                final PushNotification.UserPushNotification failedNotification = userPushNotifications.get(i);
                failedNotifications.add(failedNotification);
                LOGGER.error("error=failed_push_notification_kinesis_put error_code={} error_message={} sense_id={} account_id={}",
                        resultEntry.getErrorCode(), resultEntry.getErrorMessage(), failedNotification.getSenseId(), failedNotification.getAccountId());
            }
        }

        return failedNotifications;
    }
}
