package com.hello.suripu.admin.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceStatusBreakdown {

    @JsonProperty("senses_count")
    final public Long sensesCount;

    @JsonProperty("pills_count")
    final public Long pillsCount;


    public DeviceStatusBreakdown(final Long sensesCount, final Long pillsCount) {
        this.sensesCount = sensesCount;
        this.pillsCount = pillsCount;
    }
}