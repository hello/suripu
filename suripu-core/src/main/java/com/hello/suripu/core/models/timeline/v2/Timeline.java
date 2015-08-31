package com.hello.suripu.core.models.timeline.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;

public class Timeline {

    @JsonProperty("score")
    public final Optional<Integer> score;

    @JsonProperty("score_condition")
    public final ScoreCondition scoreCondition;

    @JsonProperty("message")
    public final String message;

    @JsonProperty("date")
    public final String dateNight;

    @JsonProperty("events")
    public final List<TimelineEvent> events;

    @JsonProperty("metrics")
    public final List<SleepMetrics> metrics;


    public Timeline(final Optional<Integer> score,
                    final ScoreCondition scoreCondition,
                    final String message,
                    final String dateNight,
                    final List<TimelineEvent> events,
                    final List<SleepMetrics> metrics) {
        this.score = score;
        this.scoreCondition = scoreCondition;
        this.message = message;
        this.dateNight = dateNight;
        this.events = events;
        this.metrics = metrics;
    }

    public static Timeline create() {
        return new Timeline(Optional.<Integer>absent(), ScoreCondition.UNAVAILABLE,
                null, null,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }

    public static Timeline createEmpty(final DateTime date, final String message) {
        return new Timeline(Optional.<Integer>absent(), ScoreCondition.UNAVAILABLE,
                message, DateTimeUtil.dateToYmdString(date),
                Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }

    public static Timeline createEmpty(DateTime date) {
        return createEmpty(date, English.TIMELINE_NO_SLEEP_DATA);
    }


    public static Timeline fromV1(final com.hello.suripu.core.models.Timeline timelineV1,
                                  final boolean notEnoughData) {
        return new Timeline(
                Optional.of(timelineV1.score),
                ScoreCondition.fromScore(timelineV1.score, notEnoughData),
                timelineV1.message,
                timelineV1.date,
                TimelineEvent.fromV1(timelineV1.events),
                SleepMetrics.fromV1(timelineV1)
        );
    }

}
