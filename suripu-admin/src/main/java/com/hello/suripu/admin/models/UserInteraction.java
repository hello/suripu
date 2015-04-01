package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;


public class UserInteraction {
    @JsonProperty("wave_count")
    public final Float waveCount;

    @JsonProperty("hold_count")
    public final Float holdCount;

    @JsonProperty("timestamp")
    public final Long timestamp;

    @JsonProperty("offset_millis")
    public final Integer offsetMillis;


    public UserInteraction(final Float waveCount, final Float holdCount, final Long timestamp, final Integer offsetMillis ) {
        this.waveCount = waveCount;
        this.holdCount = holdCount;
        this.timestamp = timestamp;
        this.offsetMillis = offsetMillis;
    }
}
