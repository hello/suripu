package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * Created by pangwu on 6/16/14.
 */
public class TimeZoneHistory {
    public static final int FALLBACK_OFFSET_MILLIS = -26200000; // PDT

    @JsonIgnore
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

    @JsonCreator
    public TimeZoneHistory(@JsonProperty("timezone_offset") int offsetMillis,
                           @JsonProperty("timezone_id") final String timeZoneId){
        this.updatedAt = 0;
        this.offsetMillis = offsetMillis;
        this.timeZoneId = timeZoneId;
    }

    @Override
    public boolean equals(final Object other){
        if (other == null){
            return false;
        }

        if (getClass() != other.getClass()){
            return false;
        }

        final TimeZoneHistory convertedObject = (TimeZoneHistory) other;
        return Objects.equal(convertedObject.offsetMillis, this.offsetMillis) &&
                Objects.equal(convertedObject.timeZoneId, this.timeZoneId);
    }
}
