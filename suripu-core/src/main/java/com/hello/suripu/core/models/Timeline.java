package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public Timeline(@JsonProperty("score")
                    final Integer score,
                    @JsonProperty("message")
                    final String message,
                    @JsonProperty("date")
                    final String date,
                    @JsonProperty("segments")
                    final List<SleepSegment> events,
                    @JsonProperty("insights")
                    final List<Insight> insights) {
        this.score = score;
        this.message = message;
        this.date = date;
        this.events = events;
        this.insights = insights;
    }


}
