package com.hello.suripu.core.preferences;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.hello.suripu.core.models.ApiVersion;

public enum PreferenceName {
    ENHANCED_AUDIO(ApiVersion.V1, "enhanced_audio"),
    TEMP_CELSIUS(ApiVersion.V1, "temp_celsius"),
    TIME_TWENTY_FOUR_HOUR(ApiVersion.V1, "time_twenty_four_hour"),
    PUSH_SCORE(ApiVersion.V1, "push_score"),
    PUSH_ALERT_CONDITIONS(ApiVersion.V1, "push_alert_conditions"),
    WEIGHT_METRIC(ApiVersion.V2, "weight_metric"),
    HEIGHT_METRIC(ApiVersion.V2, "height_metric");

    public final ApiVersion availableStarting;
    private String value;

    PreferenceName(final ApiVersion availableStarting,
                   final String value) {
        this.availableStarting = availableStarting;
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
