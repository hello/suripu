package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceStatus;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeviceAdmin {
    @JsonProperty("device_account_pair")
    final public DeviceAccountPair deviceAccountPair;

    @JsonProperty("device_status")
    final public DeviceStatus deviceStatus;

    @JsonProperty("pairing_ts")
    final public DateTime pairingTs;

    private DeviceAdmin(final DeviceAccountPair deviceAccountPair, final DeviceStatus deviceStatus) {
        this.deviceAccountPair = deviceAccountPair;
        this.deviceStatus = deviceStatus;
        this.pairingTs = deviceAccountPair.created;
    }

    @JsonProperty("paired_by_admin")
    public Boolean pairedByAdmin() {
        if (this.deviceStatus != null) {
             return "0".equals(this.deviceStatus.firmwareVersion);
        }
        return Boolean.FALSE;
    }

    public static DeviceAdmin create(@NotNull final DeviceAccountPair deviceAccountPair) {
        return new DeviceAdmin(deviceAccountPair, null);
    }

    public static DeviceAdmin create(final DeviceAccountPair deviceAccountPair, @NotNull DeviceStatus deviceStatus) {
        checkNotNull(deviceStatus, "DeviceStatus cannot be null");
        return new DeviceAdmin(deviceAccountPair, deviceStatus);
    }
}
