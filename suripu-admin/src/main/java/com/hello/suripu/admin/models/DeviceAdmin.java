package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceStatus;

public class DeviceAdmin {
    @JsonProperty("device_account_pair")
    final public DeviceAccountPair deviceAccountPair;

    @JsonProperty("device_status")
    final public DeviceStatus deviceStatus;

    public DeviceAdmin(final DeviceAccountPair deviceAccountPair, final DeviceStatus deviceStatus) {
        this.deviceAccountPair = deviceAccountPair;
        this.deviceStatus = deviceStatus;
    }
}
