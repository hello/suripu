package com.hello.suripu.core.analytics;

import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.device.v2.Pill;
import com.hello.suripu.core.notifications.settings.NotificationSetting;

import java.util.List;

public interface AnalyticsTracker {
    void trackLowBattery(final Pill pill, final Account account);
    void trackPushNotificationSettings(final List<NotificationSetting> settings, Long accountId);
}
