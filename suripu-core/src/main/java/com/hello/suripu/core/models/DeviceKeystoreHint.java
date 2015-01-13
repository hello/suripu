package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceKeystoreHint {

    @JsonProperty("device_id")
    public final String deviceId;

    @JsonProperty("hint")
    public final String hint;

    public DeviceKeystoreHint(final String deviceId, final String hint) {
        this.deviceId = deviceId;
        this.hint = hint;
    }
}
