package com.hello.suripu.core.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 3/16/15.
 */
public class DateTimeUtilTest {

    @Test
    public void testDateToYmdString(){
        final DateTime dateTime = new DateTime(2015, 3, 14, 0, 0, 0, DateTimeZone.UTC);
        final String actual = DateTimeUtil.dateToYmdString(dateTime);
        assertThat(actual, is("2015-03-14"));
    }

    @Test
    public void testGetTargetDateLocalUTCFromLocalTimeBeforeAnotherVirtualDayStarts(){
        final DateTimeZone dateTimeZone = DateTimeZone.forID("America/Los_Angeles");
        final DateTime dateTime = new DateTime(2015, 3, 14, 7 + 12, 12, 0, dateTimeZone);
        final DateTime targetDateLocalUTC = DateTimeUtil.getTargetDateLocalUTCFromLocalTime(dateTime);
        assertThat(targetDateLocalUTC, is(new DateTime(2015, 3, 13, 0, 0, DateTimeZone.UTC)));
    }

    @Test
    public void testGetTargetDateLocalUTCFromLocalTimeAfterAnotherVirtualDayStarts(){
        final DateTimeZone dateTimeZone = DateTimeZone.forID("America/Los_Angeles");
        final DateTime dateTime = new DateTime(2015, 3, 14, 9 + 12, 12, 0, dateTimeZone);
        final DateTime targetDateLocalUTC = DateTimeUtil.getTargetDateLocalUTCFromLocalTime(dateTime);
        assertThat(targetDateLocalUTC, is(new DateTime(2015, 3, 14, 0, 0, DateTimeZone.UTC)));
    }

    @Test
    public void testGetTargetDateLocalUTCFromLocalTimeAfterAnotherActualDay(){
        final DateTimeZone dateTimeZone = DateTimeZone.forID("America/Los_Angeles");
        final DateTime dateTime = new DateTime(2015, 3, 14, 1, 12, 0, dateTimeZone);
        final DateTime targetDateLocalUTC = DateTimeUtil.getTargetDateLocalUTCFromLocalTime(dateTime);
        assertThat(targetDateLocalUTC, is(new DateTime(2015, 3, 13, 0, 0, DateTimeZone.UTC)));
    }

    @Test
    public void testSanitizeDateTimeSenseThinksIts2016() {
        // This attempts to simulate the bug where Sense is off by 5 months
        final DateTime ref = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
        final DateTime sampleTime = ref.plusMonths(DateTimeUtil.MONTH_OFFSET_FOR_CLOCK_BUG);
        assertThat(DateTimeUtil.possiblySanitizeSampleTime(ref, sampleTime, 2), equalTo(ref));
    }

    @Test
    public void testSanitizeDateTimeSense() {
        // This attempts to simulate the bug where Sense is off by 5 months on a specific day
        final DateTime ref = new DateTime(2015,10,1,0,0,0, DateTimeZone.UTC);
        final DateTime sampleTime = new DateTime(2016,4,1,0,0,0,DateTimeZone.UTC);
        assertThat(DateTimeUtil.possiblySanitizeSampleTime(ref, sampleTime, 2), equalTo(ref));
    }

    @Test
    public void testSanitizeDateTimeSenseClockIsWayTooSkewed() {
        // Clock is way too skewed, we expect the sample time to be untouched
        final DateTime ref = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
        final DateTime sampleTime = ref.plusMonths(10);
        assertThat(DateTimeUtil.possiblySanitizeSampleTime(ref, sampleTime, 2), equalTo(sampleTime));
    }

    @Test
    public void testSanitizeDateTimeClockTooSkewed() {
        // Clock is too skewed, we expect the sample time to be untouched
        final DateTime ref = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
        final DateTime sampleTime = ref.plusHours(3);
        assertThat(DateTimeUtil.possiblySanitizeSampleTime(ref, sampleTime, 2), equalTo(sampleTime));
    }

    @Test
    public void testSanitizeDateTimeClockSlightlySkewed() {
        // Clock is slightly skewed, we expect the sample time to be untouched
        final DateTime ref = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
        final DateTime sampleTime = ref.plusHours(1);
        assertThat(DateTimeUtil.possiblySanitizeSampleTime(ref, sampleTime, 2), equalTo(sampleTime));
    }

    @Test
    public void testSanitizeDateTimeClockNegativeSkew() {
        // Clock is slightly skewed, we expect the sample time to be untouched
        final DateTime ref = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay();
        final DateTime sampleTime = ref.minusHours(1);
        assertThat(DateTimeUtil.possiblySanitizeSampleTime(ref, sampleTime, 2), equalTo(sampleTime));
    }

    @Test
    public void testDateTimesForStartOfMonthBetweenDatesSameMonth() {
        final DateTime start = new DateTime(2015, 10, 13, 1, 1);
        final DateTime end = new DateTime(2015, 10, 14, 1, 1);
        final List<DateTime> results = DateTimeUtil.dateTimesForStartOfMonthBetweenDates(start, end);
        assertThat(results.size(), is(1));
        assertThat(results.get(0), is(new DateTime(2015, 10, 1, 0, 0)));
    }

    @Test
    public void testDateTimesForStartOfMonthBetweenDatesTwoMonths() {
        final DateTime start = new DateTime(2015, 10, 13, 1, 1);
        final DateTime end = new DateTime(2015, 11, 10, 1, 1);
        final List<DateTime> results = DateTimeUtil.dateTimesForStartOfMonthBetweenDates(start, end);
        assertThat(results.size(), is(2));
        assertThat(results.get(0), is(new DateTime(2015, 10, 1, 0, 0)));
        assertThat(results.get(1), is(new DateTime(2015, 11, 1, 0, 0)));
    }

    @Test
    public void testDateTimesForStartOfMonthBetweenDatesEndDateAtStartOfMonth() {
        final DateTime start = new DateTime(2015, 10, 13, 1, 1);
        final DateTime end = new DateTime(2015, 11, 1, 0, 0);
        final List<DateTime> results = DateTimeUtil.dateTimesForStartOfMonthBetweenDates(start, end);
        assertThat(results.size(), is(2));
        assertThat(results.get(0), is(new DateTime(2015, 10, 1, 0, 0)));
        assertThat(results.get(1), is(new DateTime(2015, 11, 1, 0, 0)));
    }

    @Test
    public void testDateTimesForStartOfMonthBetweenDatesThreeMonths() {
        final DateTime start = new DateTime(2015, 10, 13, 1, 1);
        final DateTime end = new DateTime(2015, 12, 1, 0, 0);
        final List<DateTime> results = DateTimeUtil.dateTimesForStartOfMonthBetweenDates(start, end);
        assertThat(results.size(), is(3));
        assertThat(results.get(0), is(new DateTime(2015, 10, 1, 0, 0)));
        assertThat(results.get(1), is(new DateTime(2015, 11, 1, 0, 0)));
        assertThat(results.get(2), is(new DateTime(2015, 12, 1, 0, 0)));
    }
}
