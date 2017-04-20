package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

public enum OAuthScope {

    USER_BASIC (0),
    USER_EXTENDED (1),
    SENSORS_BASIC(2),
    SENSORS_EXTENDED(3),
    SENSORS_WRITE(4),
    SCORE_READ(5),
    SLEEP_LABEL_BASIC(6),
    SLEEP_LABEL_WRITE(7),
    ADMINISTRATION_READ(8),   // Used for dev website, or other tools which will help create/register/list applications
    ADMINISTRATION_WRITE(9),
    API_INTERNAL_DATA_READ(10),
    API_INTERNAL_DATA_WRITE(11),
    SLEEP_TIMELINE(12),
    QUESTIONS_READ(13),
    QUESTIONS_WRITE(14),
    FIRMWARE_UPDATE(15),
    ALARM_READ(16),
    ALARM_WRITE(17),
    PUSH_NOTIFICATIONS(18),
    DEVICE_INFORMATION_READ(19),
    AUTH(20),
    DEVICE_INFORMATION_WRITE(21),
    SLEEP_FEEDBACK(22),
    INSIGHTS_READ(23),
    PREFERENCES(24),
    PASSWORD_RESET(25),
    RESEARCH(26),
    TIMEZONE_READ(27),
    PCH_READ(28),
    IMPLICIT_TOKEN(29),
    ZENDESK_EXTENSION(30),
    SUPPORT(31),
    STORE_FEEDBACK(32),
    APP_STATS(33),
    STORE_READ(34),   // for linking orders
    ADMIN_QUESTIONS_WRITE(35),
    PROFILE_PHOTO_UPLOAD(36),
    SPEECH_COMMAND(37),
    EXTERNAL_APPLICATION_READ(38),
    EXTERNAL_APPLICATION_WRITE(39),
    ALERTS_READ(40),
    VIVINT_READ(41);

    private int value;
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthScope.class);

    private OAuthScope(int value) {
        this.value = value;
    }


    public static Optional<OAuthScope> fromInteger(int value){
        try{
            return Optional.of(OAuthScope.values()[value]);
        }catch (ArrayIndexOutOfBoundsException ex){
            return Optional.absent();
        }
    }

    public static Optional<OAuthScope> fromString(String value){
        try{
            return Optional.of(OAuthScope.valueOf(value));
        }catch (ArrayIndexOutOfBoundsException ex){
            LOGGER.error("error=invalid-oauth-scope-value value={}", value);
            return Optional.absent();
        }
    }

    public static OAuthScope[] fromIntegerArray(Integer[] array){
        checkNotNull(array, "Cannot convert null to OAuthScope array.");

        final ArrayList<OAuthScope> scopeArrayList = new ArrayList<OAuthScope>();
        for(int i = 0; i < array.length; i ++) {
            final Optional<OAuthScope> oAuthScopeOptional = OAuthScope.fromInteger(array[i]);
            if(oAuthScopeOptional.isPresent()){
                scopeArrayList.add(oAuthScopeOptional.get());
            }
        }

        final OAuthScope[] scopeArray = scopeArrayList.toArray(new OAuthScope[0]);
        return scopeArray;
    }

    public static OAuthScope[] fromStringArray(String[] array){
        checkNotNull(array, "Cannot convert null to OAuthScope array.");

        final ArrayList<OAuthScope> scopeArrayList = new ArrayList<OAuthScope>();
        for (final String value : array) {
            final Optional<OAuthScope> oAuthScopeOptional = OAuthScope.fromString(value);
            if(oAuthScopeOptional.isPresent()){
                scopeArrayList.add(oAuthScopeOptional.get());
            }
        }

        final OAuthScope[] scopeArray = scopeArrayList.toArray(new OAuthScope[0]);
        return scopeArray;
    }

    public int getValue() {
        return value;
    }
}
