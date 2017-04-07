package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.MainEventTimes;
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

}
