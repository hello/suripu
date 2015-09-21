package com.hello.suripu.core.models.device.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.WifiInfo;
import org.joda.time.DateTime;

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

    public static Color DEFAULT_COLOR = Color.WHITE;


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

    @JsonProperty("wifi_info")
    final public Optional<WifiInfo> wifiInfoOptional;

    private Device(final Type type, final String deviceId, final State state, final String firmwareVersion, final DateTime lastUpdated, final Color color, final Optional<WifiInfo> wifiInfoOptional) {
        this.type = type;
        this.deviceId = deviceId;
        this.state = state;
        this.firmwareVersion = firmwareVersion;
        this.lastUpdated = lastUpdated;
        this.color = color;
        this.wifiInfoOptional = wifiInfoOptional;
    }


    public static Device create(final com.hello.suripu.core.models.Device deviceV1, final Optional<WifiInfo> wifiInfoOptional) {
        return new Device(
                Device.Type.valueOf(deviceV1.type.toString()),
                deviceV1.deviceId,
                Device.State.valueOf(deviceV1.state.toString()),
                deviceV1.firmwareVersion, deviceV1.lastUpdated,
                Device.Color.valueOf(deviceV1.color.toString()),
                wifiInfoOptional
        );
    }
    private static Color getDefaultColor(final Type type) {
        if (type == Type.PILL) {
            return Color.BLUE;
        }
        return Color.BLACK;
    }
}