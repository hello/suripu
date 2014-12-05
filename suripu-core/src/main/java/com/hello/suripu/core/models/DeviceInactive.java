package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class DeviceInactive{

    @JsonProperty("device_id")
    public final String deviceId;
    public final Long id;
    public final String diff;
    @JsonProperty("max_ts")
    public final DateTime maxTs;

    public DeviceInactive(final String deviceId, final Long id, final String diff, final DateTime maxTs) {
        this.deviceId = deviceId;
        this.id = id;
        this.diff = diff;
        this.maxTs = maxTs;
    }
}
