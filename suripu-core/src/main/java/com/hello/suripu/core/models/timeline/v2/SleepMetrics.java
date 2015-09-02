package com.hello.suripu.core.models.timeline.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.CurrentRoomState;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.SleepStats;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.text.html.Option;
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
        final Optional<Long> metricValue = value
                .transform(new Function<Long, Optional<Long>>() {
                    @Nonnull
                    @Override
                    public Optional<Long> apply(@Nonnull Long v1Value) {
                        // v1Values default to 0, but what we really want is to know if it was calculated or not and thus using
                        // Optionals inside SleepStats would be ideal. Since that model is highly depended on, changing those
                        // default values might have a higher impact than we might like for now so we will convert inside v2
                        if (v1Value == 0L && (unit == Unit.TIMESTAMP || unit == Unit.MINUTES)) {
                            return Optional.absent();
                        }
                        return Optional.of(v1Value);
                    }
                })
                .or(Optional.<Long>absent()); // required to handle outcome of .transform
        return new SleepMetrics(name, metricValue, unit, condition);
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
