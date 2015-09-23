package com.hello.suripu.core.models.device.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.*;
import org.joda.time.DateTime;


public class Pill {
    public static final Color DEFAULT_COLOR = Color.BLUE;
    private static final Integer MIN_IDEAL_BATTERY_LEVEL = 10;

    public enum Color {
        BLACK("BLACK"),
        WHITE("WHITE"),
        BLUE("BLUE"),
        RED("RED"),
        AQUA("AQUA"),
        YELLOW("YELLOW");

        private final String value;
        Color(final String value) {
            this.value = value;
        }

        public static Color fromDeviceColor(final com.hello.suripu.core.models.Device.Color deviceColor) {
            return Color.valueOf(deviceColor.name());
        }
    }

    public enum State {
        NORMAL,
        LOW_BATTERY,
        UNKNOWN
    }

    @JsonIgnore
    public final Long internalId;

    @JsonProperty("external_id")
    public final String externalId;

    @JsonProperty("firmware_version")
    public final String firmwareVersion;

    @JsonProperty("battery_level")
    public final Integer batteryLevel;

    @JsonProperty("last_updated")
    public final DateTime lastUpdated;

    @JsonProperty("state")
    public final State state;

    @JsonProperty("color")
    public final Color color;

    public Pill(final Long internalId, final String externalId, final String firmwareVersion, final Integer batteryLevel, final DateTime lastUpdated, final State state, final Color color) {
        this.internalId = internalId;
        this.externalId = externalId;
        this.firmwareVersion = firmwareVersion;
        this.batteryLevel = batteryLevel;
        this.lastUpdated = lastUpdated;
        this.state = state;
        this.color = color;
    }

    public static Pill create(DeviceAccountPair pillAccountPair, Optional<DeviceStatus> pillStatusOptional, final Optional<Color> pillColorOptional) {
        final Color color = pillColorOptional.isPresent() ? pillColorOptional.get() : DEFAULT_COLOR;
        if (!pillStatusOptional.isPresent()) {
            return new Pill(pillAccountPair.internalDeviceId, pillAccountPair.externalDeviceId, null, null, null, State.UNKNOWN, color);
        }
        final DeviceStatus pillStatus = pillStatusOptional.get();
        final State state = pillStatus.batteryLevel < MIN_IDEAL_BATTERY_LEVEL ? State.LOW_BATTERY : State.NORMAL;
        return new Pill(pillAccountPair.internalDeviceId, pillAccountPair.externalDeviceId, pillStatus.firmwareVersion, pillStatus.batteryLevel, pillStatus.lastSeen, state, color);
    }
}
