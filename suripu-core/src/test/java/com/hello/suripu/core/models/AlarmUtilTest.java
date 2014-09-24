package com.hello.suripu.core.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 9/24/14.
 */
public class AlarmUtilTest {

    @Test
    public void testValidAlarms(){
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final Alarm.Builder builder = new Alarm.Builder();

        HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.MONDAY);

        builder.withYear(2014)
                .withMonth(9)
                .withDay(15)
                .withDayOfWeek(dayOfWeek)
                .withHour(0)
                .withMinute(1)
                .withIsRepeated(true)
                .withIsEnabled(true)
                .withIsEditable(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        builder.withYear(2014)
                .withMonth(9)
                .withDay(16)
                .withDayOfWeek(dayOfWeek)
                .withHour(0)
                .withMinute(1)
                .withIsRepeated(false)
                .withIsEnabled(true)
                .withIsEditable(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        assertThat(Alarm.Utils.isValidSmartAlarms(alarmList), is(true));
    }

    @Test
    public void testInvalidAlarms(){
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final Alarm.Builder builder = new Alarm.Builder();

        HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.MONDAY);

        builder.withYear(2014)
                .withMonth(9)
                .withDay(15)
                .withDayOfWeek(dayOfWeek)
                .withHour(0)
                .withMinute(1)
                .withIsRepeated(true)
                .withIsEnabled(true)
                .withIsEditable(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.MONDAY);

        builder.withYear(2014)
                .withMonth(9)
                .withDay(15)
                .withDayOfWeek(dayOfWeek)
                .withHour(1)
                .withMinute(1)
                .withIsRepeated(false)
                .withIsEnabled(true)
                .withIsEditable(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        assertThat(Alarm.Utils.isValidSmartAlarms(alarmList), is(false));
    }

    @Test
    public void testGetNextRingTimeBasedOnRepeatedAlarmList(){
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final Alarm.Builder builder = new Alarm.Builder();

        HashSet<Integer> dayOfWeek = new HashSet<Integer>();


        final DateTime alarmTime = DateTime.now().withSecondOfMinute(0).withMillisOfSecond(0);
        dayOfWeek.add(alarmTime.getDayOfWeek());
        builder.withYear(alarmTime.getYear())
                .withMonth(alarmTime.getMonthOfYear())
                .withDay(alarmTime.getDayOfMonth())
                .withDayOfWeek(dayOfWeek)
                .withHour(alarmTime.getHourOfDay())
                .withMinute(alarmTime.getMinuteOfHour())
                .withIsRepeated(true)
                .withIsEnabled(true)
                .withIsEditable(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        RingTime actualRingTime = Alarm.Utils.getNextRingTimestamp(alarmList, DateTime.now().minusMinutes(2).getMillis(), DateTimeZone.getDefault());
        assertThat(actualRingTime.isEmpty(), is(false));
        assertThat(actualRingTime.isRegular(), is(true));
        assertThat(actualRingTime.isSmart(), is(false));
        assertThat(actualRingTime.actualRingTimeUTC, is(alarmTime.getMillis()));

        // Test current time is after alarm time.
        actualRingTime = Alarm.Utils.getNextRingTimestamp(alarmList, DateTime.now().plusMinutes(2).getMillis(), DateTimeZone.getDefault());
        assertThat(actualRingTime.isEmpty(), is(false));
        assertThat(actualRingTime.isRegular(), is(true));
        assertThat(actualRingTime.isSmart(), is(false));
        assertThat(actualRingTime.actualRingTimeUTC, is(alarmTime.plusWeeks(1).getMillis()));
    }


    @Test
    public void testGetNextRingTimeBasedOnNoneRepeatedAlarmList(){
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final Alarm.Builder builder = new Alarm.Builder();

        HashSet<Integer> dayOfWeek = new HashSet<Integer>();


        final DateTime alarmTime = DateTime.now().withSecondOfMinute(0).withMillisOfSecond(0);
        dayOfWeek.add(alarmTime.getDayOfWeek());
        builder.withYear(alarmTime.getYear())
                .withMonth(alarmTime.getMonthOfYear())
                .withDay(alarmTime.getDayOfMonth())
                .withDayOfWeek(dayOfWeek)
                .withHour(alarmTime.getHourOfDay())
                .withMinute(alarmTime.getMinuteOfHour())
                .withIsRepeated(false)
                .withIsEnabled(true)
                .withIsEditable(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        RingTime actualRingTime = Alarm.Utils.getNextRingTimestamp(alarmList, DateTime.now().minusMinutes(2).getMillis(), DateTimeZone.getDefault());
        assertThat(actualRingTime.isEmpty(), is(false));
        assertThat(actualRingTime.isRegular(), is(true));
        assertThat(actualRingTime.isSmart(), is(false));
        assertThat(actualRingTime.actualRingTimeUTC, is(alarmTime.getMillis()));

        // Test current time is after alarm time.
        actualRingTime = Alarm.Utils.getNextRingTimestamp(alarmList, DateTime.now().plusMinutes(2).getMillis(), DateTimeZone.getDefault());
        assertThat(actualRingTime.isEmpty(), is(true));
        assertThat(actualRingTime.isRegular(), is(false));
        assertThat(actualRingTime.isSmart(), is(false));
    }


    @Test
    public void testGetNextRingTimeBasedOnDisabledAlarmList(){
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final Alarm.Builder builder = new Alarm.Builder();

        HashSet<Integer> dayOfWeek = new HashSet<Integer>();


        final DateTime alarmTime = DateTime.now().withSecondOfMinute(0).withMillisOfSecond(0);
        dayOfWeek.add(alarmTime.getDayOfWeek());
        builder.withYear(alarmTime.getYear())
                .withMonth(alarmTime.getMonthOfYear())
                .withDay(alarmTime.getDayOfMonth())
                .withDayOfWeek(dayOfWeek)
                .withHour(alarmTime.getHourOfDay())
                .withMinute(alarmTime.getMinuteOfHour())
                .withIsRepeated(false)
                .withIsEnabled(false)
                .withIsEditable(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        final RingTime actualRingTime = Alarm.Utils.getNextRingTimestamp(alarmList, DateTime.now().minusMinutes(2).getMillis(), DateTimeZone.getDefault());
        assertThat(actualRingTime.isEmpty(), is(true));
        assertThat(actualRingTime.isRegular(), is(false));
        assertThat(actualRingTime.isSmart(), is(false));

    }
}
