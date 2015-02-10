package com.hello.suripu.core.models.DataScience;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JoinedSensorsMinuteData {

    @JsonProperty("ts")
    public final Long ts;

    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("light")
    public final Float light;

    @JsonProperty("sound")
    public final Float sound;

    @JsonProperty("svm_no_gravity")
    public final Long svmNoGravity;

    @JsonProperty("kickoff_counts")
    public final Long kickoffCounts;

    public JoinedSensorsMinuteData(final Long ts, final Long accountId, final Float light, final Float sound, final Long svmNoGravity, final Long kickoffCounts) {
        this.ts = ts;
        this.accountId = accountId;
        this.light = light;
        this.sound = sound;
        this.svmNoGravity = svmNoGravity;
        this.kickoffCounts = kickoffCounts;
    }

}
