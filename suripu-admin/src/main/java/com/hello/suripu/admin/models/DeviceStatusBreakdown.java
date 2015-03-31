package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceStatusBreakdown {

    @JsonProperty("normal_count")
    final public Integer normalCount;

    @JsonProperty("waiting_count")
    final public Integer waitingCount;

    @JsonProperty("unknown_count")
    final public Integer unknownCount;

    public DeviceStatusBreakdown(final Integer normalCount, final Integer waitingCount, final Integer unknownCount) {
        this.normalCount = normalCount;
        this.waitingCount = waitingCount;
        this.unknownCount = unknownCount;
    }
}