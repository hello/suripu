package com.hello.suripu.core.models;

public class DeviceAccountPair {

    public final Long accountId;
    public final Long internalDeviceId;
    public final String externalDeviceId;

    public DeviceAccountPair(final Long accountId, final Long internalDeviceId, final String externalDeviceId) {
        this.accountId = accountId;
        this.internalDeviceId = internalDeviceId;
        this.externalDeviceId = externalDeviceId;
    }
}
