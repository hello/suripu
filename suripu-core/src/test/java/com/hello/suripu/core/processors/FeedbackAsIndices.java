package com.hello.suripu.core.processors;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public FeedbackAsIndices(@JsonProperty("original") Integer originalIndex, @JsonProperty("updated") Integer updatedIndex,@JsonProperty("type") String type) {
        this.originalIndex = originalIndex;
        this.updatedIndex = updatedIndex;
        this.type = type;
    }
}
