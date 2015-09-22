package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class DeviceStatus {

    @JsonProperty("id")
    public final Long id;

    @JsonProperty("device_id")
    public final Long deviceId;

    @JsonProperty("firmware_version")
    public final String firmwareVersion;

    @JsonProperty("battery_level")
    public final Integer batteryLevel;

    @JsonProperty("last_seen")
    public final DateTime lastSeen;

    @JsonProperty("uptime")
    public final Integer uptime;


    public DeviceStatus(final Long id, final Long deviceId, final String firmwareVersion, final Integer batteryLevel, final DateTime lastSeen, final Integer uptime) {
        this.id = id;
        this.deviceId = deviceId;
        this.firmwareVersion = firmwareVersion;
        this.batteryLevel = batteryLevel;
        this.lastSeen = lastSeen;
        this.uptime = uptime;
    }


    public static DeviceStatus sense(final Long deviceId, final String firmwareVersion, final DateTime lastSeen) {
        return new DeviceStatus(0L, deviceId, firmwareVersion, 100, lastSeen, 0);
    }

}
