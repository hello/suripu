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

    @JsonProperty("feedback")
    final List<Event> feedback;


    public AlphabetsAndLabels(Map<String, List<Integer>> alphabets, List<Event> feedback) {
        this.alphabets = alphabets;
        this.feedback = feedback;
    }
}
