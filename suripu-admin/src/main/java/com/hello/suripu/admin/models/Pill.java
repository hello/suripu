package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class Pill {
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

    private static Color DEFAULT_PILL_COLOR = Color.BLUE;
    @JsonProperty("device_id")
    final public String deviceId;

    @JsonProperty("state")
    final public State state;

    @JsonProperty("battery_level")
    public final Integer batteryLevel;

    @JsonProperty("uptime")
    public final Integer uptime;

    @JsonProperty("last_updated")
    final public DateTime lastUpdated;

    @JsonProperty("created")
    final public DateTime created;

    @JsonProperty("color")
    final public Color color;

    public Pill(final String deviceId, final State state, final Integer batteryLevel, final Integer uptime, final DateTime lastUpdated, final DateTime created, final Color color) {
        this.deviceId = deviceId;
        this.state = state;
        this.batteryLevel = batteryLevel;
        this.uptime = uptime;
        this.lastUpdated = lastUpdated;
        this.created = created;
        this.color = color;
    }

    public Pill(final String deviceId, final State state, final Integer batteryLevel, final Integer uptime, final DateTime lastUpdated, final DateTime created) {
        this(deviceId, state, batteryLevel, uptime, lastUpdated, created, DEFAULT_PILL_COLOR);
    }
}
