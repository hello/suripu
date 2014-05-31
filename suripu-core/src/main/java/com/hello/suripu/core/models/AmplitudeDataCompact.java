package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by pangwu on 5/30/14.
 */
public class AmplitudeDataCompact {

    @JsonProperty("val")
    public int amplitude;

    @JsonProperty("ts")
    public long timestamp;

    @JsonProperty("tz")
    public int offsetMillis;

    @JsonCreator
    public AmplitudeDataCompact(
            @JsonProperty("ts") long timestamp,
            @JsonProperty("val") int amplitude,

            @JsonProperty("tz") int offsetMillis){
        this.amplitude = amplitude;
        this.timestamp = timestamp;
        this.offsetMillis = offsetMillis;
    }
}
