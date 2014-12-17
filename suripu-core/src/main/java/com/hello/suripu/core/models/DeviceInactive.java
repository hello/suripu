package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceInactive{

    @JsonProperty("device_id")
    public final String deviceId;
    @JsonProperty("last_seen_timestamp")
    public final Long lastSeenTimestamp;

    public DeviceInactive(final String deviceId, final Long lastSeenTimestamp) {
        this.deviceId = deviceId;
        this.lastSeenTimestamp = lastSeenTimestamp;
    }
}
