package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.MainEventTimes;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/*
 * Created by jarredheinrich on 3/10/17.
 * Checks if there is a valid timeline previously generated that is should be used instead of rerunning the timeline algorithm.
 *  - checks for valid main event times
 *  - checks for sufficient sleep duration
 *  - checks for no significant motion in the hour following the timeline creation time
 *  If there is are valid main event times, with sufficient sleep and no siginficant motion following the creation time,
 *  the timeline is locked down
 */

public class TimelineLockdown {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineLockdown.class);

    private static final int NO_MOTION_WINDOW_MINUTES = 60;
    private static final int MOTION_COUNT_THRESHOLD = 4;
    private static final int MIN_SLEEP_DURATION = 6 * DateTimeConstants.MINUTES_PER_HOUR;

    public static boolean isLockedDown(final Optional<MainEventTimes> computedMainEventTimesOptional, final ImmutableList<TrackerMotion> processedTrackerMotions, final Boolean hasTimelineLockdown) {

        if (!hasTimelineLockdown){
            return false;
        }

        if(!computedMainEventTimesOptional.isPresent()){
            return false;
        }

        final MainEventTimes computedMainEventTimes = computedMainEventTimesOptional.get();

        //main event times are saved (as 0) for invalid timelines
        //if the main event times are invalid, the timeline is not locked down
        if(!computedMainEventTimes.hasValidEventTimes()){
            return false;
        }

        final int computedSleepDuration = (int) (computedMainEventTimes.eventTimeMap.get(Event.Type.WAKE_UP).time - computedMainEventTimes.eventTimeMap.get(Event.Type.SLEEP).time) / DateTimeConstants.MILLIS_PER_MINUTE;

        final Boolean hasMotionDuringWindow = motionDuringWindow(processedTrackerMotions, computedMainEventTimes.createdAt.time, NO_MOTION_WINDOW_MINUTES, MOTION_COUNT_THRESHOLD);
        final Boolean hasSufficientSleepDuration = computedSleepDuration >= MIN_SLEEP_DURATION;

        //if there is too much motion in following window, timeline is not locked down
        if (hasMotionDuringWindow ){
            return false;
        }

        //if the sleep duration is less than 6 hours, the timeline is not locked down
        if(!hasSufficientSleepDuration) {
            return false;
        }

        LOGGER.debug("action=locking-down-timeline account_id={} date={} sleep-period={}", computedMainEventTimes.accountId, computedMainEventTimes.sleepPeriod.targetDate, computedMainEventTimes.sleepPeriod.period);
        return true;

    }

    public static boolean motionDuringWindow(final List<TrackerMotion> trackerMotions, final Long windowStartTime, final int windowDurationMinutes, final int newMotionCountThreshold) {
        final int newMotionTimeWindow = windowDurationMinutes * DateTimeConstants.MILLIS_PER_MINUTE;
        int newMotionCount = 0;
        //counts motion events within a specific timewindow
        for (final TrackerMotion trackermotion : trackerMotions) {
            if (trackermotion.timestamp > windowStartTime && trackermotion.timestamp <= windowStartTime + newMotionTimeWindow) {
                newMotionCount += 1;
            }
        }
        if (newMotionCount > newMotionCountThreshold) {
            return true;
        }

        return false;
    }
}
