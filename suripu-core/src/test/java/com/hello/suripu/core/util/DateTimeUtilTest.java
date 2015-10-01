package com.hello.suripu.core.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

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
        final DateTime sampleTime = ref.plusMonths(5);
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
}
