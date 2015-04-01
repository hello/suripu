package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceStatusBreakdown {

    @JsonProperty("normal_count")
    final public Long normalCount;

    @JsonProperty("waiting_count")
    final public Long waitingCount;

    @JsonProperty("unknown_count")
    final public Long unknownCount;

    public DeviceStatusBreakdown(final Long normalCount, final Long waitingCount, final Long unknownCount) {
        this.normalCount = normalCount;
        this.waitingCount = waitingCount;
        this.unknownCount = unknownCount;
    }
}