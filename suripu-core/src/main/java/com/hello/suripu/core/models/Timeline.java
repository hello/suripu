package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.translations.English;

import java.util.Collections;
import java.util.List;

public class Timeline {

    @JsonProperty("statistics")
    public final Optional<SleepStats> statistics;

    @JsonProperty("score")
    public final Integer score;

    @JsonProperty("message")
    public final String message;

    @JsonProperty("date")
    public final String date;

    @JsonProperty("sleep_periods")
    public final List<String> sleepPeriods;

    @JsonProperty("segments")
    public final List<SleepSegment> events;

    @JsonProperty("insights")
    public final List<Insight> insights;

    private Timeline(final Integer score,  final String message, final String date,
                     final List<String> sleepPeriods, final List<SleepSegment> events,
                     final List<Insight> insights, final SleepStats sleepStats) {
        this.score = score;
        this.message = message;
        this.date = date;
        this.sleepPeriods = sleepPeriods;
        this.events = events;
        this.insights = insights;
        this.statistics = Optional.fromNullable(sleepStats);
    }

    @JsonCreator
    public static Timeline create(@JsonProperty("score") final Integer score,
                                  @JsonProperty("message") final String message,
                                  @JsonProperty("date") final String date,
                                  @JsonProperty("sleep_period") final List<String> sleepPeriods,
                                  @JsonProperty("segments") final List<SleepSegment> events,
                                  @JsonProperty("insights")  final List<Insight> insights,
                                  @JsonProperty("statistics") final SleepStats sleepStats) {
        return new Timeline(score, message, date, sleepPeriods, events, insights,
                (sleepStats == null || sleepStats.isFromNull()) ? null : sleepStats);
    }

    @JsonCreator
    public static Timeline create(@JsonProperty("score") final Integer score,
                                  @JsonProperty("message") final String message,
                                  @JsonProperty("date") final String date,
                                  @JsonProperty("segments") final List<SleepSegment> events,
                                  @JsonProperty("insights")  final List<Insight> insights,
                                  @JsonProperty("statistics") final SleepStats sleepStats) {
        return new Timeline(score, message, date, Lists.newArrayList(SleepPeriod.Period.NIGHT.shortName()), events, insights,
                (sleepStats == null || sleepStats.isFromNull()) ? null : sleepStats);
    }

    public static Timeline create(final Integer score,
                                  final String message,
                                  final String date,
                                  final List<SleepSegment> events,
                                  final List<Insight> insights) {
        return new Timeline(score, message, date, Lists.newArrayList(SleepPeriod.Period.NIGHT.shortName()), events, insights, null);
    }

    public static Timeline create(final Integer score,
                                  final String message,
                                  final String date,
                                  final List<String> sleepPeriods,
                                  final List<SleepSegment> events,
                                  final List<Insight> insights) {
        return new Timeline(score, message, date, sleepPeriods, events, insights, null);
    }



    public static Timeline createEmpty() {
        return new Timeline(0, English.TIMELINE_NO_SLEEP_DATA, "", Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
    }


    public static Timeline createEmpty(final String message) {
        return new Timeline(0, message, "", Collections.EMPTY_LIST, Collections.EMPTY_LIST,Collections.EMPTY_LIST, null);
    }

}
