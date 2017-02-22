package com.hello.suripu.core.analytics;

import com.google.common.collect.Maps;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.device.v2.Pill;
import com.hello.suripu.core.notifications.settings.NotificationSetting;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.MessageBuilder;
import com.segment.analytics.messages.TrackMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SegmentAnalyticsTracker implements AnalyticsTracker {


    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentAnalyticsTracker.class);

    private final AnalyticsTrackingDAO analyticsTrackingDAO;
    private final Analytics analytics;

    public SegmentAnalyticsTracker(final AnalyticsTrackingDAO analyticsTrackingDAO, final Analytics analytics) {
        this.analyticsTrackingDAO = analyticsTrackingDAO;
        this.analytics = analytics;
    }

    public void trackLowBattery(final Pill pill, final Account account) {
        final boolean saved = analyticsTrackingDAO.putIfAbsent(TrackingEvent.PILL_LOW_BATTERY, account.id.get());
        LOGGER.warn("message=low-battery-new-account tracking={} account_id={}", saved, account.id.get());
        if(saved) {
            final Map<String, String> tags = new HashMap<>();

            tags.put("PillExternalId", pill.externalId);
            tags.put("PillInternalId", String.valueOf(pill.internalId));
            tags.put("Email", account.email);

            if(pill.batteryLevelOptional.isPresent()) {
                tags.put("BatteryLevel", String.valueOf(pill.batteryLevelOptional.get()));
            }

            final MessageBuilder mb = TrackMessage.builder("PillLowBattery")
                    .userId(String.valueOf(account.id.get()))
                    .properties(tags)
                    .timestamp(DateTime.now(DateTimeZone.UTC).toDate());

            analytics.enqueue(mb);
        }
    }

    @Override
    public void trackPushNotificationSettings(final List<NotificationSetting> settings, Long accountId) {
        final Map<String, String> tags = Maps.newHashMap();

        for(final NotificationSetting setting : settings) {
            tags.put(setting.type().toLowerCase(), String.valueOf(setting.enabled()));
        }

        final MessageBuilder mb = TrackMessage.builder("Update Notification Settings")
                .userId(String.valueOf(accountId))
                .properties(tags)
                .timestamp(DateTime.now(DateTimeZone.UTC).toDate());

        analytics.enqueue(mb);
    }
}
