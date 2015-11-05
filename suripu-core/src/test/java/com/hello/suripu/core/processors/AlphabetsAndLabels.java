package com.hello.suripu.core.processors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Event;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 8/3/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlphabetsAndLabels {

    @JsonProperty("alphabets")
    final Map<String,List<Integer>> alphabets;

    @JsonProperty("state_sizes")
    final Map<String,Integer> stateSizes;

    @JsonProperty("feedback")
    final List<FeedbackAsIndices> feedback;

    @JsonCreator
    public AlphabetsAndLabels(@JsonProperty("alphabets") Map<String, List<Integer>> alphabets,@JsonProperty("state_sizes") final Map<String,Integer> stateSizes,@JsonProperty("feedback") List<FeedbackAsIndices> feedback) {
        this.alphabets = alphabets;
        this.stateSizes = stateSizes;
        this.feedback = feedback;
    }
}
