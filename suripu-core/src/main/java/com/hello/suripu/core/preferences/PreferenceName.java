package com.hello.suripu.core.preferences;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PreferenceName {
    ENHANCED_AUDIO("enhanced_audio"),
    TEMP_CELSIUS("temp_celsius"),
    TIME_TWENTY_FOUR_HOUR("time_twenty_four_hour"),
    PUSH_SCORE("push_score"),
    PUSH_ALERT_CONDITIONS("push_alert_conditions"),
    WEIGHT_METRIC("weight_metric"),
    HEIGHT_METRIC("height_metric");

    private String value;

    PreferenceName(final String value) {
        this.value = value;
    }

    @JsonCreator
    public static PreferenceName fromString(final String value) {
        for (final PreferenceName pref : PreferenceName.values()) {
            if (pref.value.equalsIgnoreCase(value)) {
                return pref;
            }
        }

        throw new IllegalArgumentException("Invalid preference name");
    }
}
