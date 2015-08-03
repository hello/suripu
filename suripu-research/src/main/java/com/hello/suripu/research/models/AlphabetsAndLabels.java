package com.hello.suripu.research.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.Event;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 8/3/15.
 */
public class AlphabetsAndLabels {

    @JsonProperty("alphabets")
    final Map<String,List<Integer>> alphabets;

    @JsonProperty("state_sizes")
    final Map<String,Integer> stateSizes;


    @JsonProperty("feedback")
    final List<FeedbackAsIndices> feedback;


    public AlphabetsAndLabels(Map<String, List<Integer>> alphabets, final Map<String,Integer> stateSizes, List<FeedbackAsIndices> feedback) {
        this.alphabets = alphabets;
        this.stateSizes = stateSizes;
        this.feedback = feedback;
    }
}
