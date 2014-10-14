package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Insight {

    @JsonProperty("condition")
    final public CurrentRoomState.State.Condition condition;

    @JsonProperty("message")
    final public String message;

    @JsonProperty("sensor")
    final public Sensor sensor;

    public Insight(final Sensor sensor, final CurrentRoomState.State.Condition condition, final String message) {
        this.sensor = sensor;
        this.condition  = condition;
        this.message = message;
    }
}
