package com.hello.suripu.core.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

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

}
