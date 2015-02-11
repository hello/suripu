package com.hello.suripu.workers.notifications;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.notifications.MobilePushNotificationProcessor;

public class PushNotificationsProcessorFactory implements IRecordProcessorFactory {

    private final MobilePushNotificationProcessor mobilePushNotificationProcessor;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;

    public PushNotificationsProcessorFactory(final MobilePushNotificationProcessor mobilePushNotificationProcessor, final MergedUserInfoDynamoDB mergedUserInfoDynamoDB) {
        this.mobilePushNotificationProcessor = mobilePushNotificationProcessor;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new PushNotificationsProcessor(mobilePushNotificationProcessor, mergedUserInfoDynamoDB);
    }
}
