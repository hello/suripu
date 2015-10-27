package com.hello.suripu.core.models.timeline.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.SleepStats;

import java.util.List;

public class SleepMetrics {

    public static final String TOTAL_SLEEP_NAME = "total_sleep";
    public static final String SOUND_SLEEP_NAME = "sound_sleep";
    public static final String TIME_TO_SLEEP_NAME = "time_to_sleep";
    public static final String TIMES_AWAKE_NAME = "times_awake";
    public static final String FELL_ASLEEP_NAME = "fell_asleep";
    public static final String WOKE_UP_NAME = "woke_up";
    public static final Integer MINIMUM_TOTAL_SLEEP_DURATION_MINUTES = 120;

    @JsonProperty("name")
    public final String name;

    @JsonProperty("value")
    public final Optional<Long> value;

    @JsonProperty("unit")
    public final Unit unit;

    @JsonProperty("condition")
    public final CurrentRoomState.State.Condition condition;

    private SleepMetrics(final String name, final Optional<Long> value, final Unit unit, final CurrentRoomState.State.Condition condition) {
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.condition = condition;
    }

    public static SleepMetrics create(final String name, final Optional<Long> value, final Unit unit, final CurrentRoomState.State.Condition condition) {
        final Optional<Long> metricValue;
        // v1Values default to 0, but what we really want is to know if it was calculated or not and thus using
        // Optionals inside SleepStats would be ideal. Since that model is highly depended on, changing those
        // default values might have a higher impact than we might like for now so we will convert inside v2
        if (value.or(0L) == 0L && (unit == Unit.TIMESTAMP || unit == Unit.MINUTES)) {
            metricValue = Optional.absent();
        } else {
            metricValue = value;
        }
        return new SleepMetrics(name, metricValue, unit, condition);
    }

    public static List<SleepMetrics> fromV1(final com.hello.suripu.core.models.Timeline timelineV1) {
        final List<SleepMetrics> metrics = Lists.newArrayList();

        final Optional<SleepStats> maybeStatistics = timelineV1.statistics;
        if (maybeStatistics.isPresent() && !maybeStatistics.get().isFromNull()) {
            SleepStats statistics = maybeStatistics.get();

            metrics.add(create(TOTAL_SLEEP_NAME, Optional.of(statistics.sleepDurationInMinutes.longValue()),
                    Unit.MINUTES, CurrentRoomState.State.Condition.IDEAL));
            metrics.add(create(SOUND_SLEEP_NAME, Optional.of(statistics.soundSleepDurationInMinutes.longValue()),
                    Unit.MINUTES, CurrentRoomState.State.Condition.IDEAL));
            metrics.add(create(TIME_TO_SLEEP_NAME, Optional.of(statistics.sleepOnsetTimeMinutes.longValue()),
                    Unit.MINUTES, CurrentRoomState.State.Condition.IDEAL));
            metrics.add(create(TIMES_AWAKE_NAME, Optional.of(statistics.numberOfMotionEvents.longValue()),
                    Unit.QUANTITY, CurrentRoomState.State.Condition.IDEAL));

            metrics.add(create(FELL_ASLEEP_NAME, Optional.of(statistics.sleepTime),
                    Unit.TIMESTAMP, CurrentRoomState.State.Condition.IDEAL));
            metrics.add(create(WOKE_UP_NAME, Optional.of(statistics.wakeTime),
                    Unit.TIMESTAMP, CurrentRoomState.State.Condition.IDEAL));
        }

        for (Insight insight : timelineV1.insights) {
            metrics.add(create(insight.sensor.toString(), Optional.<Long>absent(),
                    Unit.CONDITION, insight.condition));
        }


        return metrics;
    }


    public enum Unit {
        MINUTES,
        QUANTITY,
        TIMESTAMP,
        CONDITION,
    }
}
