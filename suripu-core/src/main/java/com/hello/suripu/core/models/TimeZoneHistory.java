package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by pangwu on 6/16/14.
 */
public class TimeZoneHistory {

    @JsonProperty("updated_at")
    public final long updatedAt;

    @JsonProperty("timezone_offset")
    public final int offsetMillis;

    @JsonProperty("timezone_id")
    public final String timeZoneId;

    public TimeZoneHistory(long updatedAt, int offsetMillis, final String timeZoneId){
        this.updatedAt = updatedAt;
        this.offsetMillis = offsetMillis;
        this.timeZoneId = timeZoneId;
    }
}
