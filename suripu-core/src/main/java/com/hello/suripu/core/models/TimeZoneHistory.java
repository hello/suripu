package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by pangwu on 6/16/14.
 */
public class TimeZoneHistory {

    @JsonProperty("updated_at")
    public final long updatedAt;

    @JsonProperty("tiemzone_offset")
    public final int offsetMillis;

    public TimeZoneHistory(final long updatedAt, final int offsetMillis){
        this.updatedAt = updatedAt;
        this.offsetMillis = offsetMillis;
    }
}
