package com.hello.suripu.core.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;

/**
 * Created by jarredheinrich on 2/6/17.
 */
public class SleepPeriodTest {
    @Test
    public void testSleepEventInSleepPeriod(){

        SleepPeriod sleepPeriod = SleepPeriod.night(DateTime.now(DateTimeZone.UTC));
        boolean withinSleepPeriod = sleepPeriod.sleepEventInSleepPeriod(DateTime.now(DateTimeZone.UTC).withHourOfDay(0));
        assert(withinSleepPeriod);
        withinSleepPeriod = sleepPeriod.sleepEventInSleepPeriod(DateTime.now(DateTimeZone.UTC).withHourOfDay(5));
        assert(!withinSleepPeriod);

        sleepPeriod = SleepPeriod.morning(DateTime.now(DateTimeZone.UTC));
        withinSleepPeriod = sleepPeriod.sleepEventInSleepPeriod(DateTime.now(DateTimeZone.UTC).withHourOfDay(5));
        assert(withinSleepPeriod);
        withinSleepPeriod = sleepPeriod.sleepEventInSleepPeriod(DateTime.now(DateTimeZone.UTC).withHourOfDay(13));
        assert(!withinSleepPeriod);

        sleepPeriod = SleepPeriod.afternoon(DateTime.now(DateTimeZone.UTC));
        withinSleepPeriod = sleepPeriod.sleepEventInSleepPeriod(DateTime.now(DateTimeZone.UTC).withHourOfDay(13));
        assert(withinSleepPeriod);
        withinSleepPeriod = sleepPeriod.sleepEventInSleepPeriod(DateTime.now(DateTimeZone.UTC).withHourOfDay(21));
        assert(!withinSleepPeriod);

    }

    @Test
    public void testGetSleepPeriodQueue(){
        final DateTime startTime = new DateTime(2017, 1,1,0,0,0, DateTimeZone.UTC);
        final DateTime targetDate = startTime;

        List<SleepPeriod> sleepPeriodList = SleepPeriod.getSleepPeriodQueue(targetDate, startTime);
        assert(sleepPeriodList.isEmpty());

        sleepPeriodList = SleepPeriod.getSleepPeriodQueue(targetDate, startTime.plusHours(11).plusMillis(1));
        assert(sleepPeriodList.size()==1);
        assert(sleepPeriodList.get(0).period== SleepPeriod.Period.MORNING);

        sleepPeriodList = SleepPeriod.getSleepPeriodQueue(targetDate, startTime.plusHours(19).plusMillis(1));
        assert(sleepPeriodList.size()==2);
        assert(sleepPeriodList.get(0).period== SleepPeriod.Period.MORNING);
        assert(sleepPeriodList.get(1).period== SleepPeriod.Period.AFTERNOON);

        sleepPeriodList = SleepPeriod.getSleepPeriodQueue(targetDate, startTime.plusHours(27).plusMillis(1));
        assert(sleepPeriodList.size()==3);
        assert(sleepPeriodList.get(0).period== SleepPeriod.Period.MORNING);
        assert(sleepPeriodList.get(1).period== SleepPeriod.Period.AFTERNOON);
        assert(sleepPeriodList.get(2).period== SleepPeriod.Period.NIGHT);
    }
    @Test
    public void testGetSleepPeriodTime(){
        final DateTime targetDate = new DateTime(2017,1,20,00,0,0,DateTimeZone.UTC);
        final SleepPeriod sleepPeriodNight= SleepPeriod.night(targetDate);
        final int offset = DateTimeConstants.MILLIS_PER_HOUR * (-8);
        final DateTime endTime = sleepPeriodNight.getSleepPeriodTime(SleepPeriod.Boundary.END_DATA,  offset);
        final long endTimeUTC = endTime.getMillis();
        assert(endTimeUTC == 1485028800000L);
        final long endTimeMillis= sleepPeriodNight.getSleepPeriodMillis(SleepPeriod.Boundary.END_DATA,  offset);
        assert(endTimeMillis == 1485028800000L);

    }



}
