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

    public enum Color {
        BLACK,
        WHITE,
        BLUE,
        RED,
        AQUA,
        YELLOW
    }

    public static Color DEFAULT_COLOR = Color.BLACK;


    @JsonProperty("color")
    public final Color color;

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

    public Device(final Type type, final String deviceId, final State state, final String firmwareVersion, final DateTime lastUpdated, final Color color) {
        this.type = type;
        this.deviceId = deviceId;
        this.state = state;
        this.firmwareVersion = firmwareVersion;
        this.lastUpdated = lastUpdated;
        this.color = color;
    }

    private static Color getDefaultColor(final Type type) {
        if (type == Type.PILL) {
            return Color.BLUE;
        }
        return Color.BLACK;
    }
}
