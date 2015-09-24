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
        BLUE("BLUE"),
        RED("RED");

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
    public final Optional<String> firmwareVersionOptional;

    @JsonProperty("battery_level")
    public final Optional<Integer> batteryLevelOptional;

    @JsonProperty("last_updated")
    public final Optional<DateTime> lastUpdatedOptional;

    @JsonProperty("state")
    public final State state;

    @JsonProperty("color")
    public final Color color;

    public Pill(final Long internalId, final String externalId, final Optional<String> firmwareVersionOptional, final Optional<Integer> batteryLevelOptional, final Optional<DateTime> lastUpdatedOptional, final State state, final Color color) {
        this.internalId = internalId;
        this.externalId = externalId;
        this.firmwareVersionOptional = firmwareVersionOptional;
        this.batteryLevelOptional = batteryLevelOptional;
        this.lastUpdatedOptional = lastUpdatedOptional;
        this.state = state;
        this.color = color;
    }

    public static Pill create(DeviceAccountPair pillAccountPair, Optional<DeviceStatus> pillStatusOptional, final Optional<Color> pillColorOptional) {
        final Color color = pillColorOptional.isPresent() ? pillColorOptional.get() : DEFAULT_COLOR;
        if (!pillStatusOptional.isPresent()) {
            return new Pill(pillAccountPair.internalDeviceId, pillAccountPair.externalDeviceId, Optional.<String>absent(), Optional.<Integer>absent(), Optional.<DateTime>absent(), State.UNKNOWN, color);
        }
        final DeviceStatus pillStatus = pillStatusOptional.get();
        final State state = pillStatus.batteryLevel < MIN_IDEAL_BATTERY_LEVEL ? State.LOW_BATTERY : State.NORMAL;
        return new Pill(pillAccountPair.internalDeviceId, pillAccountPair.externalDeviceId, Optional.of(pillStatus.firmwareVersion), Optional.of(pillStatus.batteryLevel), Optional.of(pillStatus.lastSeen), state, color);
    }
}
