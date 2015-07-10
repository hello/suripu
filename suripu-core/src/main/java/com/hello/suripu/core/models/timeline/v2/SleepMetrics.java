package com.hello.suripu.core.models.timeline.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.SleepStats;

import java.util.List;

public class SleepMetrics {

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
        return new SleepMetrics(name, value, unit, condition);
    }

    public static List<SleepMetrics> fromV1(final com.hello.suripu.core.models.Timeline timelineV1) {
        final List<SleepMetrics> metrics = Lists.newArrayList();

        final Optional<SleepStats> maybeStatistics = timelineV1.statistics;
        if (maybeStatistics.isPresent() && !maybeStatistics.get().isFromNull()) {
            SleepStats statistics = maybeStatistics.get();

            metrics.add(create("total_sleep", Optional.of(statistics.sleepDurationInMinutes.longValue()),
                    Unit.MINUTES, CurrentRoomState.State.Condition.IDEAL));
            metrics.add(create("sound_sleep", Optional.of(statistics.soundSleepDurationInMinutes.longValue()),
                    Unit.MINUTES, CurrentRoomState.State.Condition.IDEAL));
            metrics.add(create("time_to_sleep", Optional.of(statistics.sleepOnsetTimeMinutes.longValue()),
                    Unit.MINUTES, CurrentRoomState.State.Condition.IDEAL));
            metrics.add(create("times_awake", Optional.of(statistics.numberOfMotionEvents.longValue()),
                    Unit.QUANTITY, CurrentRoomState.State.Condition.IDEAL));

            metrics.add(create("fell_asleep", Optional.of(statistics.sleepTime),
                    Unit.TIMESTAMP, CurrentRoomState.State.Condition.IDEAL));
            metrics.add(create("woke_up", Optional.of(statistics.wakeTime),
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
