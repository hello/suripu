package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.translations.English;

import java.util.Collections;
import java.util.List;

public class Timeline {

    public enum Period {
        MORNING("morning"),
        AFTERNOON("afternoon"),
        NIGHT("night");

        private String value;

        private Period(String value) {
            this.value = value;
        }


        @Override
        public String toString() {
            return value;
        }


        public static Period fromString(final String sleepPeriodName) {
            if (sleepPeriodName != null) {
                for (final Period period : Period.values()) {
                    if (sleepPeriodName.equalsIgnoreCase(period.toString())) {
                        return period;
                    }
                }
            }
            throw new IllegalArgumentException();
        }
    }

    @JsonProperty("statistics")
    public final Optional<SleepStats> statistics;

    @JsonProperty("score")
    public final Integer score;

    @JsonProperty("message")
    public final String message;

    @JsonProperty("date")
    public final String date;

    @JsonProperty("sleep_periods")
    public final List<Period> sleepPeriods;

    @JsonProperty("segments")
    public final List<SleepSegment> events;

    @JsonProperty("insights")
    public final List<Insight> insights;

    private Timeline(final Integer score,  final String message, final String date,
                     final List<Period> sleepPeriods, final List<SleepSegment> events,
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
                                  @JsonProperty("sleep_period") final List<SleepPeriod.Period> sleepPeriods,
                                  @JsonProperty("segments") final List<SleepSegment> events,
                                  @JsonProperty("insights")  final List<Insight> insights,
                                  @JsonProperty("statistics") final SleepStats sleepStats) {
        final List<Period> periodList = Lists.newArrayList();
        for (final SleepPeriod.Period sleepPeriod : sleepPeriods){
            periodList.add(Period.fromString(sleepPeriod.shortName()));
        }
        return new Timeline(score, message, date, periodList, events, insights,
                (sleepStats == null || sleepStats.isFromNull()) ? null : sleepStats);
    }

    @JsonCreator
    public static Timeline create(@JsonProperty("score") final Integer score,
                                  @JsonProperty("message") final String message,
                                  @JsonProperty("date") final String date,
                                  @JsonProperty("segments") final List<SleepSegment> events,
                                  @JsonProperty("insights")  final List<Insight> insights,
                                  @JsonProperty("statistics") final SleepStats sleepStats) {
        return new Timeline(score, message, date, Lists.newArrayList(Period.NIGHT), events, insights,
                (sleepStats == null || sleepStats.isFromNull()) ? null : sleepStats);
    }

    public static Timeline create(final Integer score,
                                  final String message,
                                  final String date,
                                  final List<SleepSegment> events,
                                  final List<Insight> insights) {
        return new Timeline(score, message, date, Lists.newArrayList(Period.NIGHT), events, insights, null);
    }

    public static Timeline create(final Integer score,
                                  final String message,
                                  final String date,
                                  final List<Period> sleepPeriods,
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
