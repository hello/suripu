package com.hello.suripu.core.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

/**
 * Created by jarredheinrich on 2/6/17.
 */
public class SleepPeriodTest {
    @Test
    public void testSleepEventInSleepPeriod(){

        SleepPeriod sleepPeriod = SleepPeriod.night();
        boolean withinSleepPeriod = sleepPeriod.sleepEventInSleepPeriod(DateTime.now(DateTimeZone.UTC).withHourOfDay(0));
        assert(withinSleepPeriod);
        withinSleepPeriod = sleepPeriod.sleepEventInSleepPeriod(DateTime.now(DateTimeZone.UTC).withHourOfDay(5));
        assert(!withinSleepPeriod);

        sleepPeriod = SleepPeriod.morning();
        withinSleepPeriod = sleepPeriod.sleepEventInSleepPeriod(DateTime.now(DateTimeZone.UTC).withHourOfDay(5));
        assert(withinSleepPeriod);
        withinSleepPeriod = sleepPeriod.sleepEventInSleepPeriod(DateTime.now(DateTimeZone.UTC).withHourOfDay(13));
        assert(!withinSleepPeriod);

        sleepPeriod = SleepPeriod.afternoonEvening();
        withinSleepPeriod = sleepPeriod.sleepEventInSleepPeriod(DateTime.now(DateTimeZone.UTC).withHourOfDay(13));
        assert(withinSleepPeriod);
        withinSleepPeriod = sleepPeriod.sleepEventInSleepPeriod(DateTime.now(DateTimeZone.UTC).withHourOfDay(21));
        assert(!withinSleepPeriod);

    }
}
