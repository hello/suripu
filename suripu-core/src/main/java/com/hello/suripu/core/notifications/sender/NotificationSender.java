package com.hello.suripu.core.notifications.sender;

import com.hello.suripu.api.notifications.PushNotification;
import com.hello.suripu.core.notifications.PushNotificationEvent;

import java.util.List;

public interface NotificationSender {
    void send(PushNotificationEvent pushNotificationEvent);
    void sendRaw(String partitionKey, byte[] protobuf);
    List<PushNotification.UserPushNotification> sendBatch(List<PushNotification.UserPushNotification> notifications);
}
