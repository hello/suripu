package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * Created by pangwu on 9/16/14.
 */
public class Alarm {

    @JsonProperty("year")
    public final int year;

    @JsonProperty("month")
    public final int month;

    @JsonProperty("day_of_month")
    public final int day;

    @JsonProperty("hour")
    public final int hourOfDay;

    @JsonProperty("minute")
    public final int minuteOfHour;

    @JsonProperty("repeated")
    public final boolean isRepeated;

    @JsonProperty("day_of_week")
    public final int dayOfWeek;

    @JsonProperty("sound_id")
    public final long soundId;

    @JsonCreator
    public Alarm(@JsonProperty("year") int year,
                 @JsonProperty("month") int month,
                 @JsonProperty("day_of_month") int day,
                 @JsonProperty("hour")int hourOfDay,
                 @JsonProperty("minute") int minuteOfHour,
                 @JsonProperty("day_of_week") int dayOfWeek,
                 @JsonProperty("repeated") boolean isRepeated,
                 @JsonProperty("sound_id") long soundId){
        this.dayOfWeek = dayOfWeek;
        this.hourOfDay = hourOfDay;
        this.minuteOfHour = minuteOfHour;
        this.isRepeated = isRepeated;

        this.soundId = soundId;

        this.year = year;
        this.month = month;
        this.day = day;
    }

    @Override
    public boolean equals(final Object other){
        if (other == null){
            return false;
        }

        if (getClass() != other.getClass()){
            return false;
        }

        final Alarm convertedObject = (Alarm) other;
        return Objects.equal(convertedObject.year, this.year) &&
                Objects.equal(convertedObject.month, this.month) &&
                Objects.equal(convertedObject.day, this.day) &&
                Objects.equal(convertedObject.hourOfDay, this.hourOfDay) &&
                Objects.equal(convertedObject.minuteOfHour, this.minuteOfHour) &&
                Objects.equal(convertedObject.isRepeated, this.isRepeated) &&
                Objects.equal(convertedObject.soundId, this.soundId) &&
                Objects.equal(convertedObject.dayOfWeek, this.dayOfWeek);
    }
}
