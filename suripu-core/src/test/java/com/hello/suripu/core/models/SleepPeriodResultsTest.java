package com.hello.suripu.core.models;

import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.TimelineError;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

/**
 * Created by jarredheinrich on 4/5/17.
 */
public class SleepPeriodResultsTest {
    @Test
    public void testCreateEmpty(){
        final com.hello.suripu.core.models.timeline.v2.TimelineLog log = new com.hello.suripu.core.models.timeline.v2.TimelineLog(0L, DateTime.now(DateTimeZone.UTC).getMillis(), DateTime.now(DateTimeZone.UTC).getMillis(), 0L);

        final MainEventTimes mainEventTimesEmpty= MainEventTimes.createMainEventTimesEmpty(0L, SleepPeriod.createSleepPeriod(SleepPeriod.Period.NIGHT, DateTime.now(DateTimeZone.UTC)), DateTime.now(DateTimeZone.UTC).getMillis(), 0, AlgorithmType.NONE, TimelineError.NO_DATA);
        final SleepPeriodResults emptySleepPeriod  = SleepPeriodResults.createEmpty(0L, mainEventTimesEmpty, log, DataCompleteness.NOT_ENOUGH_DATA, true);

        assert(!emptySleepPeriod.resultsOptional.isPresent());
    }

}
