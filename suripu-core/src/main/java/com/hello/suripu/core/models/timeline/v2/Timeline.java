package com.hello.suripu.core.models.timeline.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.CurrentRoomState;

import java.util.Collections;
import java.util.List;

public class Timeline {

    @JsonProperty("score")
    final Integer score;

    @JsonProperty("score_condition")
    final ScoreCondition scoreCondition;

    @JsonProperty("message")
    final String message;

    @JsonProperty("date")
    final String dateNight;

    @JsonProperty("events")
    final List<TimelineEvent> events;

    @JsonProperty("metrics")
    final List<SleepMetrics> metrics;


    public Timeline(final Integer score, final ScoreCondition scoreCondition, final String message, final String dateNight, final List<TimelineEvent> events, final List<SleepMetrics> metrics) {
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

    public static Timeline createEmpty() {
        return new Timeline(null,null,null,null, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }


    public static Timeline fromV1(com.hello.suripu.core.models.Timeline timelineV1) {

        final Timeline t = new Timeline(
                timelineV1.score,
                fromScore(timelineV1.score),
                timelineV1.message,
                timelineV1.date,
                TimelineEvent.fromV1(timelineV1.events),
                Lists.newArrayList(
                        SleepMetrics.create("made_up_metric", "46", "ms", CurrentRoomState.State.Condition.ALERT),
                        SleepMetrics.create("meaning_of_life", "42", "min", CurrentRoomState.State.Condition.IDEAL)
                )
        );
        return t;
    }

    public static ScoreCondition fromScore(final Integer score) {
        if(score == 0 ) {
            return ScoreCondition.UNAVAILABLE;
        } else if(score < 50) {
            return ScoreCondition.ALERT;
        } else if(score < 80) {
            return ScoreCondition.WARNING;
        }

        return ScoreCondition.IDEAL;
    }
}
