package com.hello.suripu.core.models;

public class Insight {

    final public CurrentRoomState.State.Condition condition;
    final public String message;
    final public Sensor sensor;

    public Insight(final Sensor sensor, final CurrentRoomState.State.Condition condition, final String message) {
        this.sensor = sensor;
        this.condition  = condition;
        this.message = message;
    }
}
