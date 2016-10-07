package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.hello.suripu.core.roomstate.State;

public class SensorReading {

    @JsonProperty("name")
    public final String name;

    @JsonProperty("value")
    public final Integer value;

    @JsonProperty("unit")
    public final State.Unit unit;


    public SensorReading(final String name, final Integer value, final State.Unit unit) {
        this.name = name;
        this.value = value;
        this.unit = unit;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(SensorReading.class)
                .add("name", name)
                .add("value", value)
                .add("unit", unit)
                .toString();
    }
}