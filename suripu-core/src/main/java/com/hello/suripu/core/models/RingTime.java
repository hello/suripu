package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

/**
 * Created by pangwu on 9/23/14.
 */
public class RingTime {

    private static final long EMPTY = 0;

    @JsonProperty("actual_ring_time_utc")
    public final long actualRingTimeUTC;

    @JsonProperty("expected_ring_time_utc")
    public final long expectedRingTimeUTC;

    @JsonProperty("created_at_utc")
    public final long createdAtUTC;

    @JsonCreator
    public RingTime(@JsonProperty("actual_ring_time_utc") long actual,
                    @JsonProperty("expected_ring_time_utc") long expected,
                    @JsonProperty("created_at_utc") long createdAt){
        if(expected < actual){
            throw new IllegalArgumentException("Actual ring behind deadline.");
        }

        this.actualRingTimeUTC = actual;
        this.expectedRingTimeUTC = expected;
        this.createdAtUTC = createdAt;

    }

    public static RingTime createEmpty(){
        return new RingTime(EMPTY, EMPTY, DateTime.now().getMillis());
    }

    @JsonIgnore
    public boolean isEmpty(){
        return this.expectedRingTimeUTC == EMPTY;
    }

    @JsonIgnore
    public boolean isSmart(){
        return isEmpty() == false && this.expectedRingTimeUTC != this.actualRingTimeUTC;
    }

    @JsonIgnore
    public boolean isRegular(){
        return isEmpty() == false && this.expectedRingTimeUTC == this.actualRingTimeUTC;
    }

    @Override
    public int hashCode(){
        return Objects.hashCode(this.expectedRingTimeUTC);
    }

    @Override
    public boolean equals(final Object other){
        if (other == null){
            return false;
        }

        if (getClass() != other.getClass()){
            return false;
        }

        final RingTime convertedObject = (RingTime) other;
        return Objects.equal(convertedObject.expectedRingTimeUTC, this.expectedRingTimeUTC);
    }
}
