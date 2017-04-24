package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.FallingAsleepEvent;
import com.hello.suripu.core.models.Events.InBedEvent;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;
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

    public static boolean isInBedPeriodTooShort(final SleepEvents<Optional<Event>> finalEvents, final long longEnoughThresholdMillis){
        return finalEvents.goToBed.isPresent() && finalEvents.outOfBed.isPresent() &&
                finalEvents.outOfBed.get().getStartTimestamp() - finalEvents.goToBed.get().getEndTimestamp() < longEnoughThresholdMillis;
    }

    public static boolean isSleepPeriodTooShort(final SleepEvents<Optional<Event>> finalEvents, final long longEnoughThresholdMillis){
        return finalEvents.fallAsleep.isPresent() && finalEvents.wakeUp.isPresent() &&
                finalEvents.wakeUp.get().getStartTimestamp() - finalEvents.fallAsleep.get().getEndTimestamp() < longEnoughThresholdMillis;
    }

    public static boolean isTwoSupposedCloseEventsDeviateTooMuch(final Event event1, final Event event2, final long tooOffThresholdMillis){
        return Math.abs(event1.getStartTimestamp() - event2.getStartTimestamp()) > tooOffThresholdMillis;
    }

    public static boolean userLeftBedInBetweenForALongTime(final SleepEvents<Optional<Event>> finalEvents, final List<AmplitudeData> hourlyMotionCounts){
        // please refer to mark@prigg.com, night of 2015-02-14
        // At that night, the user in bed for a while, then turn light off and go out.
        // 2 hours later he came back, go in bed WITHOUT switching the light on and off

        long inBedOrFallAsleepTimeMillis = 0;
        if(finalEvents.goToBed.isPresent()){
            inBedOrFallAsleepTimeMillis = finalEvents.goToBed.get().getStartTimestamp();
        }

        if(finalEvents.fallAsleep.isPresent()){
            inBedOrFallAsleepTimeMillis = finalEvents.fallAsleep.get().getStartTimestamp();
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

    public static SleepEvents<Optional<Event>> sleepEventsHeuristicFix(final SleepEvents<Event> sleepEvents, final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features){
        Optional<Event> goToBed = Optional.of(sleepEvents.goToBed);
        Optional<Event> sleep = Optional.of(sleepEvents.fallAsleep);
        Optional<Event> wakeUp = Optional.of(sleepEvents.wakeUp);
        Optional<Event> outOfBed = Optional.of(sleepEvents.outOfBed);


        SleepEvents<Optional<Event>> fixedSleepEvents = SleepEvents.create(goToBed, sleep, wakeUp, outOfBed);


        if(isEventOverlapped(sleep.get(), goToBed.get())){
            sleep = Optional.of((Event) new FallingAsleepEvent(sleep.get().getSleepPeriod(),sleep.get().getStartTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    sleep.get().getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,  sleep.get().getTimezoneOffset()));
            LOGGER.warn("Sleep {} has the same time with in bed, set to in bed +1 minute.",
                    new DateTime(sleep.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.get().getTimezoneOffset())));
        }

        if(isEventOverlapped(wakeUp.get(), outOfBed.get())){
            outOfBed = Optional.of((Event) new OutOfBedEvent(outOfBed.get().getSleepPeriod(), outOfBed.get().getStartTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    outOfBed.get().getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    outOfBed.get().getTimezoneOffset()));
            LOGGER.warn("Out of bed {} has the same time with wake up, set to wake up +1 minute.",
                    new DateTime(outOfBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.get().getTimezoneOffset())));
        }

        // Heuristic fix
        if(isEventsOutOfOrder(goToBed.get(), sleep.get())) {

            if(goToBed.get().getStartTimestamp() - sleep.get().getEndTimestamp() < 40 * DateTimeConstants.MILLIS_PER_MINUTE) {
                LOGGER.warn("Go to bed {} later then fall asleep {}, go to bed set to sleep.",
                        new DateTime(goToBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBed.get().getTimezoneOffset())),
                        new DateTime(sleep.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.get().getTimezoneOffset())));

                goToBed = Optional.of((Event) new InBedEvent(sleep.get().getSleepPeriod(), sleep.get().getStartTimestamp() - DateTimeConstants.MILLIS_PER_MINUTE,
                        sleep.get().getEndTimestamp() - DateTimeConstants.MILLIS_PER_MINUTE,
                        sleep.get().getTimezoneOffset()));
            }else{
                LOGGER.warn("go to bed later than sleep too much, go to bed {}, sleep {}, remove go to bed.",
                        new DateTime(goToBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBed.get().getTimezoneOffset())),
                        new DateTime(sleep.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.get().getTimezoneOffset())));
                goToBed = Optional.absent(); // TODO: add it back
            }

        }

        // Heuristic fix: wake up time is later than out of bed, use out of bed because it looks
        // for the most significant motion
        if(isEventsOutOfOrder(wakeUp.get(), outOfBed.get())){
            // Huge spike before motion+spikes, has motion in between
            // already wake up?
            if(wakeUp.get().getStartTimestamp() - outOfBed.get().getEndTimestamp() < 40 * DateTimeConstants.MILLIS_PER_MINUTE) {
                LOGGER.warn("Wake up later than out of bed, wake up {}, out of bed {}, out of bed set to wake up + 1.",
                        new DateTime(wakeUp.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.get().getTimezoneOffset())),
                        new DateTime(outOfBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.get().getTimezoneOffset())));


                outOfBed = Optional.of((Event) new OutOfBedEvent(wakeUp.get().getSleepPeriod(), wakeUp.get().getEndTimestamp(),
                        wakeUp.get().getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                        wakeUp.get().getTimezoneOffset()));
            }else{
                LOGGER.warn("Wake up later than out of bed too much, wake up {}, out of bed {}, remove out of bed.",
                        new DateTime(wakeUp.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.get().getTimezoneOffset())),
                        new DateTime(outOfBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.get().getTimezoneOffset())));

                outOfBed = Optional.absent(); // TODO: add it back

            }

        }


        if(goToBed.isPresent() && isTwoSupposedCloseEventsDeviateTooMuch(sleep.get(), goToBed.get(), 120 * DateTimeConstants.MILLIS_PER_MINUTE)){
            LOGGER.warn("Go to bed and sleep off too much, go to bed {}, sleep {}, eliminate sleep.",
                    new DateTime(goToBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBed.get().getTimezoneOffset())),
                    new DateTime(sleep.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.get().getTimezoneOffset())));
            sleep = Optional.absent(); // TODO: add it back - pick the first time motion becomes zero??
        }

        if(outOfBed.isPresent() && isTwoSupposedCloseEventsDeviateTooMuch(wakeUp.get(), outOfBed.get(), 120 * DateTimeConstants.MILLIS_PER_MINUTE)){

            if(features.get(MotionFeatures.FeatureType.MAX_AMPLITUDE).size() > 0){
                final List<AmplitudeData> motion = features.get(MotionFeatures.FeatureType.MAX_AMPLITUDE);
                final long lastMotionTimestamp = motion.get(motion.size() - 1).timestamp;
                if(Math.abs(outOfBed.get().getStartTimestamp() - lastMotionTimestamp) > 2 * DateTimeConstants.MILLIS_PER_HOUR){
                    LOGGER.warn("Wake up and out of bed off too much, out of bed {}, wake up {}, eliminate both.",
                            new DateTime(outOfBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.get().getTimezoneOffset())),
                            new DateTime(wakeUp.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.get().getTimezoneOffset())));
                    wakeUp = Optional.<Event>absent();
                    outOfBed = Optional.<Event>absent(); // TODO: maids fix??

                }else{
                    LOGGER.warn("Wake up and out of bed off too much, out of bed {}, wake up {}, eliminate wake up.",
                            new DateTime(outOfBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.get().getTimezoneOffset())),
                            new DateTime(wakeUp.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.get().getTimezoneOffset())));

                    // The one more close to last motion is more likely to be correct
                    wakeUp = Optional.<Event>absent(); // todo: out of bed closer to last motion, more likely to be correct. set wake up to be between?
                }
            }

        }

        fixedSleepEvents = SleepEvents.create(goToBed, sleep, wakeUp, outOfBed);
        if(goToBed.isPresent() && sleep.isPresent() && userLeftBedInBetweenForALongTime(fixedSleepEvents, features.get(MotionFeatures.FeatureType.HOURLY_MOTION_COUNT))){
            LOGGER.warn("User left bed too long, dismiss in bed and fall asleep events");
            goToBed = Optional.<Event>absent();
            sleep = Optional.<Event>absent();
        }

        if(goToBed.isPresent() && outOfBed.isPresent() && isInBedPeriodTooShort(fixedSleepEvents, 4 * DateTimeConstants.MILLIS_PER_HOUR)){
            LOGGER.warn("In bed {} - {} less than 4 hours, eliminate both.",
                    new DateTime(goToBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBed.get().getTimezoneOffset())),
                    new DateTime(outOfBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.get().getTimezoneOffset())));

            goToBed = Optional.<Event>absent();
            outOfBed = Optional.<Event>absent();
        }

        if(sleep.isPresent() && wakeUp.isPresent() && isSleepPeriodTooShort(fixedSleepEvents, 4 * DateTimeConstants.MILLIS_PER_HOUR)){
            LOGGER.warn("sleep {} - {} less than 4 hours, eliminate both.",
                    new DateTime(sleep.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.get().getTimezoneOffset())),
                    new DateTime(wakeUp.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.get().getTimezoneOffset())));

            sleep = Optional.<Event>absent();
            wakeUp = Optional.<Event>absent();
        }

        fixedSleepEvents = SleepEvents.create(goToBed, sleep, wakeUp, outOfBed);

        return fixedSleepEvents;
    }


    public static SleepEvents<Optional<Event>> sleepEventsAggressiveHeuristicFix(final SleepEvents<Event> sleepEvents, final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features){
        Optional<Event> goToBed = Optional.of(sleepEvents.goToBed);
        Optional<Event> sleep = Optional.of(sleepEvents.fallAsleep);
        Optional<Event> wakeUp = Optional.of(sleepEvents.wakeUp);
        Optional<Event> outOfBed = Optional.of(sleepEvents.outOfBed);


        SleepEvents<Optional<Event>> fixedSleepEvents = SleepEvents.create(goToBed, sleep, wakeUp, outOfBed);


        if(isEventOverlapped(sleep.get(), goToBed.get())){
            sleep = Optional.of((Event) new FallingAsleepEvent(sleep.get().getStartTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    sleep.get().getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,  sleep.get().getTimezoneOffset()));
            LOGGER.warn("Sleep {} has the same time with in bed, set to in bed +1 minute.",
                    new DateTime(sleep.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.get().getTimezoneOffset())));
        }

        if(isEventOverlapped(wakeUp.get(), outOfBed.get())){
            outOfBed = Optional.of((Event) new OutOfBedEvent(outOfBed.get().getStartTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    outOfBed.get().getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                    outOfBed.get().getTimezoneOffset()));
            LOGGER.warn("Out of bed {} has the same time with wake up, set to wake up +1 minute.",
                    new DateTime(outOfBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.get().getTimezoneOffset())));
        }

        // Heuristic fix
        if(isEventsOutOfOrder(goToBed.get(), sleep.get())) {

            if(goToBed.get().getStartTimestamp() - sleep.get().getEndTimestamp() < 40 * DateTimeConstants.MILLIS_PER_MINUTE) {
                LOGGER.warn("Go to bed {} later then fall asleep {}, go to bed set to sleep.",
                        new DateTime(goToBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBed.get().getTimezoneOffset())),
                        new DateTime(sleep.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.get().getTimezoneOffset())));

                goToBed = Optional.of((Event) new InBedEvent(sleep.get().getStartTimestamp() - DateTimeConstants.MILLIS_PER_MINUTE,
                        sleep.get().getEndTimestamp() - DateTimeConstants.MILLIS_PER_MINUTE,
                        sleep.get().getTimezoneOffset()));
            }else{
                goToBed = Optional.of((Event) new InBedEvent(sleep.get().getStartTimestamp() - 5 * DateTimeConstants.MILLIS_PER_MINUTE,
                        sleep.get().getEndTimestamp() - 5 * DateTimeConstants.MILLIS_PER_MINUTE,
                        sleep.get().getTimezoneOffset()));
            }

        }

        // Heuristic fix: wake up time is later than out of bed, use out of bed because it looks
        // for the most significant motion
        if(isEventsOutOfOrder(wakeUp.get(), outOfBed.get())){
            // Huge spike before motion+spikes, has motion in between
            // already wake up?
            if(wakeUp.get().getStartTimestamp() - outOfBed.get().getEndTimestamp() < 40 * DateTimeConstants.MILLIS_PER_MINUTE) {
                LOGGER.warn("Wake up later than out of bed, wake up {}, out of bed {}, out of bed set to wake up + 1.",
                        new DateTime(wakeUp.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.get().getTimezoneOffset())),
                        new DateTime(outOfBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.get().getTimezoneOffset())));


                outOfBed = Optional.of((Event) new OutOfBedEvent(wakeUp.get().getEndTimestamp(),
                        wakeUp.get().getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                        wakeUp.get().getTimezoneOffset()));
            }else{
                LOGGER.warn("Wake up later than out of bed too much, wake up {}, out of bed {}, out of bed set to wake up + 5.",
                        new DateTime(wakeUp.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.get().getTimezoneOffset())),
                        new DateTime(outOfBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.get().getTimezoneOffset())));

                outOfBed = Optional.of((Event) new OutOfBedEvent(wakeUp.get().getEndTimestamp(),
                        wakeUp.get().getEndTimestamp() + 5 * DateTimeConstants.MILLIS_PER_MINUTE,
                        wakeUp.get().getTimezoneOffset()));

            }

        }


        if(goToBed.isPresent() && isTwoSupposedCloseEventsDeviateTooMuch(sleep.get(), goToBed.get(), 120 * DateTimeConstants.MILLIS_PER_MINUTE)){
            LOGGER.warn("Go to bed and sleep off too much, go to bed {}, sleep {}, eliminate sleep.",
                    new DateTime(goToBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBed.get().getTimezoneOffset())),
                    new DateTime(sleep.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.get().getTimezoneOffset())));
            // pick the first time motion becomes zero??
            final long goToBedEnd = goToBed.get().getEndTimestamp();
            final List<AmplitudeData> motionAmplitudes = features.get(MotionFeatures.FeatureType.MAX_AMPLITUDE);
            for(final AmplitudeData datum:motionAmplitudes){
                if(datum.timestamp < goToBedEnd){
                    continue;
                }
                if(datum.amplitude == 0){
                    sleep = Optional.of((Event) new FallingAsleepEvent(datum.timestamp,
                            datum.timestamp + DateTimeConstants.MILLIS_PER_MINUTE,  datum.offsetMillis));
                    break;
                }

            }
        }

        if(outOfBed.isPresent() && isTwoSupposedCloseEventsDeviateTooMuch(wakeUp.get(), outOfBed.get(), 120 * DateTimeConstants.MILLIS_PER_MINUTE)){

            if(features.get(MotionFeatures.FeatureType.MAX_AMPLITUDE).size() > 0){
                final List<AmplitudeData> motion = features.get(MotionFeatures.FeatureType.MAX_AMPLITUDE);
                final long lastMotionTimestamp = motion.get(motion.size() - 1).timestamp;
                if(Math.abs(outOfBed.get().getStartTimestamp() - lastMotionTimestamp) > 2 * DateTimeConstants.MILLIS_PER_HOUR){
                    LOGGER.warn("Wake up and out of bed off too much, out of bed {}, wake up {}, eliminate both.",
                            new DateTime(outOfBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.get().getTimezoneOffset())),
                            new DateTime(wakeUp.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.get().getTimezoneOffset())));
                    wakeUp = Optional.<Event>absent();
                    outOfBed = Optional.<Event>absent(); // TODO: maids fix??

                }else{
                    LOGGER.warn("Wake up and out of bed off too much, out of bed {}, wake up {}, set wake up to out of bed - 1 min.",
                            new DateTime(outOfBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.get().getTimezoneOffset())),
                            new DateTime(wakeUp.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.get().getTimezoneOffset())));

                    // The one more close to last motion is more likely to be correct
                    wakeUp = Optional.of((Event) new WakeupEvent(outOfBed.get().getStartTimestamp() - DateTimeConstants.MILLIS_PER_MINUTE,
                            outOfBed.get().getStartTimestamp(),
                            outOfBed.get().getTimezoneOffset()));
                }
            }

        }

        fixedSleepEvents = SleepEvents.create(goToBed, sleep, wakeUp, outOfBed);
        if(goToBed.isPresent() && sleep.isPresent() && userLeftBedInBetweenForALongTime(fixedSleepEvents, features.get(MotionFeatures.FeatureType.HOURLY_MOTION_COUNT))){
            LOGGER.warn("User left bed too long, dismiss in bed and fall asleep events");
            goToBed = Optional.<Event>absent();
            sleep = Optional.<Event>absent();
        }

        if(goToBed.isPresent() && outOfBed.isPresent() && isInBedPeriodTooShort(fixedSleepEvents, 4 * DateTimeConstants.MILLIS_PER_HOUR)){
            LOGGER.warn("In bed {} - {} less than 4 hours, eliminate both.",
                    new DateTime(goToBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(goToBed.get().getTimezoneOffset())),
                    new DateTime(outOfBed.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(outOfBed.get().getTimezoneOffset())));

            goToBed = Optional.<Event>absent();
            outOfBed = Optional.<Event>absent();
        }

        if(sleep.isPresent() && wakeUp.isPresent() && isSleepPeriodTooShort(fixedSleepEvents, 4 * DateTimeConstants.MILLIS_PER_HOUR)){
            LOGGER.warn("sleep {} - {} less than 4 hours, eliminate both.",
                    new DateTime(sleep.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleep.get().getTimezoneOffset())),
                    new DateTime(wakeUp.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(wakeUp.get().getTimezoneOffset())));

            sleep = Optional.<Event>absent();
            wakeUp = Optional.<Event>absent();
        }

        fixedSleepEvents = SleepEvents.create(goToBed, sleep, wakeUp, outOfBed);

        return fixedSleepEvents;
    }
}
