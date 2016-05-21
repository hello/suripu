package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

/**
 * Created by km on 11/18/15.
 */
public class MultiDensityImage {
    @JsonProperty("phone_1x")
    public final Optional<String> phoneDensityNormal;

    @JsonProperty("phone_2x")
    public final Optional<String> phoneDensityHigh;

    @JsonProperty("phone_3x")
    public final Optional<String> phoneDensityExtraHigh;

    public MultiDensityImage(@JsonProperty("phone_1x") final Optional<String> phoneDensityNormal,
                             @JsonProperty("phone_2x") final Optional<String> phoneDensityHigh,
                             @JsonProperty("phone_3x") final Optional<String> phoneDensityExtraHigh) {
        this.phoneDensityNormal = phoneDensityNormal;
        this.phoneDensityHigh = phoneDensityHigh;
        this.phoneDensityExtraHigh = phoneDensityExtraHigh;
    }

    public static MultiDensityImage empty(){
        return new MultiDensityImage(Optional.<String>absent(),Optional.<String>absent(),Optional.<String>absent());
    }
}
