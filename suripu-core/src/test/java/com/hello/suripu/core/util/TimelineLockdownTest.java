package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.MainEventTimes;
import com.hello.suripu.core.models.SleepDay;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;

/**
 * Created by jarredheinrich on 3/10/17.
 */
public class TimelineLockdownTest {
    @Test
    public void testIsLockedDown(){
        final long accountId = 0L;
        final DateTime start = new DateTime(2016, 05, 20, 20, 29, DateTimeZone.forID("America/Los_Angeles"));
        long inbed = start.getMillis();
        long sleep = start.plusMinutes(15).getMillis();
        long wake = start.plusHours(8).getMillis();
        long outOfBed = start.plusHours(10).plusMinutes(30).getMillis();
        long createdAt = start.plusHours(15).getMillis();
        final int offset = 0;
        final Optional<MainEventTimes> computedMainEventTimesOptionalSuccess = Optional.of(MainEventTimes.createMainEventTimes(accountId, inbed, offset, sleep, offset, wake, offset, outOfBed, offset, createdAt, offset, AlgorithmType.NONE, TimelineError.NO_ERROR));

        final List<TrackerMotion> originalTrackerMotions = CSVLoader.loadTrackerMotionFromCSV("fixtures/tracker_motion/nn_raw_tracker_motion.csv");

        //locked down: sufficient duration with minimal motion after oob
        boolean isLockedDown= TimelineLockdown.isLockedDown(computedMainEventTimesOptionalSuccess,ImmutableList.copyOf(originalTrackerMotions), true);
        assert(isLockedDown);

        //not locked down: insufficient time
        wake = start.plusHours(6).getMillis();
        outOfBed = start.plusHours(9).plusMinutes(5).getMillis();
        createdAt = start.plusHours(16).getMillis();

        final Optional<MainEventTimes> computedMainEventTimesOptionalFailDuration = Optional.of(MainEventTimes.createMainEventTimes(accountId, inbed, offset, sleep, offset, wake, offset, outOfBed, offset, createdAt, offset, AlgorithmType.NONE, TimelineError.NO_ERROR));
        isLockedDown= TimelineLockdown.isLockedDown(computedMainEventTimesOptionalFailDuration,ImmutableList.copyOf(originalTrackerMotions), true);
        assert(!isLockedDown);

        // not locked down: lots of motion after oob
        wake = start.plusHours(8).getMillis();
        outOfBed = start.plusHours(8).plusMinutes(1).getMillis();
        createdAt = start.plusHours(8).plusMinutes(2).getMillis();
        final Optional<MainEventTimes> computedMainEventTimesOptionalFailMotion = Optional.of(MainEventTimes.createMainEventTimes(accountId, inbed, offset, sleep, offset, wake, offset, outOfBed, offset, createdAt, offset, AlgorithmType.NONE, TimelineError.NO_ERROR));
        isLockedDown= TimelineLockdown.isLockedDown(computedMainEventTimesOptionalFailMotion,ImmutableList.copyOf(originalTrackerMotions), true);
        assert(!isLockedDown);

    }

