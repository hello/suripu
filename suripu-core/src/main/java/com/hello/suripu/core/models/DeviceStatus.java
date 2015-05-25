package com.hello.suripu.core.models;

import org.joda.time.DateTime;

public class DeviceStatus {

    public final Long id;
    public final Long deviceId;
    public final String firmwareVersion;
    public final Integer batteryLevel;
    public final DateTime lastSeen;
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
