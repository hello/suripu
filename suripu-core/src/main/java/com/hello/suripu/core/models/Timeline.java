package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Timeline {

    @JsonProperty("score")
    public final Integer score;

    @JsonProperty("message")
    public final String message;

    @JsonProperty("date")
    public final String date;

    @JsonProperty("segments")
    public final List<SleepSegment> events;

    @JsonProperty("insights")
    public final List<Insight> insights;

    public Timeline(final Integer score, final String message, final String date, final List<SleepSegment> events, final List<Insight> insights) {
        this.score = score;
        this.message = message;
        this.date = date;
        this.events = events;
        this.insights = insights;
    }


}
