package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by jimmy on 9/22/14.
 */
public class Device {

    public enum Type {
        PILL,
        SENSE
    }

    public enum State {
        NORMAL,
        LOW_BATTERY
    }

    @JsonProperty("type")
    final public Type type;

    /**
     * This is the external device id / mac address of the device
     */
    @JsonProperty("device_id")
    final public String deviceId;

    @JsonProperty("state")
    final public State state;

    public Device(final Type type, final String deviceId, final State state) {
        this.type = type;
        this.deviceId = deviceId;
        this.state = state;
    }

}
