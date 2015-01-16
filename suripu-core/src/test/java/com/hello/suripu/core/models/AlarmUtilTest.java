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
        final Alarm.Utils.AlarmVerificationResult result = Alarm.Utils.isValidAlarms(alarmList, DateTime.now(), DateTimeZone.getDefault());
        assertThat(result, is(Alarm.Utils.AlarmVerificationResult.OK));
    }

    @Test
    public void testIsExpired(){
        final HashSet<Integer> dayOfWeek =  new HashSet<>();
        dayOfWeek.add(DateTime.now().getDayOfWeek());
        final Alarm repeated = new Alarm(0,0,0,7,30,dayOfWeek,true, true, true, true, new AlarmSound(0, "god save the queen"));
        assertThat(Alarm.Utils.isAlarmExpired(repeated, DateTime.now(), DateTimeZone.getDefault()), is(false));

        final DateTime now = DateTime.now().withMillis(0);
        Alarm nonRepeated = new Alarm(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), now.getHourOfDay(), now.getMinuteOfHour(), dayOfWeek, false, true, true, true, new AlarmSound(0, "god save the queen"));
        assertThat(Alarm.Utils.isAlarmExpired(nonRepeated, now, DateTimeZone.getDefault()), is(false));

        final DateTime future = now.plusHours(1);
        nonRepeated = new Alarm(future.getYear(), future.getMonthOfYear(), future.getDayOfMonth(), future.getHourOfDay(), future.getMinuteOfHour(), dayOfWeek, false, true, true,true,  new AlarmSound(0, "god save the queen"));
        assertThat(Alarm.Utils.isAlarmExpired(nonRepeated, now, DateTimeZone.getDefault()), is(false));

        final DateTime past = now.minusHours(1);
        nonRepeated = new Alarm(past.getYear(), past.getMonthOfYear(), past.getDayOfMonth(), past.getHourOfDay(), past.getMinuteOfHour(), dayOfWeek, false, true, true, true, new AlarmSound(0, "god save the queen"));
        assertThat(Alarm.Utils.isAlarmExpired(nonRepeated, now, DateTimeZone.getDefault()), is(true));


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
                .withIsSmart(true)
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
                .withIsSmart(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        final DateTime now = new DateTime(2014,9,15,0,0,DateTimeZone.getDefault());

        final Alarm.Utils.AlarmVerificationResult result = Alarm.Utils.isValidAlarms(alarmList, now, DateTimeZone.getDefault());
        assertThat(result, is(Alarm.Utils.AlarmVerificationResult.SMART_ALARM_ALREADY_SET));

        alarmList.clear();
        // Non repeated expired

        builder.withYear(2014)
                .withMonth(9)
                .withDay(15)
                .withDayOfWeek(dayOfWeek)
                .withHour(0)
                .withMinute(1)
                .withIsRepeated(true)
                .withIsEnabled(true)
                .withIsEditable(true)
                .withIsSmart(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.SUNDAY);

        builder.withYear(2014)
                .withMonth(9)
                .withDay(14)
                .withDayOfWeek(dayOfWeek)
                .withHour(1)
                .withMinute(1)
                .withIsRepeated(false)
                .withIsEnabled(true)
                .withIsEditable(true)
                .withIsSmart(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());
        Alarm.Utils.AlarmVerificationResult result1 = Alarm.Utils.isValidAlarms(alarmList, now, DateTimeZone.getDefault());
        assertThat(result1, is(Alarm.Utils.AlarmVerificationResult.OK));

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
                .withIsSmart(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        RingTime actualRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplates(alarmList, DateTime.now().minusMinutes(2).getMillis(), DateTimeZone.getDefault());
        assertThat(actualRingTime.isEmpty(), is(false));
        assertThat(actualRingTime.processed(), is(false));
        assertThat(actualRingTime.actualRingTimeUTC, is(alarmTime.getMillis()));

        // Test current time is after alarm time.
        actualRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplates(alarmList, DateTime.now().plusMinutes(2).getMillis(), DateTimeZone.getDefault());
        assertThat(actualRingTime.isEmpty(), is(false));
        assertThat(actualRingTime.processed(), is(false));
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
                .withIsSmart(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        RingTime actualRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplates(alarmList, DateTime.now().minusMinutes(2).getMillis(), DateTimeZone.getDefault());
        assertThat(actualRingTime.isEmpty(), is(false));
        assertThat(actualRingTime.processed(), is(false));
        assertThat(actualRingTime.actualRingTimeUTC, is(alarmTime.getMillis()));

        // Test current time is after alarm time.
        actualRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplates(alarmList, DateTime.now().plusMinutes(2).getMillis(), DateTimeZone.getDefault());
        assertThat(actualRingTime.isEmpty(), is(true));
        assertThat(actualRingTime.processed(), is(false));
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
                .withIsSmart(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        final RingTime actualRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplates(alarmList, DateTime.now().minusMinutes(2).getMillis(), DateTimeZone.getDefault());
        assertThat(actualRingTime.isEmpty(), is(true));
        assertThat(actualRingTime.processed(), is(false));

    }
}
