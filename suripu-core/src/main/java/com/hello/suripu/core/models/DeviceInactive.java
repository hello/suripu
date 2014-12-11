package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceInactive{

    @JsonProperty("device_id")
    public final String deviceId;
    @JsonProperty("inactive_period")
    public final Long inactivePeriodInMilliseconds;

    public DeviceInactive(final String deviceId, final Long inactivePeriodInMilliseconds) {
        this.deviceId = deviceId;
        this.inactivePeriodInMilliseconds = inactivePeriodInMilliseconds;
    }
}
