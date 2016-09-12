package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.roomstate.Condition;

public class Insight {

    @JsonProperty("condition")
    final public Condition condition;

    @JsonProperty("message")
    final public String message;

    @JsonProperty("sensor")
    final public Sensor sensor;

    public Insight(final Sensor sensor, final Condition condition, final String message) {
        this.sensor = sensor;
        this.condition  = condition;
        this.message = message;
    }

    @JsonCreator
    public static Insight create(
            @JsonProperty("sensor") final Sensor sensor,
            @JsonProperty("condition") final Condition condition,
            @JsonProperty("message") final String message) {
        return new Insight(sensor, condition, message);
    }
}
