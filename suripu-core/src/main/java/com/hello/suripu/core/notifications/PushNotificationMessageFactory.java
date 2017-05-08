package com.hello.suripu.core.notifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.MobilePushRegistration;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class PushNotificationMessageFactory {

    private final static Logger LOGGER = LoggerFactory.getLogger(PushNotificationMessageFactory.class);

    final private ObjectMapper mapper;

    public PushNotificationMessageFactory(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<String> make(MobilePushRegistration.OS os, HelloPushMessage message) {
        switch(os) {
            case ANDROID:
                return makeAndroidMessage(message);
            case IOS:
                return makeAPNSMessage(message);
        }
        return Optional.absent();
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

        content.put("hlo_title", "Sense");
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
}
