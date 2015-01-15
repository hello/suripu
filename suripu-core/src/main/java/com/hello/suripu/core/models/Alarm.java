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

    @JsonProperty("smart")
    public final boolean isSmart;

    /*
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
        // TODO: find out why some alarms don't have a default sound
        if(sound == null) {
            this.sound = new AlarmSound(1, "Digital 3");
        } else {
            this.sound = sound;
        }

        this.year = year;
        this.month = month;
        this.day = day;

        this.isEnabled = isEnabled;
        this.isEditable = isEditable;

        this.isSmart = true;

        for(final Integer d:dayOfWeek){
            if(d < DateTimeConstants.MONDAY || d > DateTimeConstants.SUNDAY){
                throw new IllegalArgumentException("Invalid day of week.");
            }
        }
    }
    */

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
                 @JsonProperty("smart") boolean isSmart,
                 @JsonProperty("sound") final AlarmSound sound){
        this.dayOfWeek = dayOfWeek;
        this.hourOfDay = hourOfDay;
        this.minuteOfHour = minuteOfHour;
        this.isRepeated = isRepeated;
        // TODO: find out why some alarms don't have a default sound
        if(sound == null) {
            this.sound = new AlarmSound(1, "Digital 3");
        } else {
            this.sound = sound;
        }

        this.year = year;
        this.month = month;
        this.day = day;

        this.isEnabled = isEnabled;
        this.isEditable = isEditable;

        this.isSmart = isSmart;

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

        if(!this.isRepeated) {
            return Objects.equal(convertedObject.year, this.year) &&
                    Objects.equal(convertedObject.month, this.month) &&
                    Objects.equal(convertedObject.day, this.day) &&
                    Objects.equal(convertedObject.hourOfDay, this.hourOfDay) &&
                    Objects.equal(convertedObject.minuteOfHour, this.minuteOfHour) &&
                    Objects.equal(convertedObject.sound, this.sound);
        }else{
            return Objects.equal(convertedObject.hourOfDay, this.hourOfDay) &&
                    Objects.equal(convertedObject.minuteOfHour, this.minuteOfHour) &&
                    Iterables.elementsEqual(this.dayOfWeek, convertedObject.dayOfWeek) &&
                    Objects.equal(convertedObject.sound, this.sound);
        }
    }

    @Override
    public String toString(){
        final StringBuilder builder = new StringBuilder(100);
        builder.append(this.year).append("-")
                .append(this.month).append("-")
                .append(this.day).append(" ")
                .append(this.hourOfDay).append(":")
                .append(this.minuteOfHour).append(":")
                .append("00");
        return builder.toString();
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
        private boolean isSmart;
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

        @JsonProperty("smart")
        public Builder withIsSmart(boolean isSmart){
            this.isSmart = isSmart;
            return this;
        }


        public Alarm build(){
            return new Alarm(this.year, this.month, this.day, this.hourOfDay, this.minuteOfHour, this.dayOfWeek,
                    this.isRepeated,
                    this.isEnabled,
                    this.isEditable,
                    this.isSmart,
                    this.sound);
        }



    }


    public static class Utils{

        /**
         * Make sure there is only one alarm per user per day
         * @param alarms
         * @return
         */
        public static boolean isValidAlarms(final List<Alarm> alarms, final DateTime now, final DateTimeZone timeZone){
            final Set<Integer> alarmDays = new HashSet<Integer>();
            for(final Alarm alarm: alarms){
                if(!alarm.isRepeated){
                    if (!isValidNoneRepeatedAlarm(alarm)) {
                        return false;
                    }

                    if(alarm.isSmart){
                        final DateTime expectedRingTime = new DateTime(alarm.year, alarm.month, alarm.day, alarm.hourOfDay, alarm.minuteOfHour, timeZone);
                        if(alarmDays.contains(expectedRingTime.getDayOfWeek())){
                            return false;
                        }

                        alarmDays.add(expectedRingTime.getDayOfWeek());
                    }
                }else{
                    if(!alarm.isSmart) {
                        continue;
                    }

                    for (final Integer dayOfWeek : alarm.dayOfWeek) {
                        if (alarmDays.contains(dayOfWeek)) {
                            return false;
                        }

                        alarmDays.add(dayOfWeek);
                    }

                }
            }

            return true;
        }

        public static boolean isValidNoneRepeatedAlarm(final Alarm alarm){
            if(alarm.isRepeated){
                return true;
            }

            try{
                final DateTime validDateTime = new DateTime(alarm.year, alarm.month, alarm.day, alarm.hourOfDay, alarm.minuteOfHour, DateTimeZone.UTC);
            }catch (Exception ex){
                return false;
            }

            return true;
        }

        public static boolean isAlarmExpired(final Alarm alarm, final DateTime currentTime, final DateTimeZone timeZone){
            if(alarm.isRepeated){
                return false;
            }

            final DateTime ringTime = new DateTime(alarm.year, alarm.month, alarm.day, alarm.hourOfDay, alarm.minuteOfHour, 0, timeZone);
            return ringTime.isBefore(currentTime);
        }

        public static List<Alarm> disableExpiredNoneRepeatedAlarms(final List<Alarm> alarms, long currentTimestampUTC, final DateTimeZone timeZone){
            final ArrayList<Alarm> newAlarmList = new ArrayList<>();
            for(final Alarm alarm:alarms){

                if(isAlarmExpired(alarm, new DateTime(currentTimestampUTC, DateTimeZone.UTC), timeZone)){
                    final Alarm disabledAlarm = new Alarm(alarm.year, alarm.month, alarm.day, alarm.hourOfDay, alarm.minuteOfHour,
                            new HashSet<Integer>(),
                            false,
                            false,
                            alarm.isEditable,
                            alarm.isSmart,
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
        public static RingTime generateNextRingTimeFromAlarmTemplates(final List<Alarm> alarms, long currentTimestampUTC, final DateTimeZone timeZone){

            if(!isValidAlarms(alarms, new DateTime(currentTimestampUTC, DateTimeZone.UTC), timeZone)){
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
                            possibleRings.add(new RingTime(ringTime.getMillis(), ringTime.getMillis(), alarm.sound.id, alarm.isSmart));
                        }
                    }
                }else{
                    // None repeated alarm, check if still valid
                    final DateTime ringTime = new DateTime(alarm.year, alarm.month, alarm.day, alarm.hourOfDay, alarm.minuteOfHour, 0, timeZone);
                    if(ringTime.isAfter(currentLocalTime)){
                        possibleRings.add(new RingTime(ringTime.getMillis(), ringTime.getMillis(), alarm.sound.id, alarm.isSmart));
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

        public static final String getSoundPathFromSoundId(final int soundId){
            switch (soundId){
                case 0:
                case 1:
                case 2:
                case 3:
                    return "/RINGTONE/DIGO00" + String.valueOf(soundId) + ".raw";
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                    return "/RINGTONE/DIG00" + String.valueOf(soundId - 3) + ".raw";
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                    return "/RINGTONE/NEW00" + String.valueOf(soundId - 8) + ".raw";
                case 15:
                case 16:
                case 17:
                case 18:
                    return "/RINGTONE/ORG00" + String.valueOf(soundId - 14) + ".raw";

            }

            return "/RINGTONE/DIG001.raw";
        }
    }
}
