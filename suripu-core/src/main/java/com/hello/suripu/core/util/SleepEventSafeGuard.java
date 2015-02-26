package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.FallingAsleepEvent;
import com.hello.suripu.core.models.Events.InBedEvent;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 2/17/15.
 */
public class SleepEventSafeGuard {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepEventSafeGuard.class);

    public static boolean isEventOverlapped(final Event event1, final Event event2){
        return event1.getStartTimestamp() == event2.getStartTimestamp();
    }

    public static boolean isEventsOutOfOrder(final Event eventThatShouldComeFirst, final Event eventThatShouldFollow){
        return eventThatShouldComeFirst.getStartTimestamp() > eventThatShouldFollow.getStartTimestamp();
    }

    public static boolean isInBedPeriodTooShort(final List<Optional<Event>> finalEvents, final long longEnoughThresholdMillis){
        return finalEvents.get(0).isPresent() && finalEvents.get(3).isPresent() &&
                finalEvents.get(3).get().getStartTimestamp() - finalEvents.get(0).get().getEndTimestamp() < longEnoughThresholdMillis;
    }

    public static boolean isSleepPeriodTooShort(final List<Optional<Event>> finalEvents, final long longEnoughThresholdMillis){
        return finalEvents.get(1).isPresent() && finalEvents.get(2).isPresent() &&
                finalEvents.get(2).get().getStartTimestamp() - finalEvents.get(1).get().getEndTimestamp() < longEnoughThresholdMillis;
    }

    public static boolean isTwoSupposedCloseEventsDeviateTooMuch(final Event event1, final Event event2, final long tooOffThresholdMillis){
        return Math.abs(event1.getStartTimestamp() - event2.getStartTimestamp()) > tooOffThresholdMillis;
    }

    public static boolean userLeftBedInBetweenForALongTime(final List<Optional<Event>> finalEvents, final List<AmplitudeData> hourlyMotionCounts){
        // please refer to mark@prigg.com, night of 2015-02-14
        // At that night, the user in bed for a while, then turn light off and go out.
        // 2 hours later he came back, go in bed WITHOUT switching the light on and off

        long inBedOrFallAsleepTimeMillis = 0;
        if(finalEvents.get(0).isPresent()){
            inBedOrFallAsleepTimeMillis = finalEvents.get(0).get().getStartTimestamp();
        }

        if(finalEvents.get(1).isPresent()){
            inBedOrFallAsleepTimeMillis = finalEvents.get(1).get().getStartTimestamp();
        }

        if(inBedOrFallAsleepTimeMillis == 0){
            return false;
        }

        final List<Segment> quietPeriods = getQuietPeriods(hourlyMotionCounts, 90 * DateTimeConstants.MILLIS_PER_MINUTE);
        if(quietPeriods.size() > 0 && hourlyMotionCounts.size() >= 2){
            final long nearestQuietPeriodStartTimeMillis = quietPeriods.get(0).getStartTimestamp();
            final long middleOfNightTimestamp = getMiddleOfMotionPeriodTimestamp(hourlyMotionCounts);
            final int offsetMillis = quietPeriods.get(0).getOffsetMillis();

            if(nearestQuietPeriodStartTimeMillis < middleOfNightTimestamp && inBedOrFallAsleepTimeMillis < nearestQuietPeriodStartTimeMillis){

                // The long quite period starts before middle of the night
                // the user is likely left the bed
                LOGGER.warn("User in bed/sleep at {}, left bed at {}, for {} millis, middle of night {}.",
                        new DateTime(inBedOrFallAsleepTimeMillis, DateTimeZone.forOffsetMillis(offsetMillis)),
                        new DateTime(nearestQuietPeriodStartTimeMillis, DateTimeZone.forOffsetMillis(offsetMillis)),
                        quietPeriods.get(0).getDuration(),
                        new DateTime(middleOfNightTimestamp, DateTimeZone.forOffsetMillis(offsetMillis)));
                return true;
            }
        }

        return false;

    }

    private static long getMiddleOfMotionPeriodTimestamp(final List<AmplitudeData> motionRelatedFeature){
        // (end - start)/2 + start
        return (motionRelatedFeature.get(motionRelatedFeature.size() - 1).timestamp
                - motionRelatedFeature.get(0).timestamp) / 2
                + motionRelatedFeature.get(0).timestamp;
    }

    private static List<Segment> getQuietPeriods(final List<AmplitudeData> hourlyMotionCounts, final long quietPeriodThresholdMillis){
        final ArrayList<AmplitudeData> periodBounds = new ArrayList<>();
        final ArrayList<Segment> quietPeriods = new ArrayList<>();

        for(final AmplitudeData datum:hourlyMotionCounts){
            if(datum.amplitude > 0 && periodBounds.size() > 0){
                periodBounds.add(datum);
            }

            if(datum.amplitude == 0 && periodBounds.size() == 0){
                periodBounds.add(datum);
            }

            if(periodBounds.size() == 2){
                final long durationMillis = periodBounds.get(1).timestamp - periodBounds.get(0).timestamp;
                if(durationMillis >= quietPeriodThresholdMillis){
                    quietPeriods.add(new Segment(periodBounds.get(0).timestamp, periodBounds.get(1).timestamp, periodBounds.get(0).offsetMillis));
                }
                periodBounds.clear();
            }
        }

        return quietPeriods;
    }

    public static List<Optional<Event>> sleepEventsHeuristicFix(final List<Event> sleepEvents, final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features){
        final Event goToBed = sleepEvents.get(0);
        final Event sleep = sleepEvents.get(1);
        final Event wakeUp = sleepEvents.get(2);
        final Event outOfBed = sleepEvents.get(3);


        final ArrayList<Optional<Event>> fixedSleepEvents = Lists.newArrayList(
                Optional.of(goToBed),
                Optional.of(sleep),
                Optional.of(wakeUp),
                Optional.of(outOfBed)
        );


        if(isEventOverlapped(sleep, goToBed)){
            fixedSleepEvents.set(1, Optional.of((Event) new FallingAsleepEvent(sleep.getStartTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    sleep.getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,  sleep.getTimezoneOffset())));
            LOGGER.warn("Sleep {} has the same time with in bed, set to in bed +1 minute.",
                    new DateTime(sleep.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.getTimezoneOffset())));
        }

        if(isEventOverlapped(wakeUp, outOfBed)){
            fixedSleepEvents.set(3, Optional.of((Event) new OutOfBedEvent(outOfBed.getStartTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    outOfBed.getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    outOfBed.getTimezoneOffset())));
            LOGGER.warn("Out of bed {} has the same time with wake up, set to wake up +1 minute.",
                    new DateTime(outOfBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.getTimezoneOffset())));
        }

        // Heuristic fix
        if(isEventsOutOfOrder(goToBed, sleep)) {

            if(goToBed.getStartTimestamp() - sleep.getEndTimestamp() < 40 * DateTimeConstants.MILLIS_PER_MINUTE) {
                LOGGER.warn("Go to bed {} later then fall asleep {}, go to bed set to sleep.",
                        new DateTime(goToBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBed.getTimezoneOffset())),
                        new DateTime(sleep.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.getTimezoneOffset())));

                fixedSleepEvents.set(0, Optional.of((Event) new InBedEvent(sleep.getStartTimestamp() - DateTimeConstants.MILLIS_PER_MINUTE,
                        sleep.getEndTimestamp() - DateTimeConstants.MILLIS_PER_MINUTE,
                        sleep.getTimezoneOffset())));
            }else{
                //fixedSleepEvents.set(0, Optional.<Event>absent());
                fixedSleepEvents.set(0, Optional.of((Event) new InBedEvent(sleep.getStartTimestamp() - 5 * DateTimeConstants.MILLIS_PER_MINUTE,
                        sleep.getEndTimestamp() - 5 * DateTimeConstants.MILLIS_PER_MINUTE,
                        sleep.getTimezoneOffset())));
            }

        }

        // Heuristic fix: wake up time is later than out of bed, use out of bed because it looks
        // for the most significant motion
        if(isEventsOutOfOrder(wakeUp, outOfBed)){
            // Huge spike before motion+spikes, has motion in between
            // already wake up?
            if(wakeUp.getStartTimestamp() - outOfBed.getEndTimestamp() < 40 * DateTimeConstants.MILLIS_PER_MINUTE) {
                LOGGER.warn("Wake up later than out of bed, wake up {}, out of bed {}, out of bed set to wake up + 1.",
                        new DateTime(wakeUp.getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.getTimezoneOffset())),
                        new DateTime(outOfBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.getTimezoneOffset())));


                fixedSleepEvents.set(3, Optional.of((Event) new OutOfBedEvent(wakeUp.getEndTimestamp(),
                        wakeUp.getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                        wakeUp.getTimezoneOffset())));
            }else{
                LOGGER.warn("Wake up later than out of bed too much, wake up {}, out of bed {}, remove out of bed.",
                        new DateTime(wakeUp.getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.getTimezoneOffset())),
                        new DateTime(outOfBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.getTimezoneOffset())));

                //fixedSleepEvents.set(3, Optional.<Event>absent());
                fixedSleepEvents.set(3, Optional.of((Event) new OutOfBedEvent(wakeUp.getEndTimestamp(),
                        wakeUp.getEndTimestamp() + 5 * DateTimeConstants.MILLIS_PER_MINUTE,
                        wakeUp.getTimezoneOffset())));

            }

        }


        if(isTwoSupposedCloseEventsDeviateTooMuch(sleep, goToBed, 120 * DateTimeConstants.MILLIS_PER_MINUTE)){
            LOGGER.warn("Go to bed and sleep off too much, out of bed {}, sleep {}, eliminate sleep.",
                    new DateTime(goToBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBed.getTimezoneOffset())),
                    new DateTime(sleep.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.getTimezoneOffset())));
            fixedSleepEvents.set(1, Optional.<Event>absent());
        }

        if(isTwoSupposedCloseEventsDeviateTooMuch(wakeUp, outOfBed, 120 * DateTimeConstants.MILLIS_PER_MINUTE)){


            if(features.get(MotionFeatures.FeatureType.MAX_AMPLITUDE).size() > 0){
                final List<AmplitudeData> motion = features.get(MotionFeatures.FeatureType.MAX_AMPLITUDE);
                final long lastMotionTimestamp = motion.get(motion.size() - 1).timestamp;
                if(Math.abs(outOfBed.getStartTimestamp() - lastMotionTimestamp) > 2 * DateTimeConstants.MILLIS_PER_HOUR){
                    LOGGER.warn("Wake up and out of bed off too much, out of bed {}, wake up {}, eliminate both.",
                            new DateTime(outOfBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.getTimezoneOffset())),
                            new DateTime(wakeUp.getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.getTimezoneOffset())));
                    fixedSleepEvents.set(2, Optional.<Event>absent());
                    fixedSleepEvents.set(3, Optional.<Event>absent());

                }else{
                    LOGGER.warn("Wake up and out of bed off too much, out of bed {}, wake up {}, eliminate wake up.",
                            new DateTime(outOfBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.getTimezoneOffset())),
                            new DateTime(wakeUp.getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.getTimezoneOffset())));

                    // The one more close to last motion is more likely to be correct
                    fixedSleepEvents.set(2, Optional.<Event>absent());
                }
            }

        }

        if(userLeftBedInBetweenForALongTime(fixedSleepEvents, features.get(MotionFeatures.FeatureType.HOURLY_MOTION_COUNT))){
            LOGGER.warn("User left bed too long, dismiss in bed and fall asleep events");
            fixedSleepEvents.set(0, Optional.<Event>absent());
            fixedSleepEvents.set(1, Optional.<Event>absent());
        }

        if(isInBedPeriodTooShort(fixedSleepEvents, 4 * DateTimeConstants.MILLIS_PER_HOUR)){
            fixedSleepEvents.set(0, Optional.<Event>absent());
            fixedSleepEvents.set(3, Optional.<Event>absent());
            LOGGER.warn("In bed {} - {} less than 4 hours, eliminate both.",
                    new DateTime(goToBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBed.getTimezoneOffset())),
                    new DateTime(outOfBed.getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.getTimezoneOffset())));
        }

        if(isSleepPeriodTooShort(fixedSleepEvents, 4 * DateTimeConstants.MILLIS_PER_HOUR)){

            fixedSleepEvents.set(1, Optional.<Event>absent());
            fixedSleepEvents.set(2, Optional.<Event>absent());
            LOGGER.warn("sleep {} - {} less than 4 hours, eliminate both.",
                    new DateTime(sleep.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.getTimezoneOffset())),
                    new DateTime(wakeUp.getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.getTimezoneOffset())));
        }



        return fixedSleepEvents;
    }
}
