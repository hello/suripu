package com.hello.suripu.core.models;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

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

    @JsonProperty("expansions")
    public final List<AlarmExpansion> expansions;

    @JsonCreator
    public RingTime(@JsonProperty("actual_ring_time_utc") long actual,
                    @JsonProperty("expected_ring_time_utc") long expected,
                    @JsonProperty("sound_ids") final long[] soundIds,
                    @JsonProperty("from_smart_alarm") final boolean fromSmartAlarm,
                    @JsonProperty("expansions") final List<AlarmExpansion> expansions){
        if(expected < actual){
            throw new IllegalArgumentException("Actual ring behind deadline.");
        }

        this.actualRingTimeUTC = actual;
        this.expectedRingTimeUTC = expected;
        this.soundIds = soundIds;
        this.fromSmartAlarm = fromSmartAlarm;
        this.expansions = expansions;

    }

    public RingTime(long actual, long expected, final Long[] soundIds, final boolean fromSmartAlarm, final List<AlarmExpansion> expansions){
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
        this.expansions = expansions;

    }
    public RingTime(long actual, long expected, final long[] soundIds, final boolean fromSmartAlarm){
        this(actual, expected, soundIds, fromSmartAlarm, Lists.newArrayList());
    }

    public RingTime(long actual, long expected, long soundId, final boolean fromSmartAlarm, final List<AlarmExpansion> expansions){
        if(expected < actual){
            throw new IllegalArgumentException("Actual ring behind deadline.");
        }

        this.actualRingTimeUTC = actual;
        this.expectedRingTimeUTC = expected;
        this.soundIds = new long[]{ soundId };
        this.fromSmartAlarm = fromSmartAlarm;
        this.expansions = expansions;

    }

    public RingTime(long actual, long expected, long soundId, final boolean fromSmartAlarm){
        this(actual, expected, soundId, fromSmartAlarm, Lists.newArrayList());
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

    @Override
    public String toString() {
        return Objects.toStringHelper(RingTime.class)
                .add("acualRingTimeUTC", new DateTime(actualRingTimeUTC, DateTimeZone.UTC))
                .add("expectedRingTimeUTC", new DateTime(expectedRingTimeUTC, DateTimeZone.UTC))
                .add("soundIds", soundIds)
                .add("fromSmartAlarm", fromSmartAlarm)
                .add("expansions", expansions)
                .toString();
    }
}
