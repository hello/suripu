package com.hello.suripu.core.models;

import org.joda.time.DateTime;

public class DeviceAccountPair {

    public final Long accountId;
    public final Long internalDeviceId;
    public final String externalDeviceId;
    public final DateTime created;

    public DeviceAccountPair(final Long accountId, final Long internalDeviceId, final String externalDeviceId, final DateTime created) {
        this.accountId = accountId;
        this.internalDeviceId = internalDeviceId;
        this.externalDeviceId = externalDeviceId;
        this.created = created;
    }

    public DeviceAccountPair(final Long accountId, final Long internalDeviceId, final String externalDeviceId) {
        this(accountId, internalDeviceId, externalDeviceId, null);
    }
}
