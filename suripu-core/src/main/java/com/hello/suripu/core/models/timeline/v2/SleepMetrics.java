package com.hello.suripu.core.models.timeline.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.CurrentRoomState;

public class SleepMetrics {

    @JsonProperty("name")
    public final String name;

    @JsonProperty("value")
    public final String value;

    @JsonProperty("unit")
    public final String unit;

    @JsonProperty("condition")
    public final CurrentRoomState.State.Condition condition;

    private SleepMetrics(final String name, final String value, final String unit, final CurrentRoomState.State.Condition condition) {
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.condition = condition;
    }

    public static SleepMetrics create(final String name, final String value, final String unit, final CurrentRoomState.State.Condition condition) {
        return new SleepMetrics(name, value, unit, condition);
    }
}
