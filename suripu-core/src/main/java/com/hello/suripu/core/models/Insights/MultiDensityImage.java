package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

/**
 * Created by km on 11/18/15.
 */
public class MultiDensityImage {
    @JsonProperty("1x")
    public final Optional<String> normalDensity;

    @JsonProperty("2x")
    public final Optional<String> highDensity;

    @JsonProperty("3x")
    public final Optional<String> extraHighDensity;

    public MultiDensityImage(@JsonProperty("1x") final Optional<String> normalDensity,
                             @JsonProperty("2x") final Optional<String> highDensity,
                             @JsonProperty("3x") final Optional<String> extraHighDensity) {
        this.normalDensity = normalDensity;
        this.highDensity = highDensity;
        this.extraHighDensity = extraHighDensity;
    }
}
