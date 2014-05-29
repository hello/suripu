package com.hello.suripu.core;

public class DeviceAccountPair {

    public final Long accountId;
    public final Long internalDeviceId;

    public DeviceAccountPair(final Long accountId, final Long internalDeviceId) {
        this.accountId = accountId;
        this.internalDeviceId = internalDeviceId;
    }
}
