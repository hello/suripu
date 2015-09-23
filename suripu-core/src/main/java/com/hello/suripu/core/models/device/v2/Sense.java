package com.hello.suripu.core.models.device.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.WifiInfo;
import org.joda.time.DateTime;

public class Sense {

    public enum Color {
        BLACK,
        WHITE,
        BLUE,
        RED,
        AQUA,
        YELLOW
    }

    public static final Color DEFAULT_COLOR = Color.BLACK;

    public enum State {
        NORMAL,
        UNKNOWN
    }

    @JsonIgnore
    public final Long internalId;

    @JsonProperty("id")
    public final String externalId;

    @JsonProperty("firmware_version")
    public final String firmwareVersion;

    @JsonProperty("state")
    public final State state;

    @JsonProperty("last_updated")
    public final DateTime lastUpdated;

    @JsonProperty("color")
    public final Color color;

    @JsonProperty("wifi_info")
    public final Optional<WifiInfo> wifiInfoOptional;


    private Sense(final Long internalId, final String externalId, final String firmwareVersion, final State state, final DateTime lastUpdated, final Color color, final Optional<WifiInfo> wifiInfoOptional) {
        this.internalId = internalId;
        this.externalId = externalId;
        this.firmwareVersion = firmwareVersion;
        this.state = state;
        this.lastUpdated = lastUpdated;
        this.color = color;
        this.wifiInfoOptional = wifiInfoOptional;
    }

    public static Sense create(final DeviceAccountPair senseAccountPair, final Optional<DeviceStatus> senseStatusOptional, final Optional<Color> colorOptional, final Optional<WifiInfo> wifiInfoOptional) {
        final Color color = colorOptional.isPresent() ? colorOptional.get() : DEFAULT_COLOR;
        if (!senseStatusOptional.isPresent()) {
            return new Sense(senseAccountPair.internalDeviceId, senseAccountPair.externalDeviceId, null, State.UNKNOWN, null, color, wifiInfoOptional);
        }
        final DeviceStatus senseStatus = senseStatusOptional.get();
        return new Sense(senseAccountPair.internalDeviceId, senseAccountPair.externalDeviceId, senseStatus.firmwareVersion, State.NORMAL, senseStatus.lastSeen, color, wifiInfoOptional);
    }
}