    @Test
    public void testAttemptTime() {
        final long accountId = 0L;
        final DateTime start = new DateTime(2016, 05, 20, 20, 29, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime targetDate = start.withTimeAtStartOfDay().withZone(DateTimeZone.UTC);
        long inbed = start.getMillis();
        long sleep = start.plusMinutes(15).getMillis();
        long wake = start.plusHours(8).getMillis();
        long outOfBed = start.plusHours(10).plusMinutes(30).getMillis();
        long createdAt = start.plusHours(15).getMillis();
        final int offset = 0;

        List<MainEventTimes> generatedMainEventTimesList = Lists.newArrayList(MainEventTimes.createMainEventTimes(accountId, inbed, offset, sleep, offset, wake, offset, outOfBed, offset, createdAt, offset,AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR));
        SleepDay targetSleepDay = SleepDay.createSleepDay(accountId, targetDate, generatedMainEventTimesList);

        final ImmutableList<TrackerMotion> originalTrackerMotions = ImmutableList.copyOf(CSVLoader.loadTrackerMotionFromCSV("fixtures/tracker_motion/nn_raw_tracker_motion.csv"));

        //missing sleep period - attempt
        final SleepPeriod targetSleepPeriod1 = SleepPeriod.afternoon(targetDate);
        final boolean attemptTimeline1 = TimelineLockdown.isAttemptNeededForSleepPeriod(targetSleepDay, targetSleepPeriod1, Optional.absent(), originalTrackerMotions, true);
        assert(attemptTimeline1);

        //lockedDowntimeline present - dont attempt
        final SleepPeriod targetSleepPeriod2 = SleepPeriod.night(targetDate);
        final boolean attemptTimeline2 = TimelineLockdown.isAttemptNeededForSleepPeriod(targetSleepDay, targetSleepPeriod2, Optional.absent(),  originalTrackerMotions, true);
        assert(!attemptTimeline2);

        //not locked down - lots of movment, created < end so should attempt
        wake = start.plusHours(8).getMillis();
        outOfBed = start.plusHours(8).plusMinutes(1).getMillis();
        createdAt = start.plusHours(8).plusMinutes(2).getMillis();

        generatedMainEventTimesList = Lists.newArrayList(MainEventTimes.createMainEventTimes(accountId, inbed, offset, sleep, offset, wake, offset, outOfBed, offset, createdAt, offset,AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR));
        targetSleepDay = SleepDay.createSleepDay(accountId, targetDate, generatedMainEventTimesList);

        final boolean attemptTimeline3 = TimelineLockdown.isAttemptNeededForSleepPeriod(targetSleepDay, targetSleepPeriod2, Optional.absent(), originalTrackerMotions,  true);
        assert(attemptTimeline3);

        //invalid timeline, created < end so should attempt
        generatedMainEventTimesList = Lists.newArrayList(MainEventTimes.createMainEventTimesEmpty(accountId,targetSleepPeriod2, createdAt, offset,AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR));
        targetSleepDay = SleepDay.createSleepDay(accountId, targetDate, generatedMainEventTimesList);


        final boolean attemptTimeline3b = TimelineLockdown.isAttemptNeededForSleepPeriod(targetSleepDay, targetSleepPeriod2,Optional.absent(),  originalTrackerMotions, true);
        assert(attemptTimeline3b);

        //not lockedDown (insufficient dur) - created > end
        wake = start.plusHours(5).getMillis();
        outOfBed = start.plusHours(9).plusMinutes(5).getMillis();
        createdAt = start.plusHours(16).getMillis();
        generatedMainEventTimesList = Lists.newArrayList(MainEventTimes.createMainEventTimes(accountId, inbed, offset, sleep, offset, wake, offset, outOfBed, offset, createdAt, offset,AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR));
        targetSleepDay = SleepDay.createSleepDay(accountId, targetDate, generatedMainEventTimesList);
        final boolean attemptTimeline4 = TimelineLockdown.isAttemptNeededForSleepPeriod(targetSleepDay, targetSleepPeriod2,Optional.absent(), originalTrackerMotions, true);
        assert(!attemptTimeline4);

        //not lockeddown, invalid timeline created > end
        generatedMainEventTimesList = Lists.newArrayList(MainEventTimes.createMainEventTimesEmpty(accountId,targetSleepPeriod2, createdAt, offset,AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.NO_ERROR));
        targetSleepDay = SleepDay.createSleepDay(accountId, targetDate, generatedMainEventTimesList);
        final boolean attemptTimeline5 = TimelineLockdown.isAttemptNeededForSleepPeriod(targetSleepDay, targetSleepPeriod2, Optional.absent(), originalTrackerMotions, true);
        assert(!attemptTimeline5);
        //notLockedDown created>=end invalid
        //notLockedDown created>=end valid
    }

}
