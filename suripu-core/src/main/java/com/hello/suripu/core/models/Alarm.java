package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;

import java.util.Set;

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


    @JsonProperty("enabled")
    public final boolean isEnabled;

    @JsonProperty("editable")
    public final boolean isEditable;


    @JsonProperty("day_of_week")
    public final Set<Integer> dayOfWeek;

    @JsonProperty("sound")
    public final AlarmSound sound;

    @JsonCreator
    public Alarm(@JsonProperty("year") int year,
                 @JsonProperty("month") int month,
                 @JsonProperty("day_of_month") int day,
                 @JsonProperty("hour")int hourOfDay,
                 @JsonProperty("minute") int minuteOfHour,
                 @JsonProperty("day_of_week") final Set<Integer> dayOfWeek,
                 @JsonProperty("repeated") boolean isRepeated,
                 @JsonProperty("enabled") boolean isEnabled,
                 @JsonProperty("editable") boolean isEditable,
                 @JsonProperty("sound") final AlarmSound sound){
        this.dayOfWeek = dayOfWeek;
        this.hourOfDay = hourOfDay;
        this.minuteOfHour = minuteOfHour;
        this.isRepeated = isRepeated;

        this.sound = sound;

        this.year = year;
        this.month = month;
        this.day = day;

        this.isEnabled = isEnabled;
        this.isEditable = isEditable;
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
                Objects.equal(convertedObject.sound, this.sound) &&
                Iterables.elementsEqual(convertedObject.dayOfWeek, this.dayOfWeek);
    }


    public static class Builder{

        private int year;
        private int month;
        private int day;
        private int hourOfDay;
        private int minuteOfHour;
        private boolean isRepeated;
        private boolean isEnabled;
        private boolean isEditable;
        private Set<Integer> dayOfWeek;
        private AlarmSound sound;

        public Builder(){
            sound = null;
        }


        @JsonProperty("year")
        public Builder withYear(int year){
            this.year = year;
            return this;
        }

        @JsonProperty("month")
        public Builder withMonth(int month){
            this.month = month;
            return this;
        }


        @JsonProperty("day_of_month")
        public Builder withDay(int day){
            this.day = day;
            return this;
        }

        @JsonProperty("hour")
        public Builder withHour(int hour){
            this.hourOfDay = hour;
            return this;
        }

        @JsonProperty("minute")
        public Builder withMinute(int minuteOfHour){
            this.minuteOfHour = minuteOfHour;
            return this;
        }

        @JsonProperty("day_of_week")
        public Builder withDayOfWeek(final Set<Integer> dayOfWeek){
            this.dayOfWeek = dayOfWeek;
            return this;
        }

        @JsonProperty("repeated")
        public Builder withIsRepeated(boolean repeated){
            this.isRepeated = repeated;
            return this;
        }

        @JsonProperty("sound")
        public Builder withAlarmSound(final AlarmSound sound){
            this.sound = sound;
            return this;
        }


        @JsonProperty("enabled")
        public Builder withIsEnabled(boolean enabled){
            this.isEnabled = enabled;
            return this;
        }

        @JsonProperty("editable")
        public Builder withIsEditable(boolean editable){
            this.isEditable = editable;
            return this;
        }


        public Alarm build(){
            return new Alarm(this.year, this.month, this.day, this.hourOfDay, this.minuteOfHour, this.dayOfWeek,
                    this.isRepeated,
                    this.isEnabled,
                    this.isEditable,
                    this.sound);
        }
    }
}
