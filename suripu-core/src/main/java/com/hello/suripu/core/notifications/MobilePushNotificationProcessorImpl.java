package com.hello.suripu.core.notifications;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.EndpointDisabledException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AppStatsDAO;
import com.hello.suripu.core.db.TimeZoneHistoryDAO;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.MobilePushRegistration;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.hello.suripu.core.preferences.AccountPreferencesDAO;
import com.hello.suripu.core.preferences.PreferenceName;
import com.librato.rollout.RolloutClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by jakepiccolo on 5/19/16.
 */
public class MobilePushNotificationProcessorImpl implements MobilePushNotificationProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(MobilePushNotificationProcessorImpl.class);

    private final ObjectMapper mapper;

    private final AmazonSNS sns;
    private final NotificationSubscriptionsDAO subscriptionDAO;
    private final PushNotificationEventDynamoDB pushNotificationEventDynamoDB;
    private final RolloutClient featureFlipper;
    private final AppStatsDAO appStatsDAO;
    private final AccountPreferencesDAO accountPreferencesDAO;
    private final TimeZoneHistoryDAO timeZoneHistoryDAO;

    private MobilePushNotificationProcessorImpl(final AmazonSNS sns, final NotificationSubscriptionsDAO subscriptionDAO,
                                               final PushNotificationEventDynamoDB pushNotificationEventDynamoDB,
                                               final ObjectMapper mapper,
                                               final RolloutClient featureFlipper,
                                               final AppStatsDAO appStatsDAO,
                                               final AccountPreferencesDAO accountPreferencesDAO,
                                                final TimeZoneHistoryDAO timeZoneHistoryDAO) {
        this.sns = sns;
        this.subscriptionDAO = subscriptionDAO;
        this.pushNotificationEventDynamoDB = pushNotificationEventDynamoDB;
        this.mapper = mapper;
        this.featureFlipper = featureFlipper;
        this.appStatsDAO = appStatsDAO;
        this.accountPreferencesDAO = accountPreferencesDAO;
        this.timeZoneHistoryDAO = timeZoneHistoryDAO;
    }

    @Override
    public void push(final PushNotificationEvent event) {

        if(!featureFlipper.userFeatureActive(FeatureFlipper.PUSH_NOTIFICATIONS_ENABLED, event.accountId, Collections.EMPTY_LIST)) {
            return;
        }

        final Optional<TimeZoneHistory> timeZoneHistoryOptional = timeZoneHistoryDAO.getCurrentTimeZone(event.accountId);
        if(!timeZoneHistoryOptional.isPresent()) {
            LOGGER.error("error=missing-timezone-history account_id={}", event.accountId);
            return;
        }

        final Map<PreferenceName, Boolean> preferences = accountPreferencesDAO.get(event.accountId);


        if(PushNotificationEventType.SLEEP_SCORE.equals(event.type)) {
            final boolean enabled = preferences.getOrDefault(PreferenceName.PUSH_SCORE, false);
            if(!enabled) {
                LOGGER.info("account_id={} preference={} enabled={}", event.accountId, PreferenceName.PUSH_SCORE, enabled);
                return;
            }

            final Optional<DateTime> lastViewed = appStatsDAO.getQuestionsLastViewed(event.accountId);
            if(lastViewed.isPresent()) {

                final DateTimeZone dateTimeZone = DateTimeZone.forID(timeZoneHistoryOptional.get().timeZoneId);
                final DateTime lastViewedLocalTime = new DateTime(lastViewed.get(), dateTimeZone).withTimeAtStartOfDay();
                final DateTime nowLocalTime = DateTime.now().withTimeAtStartOfDay();
                final int minutes = Minutes.minutesBetween(nowLocalTime, lastViewedLocalTime).getMinutes();
                if(minutes > 0) {
                    LOGGER.warn("action=skip-push-notification status=app-opened account_id={} last_seen={}", event.accountId, lastViewedLocalTime);
                    return;
                }
            }
        }

        // We often want at-most-once delivery of push notifications, so we insert the record to DDB first.
        // That way if something later in this method fails, we won't accidentally send the same notification twice.
        final PushNotificationEvent eventAdjusted = new PushNotificationEvent(
                event.accountId,
                event.type,
                new DateTime(event.timestamp, DateTimeZone.forID(timeZoneHistoryOptional.get().timeZoneId)),
                event.helloPushMessage,
                event.senseId
        );

        final boolean successfullyInserted = pushNotificationEventDynamoDB.insert(eventAdjusted);
        if (!successfullyInserted) {
            LOGGER.warn("action=duplicate-push-notification account_id={} type={}", event.accountId, event.type);
            return;
        }

        final Long accountId = event.accountId;
        final HelloPushMessage pushMessage = event.helloPushMessage;

        final List<MobilePushRegistration> registrations = subscriptionDAO.getMostRecentSubscriptions(accountId, 5);
        LOGGER.info("action=list-registrations account_id={} num_subscriptions={}", event.accountId, registrations.size());

        final List<MobilePushRegistration> toDelete = Lists.newArrayList();

        for (final MobilePushRegistration reg : registrations) {
            if(reg.endpoint.isPresent()) {
                final MobilePushRegistration.OS os = MobilePushRegistration.OS.fromString(reg.os);
                final Optional<String> message = makeMessage(os, pushMessage);
                if(!message.isPresent()) {
                    LOGGER.warn("warn=failed-to-generate-message os={} push_message={}", os, pushMessage);
                    continue;
                }

                final PublishRequest pr = new PublishRequest();
                pr.setMessageStructure("json");
                pr.setMessage(message.get());
                pr.setTargetArn(reg.endpoint.get());
                try {
                    final PublishResult result = sns.publish(pr);
                    LOGGER.info("account_id={} message_id={} os={}", event.accountId, result.getMessageId(), reg.os);
                } catch (EndpointDisabledException endpointDisabled) {
                    toDelete.add(reg);
                }
                catch (Exception e) {
                    LOGGER.error("error=failed-sending-sns-message message={}", e.getMessage());
                }
            }
        }

        for(final MobilePushRegistration registration : toDelete) {
            LOGGER.info("action=delete-by-device-token account_id={} device_token={} os={}", registration.accountId.or(0L), registration.deviceToken, registration.os);
            subscriptionDAO.deleteByDeviceToken(registration.deviceToken);
        }
    }

    private Optional<String> makeAPNSMessage(final HelloPushMessage message) {
        final Map<String, String> messageMap = new HashMap<>();
        final Map<String, String> content = new HashMap<>();
        final Map<String, Object> appleMessageMap = new HashMap<>();
        final Map<String, Object> appMessageMap = new HashMap<>();

        content.put("body", message.body);

        appMessageMap.put("alert", content);
        appMessageMap.put("sound", "default");

        appleMessageMap.put("aps", appMessageMap);

        // Hello custom keys
        // https://developer.apple.com/library/content/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/CreatingtheNotificationPayload.html#//apple_ref/doc/uid/TP40008194-CH10-SW1
        appleMessageMap.put("hlo-type", message.target);
        appleMessageMap.put("hlo-detail", message.details);

        try {
            final String jsonString = mapper.writeValueAsString(appleMessageMap);

            messageMap.put("APNS", jsonString);
            return Optional.of(mapper.writeValueAsString(messageMap));

        } catch (JsonProcessingException e) {
            LOGGER.error("Failed serializing to JSON: {}", e.getMessage());
        }

        return Optional.absent();
    }

    private Optional<String> makeAndroidMessage(final HelloPushMessage message) {
        final Map<String, String> messageMap = new HashMap<>();
        final Map<String, String> content = new HashMap<>();
        final Map<String, Object> appMessageMap = new HashMap<>();

        content.put("hlo_title", "Sense – XXXX");
        content.put("hlo_body", message.body);
        content.put("hlo_type", message.target);
        content.put("hlo_detail", message.details);
        appMessageMap.put("time_to_live", DateTimeConstants.SECONDS_PER_HOUR * 23);
        appMessageMap.put("data", content);

//        appMessageMap.put("collapse_key", "Welcome");
//        appMessageMap.put("delay_while_idle", true);
//        appMessageMap.put("dry_run", false);

        try {
            final String jsonString = mapper.writeValueAsString(appMessageMap);

            messageMap.put("GCM", jsonString);
            return Optional.of(mapper.writeValueAsString(messageMap));

        } catch (JsonProcessingException e) {
            LOGGER.error("Failed serializing to JSON: {}", e.getMessage());
        }

        return Optional.absent();
    }

    private Optional<String> makeMessage(final MobilePushRegistration.OS os, final HelloPushMessage message) {
        switch(os) {
            case ANDROID:
                return makeAndroidMessage(message);
            case IOS:
                return makeAPNSMessage(message);
        }
        return Optional.absent();
    }

    public static class Builder {

        private AmazonSNS sns;
        private NotificationSubscriptionsDAO subscriptionDAO;
        private PushNotificationEventDynamoDB pushNotificationEventDynamoDB;
        private ObjectMapper mapper = new ObjectMapper();
        private RolloutClient featureFlipper;
        private AppStatsDAO appStatsDAO;
        private AccountPreferencesDAO accountPreferencesDAO;
        private TimeZoneHistoryDAO timeZoneHistoryDAO;

        public Builder withSns(final AmazonSNS sns) {
            this.sns = sns;
            return this;
        }

        public Builder withSubscriptionDAO(final NotificationSubscriptionsDAO subscriptionDAO) {
            this.subscriptionDAO = subscriptionDAO;
            return this;
        }

        public Builder withPushNotificationEventDynamoDB(final PushNotificationEventDynamoDB pushNotificationEventDynamoDB) {
            this.pushNotificationEventDynamoDB = pushNotificationEventDynamoDB;
            return this;
        }

        public Builder withMapper(final ObjectMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder withFeatureFlipper(final RolloutClient featureFlipper) {
            this.featureFlipper = featureFlipper;
            return this;
        }

        public Builder withAppStatsDAO(final AppStatsDAO appStatsDAO) {
            this.appStatsDAO = appStatsDAO;
            return this;
        }

        public Builder withAccountPreferencesDAO(final AccountPreferencesDAO accountPreferencesDAO) {
            this.accountPreferencesDAO = accountPreferencesDAO;
            return this;
        }

        public Builder withTimeZoneHistory(final TimeZoneHistoryDAO timeZoneHistoryDAO) {
            this.timeZoneHistoryDAO = timeZoneHistoryDAO;
            return this;
        }

        public MobilePushNotificationProcessor build() {
            checkNotNull(sns, "sns can not be null");
            checkNotNull(subscriptionDAO, "subscription can not be null");
            checkNotNull(pushNotificationEventDynamoDB, "pushNotificationEventDynamoDB can not be null");
            checkNotNull(featureFlipper, "featureFlipper can not be null");
            checkNotNull(appStatsDAO, "appStatsDAO can not be null");
            checkNotNull(accountPreferencesDAO, "accountPreferencesDAO can not be null");

            return new MobilePushNotificationProcessorImpl(sns, subscriptionDAO, pushNotificationEventDynamoDB, mapper,
                    featureFlipper, appStatsDAO, accountPreferencesDAO, timeZoneHistoryDAO);
        }

    }
}
