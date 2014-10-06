package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

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
        LOW_BATTERY,
        UNKNOWN
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

    @JsonProperty("firmware_version")
    final public String firmwareVersion;


    @JsonProperty("last_updated")
    final public DateTime lastUpdated;

    public Device(final Type type, final String deviceId, final State state, final String firmwareVersion, final DateTime lastUpdated) {
        this.type = type;
        this.deviceId = deviceId;
        this.state = state;
        this.firmwareVersion = firmwareVersion;
        this.lastUpdated = lastUpdated;
    }

}
