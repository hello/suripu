package com.hello.suripu.core.models;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
    public void testGetNextRingTimeBasedClockChanges(){

        // Use case:
        // Someone sets an alarm the day before DST changes
        // User experienced the alarm ringing an hour earlier than expected.
        // Possible cause, the alarm timestamp is in UTC and not taking into account FUTURE dst change
        // Possible solutions:
        // + check if we are on a DST boundary (today is not and next scheduled alarm is)
        // + check if we are on a opposite DST boundary (today is DST and next scheduled alarm is not)
        // + compute the UTC timestamp based on the local time / timezone of the user
        // + ???

        final List<Alarm> alarmList = Lists.newArrayList();
        final Alarm.Builder builder = new Alarm.Builder();

        final DateTimeZone amsterdam = DateTimeZone.forID("Europe/Amsterdam");

        final DateTime alarmTime = new DateTime(2015,10,25, 6,20,0, amsterdam).withMillisOfSecond(0);


        builder.withDayOfWeek(Sets.newHashSet(DateTimeConstants.SUNDAY, DateTimeConstants.MONDAY, DateTimeConstants.SATURDAY))
                .withHour(alarmTime.getHourOfDay())
                .withMinute(alarmTime.getMinuteOfHour())
                .withIsRepeated(true)
                .withIsEnabled(true)
                .withIsEditable(true)
                .withIsSmart(false)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        final DateTime saturdayNight = new DateTime(2015,10,24,23,0,0, amsterdam);

        final RingTime actualRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList, saturdayNight.getMillis(), amsterdam);
        assertThat(actualRingTime.isEmpty(), is(false));
        assertThat(actualRingTime.processed(), is(false));
        assertThat(actualRingTime.actualRingTimeUTC, is(alarmTime.getMillis()));


        // Daylight Saving Time (United States) 2016 begins at 2:00 AM on
        // Sunday, March 13
        // and ends at 2:00 AM on
        // Sunday, November 6


        // Where in the world
        final DateTimeZone pacific = DateTimeZone.forID("America/Los_Angeles");


        // DST OFF 2015 -
        // when alarm is created
        final DateTime wednesdayMorning = new DateTime(2015,10,28,10,53,0, pacific);

        // when alarm is supposed to ring
        final DateTime alarmTimeForNextSundayWithDSTChanges = new DateTime(2015,11,1,7,0,0, pacific);
        final Alarm alarmForNextSundayWithDSTChange  = builder
                .withDayOfWeek(Sets.newHashSet(DateTimeConstants.SUNDAY))
                .withHour(alarmTimeForNextSundayWithDSTChanges.getHourOfDay())
                .withMinute(alarmTimeForNextSundayWithDSTChanges.getMinuteOfHour())
                .withIsRepeated(true)
                .build();

        final RingTime computedRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(Lists.newArrayList(alarmForNextSundayWithDSTChange), wednesdayMorning.getMillis(), pacific);
        assertThat(!computedRingTime.isEmpty(), is(true));
        assertThat(computedRingTime.processed(), is(false));
        assertThat(computedRingTime.actualRingTimeUTC, is(alarmTimeForNextSundayWithDSTChanges.getMillis()));


        // DST ON 2016
        final DateTime fridayMorningBeforeDSTOn = new DateTime(2016,3,11,12,0,0, pacific);
        final DateTime firstAlarmTimeAfterDSTOn = new DateTime(2016,3,13,5,5,0, pacific);
        final Alarm firstAlarmAfterDSTOn = builder
                .withDayOfWeek(Sets.newHashSet(DateTimeConstants.SUNDAY))
                .withHour(firstAlarmTimeAfterDSTOn.getHourOfDay())
                .withMinute(firstAlarmTimeAfterDSTOn.getMinuteOfHour())
                .withIsRepeated(true)
                .build();

        final RingTime ringTimeDstOn = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(Lists.newArrayList(firstAlarmAfterDSTOn), fridayMorningBeforeDSTOn.getMillis(), pacific);
        assertThat(!ringTimeDstOn.isEmpty(), is(true));
        assertThat(ringTimeDstOn.processed(), is(false));
        assertThat(ringTimeDstOn.actualRingTimeUTC, is(firstAlarmTimeAfterDSTOn.getMillis()));



        // DST OFF 2016
        final DateTime fridayMorningBeforeDSTOff = new DateTime(2016,11,4,14,0,0, pacific);
        final DateTime firstAlarmTimeAfterDSTOff = new DateTime(2016,11,6,2,5,0, pacific);
        final Alarm firstAlarmAfterDSTOff = builder
                .withDayOfWeek(Sets.newHashSet(DateTimeConstants.SUNDAY))
                .withHour(firstAlarmTimeAfterDSTOff.getHourOfDay())
                .withMinute(firstAlarmTimeAfterDSTOff.getMinuteOfHour())
                .withIsRepeated(true)
                .build();

        final RingTime ringTimeDstOff = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(Lists.newArrayList(firstAlarmAfterDSTOff), fridayMorningBeforeDSTOff.getMillis(), pacific);
        assertThat(!ringTimeDstOff.isEmpty(), is(true));
        assertThat(ringTimeDstOff.processed(), is(false));
        assertThat(ringTimeDstOff.actualRingTimeUTC, is(firstAlarmTimeAfterDSTOff.getMillis()));
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

    @Test
    public void testGetExpansionsAtExpectedTimeWithRepeatedAlarm() {
        final List<Alarm> alarmList = new ArrayList<Alarm>();

        final DateTime alarmTime = DateTime.now().withSecondOfMinute(0).withMillisOfSecond(0);

        HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(alarmTime.getDayOfWeek());

        final AlarmExpansion expansion = new AlarmExpansion(1L, true, "Lights", "HUE", new ValueRange(100, 100));
        final List<AlarmExpansion> expansionsList = Lists.newArrayList(expansion);

        final Alarm.Builder builder = new Alarm.Builder();
        builder.withYear(alarmTime.getYear())
            .withMonth(alarmTime.getMonthOfYear())
            .withDay(alarmTime.getDayOfMonth())
            .withDayOfWeek(dayOfWeek)
            .withHour(alarmTime.getHourOfDay())
            .withMinute(alarmTime.getMinuteOfHour())
            .withIsRepeated(true)
            .withIsEnabled(true)
            .withIsEditable(true)
            .withIsSmart(false)
            .withExpansions(expansionsList)
            .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        final List<AlarmExpansion> alarmExpansions = Alarm.Utils.getExpansionsAtExpectedTime(alarmTime, alarmList);

        assertThat(alarmExpansions.isEmpty(), is(false));
    }

    @Test
    public void testGetExpansionsAtExpectedTimeWithNonRepeatingAlarm() {
        final List<Alarm> alarmList = new ArrayList<Alarm>();

        final DateTime alarmTime = DateTime.now().withSecondOfMinute(0).withMillisOfSecond(0);

        HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(alarmTime.getDayOfWeek());

        final AlarmExpansion expansion = new AlarmExpansion(1L, true, "Lights", "HUE", new ValueRange(100, 100));
        final List<AlarmExpansion> expansionsList = Lists.newArrayList(expansion);

        final Alarm.Builder builder = new Alarm.Builder();
        builder.withYear(alarmTime.getYear())
            .withMonth(alarmTime.getMonthOfYear())
            .withDay(alarmTime.getDayOfMonth())
            .withDayOfWeek(dayOfWeek)
            .withHour(alarmTime.getHourOfDay())
            .withMinute(alarmTime.getMinuteOfHour())
            .withIsRepeated(false)
            .withIsEnabled(true)
            .withIsEditable(true)
            .withIsSmart(false)
            .withExpansions(expansionsList)
            .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        final List<AlarmExpansion> alarmExpansions = Alarm.Utils.getExpansionsAtExpectedTime(alarmTime, alarmList);

        assertThat(alarmExpansions.isEmpty(), is(false));
    }

    @Test
    public void testGetExpansionsAtExpectedTimeNoExpansions() {
        final List<Alarm> alarmList = new ArrayList<Alarm>();

        final DateTime alarmTime = DateTime.now().withSecondOfMinute(0).withMillisOfSecond(0);

        HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(alarmTime.getDayOfWeek());



        final Alarm.Builder builder = new Alarm.Builder();
        builder.withYear(alarmTime.getYear())
            .withMonth(alarmTime.getMonthOfYear())
            .withDay(alarmTime.getDayOfMonth())
            .withDayOfWeek(dayOfWeek)
            .withHour(alarmTime.getHourOfDay())
            .withMinute(alarmTime.getMinuteOfHour())
            .withIsRepeated(false)
            .withIsEnabled(true)
            .withIsEditable(true)
            .withIsSmart(false)
            .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        final List<AlarmExpansion> alarmExpansions = Alarm.Utils.getExpansionsAtExpectedTime(alarmTime, alarmList);

        assertThat(alarmExpansions.isEmpty(), is(true));
    }

    @Test
    public void testGetExpansionsAtExpectedTimeWrongTime() {
        final List<Alarm> alarmList = new ArrayList<Alarm>();

        final DateTime alarmTime = DateTime.now().withSecondOfMinute(0).withMillisOfSecond(0);

        HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(alarmTime.getDayOfWeek());

        final AlarmExpansion expansion = new AlarmExpansion(1L, true, "Lights", "HUE", new ValueRange(100, 100));
        final List<AlarmExpansion> expansionsList = Lists.newArrayList(expansion);

        final Alarm.Builder builder = new Alarm.Builder();
        builder.withYear(alarmTime.getYear())
            .withMonth(alarmTime.getMonthOfYear())
            .withDay(alarmTime.getDayOfMonth())
            .withDayOfWeek(dayOfWeek)
            .withHour(alarmTime.getHourOfDay())
            .withMinute(alarmTime.getMinuteOfHour())
            .withIsRepeated(false)
            .withIsEnabled(true)
            .withIsEditable(true)
            .withIsSmart(false)
            .withExpansions(expansionsList)
            .withAlarmSound(new AlarmSound(1, "god save the queen"));

        alarmList.add(builder.build());

        final List<AlarmExpansion> alarmExpansions = Alarm.Utils.getExpansionsAtExpectedTime(alarmTime.minusMinutes(1), alarmList);

        assertThat(alarmExpansions.isEmpty(), is(true));
    }
}
