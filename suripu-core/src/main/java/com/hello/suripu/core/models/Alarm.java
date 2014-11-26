package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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

        for(final Integer d:dayOfWeek){
            if(d < DateTimeConstants.MONDAY || d > DateTimeConstants.SUNDAY){
                throw new IllegalArgumentException("Invalid day of week.");
            }
        }
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


    public static class Utils{

        /**
         * Make sure there is only one alarm per user per day
         * @param alarms
         * @return
         */
        public static boolean isValidSmartAlarms(final List<Alarm> alarms){
            final Set<Integer> alarmDays = new HashSet<Integer>();
            for(final Alarm alarm: alarms){
                for(final Integer dayOfWeek:alarm.dayOfWeek) {
                    if (alarmDays.contains(dayOfWeek)) {
                        return false;
                    } else {
                        alarmDays.add(dayOfWeek);
                    }
                }
            }

            return true;
        }

        public static List<Alarm> disableExpiredNoneRepeatedAlarms(final List<Alarm> alarms, long currentTimestampUTC, final DateTimeZone timeZone){
            final ArrayList<Alarm> newAlarmList = new ArrayList<>();
            for(final Alarm alarm:alarms){
                if(alarm.isRepeated){
                    newAlarmList.add(alarm);
                }

                final DateTime ringTime = new DateTime(alarm.year, alarm.month, alarm.day, alarm.hourOfDay, alarm.minuteOfHour, 0, timeZone);
                if(ringTime.isBefore(currentTimestampUTC)){
                    final Alarm disabledAlarm = new Alarm(alarm.year, alarm.month, alarm.day, alarm.hourOfDay, alarm.minuteOfHour,
                            new HashSet<Integer>(),
                            false,
                            false,
                            alarm.isEditable,
                            alarm.sound);
                    newAlarmList.add(disabledAlarm);
                }else{
                    newAlarmList.add(alarm);
                }
            }

            return ImmutableList.copyOf(newAlarmList);
        }

        /**
         * Computes the next moment at which the alarm should ring
         * @param alarms list of alarm templates
         * @param currentTimestampUTC - current moment
         * @param timeZone - time zone of the user's Sense
         * @return
         */
        public static RingTime getNextRingTime(final List<Alarm> alarms, long currentTimestampUTC, final DateTimeZone timeZone){

            if(!isValidSmartAlarms(alarms)){
                throw new IllegalArgumentException("Invalid alarms.");
            }

            final ArrayList<RingTime> possibleRings = new ArrayList<RingTime>();
            final DateTime currentLocalTime = new DateTime(currentTimestampUTC, timeZone);
            for(final Alarm alarm:alarms){
                if(!alarm.isEnabled){
                    continue;
                }

                if(alarm.isRepeated){
                    for(final Integer dayOfWeek:alarm.dayOfWeek){
                        int dayDifference = dayOfWeek - currentLocalTime.getDayOfWeek();
                        DateTime ringTime = currentLocalTime.withTimeAtStartOfDay().plusDays(dayDifference).plusHours(alarm.hourOfDay).plusMinutes(alarm.minuteOfHour);
                        if(ringTime.isBefore(currentLocalTime)){
                            // this alarm should be in next week.
                            ringTime = ringTime.plusWeeks(1);
                        }

                        if(ringTime.isAfter(currentLocalTime)) {
                            possibleRings.add(new RingTime(ringTime.getMillis(), ringTime.getMillis(), alarm.sound.id));
                        }
                    }
                }else{
                    // None repeated alarm, check if still valid
                    final DateTime ringTime = new DateTime(alarm.year, alarm.month, alarm.day, alarm.hourOfDay, alarm.minuteOfHour, 0, timeZone);
                    if(ringTime.isAfter(currentLocalTime)){
                        possibleRings.add(new RingTime(ringTime.getMillis(), ringTime.getMillis(), alarm.sound.id));
                    }
                }
            }

            final RingTime[] rings = possibleRings.toArray(new RingTime[0]);
            Arrays.sort(rings, new Comparator<RingTime>() {
                @Override
                public int compare(final RingTime o1, final RingTime o2) {
                    return Long.valueOf(o1.actualRingTimeUTC).compareTo(Long.valueOf(o2.actualRingTimeUTC));
                }
            });

            if(rings.length > 0){
                return rings[0];
            }

            return RingTime.createEmpty();
        }
    }
}
