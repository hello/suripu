package com.hello.suripu.core.notifications;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.amazonaws.services.kinesis.model.PutRecordsResultEntry;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.api.notifications.PushNotification;
import com.hello.suripu.core.notifications.sender.KinesisNotificationSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jakepiccolo on 5/18/16.
 */
public class KinesisNotificationSenderTest {

    private KinesisNotificationSender sender;
    private AmazonKinesis amazonKinesis;
    private static String STREAM_NAME = "push_notifications_test";

    @Before
    public void setUp() throws Exception {
        amazonKinesis = Mockito.mock(AmazonKinesis.class);
        sender = new KinesisNotificationSender(amazonKinesis, STREAM_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutNotificationsEnsuresSenseIdPresent() throws Exception {
        final List<PushNotification.UserPushNotification> pushNotifications = ImmutableList.of(
                makeNotification("sense1", 1L),
                PushNotification.UserPushNotification.newBuilder().build() // Missing account id
        );
        sender.sendBatch(pushNotifications);
    }

    @Test
    public void testPutNotificationsHappyPath() throws Exception {
        final List<PushNotification.UserPushNotification> pushNotifications = makeNotificationList("senseId", 1L, 700L);
        Mockito.when(amazonKinesis.putRecords(Mockito.any(PutRecordsRequest.class))).thenReturn(new PutRecordsResult().withFailedRecordCount(0));
        final List<PushNotification.UserPushNotification> failedNotifications = sender.sendBatch(pushNotifications);
        assertThat(failedNotifications.isEmpty(), is(true));
    }

    @Test
    public void testPutNotificationsSomeRecordsFailed() throws Exception {
        final List<PushNotification.UserPushNotification> pushNotifications = makeNotificationList("senseId", 1L, 400L);

        // Make the first 100 fail
        final List<PutRecordsResultEntry> resultEntries = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            final PutRecordsResultEntry entry = new PutRecordsResultEntry();
            if (i < 100) {
                entry.setErrorCode("Error code");
                entry.setErrorMessage("Error message");
            }
            resultEntries.add(entry);
        }
        final PutRecordsResult putRecordsResult = new PutRecordsResult().withFailedRecordCount(200).withRecords(resultEntries);
        Mockito.when(amazonKinesis.putRecords(Mockito.any(PutRecordsRequest.class))).thenReturn(putRecordsResult);

        final List<PushNotification.UserPushNotification> failedNotifications = sender.sendBatch(pushNotifications);
        assertThat(failedNotifications.isEmpty(), is(false));
        assertThat(failedNotifications.size(), is(100));
        assertThat(failedNotifications, is(pushNotifications.subList(0, 100)));
    }


    private List<PushNotification.UserPushNotification> makeNotificationList(final String senseId, final Long accountStart, final Long accountEnd) {
        final List<PushNotification.UserPushNotification> notificationList = new ArrayList<>();
        for (long i = accountStart; i < accountEnd; i++) {
            notificationList.add(makeNotification(senseId, i));
        }
        return notificationList;
    }

    private PushNotification.UserPushNotification makeNotification(final String senseId, final Long accountId) {
        return PushNotification.UserPushNotification.newBuilder().setSenseId(senseId).setAccountId(accountId).build();
    }
}
