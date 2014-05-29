package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by pangwu on 5/6/14.
 */
public class TrackerMotion {
    public static final float FLOAT_TO_INT_CONVERTER = 10000000;

    @JsonProperty("id")
    public final long id;

    @JsonProperty("account_id")
    public final long accountId;

    @JsonProperty("tracker_id")
    public final String trackerId;

    @JsonProperty("timestamp")
    public final long timestamp;

    @JsonProperty("value")
    public final int value;

    @JsonProperty("timezone_offset")
    public final int offsetMillis;

    @JsonCreator
    public TrackerMotion(@JsonProperty("id") final long id,
                         @JsonProperty("account_id") final long accountId,
                         @JsonProperty("tracker_id") final String trackerId,
                         @JsonProperty("timestamp") final long timestamp,
                         @JsonProperty("value") final int value,
                         @JsonProperty("timezone_offset") final int timeZoneOffset){

        this.id = id;
        this.accountId = accountId;
        this.trackerId = trackerId;
        this.timestamp = timestamp;
        this.value = value;
        this.offsetMillis = timeZoneOffset;


    }

    public static float intToFloatValue(final int value){
        return value / FLOAT_TO_INT_CONVERTER;
    }
}
