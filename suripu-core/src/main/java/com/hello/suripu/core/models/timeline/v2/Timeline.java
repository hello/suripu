package com.hello.suripu.core.models.timeline.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.CurrentRoomState;

import java.util.Collections;
import java.util.List;

public class Timeline {

    @JsonProperty("score")
    final Integer score;

    @JsonProperty("score_condition")
    final CurrentRoomState.State.Condition scoreCondition;

    @JsonProperty("message")
    final String message;

    @JsonProperty("date")
    final String dateNight;

    @JsonProperty("events")
    final List<TimelineEvent> events;

    @JsonProperty("metrics")
    final List<SleepMetrics> metrics;


    public Timeline(final Integer score, final CurrentRoomState.State.Condition scoreCondition, final String message, final String dateNight, final List<TimelineEvent> events, final List<SleepMetrics> metrics) {
        this.score = score;
        this.scoreCondition = scoreCondition;
        this.message = message;
        this.dateNight = dateNight;
        this.events = events;
        this.metrics = metrics;
    }

    public static Timeline create() {
        return new Timeline(null,null,null,null, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }
}
