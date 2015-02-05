package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JoinedSensorMinuteData {

    @JsonProperty("account_id")
    public final Integer accountId;

    @JsonProperty("ts")
    public final Long ts;

    @JsonProperty("ambient_light")
    public final Integer ambientLight;

    @JsonProperty("audio_peak_disturbances")
    public final Integer audioPeakDisturbancesDb;

    @JsonProperty("audio_num_disturbances")
    public final Integer audioNumDisturbances;

    @JsonProperty("svm_no_gravity")
    public final Integer svmNoGravity;

    @JsonProperty("kickoff_counts")
    public final Integer kickoffCounts;

    public JoinedSensorMinuteData(final Integer accountId, final Long ts, final Integer ambientLight, final Integer audioPeakDisturbancesDb, final Integer audioNumDisturbances, final Integer svmNoGravity, final Integer kickoffCounts) {
        this.accountId = accountId;
        this.ts = ts;
        this.ambientLight = ambientLight;
        this.audioPeakDisturbancesDb = audioPeakDisturbancesDb;
        this.audioNumDisturbances = audioNumDisturbances;
        this.svmNoGravity = svmNoGravity;
        this.kickoffCounts = kickoffCounts;
    }
}
