package com.hello.suripu.core.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
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
}
