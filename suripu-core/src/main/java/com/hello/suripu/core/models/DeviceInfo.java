package com.hello.suripu.core.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class DeviceInfo {
    @JsonProperty("id")
    public final Long id;
    @JsonProperty("device_id")
    public final String deviceId;
    @JsonProperty("account_id")
    public final Long accountId;
    @JsonProperty("last_updated")
    public final DateTime lastUpdated;
    @JsonProperty("created")
    public final DateTime created;

    public DeviceInfo(final Long id, final String deviceId, final Long accountId, final DateTime lastUpdated, final DateTime created) {
        this.id = id;
        this.deviceId = deviceId;
        this.accountId = accountId;
        this.lastUpdated = lastUpdated;
        this.created = created;
    }
}
