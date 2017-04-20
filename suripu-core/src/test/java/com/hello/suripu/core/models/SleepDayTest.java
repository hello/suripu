package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.TimelineError;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;

/**
 * Created by jarredheinrich on 3/15/17.
 */
public class SleepDayTest {

    @Test
    public void testGetPreviousOutOfBed(){

        //no previous events
        final long accountId = 0L;
        final DateTime targetDate = new DateTime(2017,1,1,0,0,0,0, DateTimeZone.UTC);

        final List<MainEventTimes> mainEventTimesList= Lists.newArrayList();
        final SleepDay testSleepDay = SleepDay.createSleepDay(accountId, targetDate, mainEventTimesList);
        MainEventTimes prevNightMainEventTimes = MainEventTimes.createMainEventTimesEmpty(accountId, SleepPeriod.night(targetDate.minusDays(1)), targetDate.getMillis(), 0, AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR);
        Optional<Long> prevOOBOptional = testSleepDay.getPreviousOutOfBedTime(SleepPeriod.Period.MORNING, prevNightMainEventTimes);
        assert(!prevOOBOptional.isPresent());

        final long eventTime = 100L;
        prevNightMainEventTimes = MainEventTimes.createMainEventTimes(accountId, eventTime,0,eventTime,0,eventTime,0,eventTime,0, targetDate.getMillis(), 0, AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR);
        prevOOBOptional = testSleepDay.getPreviousOutOfBedTime(SleepPeriod.Period.MORNING, prevNightMainEventTimes);
        assert(prevOOBOptional.isPresent());
        assert(prevOOBOptional.get() == eventTime);

        final long inbed = targetDate.plusHours(13).getMillis();
        final long sleep = targetDate.plusHours(14).getMillis();
        final long wake = targetDate.plusHours(21).getMillis();
        final long oob = targetDate.plusHours(22).getMillis();
        final MainEventTimes targetAfternoonMainEventTimes = MainEventTimes.createMainEventTimes(accountId, inbed, 0,sleep,0,wake,0,oob,0, targetDate.getMillis(), 0, AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR);
        final SleepPeriodResults afternoonResults = SleepPeriodResults.create(targetAfternoonMainEventTimes, Optional.absent(), Optional.absent(),new TimelineLog(accountId, targetDate.getMillis(), DateTime.now(DateTimeZone.UTC).getMillis()),DataCompleteness.ENOUGH_DATA,true);
        testSleepDay.updateSleepPeriod(afternoonResults);
        final SleepPeriod targetNight = SleepPeriod.night(targetDate);
        prevOOBOptional = testSleepDay.getPreviousOutOfBedTime(SleepPeriod.Period.NIGHT, prevNightMainEventTimes);
        assert(prevOOBOptional.isPresent());
        assert(prevOOBOptional.get() == oob);

    }
}
