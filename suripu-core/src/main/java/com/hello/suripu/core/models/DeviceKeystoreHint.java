package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceKeystoreHint {

    @JsonProperty("device_type")
    public final String deviceType;

    @JsonProperty("device_id")
    public final String deviceId;

    @JsonProperty("hint")
    public final String hint;

    public DeviceKeystoreHint(final String deviceType, final String deviceId, final String hint) {
        this.deviceType = deviceType;
        this.deviceId = deviceId;
        this.hint = hint;
    }
}
