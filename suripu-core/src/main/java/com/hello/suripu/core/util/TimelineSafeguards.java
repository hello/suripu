package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.TrackerMotion;
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

    /* takes sensor data, and timeline events and decides if there might be some problems with this timeline  */
    public TimelineError checkIfValidTimeline (final long accountId, final AlgorithmType algorithmType, SleepEvents<Optional<Event>> sleepEvents, ImmutableList<Event> extraEvents, final ImmutableList<Sample> lightData, final ImmutableList<TrackerMotion> originalTrackerMotions) {

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
            final boolean motionDuringSleep = TimelineUtils.motionDuringSleepCheck(originalTrackerMotions, sleepEvents.fallAsleep.get().getStartTimestamp(), sleepEvents.wakeUp.get().getStartTimestamp());
            if (!motionDuringSleep) {
                LOGGER.warn("action=invalidating-timeline reason=insufficient-motion-during-sleeptime account_id={}", accountId);
                return TimelineError.NO_MOTION_DURING_SLEEP;
            }
        }

        return TimelineError.NO_ERROR;

    }

}
