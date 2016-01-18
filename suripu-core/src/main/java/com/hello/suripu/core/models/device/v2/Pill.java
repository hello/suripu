package com.hello.suripu.core.models.device.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.*;
import com.hello.suripu.core.pill.heartbeat.PillHeartBeat;
import org.joda.time.DateTime;


public class Pill {
    public static final Color DEFAULT_COLOR = Color.BLUE;
    private static final Integer MIN_IDEAL_BATTERY_LEVEL = 15;

    // These should match the colors in `PillColorUtil`
    public enum Color {
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

    @JsonProperty("id")
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

    private Pill(final Long internalId, final String externalId, final Optional<String> firmwareVersionOptional, final Optional<Integer> batteryLevelOptional, final Optional<DateTime> lastUpdatedOptional, final State state, final Color color) {
        this.internalId = internalId;
        this.externalId = externalId;
        this.firmwareVersionOptional = firmwareVersionOptional;
        this.batteryLevelOptional = batteryLevelOptional;
        this.lastUpdatedOptional = lastUpdatedOptional;
        this.state = state;
        this.color = color;
    }

    public static Pill create(DeviceAccountPair pillAccountPair, Optional<PillHeartBeat> pillHeartBeatOptional, final Optional<Color> pillColorOptional) {
        final Color color = pillColorOptional.isPresent() ? pillColorOptional.get() : DEFAULT_COLOR;
        if (!pillHeartBeatOptional.isPresent()) {
            return new Pill(pillAccountPair.internalDeviceId, pillAccountPair.externalDeviceId, Optional.<String>absent(), Optional.<Integer>absent(), Optional.<DateTime>absent(), State.UNKNOWN, color);
        }
        final PillHeartBeat pillHeartBeat = pillHeartBeatOptional.get();
        final State state = pillHeartBeat.batteryLevel < MIN_IDEAL_BATTERY_LEVEL ? State.LOW_BATTERY : State.NORMAL;
        return new Pill(pillAccountPair.internalDeviceId, pillAccountPair.externalDeviceId, Optional.of(String.valueOf(pillHeartBeat.firmwareVersion)), Optional.of(pillHeartBeat.batteryLevel), Optional.of(pillHeartBeat.createdAtUTC), state, color);
    }

}
