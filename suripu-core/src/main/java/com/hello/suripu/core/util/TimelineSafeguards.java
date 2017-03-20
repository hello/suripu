package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.UUID;

/**
 * Created by benjo on 5/7/15.
 */
public class TimelineSafeguards {

    private static final long NUM_MILLIS_IN_A_MINUTE = 60000L;

    public static final int MINIMUM_SLEEP_DURATION_MINUTES = 180; //three hours
    private static final int MAXIMUM_ALLOWABLE_DATAGAP = 60; //one hour

    //sleep period specific thresholds
    public static final int MAXIMUM_ALLOWABLE_MOTION_GAP_PRIMARY_PERIOD = 240; // need motion 1 at least every 4 hours; 97% qualify
    public static final int MAXIMUM_ALLOWABLE_MOTION_GAP_ALTERNATIVE_PERIOD = 116; // need motion at least every 116 mins; 90% qualify
    public static final int MINIMUM_MOTION_COUNT_DURING_SLEEP_PRIMARY_PERIOD = 2; //99.5% qualify
    public static final int MINIMUM_MOTION_COUNT_DURING_SLEEP_ALTERNATIVE_PERIOD = 18 ;//90% qualify
    //combined: primary period - 97% of nights valid; alternative period - 81% of nights valid (for 5000 timelines generated jan 2017)



    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(TimelineSafeguards.class);

    private final Logger LOGGER;

    public TimelineSafeguards(final UUID uuid) {
        this(Optional.of(uuid));
    }

    public TimelineSafeguards() {
        this(Optional.<UUID>absent());
    }

    public  TimelineSafeguards(final Optional<UUID> uuid) {
        if (uuid.isPresent()) {
            LOGGER = new LoggerWithSessionId(STATIC_LOGGER,uuid.get());
        }
        else {
            LOGGER = new LoggerWithSessionId(STATIC_LOGGER);
        }
    }

    public boolean checkEventOrdering(final long accountId, final AlgorithmType algorithmType, SleepEvents<Optional<Event>> sleepEvents, ImmutableList<Event> extraEvents) {
        //check main events ordering
        if (sleepEvents.wakeUp.isPresent() && sleepEvents.fallAsleep.isPresent()) {
            if (sleepEvents.wakeUp.get().getStartTimestamp() < sleepEvents.fallAsleep.get().getStartTimestamp()) {
                LOGGER.warn("error=event-order ordering=wake-before-sleep account_id={} algorithm={} sleep_time={} wake_time={}", accountId, algorithmType.name(), sleepEvents.fallAsleep.get().getStartTimestamp(), sleepEvents.wakeUp.get().getStartTimestamp());
                return false;
            }
        }

        if (sleepEvents.outOfBed.isPresent() && sleepEvents.goToBed.isPresent()) {
            if (sleepEvents.outOfBed.get().getStartTimestamp() < sleepEvents.goToBed.get().getStartTimestamp()) {
                LOGGER.warn("error=event-order ordering=outofbed-before-bed account_id={} algorithm={} inbed_time={} outofbed_time={}", accountId, algorithmType.name(),sleepEvents.goToBed.get().getStartTimestamp() ,sleepEvents.outOfBed.get().getStartTimestamp() );
                return false;
            }
        }

        if (sleepEvents.goToBed.isPresent() && sleepEvents.fallAsleep.isPresent()) {
            if (sleepEvents.fallAsleep.get().getStartTimestamp() < sleepEvents.goToBed.get().getStartTimestamp()) {
                LOGGER.warn("error=event-order ordering=asleep-before-inbed account_id={} algorithm={} inbed_time={} sleep_time={}", accountId, algorithmType.name(),sleepEvents.goToBed.get().getStartTimestamp() ,sleepEvents.fallAsleep.get().getStartTimestamp() );
                return false;
            }
        }

        if (sleepEvents.outOfBed.isPresent() && sleepEvents.wakeUp.isPresent()) {
            if (sleepEvents.outOfBed.get().getStartTimestamp() < sleepEvents.wakeUp.get().getStartTimestamp()) {
                LOGGER.warn("error=event-order ordering=outofbed-before-wake account_id={} algorithm={} wake_time={} outofbed_time={}", accountId, algorithmType.name(),sleepEvents.wakeUp.get().getStartTimestamp() ,sleepEvents.outOfBed.get().getStartTimestamp() );
                return false;
            }
        }

        //check extra events only if primary wake/sleep are present
        if (sleepEvents.wakeUp.isPresent() && sleepEvents.fallAsleep.isPresent()) {
            final long sleepTime = sleepEvents.fallAsleep.get().getStartTimestamp();
            final long wakeTime = sleepEvents.wakeUp.get().getStartTimestamp();
            long lastTimeStamp = sleepTime;

            boolean foundWake = false;
            boolean foundOutOfBed = false;

            for (final Event event : extraEvents) {
                if (event.getType() == Event.Type.WAKE_UP) {

                    if (foundWake) {
                        LOGGER.warn("found two wakes in a row");
                        return false;
                    }

                    if (event.getStartTimestamp() > wakeTime || event.getStartTimestamp() < sleepTime) {
                        LOGGER.warn("found wake event outside of bounds of first sleep and last wake");
                        return false;
                    }

                    if (event.getStartTimestamp() < lastTimeStamp) {
                        LOGGER.warn("found wake event that happened before a sleep event");
                        return false;
                    }

                    foundWake = true;
                    lastTimeStamp = event.getStartTimestamp();


                }
                else if (event.getType() == Event.Type.SLEEP) {
                    if (!foundWake) {
                        LOGGER.warn("found two sleeps in a row");
                        return false;
                    }

                    if (event.getStartTimestamp() > wakeTime || event.getStartTimestamp() < sleepTime) {
                        LOGGER.warn("found sleep event outside of bounds of first sleep and last wake");
                        return false;
                    }

                    if (event.getStartTimestamp() < lastTimeStamp) {
                        LOGGER.warn("found sleep event that happened before a wake event");
                        return false;
                    }

                    foundWake = false;
                    lastTimeStamp = event.getStartTimestamp();
                }
                else if (event.getType() == Event.Type.IN_BED) {

                    if (!foundOutOfBed) {
                        LOGGER.warn("found an in-bed but it was not after an out-of-bed");
                        return false;
                    }

                    foundOutOfBed = false;
                }
                else if (event.getType() == Event.Type.OUT_OF_BED) {

                    if (foundOutOfBed) {
                        LOGGER.warn("found two  consecutive out of beds");
                        return false;
                    }

                    foundOutOfBed = true;
                }
            }

            if (foundWake) {
                LOGGER.warn("found a wake with an unmatched sleep");
                return false;
            }

            if (foundOutOfBed) {
                LOGGER.warn("found an out-of-bed with an unmatched in-bed");
                return false;

            }
        }


        return true;
    }

