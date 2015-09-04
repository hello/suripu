package com.hello.suripu.core.models;

import com.hello.suripu.core.util.AlarmUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 9/24/14.
 */
public class AlarmUtilsTest {

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
        final Alarm.Utils.AlarmStatus status = Alarm.Utils.isValidAlarms(alarmList, DateTime.now(), DateTimeZone.getDefault());
        assertThat(status, is(Alarm.Utils.AlarmStatus.OK));
    }

    @Test
    public void testIsExpired(){
        final HashSet<Integer> dayOfWeek =  new HashSet<>();
        dayOfWeek.add(DateTime.now().getDayOfWeek());

        final Alarm repeated = new Alarm.Builder()
                .withYear(1900).withMonth(12).withDay(1)
                .withHour(7).withMinute(30)
                .withDayOfWeek(dayOfWeek)
                .withIsRepeated(true).withIsEnabled(true).withIsEditable(true).withIsSmart(true)
                .withAlarmSound(new AlarmSound(0, "god save the queen"))
                .withId(UUID.randomUUID().toString())
                .build();

        assertThat(Alarm.Utils.isAlarmExpired(repeated, DateTime.now(), DateTimeZone.getDefault()), is(false));

        final DateTime now = DateTime.now().withMillis(0);
        final Alarm nonRepeated = new Alarm.Builder()
                .withYear(now.getYear()).withMonth(now.getMonthOfYear()).withDay(now.getDayOfMonth())
                .withHour(now.getHourOfDay()).withMinute(now.getMinuteOfHour())
                .withDayOfWeek(dayOfWeek)
                .withIsRepeated(false).withIsEnabled(true).withIsEditable(true).withIsSmart(true)
                .withAlarmSound(new AlarmSound(0, "god save the queen"))
                .withId(UUID.randomUUID().toString())
                .build();

        assertThat(Alarm.Utils.isAlarmExpired(nonRepeated, now, DateTimeZone.getDefault()), is(false));

        final DateTime future = now.plusHours(1);

        final Alarm nonRepeatedFuture = new Alarm.Builder()
                .withYear(future.getYear()).withMonth(future.getMonthOfYear()).withDay(future.getDayOfMonth())
                .withHour(future.getHourOfDay()).withMinute(future.getMinuteOfHour())
                .withDayOfWeek(dayOfWeek)
                .withIsRepeated(false).withIsEnabled(true).withIsEditable(true).withIsSmart(true)
                .withAlarmSound(new AlarmSound(0, "god save the queen"))
                .withId(UUID.randomUUID().toString())
                .build();

        assertThat(Alarm.Utils.isAlarmExpired(nonRepeatedFuture, now, DateTimeZone.getDefault()), is(false));

        final DateTime past = now.minusHours(1);
        final Alarm nonRepeatedPast = new Alarm.Builder()
                .withYear(past.getYear()).withMonth(past.getMonthOfYear()).withDay(past.getDayOfMonth())
                .withHour(past.getHourOfDay()).withMinute(past.getMinuteOfHour())
                .withDayOfWeek(dayOfWeek)
                .withIsRepeated(false).withIsEnabled(true).withIsEditable(true).withIsSmart(true)
                .withAlarmSound(new AlarmSound(0, "god save the queen"))
                .withId(UUID.randomUUID().toString())
                .build();
        assertThat(Alarm.Utils.isAlarmExpired(nonRepeatedPast, now, DateTimeZone.getDefault()), is(true));


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

        final Alarm.Utils.AlarmStatus status = Alarm.Utils.isValidAlarms(alarmList, now, DateTimeZone.getDefault());
        assertThat(status, is(Alarm.Utils.AlarmStatus.SMART_ALARM_ALREADY_SET));

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
        Alarm.Utils.AlarmStatus result1 = Alarm.Utils.isValidAlarms(alarmList, now, DateTimeZone.getDefault());
        assertThat(result1, is(Alarm.Utils.AlarmStatus.OK));

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

        RingTime actualRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList, DateTime.now().minusMinutes(2).getMillis(), DateTimeZone.getDefault());
        assertThat(actualRingTime.isEmpty(), is(false));
        assertThat(actualRingTime.processed(), is(false));
        assertThat(actualRingTime.actualRingTimeUTC, is(alarmTime.getMillis()));

        // Test current time is after alarm time.
        actualRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList, DateTime.now().plusMinutes(2).getMillis(), DateTimeZone.getDefault());
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

        RingTime actualRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList, DateTime.now().minusMinutes(2).getMillis(), DateTimeZone.getDefault());
        assertThat(actualRingTime.isEmpty(), is(false));
        assertThat(actualRingTime.processed(), is(false));
        assertThat(actualRingTime.actualRingTimeUTC, is(alarmTime.getMillis()));

        // Test current time is after alarm time.
        actualRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList, DateTime.now().plusMinutes(2).getMillis(), DateTimeZone.getDefault());
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

        final RingTime actualRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList, DateTime.now().minusMinutes(2).getMillis(), DateTimeZone.getDefault());
        assertThat(actualRingTime.isEmpty(), is(true));
        assertThat(actualRingTime.processed(), is(false));

    }

    @Test
    public void testClockIsGood() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Long clientTimestamp = now.getMillis();
        final boolean ok = AlarmUtils.isWithinReasonableBounds(now, clientTimestamp);
        assertThat(ok, is(true));
    }

    @Test
    public void testClockIsGoodOnlyWithAddedOffset() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Long clientTimestamp = now.plusSeconds(65).getMillis();
        assertThat(AlarmUtils.isWithinReasonableBoundsApproximately(now, clientTimestamp), is(true));
        assertThat(AlarmUtils.isWithinReasonableBounds(now, clientTimestamp), is(false));
        assertThat(AlarmUtils.isWithinReasonableBounds(now, clientTimestamp, 5000), is(true));
    }


    @Test
    public void testClockIsGoodOnlyWithAddedOffsetRedux() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Long clientTimestamp = now.minusSeconds(65).getMillis();
        assertThat(AlarmUtils.isWithinReasonableBoundsApproximately(now, clientTimestamp), is(true));
        assertThat(AlarmUtils.isWithinReasonableBounds(now, clientTimestamp), is(false));
        assertThat(AlarmUtils.isWithinReasonableBounds(now, clientTimestamp, 5000), is(true));
    }

    @Test
    public void testClockIsOutOfSync() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Long clientTimestamp = now.plusSeconds(75).getMillis();
        assertThat(AlarmUtils.isWithinReasonableBoundsApproximately(now, clientTimestamp), is(false));
        assertThat(AlarmUtils.isWithinReasonableBounds(now, clientTimestamp), is(false));
    }
}
