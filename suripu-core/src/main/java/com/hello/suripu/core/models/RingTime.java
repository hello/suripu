package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * Created by pangwu on 9/23/14.
 */
public class RingTime {

    private static final long EMPTY = 0;

    @JsonProperty("actual_ring_time_utc")
    public final long actualRingTimeUTC;

    @JsonProperty("expected_ring_time_utc")
    public final long expectedRingTimeUTC;

    @JsonProperty("sound_ids")
    public final long[] soundIds;

    @JsonProperty("from_smart_alarm")
    public final boolean fromSmartAlarm;

    @JsonCreator
    public RingTime(@JsonProperty("actual_ring_time_utc") long actual,
                    @JsonProperty("expected_ring_time_utc") long expected,
                    @JsonProperty("sound_ids") final long[] soundIds,
                    @JsonProperty("from_smart_alarm") final boolean fromSmartAlarm){
        if(expected < actual){
            throw new IllegalArgumentException("Actual ring behind deadline.");
        }

        this.actualRingTimeUTC = actual;
        this.expectedRingTimeUTC = expected;
        this.soundIds = soundIds;
        this.fromSmartAlarm = fromSmartAlarm;

    }

    public RingTime(long actual, long expected, final Long[] soundIds, final boolean fromSmartAlarm){
        if(expected < actual){
            throw new IllegalArgumentException("Actual ring behind deadline.");
        }

        this.actualRingTimeUTC = actual;
        this.expectedRingTimeUTC = expected;
        this.soundIds  = new long[soundIds.length];
        for(int i = 0; i < soundIds.length; i++){
            this.soundIds[i] = soundIds[i];
        }

        this.fromSmartAlarm = fromSmartAlarm;

    }

    public RingTime(long actual, long expected, long soundId, final boolean fromSmartAlarm){
        if(expected < actual){
            throw new IllegalArgumentException("Actual ring behind deadline.");
        }

        this.actualRingTimeUTC = actual;
        this.expectedRingTimeUTC = expected;
        this.soundIds = new long[]{ soundId };
        this.fromSmartAlarm = fromSmartAlarm;

    }

    public static RingTime createEmpty(){
        return new RingTime(EMPTY, EMPTY, new long[0], false);
    }

    @JsonIgnore
    public boolean isEmpty(){
        return this.expectedRingTimeUTC == EMPTY;
    }

    @JsonIgnore
    public boolean processed(){
        return this.fromSmartAlarm && this.expectedRingTimeUTC != this.actualRingTimeUTC;
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
        return Objects.equal(convertedObject.expectedRingTimeUTC, this.expectedRingTimeUTC) &&
                Objects.equal(convertedObject.fromSmartAlarm, this.fromSmartAlarm);
    }
}