    /* Assumes intermediate events are chronologially sorted */
    public int getTotalSleepDurationInMinutes(final Event firstSleep, final Event lastWake,ImmutableList<Event> intermediateEvents) {
        Event prevSleep = firstSleep;
        int durationInMinutes = 0;
        for (final Event event: intermediateEvents) {

            if (event.getType() == Event.Type.WAKE_UP) {
                durationInMinutes += (event.getStartTimestamp() - prevSleep.getStartTimestamp()) / NUM_MILLIS_IN_A_MINUTE;
            }
            else if (event.getType() == Event.Type.SLEEP) {
                prevSleep = event;
            }
        }

        durationInMinutes += (lastWake.getStartTimestamp() - prevSleep.getStartTimestamp()) / NUM_MILLIS_IN_A_MINUTE;

        return durationInMinutes;
    }

    public int getMaximumDataGapInMinutes(final ImmutableList<Sample> lightdata) {
        Iterator<Sample> it = lightdata.iterator();
        boolean first = true;
        Long lastTime = 0L;
        int maxGapInMinutes = 0;
        while (it.hasNext()) {
            final Sample sample = it.next();

            if (first) {
                first = false;
            }
            else {
                final int gapInMinutes = (int) ((sample.dateTime - lastTime - NUM_MILLIS_IN_A_MINUTE) / NUM_MILLIS_IN_A_MINUTE);

                if (gapInMinutes > maxGapInMinutes) {
                    maxGapInMinutes = gapInMinutes;
                }
            }


            lastTime = sample.dateTime;

        }

        return maxGapInMinutes;
    }


    public static int getMaximumMotionGapInMinutes(final ImmutableList<TrackerMotion> trackerMotions, final long sleeptime, final long wakeTime) {
        Iterator<TrackerMotion> it = trackerMotions.iterator();
        boolean first = true;
        Long lastTime = sleeptime;
        int maxGapInMinutes = 0;
        while (it.hasNext()) {
            final TrackerMotion trackerMotion = it.next();
            if (trackerMotion.timestamp < sleeptime) {
                continue;
            }
            if (trackerMotion.timestamp > wakeTime) {
                break;
            }

            final int gapInMinutes = (int) ((trackerMotion.timestamp - lastTime - NUM_MILLIS_IN_A_MINUTE) / NUM_MILLIS_IN_A_MINUTE);

            if (gapInMinutes > maxGapInMinutes) {
                maxGapInMinutes = gapInMinutes;
            }


            lastTime = trackerMotion.timestamp;

        }
        final int gapInMinutes = (int) ((wakeTime - lastTime - NUM_MILLIS_IN_A_MINUTE) / NUM_MILLIS_IN_A_MINUTE);
        if (gapInMinutes > maxGapInMinutes) {
            maxGapInMinutes = gapInMinutes;
        }

        return maxGapInMinutes;
    }

