package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class Sense {
    public enum State {
        NORMAL,
        WAITING,
        UNKNOWN
    }

    public enum Color {
        BLACK,
        WHITE,
        BLUE,
        RED
    }

    private static Color DEFAULT_SENSE_COLOR = Color.BLACK;
    @JsonProperty("device_id")
    final public String deviceId;

    @JsonProperty("state")
    final public State state;

    @JsonProperty("firmware_version")
    final public String firmwareVersion;

    @JsonProperty("last_updated")
    final public DateTime lastUpdated;

    @JsonProperty("created")
    final public DateTime created;

    @JsonProperty("color")
    final public Color color;

    public Sense(final String deviceId, final State state, final String firmwareVersion, final DateTime lastUpdated, final DateTime created, final Color color) {
        this.deviceId = deviceId;
        this.state = state;
        this.firmwareVersion = firmwareVersion;
        this.lastUpdated = lastUpdated;
        this.created = created;
        this.color = color;
    }

    public Sense(final String deviceId, final State state, final String firmwareVersion, final DateTime lastUpdated, final DateTime created) {
        this(deviceId, state, firmwareVersion, lastUpdated, created, DEFAULT_SENSE_COLOR);
    }
}
