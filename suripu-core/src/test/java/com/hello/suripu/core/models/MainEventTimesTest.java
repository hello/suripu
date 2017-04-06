package com.hello.suripu.core.models;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * Created by jarredheinrich on 3/7/17.
 */
public class MainEventTimesTest {

    @Test
    public void testHasValidEventTimes(){
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final MainEventTimes mainEventTimesValid = MainEventTimes.createMainEventTimes(0L, now.minusHours(12).getMillis(), 0, now.minusHours(11).getMillis(),0,now.minusHours(2).getMillis(),0,now.minusHours(1).getMillis(),0,now.getMillis(),0 );
        assert(mainEventTimesValid.hasValidEventTimes());
        final MainEventTimes mainEventTimesInvalid = MainEventTimes.createMainEventTimesEmpty(0L,SleepPeriod.night(now), now.getMillis(),0 );
        assert(!mainEventTimesInvalid.hasValidEventTimes());

    }

    @Test
    public void getSleepPeriodsMainEventTimesMapForDate(){
        final long accountId = 0L;

        final DateTime startTime = new DateTime(2017,2,1,0,0, DateTimeZone.UTC);
        final DateTime createdAt = startTime.plusDays(3);
        final List<MainEventTimes> testMainEventTimesList = Lists.newArrayList();
        for(int i = 0; i < 3; i ++){
            final MainEventTimes testMainEventTimes = MainEventTimes.createMainEventTimesEmpty(accountId,SleepPeriod.createSleepPeriod(SleepPeriod.Period.fromInteger(i), startTime), createdAt.getMillis(), 0);
            testMainEventTimesList.add(testMainEventTimes);
        }
        final MainEventTimes testMainEventTimes = MainEventTimes.createMainEventTimesEmpty(accountId,SleepPeriod.createSleepPeriod(SleepPeriod.Period.fromInteger(2), startTime.plusDays(1)), createdAt.getMillis(), 0);
        testMainEventTimesList.add(testMainEventTimes);

        final Map<SleepPeriod.Period, MainEventTimes> sleepPeriodsMainEventTimesMapD1 = MainEventTimes.getSleepPeriodsMainEventTimesMapForDate(testMainEventTimesList, startTime);
        assert(sleepPeriodsMainEventTimesMapD1.size()==3);
        final Map<SleepPeriod.Period, MainEventTimes> sleepPeriodsMainEventTimesMapD2 = MainEventTimes.getSleepPeriodsMainEventTimesMapForDate(testMainEventTimesList, startTime.plusDays(1));
        assert(sleepPeriodsMainEventTimesMapD2.size()==1);
        assert(sleepPeriodsMainEventTimesMapD2.containsKey(SleepPeriod.Period.NIGHT));
    }
}
