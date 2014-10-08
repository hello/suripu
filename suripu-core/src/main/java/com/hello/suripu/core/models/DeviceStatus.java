package com.hello.suripu.core.models;

import org.joda.time.DateTime;

public class DeviceStatus {

    public final Long id;
    public final Long deviceId;
    public final String firmwareVersion;
    public final Integer batteryLevel;
    public final DateTime lastSeen;

    public DeviceStatus(final Long id, final Long deviceId, final String firmwareVersion, final Integer batteryLevel, final DateTime lastSeen) {
        this.id = id;
        this.deviceId = deviceId;
        this.firmwareVersion = firmwareVersion;
        this.batteryLevel = batteryLevel;
        this.lastSeen = lastSeen;
    }
}
