package com.hello.suripu.core.analytics;

import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.device.v2.Pill;
import com.hello.suripu.core.notifications.settings.NotificationSetting;

import java.util.List;

/**
 * Created by ksg on 3/23/17
 */
public class AnalyticsNullTracker implements AnalyticsTracker{
    @Override
    public void trackLowBattery(final Pill pill, final Account account) {
    }

    @Override
    public void trackPushNotificationSettings(final List<NotificationSetting> settings, final Long accountId) {

    }
}
