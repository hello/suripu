package com.hello.suripu.research.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by benjo on 8/3/15.
 */
public class FeedbackAsIndices {
    @JsonProperty("original")
    public final Integer originalIndex;

    @JsonProperty("updated")
    public final Integer updatedIndex;

    @JsonProperty("type")
    public final String type;

    public FeedbackAsIndices(Integer originalIndex, Integer updatedIndex, String type) {
        this.originalIndex = originalIndex;
        this.updatedIndex = updatedIndex;
        this.type = type;
    }
}
