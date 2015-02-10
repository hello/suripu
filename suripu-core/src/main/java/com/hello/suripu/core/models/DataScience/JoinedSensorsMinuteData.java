package com.hello.suripu.core.models.DataScience;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JoinedSensorsMinuteData {

    @JsonProperty("ts")
    public final Long ts;

    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("sound_num_disturbances")
    public final Float soundNumDisturbances;

    @JsonProperty("sound_peak_disturbances")
    public final Float soundPeakDisturbances;

    @JsonProperty("svm_no_gravity")
    public final Integer svmNoGravity;

    @JsonProperty("kickoff_counts")
    public final Long kickoffCounts;

    public JoinedSensorsMinuteData(final Long ts, final Long accountId, final Float soundNumDisturbances, final Float soundPeakDisturbances, final Integer svmNoGravity, final Long kickoffCounts) {
        this.ts = ts;
        this.accountId = accountId;
        this.soundNumDisturbances = soundNumDisturbances;
        this.soundPeakDisturbances = soundPeakDisturbances;
        this.svmNoGravity = svmNoGravity;
        this.kickoffCounts = kickoffCounts;
    }

}
