package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class DeviceAccountPair {

    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("internal_device_id")
    public final Long internalDeviceId;

    @JsonProperty("external_device_id")
    public final String externalDeviceId;

    @JsonIgnore
    public final DateTime created;

    public DeviceAccountPair(final Long accountId, final Long internalDeviceId, final String externalDeviceId, final DateTime created) {
        this.accountId = accountId;
        this.internalDeviceId = internalDeviceId;
        this.externalDeviceId = externalDeviceId;
        this.created = created;
    }
}
