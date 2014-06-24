package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.List;

/**
 * Created by pangwu on 5/6/14.
 */
public class TrackerMotion {
    public static final float FLOAT_TO_INT_CONVERTER = 10000000;

    @JsonProperty("account_id")
    public final long accountId;

    @JsonProperty("timestamp")
    public final long timestamp;

    @JsonProperty("value")
    public final int value;

    @JsonProperty("timezone_offset")
    public final int offsetMillis;

    @JsonCreator
    public TrackerMotion(@JsonProperty("account_id") final long accountId,
                         @JsonProperty("timestamp") final long timestamp,
                         @JsonProperty("value") final int value,
                         @JsonProperty("timezone_offset") final int timeZoneOffset){

        this.accountId = accountId;
        this.timestamp = timestamp;
        this.value = value;
        this.offsetMillis = timeZoneOffset;


    }

    public static float intToFloatValue(final int value){
        return value / FLOAT_TO_INT_CONVERTER;
    }


    @Override
    public boolean equals(Object other){
        if (other == null){
            return false;
        }

        if (getClass() != other.getClass()){
            return false;
        }

        final TrackerMotion convertedObject = (TrackerMotion) other;

        return   Objects.equal(this.timestamp, convertedObject.timestamp)
                && Objects.equal(this.value, convertedObject.value)
                && Objects.equal(this.offsetMillis, convertedObject.offsetMillis);
    }

    public static class Batch {
        public final long accountId;
        public final long timestamp;
        public final int offsetMillis;
        public final List<TrackerMotion> motionData;

        public Batch(final long accountId, final long timestamp, final int offsetMillis, final List<TrackerMotion> motionData){
            this.accountId = accountId;
            this.motionData = motionData;
            this.timestamp = timestamp;
            this.offsetMillis = offsetMillis;
        }

    }
}
