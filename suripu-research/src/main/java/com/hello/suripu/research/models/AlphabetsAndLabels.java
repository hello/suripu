package com.hello.suripu.research.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;

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

    @JsonProperty("account_id")
    final Long accountId;

    @JsonProperty("date_of_night")
    final String dateOfNight;


    public AlphabetsAndLabels(Map<String, List<Integer>> alphabets, final Map<String,Integer> stateSizes, List<FeedbackAsIndices> feedback, final Long accountId, final DateTime evening) {
        this.alphabets = alphabets;
        this.stateSizes = stateSizes;
        this.feedback = feedback;
        this.accountId = accountId;
        dateOfNight = DateTimeUtil.dateToYmdString(evening);
    }
}
