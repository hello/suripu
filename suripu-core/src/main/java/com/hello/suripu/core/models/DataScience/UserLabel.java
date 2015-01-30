package com.hello.suripu.core.models.DataScience;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by kingshy on 1/23/15.
 */
public class UserLabel {
    public enum UserLabelType {
        NONE(-1),
        MAKE_BED(0),
        WENT_TO_BED(1),
        FALL_ASLEEP(2),
        AWAKE(3),
        OUT_OF_BED(4),
        AWAKE_IN_BED(5),
        SOUND_DISTURBANCE(6),
        GOT_UP_AT_NIGHT(7),
        OTHER_DISTURBANCE(8);

        private int value;
        private UserLabelType (final int value) {this.value = value;}

        public static UserLabelType fromString(final String text) {
            if (text != null) {
                for (final UserLabelType label : UserLabelType.values()) {
                    if (text.equalsIgnoreCase(label.toString()))
                        return label;
                }
            }
            throw new IllegalArgumentException("Invalid User Label Type string");
        }

    }

    @JsonProperty("email")
    public final String email;

    @JsonProperty("night")
    public final String night;

    @JsonProperty("ts_utc")
    public final long ts;

    @JsonProperty("duration_millis")
    public final int durationMillis;

    @JsonProperty("label")
    public final String labelString;

    @JsonProperty("tz_offset")
    public final int tzOffsetMillis;

    @JsonProperty("note")
    public final String note;

    @JsonCreator
    public UserLabel(@JsonProperty("email") final String email,
                     @JsonProperty("night") final String night,
                     @JsonProperty("ts_utc") final long ts,
                     @JsonProperty("duration_millis") final int durationMillis,
                     @JsonProperty("label") final String labelString,
                     @JsonProperty("tz_offset") final int tzOffsetMillis,
                     @JsonProperty("note") final String note) {
        this.email = email;
        this.night = night;
        this.ts = ts;
        this.durationMillis = durationMillis;
        this.labelString = labelString;
        this.tzOffsetMillis = tzOffsetMillis;
        this.note = note;
    }
}