    //checks if there is any motion observed during during sleep - We should expect some motion during sleep.
    public static boolean motionDuringSleepCheck(final int minMotionCount, final ImmutableList<TrackerMotion> trackerMotions, final Long fallAsleepTimestamp, final Long wakeUpTimestamp) {

        final float sleepDuration = (int) ((double) (wakeUpTimestamp - fallAsleepTimestamp) / 60000.0);
        final int requiredSleepDuration = 120; // taking into account sleep window padding - this requires a minimal of 3 hours of sleep with no motion
        final int sleepWindowPadding = 30; //excludes first 30 and last 30 minutes of sleeps
        int motionCount = 0;

        // Compute first to last motion time delta
        for (final TrackerMotion motion : trackerMotions) {
            if (motion.timestamp > wakeUpTimestamp - sleepWindowPadding * DateTimeConstants.MILLIS_PER_MINUTE) {
                break;
            }
            if (motion.timestamp > fallAsleepTimestamp + sleepWindowPadding * DateTimeConstants.MILLIS_PER_MINUTE) {
                motionCount += 1;
            }
        }
        if (motionCount < minMotionCount && sleepDuration > requiredSleepDuration) {
            return false;
        }
        return true;
    }


    /* takes sensor data, and timeline events and decides if there might be some problems with this timeline  */
    public TimelineError checkIfValidTimeline (final long accountId, final boolean primaryPeriod, final AlgorithmType algorithmType, SleepEvents<Optional<Event>> sleepEvents, ImmutableList<Event> extraEvents, final ImmutableList<Sample> lightData, final ImmutableList<TrackerMotion> processedTrackerMotions) {

        final int maxAllowableMotionGap, minMotionCountThreshold;
        if (primaryPeriod){
            maxAllowableMotionGap= MAXIMUM_ALLOWABLE_MOTION_GAP_PRIMARY_PERIOD;
            minMotionCountThreshold = MINIMUM_MOTION_COUNT_DURING_SLEEP_PRIMARY_PERIOD;
        } else{
            maxAllowableMotionGap= MAXIMUM_ALLOWABLE_MOTION_GAP_ALTERNATIVE_PERIOD;
            minMotionCountThreshold = MINIMUM_MOTION_COUNT_DURING_SLEEP_ALTERNATIVE_PERIOD;
        }

        //make sure events occur in proper order
        if (!checkEventOrdering(accountId, algorithmType, sleepEvents, extraEvents)) {
            return TimelineError.EVENTS_OUT_OF_ORDER;
        }

        //make sure all events are present
        for (final Optional<Event> event : sleepEvents.toList()) {
            if (!event.isPresent()) {
                return TimelineError.MISSING_KEY_EVENTS;
            }
        }

        if (sleepEvents.wakeUp.isPresent() && sleepEvents.fallAsleep.isPresent()) {
            final int sleepDurationInMinutes = getTotalSleepDurationInMinutes(sleepEvents.fallAsleep.get(), sleepEvents.wakeUp.get(), extraEvents);

            if (sleepDurationInMinutes <= MINIMUM_SLEEP_DURATION_MINUTES) {
                LOGGER.warn("action=invalidating-timeline reason=insufficient-sleep-duration account_id={} sleep_duration={} ", accountId, sleepDurationInMinutes);
                return TimelineError.NOT_ENOUGH_HOURS_OF_SLEEP;
            }
        }

        final int maxDataGapInMinutes = getMaximumDataGapInMinutes(lightData);

        if (maxDataGapInMinutes > MAXIMUM_ALLOWABLE_DATAGAP) {
            LOGGER.warn("action=invalidating-timeline reason=max-data-gap-greater-than-limit account_id={} data-gap-minutes={} limit={} ", accountId, maxDataGapInMinutes, MAXIMUM_ALLOWABLE_DATAGAP);
            return TimelineError.DATA_GAP_TOO_LARGE;
        }

        if (sleepEvents.wakeUp.isPresent() && sleepEvents.fallAsleep.isPresent()) {
            //check to see if motion interval during sleep is greater than 1 hour for "natural" timelines
            final boolean motionDuringSleep = motionDuringSleepCheck(minMotionCountThreshold, processedTrackerMotions, sleepEvents.fallAsleep.get().getStartTimestamp(), sleepEvents.wakeUp.get().getStartTimestamp());
            if (!motionDuringSleep) {
                LOGGER.warn("action=invalidating-timeline reason=insufficient-motion-during-sleeptime account_id={}", accountId);
                return TimelineError.NO_MOTION_DURING_SLEEP;
            }

            final int maxMotionGapInMinutes = getMaximumMotionGapInMinutes(processedTrackerMotions, sleepEvents.fallAsleep.get().getStartTimestamp(), sleepEvents.wakeUp.get().getStartTimestamp());
            if (maxMotionGapInMinutes > maxAllowableMotionGap) {
                LOGGER.warn("max motion gap {} minutes is greaten than limit {} minutes -- invalidating timeline", maxMotionGapInMinutes, maxAllowableMotionGap);
                return TimelineError.MOTION_GAP_TOO_LARGE;

            }
        }

        return TimelineError.NO_ERROR;

    }

}
