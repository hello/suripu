package com.hello.suripu.core.models.DataScience;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JoinedSensorsMinuteData {

    @JsonProperty("ts")
    public final Long ts;

    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("light")
    public final Float light;

    @JsonProperty("sound_num_disturbances")
    public final Float soundNumDisturbances;

    @JsonProperty("sound_peak_disturbances")
    public final Float soundPeakDisturbances;

    @JsonProperty("svm_no_gravity")
    public final Integer svmNoGravity;

    @JsonProperty("kickoff_counts")
    public final Long kickoffCounts;

    @JsonProperty("offset_millis")
    public final Integer offsetMillis;

    public JoinedSensorsMinuteData(final Long ts, final Long accountId, final Float light, final Float soundNumDisturbances, final Float soundPeakDisturbances, final Integer svmNoGravity, final Long kickoffCounts, final Integer offsetMillis) {
        this.ts = ts;
        this.accountId = accountId;
        this.light = light;
        this.soundNumDisturbances = soundNumDisturbances;
        this.soundPeakDisturbances = soundPeakDisturbances;
        this.svmNoGravity = svmNoGravity;
        this.kickoffCounts = kickoffCounts;
        this.offsetMillis = offsetMillis;
    }

}
