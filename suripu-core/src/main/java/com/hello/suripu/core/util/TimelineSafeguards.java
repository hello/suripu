package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.algorithmintegration.OneDaysSensorData;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Created by benjo on 5/7/15.
 */
public class TimelineSafeguards {

    private static final long NUM_MILLIS_IN_A_MINUTE = 60000L;

    public static final int MINIMUM_SLEEP_DURATION_MINUTES = 180; //three hours
    private static final int MAXIMUM_ALLOWABLE_DATAGAP = 60; //one hour

    //sleep period specific thresholds
    public static final int MAXIMUM_ALLOWABLE_MOTION_GAP_PRIMARY_PERIOD = 180;
    public static final int MAXIMUM_ALLOWABLE_MOTION_GAP_ALTERNATIVE_PERIOD = 120;
    public static final int MINIMUM_MOTION_COUNT_DURING_SLEEP_PRIMARY_PERIOD = 3;
    public static final int MINIMUM_MOTION_COUNT_DURING_SLEEP_ALTERNATIVE_PERIOD = 10;

    private static final int NUM_WINDOWS = 5;
    private static final double[] VALID_NIGHT_PROB_THRESHOLD ={0.4, 0.5, 0.03};
    private static final double[] VALID_NIGHT_PROB_THRESHOLD_DAYSLEEPER ={0.05, .4, 0.1};
    private static final double[] VALID_NIGHT_PROB_PARTNERED_THRESHOLD ={0.0, 0.0, 0.0};
    private static final double[] VALID_NIGHT_PROB_PARTNERED_THRESHOLD_DAYSLEEPER ={0.1, 0.4, 0.5};

    private static final float[] LOG_REG_COEFS = {6.33084651f, -2.94445174e-03f,   3.95446643e-04f,  -3.94746094e-02f,
            6.81937941e-02f,   9.57748145e-04f,  -1.05556490e-02f,
            -3.32549048e-02f,  -3.57850343e-03f,   6.56891606e-04f,
            -7.47461201e-03f,  -2.70489331e-02f,   1.04637402e-02f,
            3.72090772e-03f,  -1.86005590e-02f,  -7.32477756e-02f,
            1.24426925e-02f,  -5.21065766e-04f,  -1.64868728e-02f,
            1.83012667e-02f,  -2.22101831e-01f,  -1.16461367f};

    private static final Integer [][][] FEATURE_WINDOW_TIME_OFFSETS= {{{0,0},{1,45},{2,0}},{{2,0},{3,45},{4,0}},{{4,0},{5,45},{6,0}},{{6,0},{7,45},{8,0}},{{8,0},{9,45},{10,0}}};

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


    public static int getMaximumMotionGapInMinutes(final ImmutableList<TrackerMotion> trackerMotions, final long sleepTime, final long wakeTime) {
        Iterator<TrackerMotion> it = trackerMotions.iterator();
        boolean first = true;
        Long lastTime = sleepTime;
        int maxGapInMinutes = 0;
        while (it.hasNext()) {
            final TrackerMotion trackerMotion = it.next();
            if (trackerMotion.timestamp < sleepTime) {
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
        final int sleepWindowPadding = 30 * DateTimeConstants.MILLIS_PER_MINUTE; //excludes first 30 and last 30 minutes of sleeps
        final int motionCount = getMotionCount(trackerMotions, fallAsleepTimestamp + sleepWindowPadding, wakeUpTimestamp - sleepWindowPadding);
        if (motionCount < minMotionCount && sleepDuration > requiredSleepDuration) {
            return false;
        }
        return true;
    }

    public static int getMotionCount(final ImmutableList<TrackerMotion> trackerMotions, final Long startTime, final Long endTime){

        int motionCount = 0;

        // Compute first to last motion time delta
        for (final TrackerMotion motion : trackerMotions) {
            if (motion.timestamp > endTime) {
                break;
            }
            if (motion.timestamp >= startTime) {
                motionCount += 1;
            }
        }
        return motionCount;
    }

    public static double getSensorAvg(final Sensor sensor, final List<Sample> samples, final Long startTime, final Long endTime){
        int sensorCount = 0;
        float sensorSum = 0;

        // Compute first to last motion time delta
        for (final Sample sample: samples) {
            if (sample.dateTime > endTime) {
                break;
            }
            if (sample.dateTime >= startTime) {
                double val = sample.value;
                if (sensor == Sensor.LIGHT){
                    val = Math.log((val * 65536)/256  + 1.0) / Math.log(2);
                }
                if (sensor == Sensor.SOUND_PEAK_DISTURBANCE){
                    val = Math.max(val / 10 - 4.0,0);

                }
                sensorCount += 1;
                sensorSum += val;
            }
        }
        if (sensorCount == 0){
            return 10;
        }
        final double meanSensorCount = sensorSum / sensorCount;
        return meanSensorCount;
    }


    /* takes sensor data, and timeline events and decides if there might be some problems with this timeline  */
    public TimelineError checkIfValidTimeline (final long accountId, final boolean isPrimaryPeriod, final Optional<SleepPeriod> checkSleepPeriod, final AlgorithmType algorithmType, SleepEvents<Optional<Event>> sleepEvents, ImmutableList<Event> extraEvents, final ImmutableList<Sample> lightData, final ImmutableList<TrackerMotion> processedTrackerMotions) {
        final SleepPeriod.Period period;
        if (checkSleepPeriod.isPresent()){
            period = checkSleepPeriod.get().period;
        } else{
            period = SleepPeriod.Period.NIGHT;
        }

        final int minMotionCountThreshold, maxMotionGapThreshold;

        if (isPrimaryPeriod) {
            minMotionCountThreshold = MINIMUM_MOTION_COUNT_DURING_SLEEP_PRIMARY_PERIOD;
            maxMotionGapThreshold = MAXIMUM_ALLOWABLE_MOTION_GAP_PRIMARY_PERIOD;
        } else {
            minMotionCountThreshold = MINIMUM_MOTION_COUNT_DURING_SLEEP_ALTERNATIVE_PERIOD;
            maxMotionGapThreshold = MAXIMUM_ALLOWABLE_MOTION_GAP_ALTERNATIVE_PERIOD;
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
                LOGGER.warn("action=invalidating-timeline reason=insufficient-sleep-duration account_id={} sleep_duration={} period_period={}", accountId, sleepDurationInMinutes, period);
                return TimelineError.NOT_ENOUGH_HOURS_OF_SLEEP;
            }
        }

        final int maxDataGapInMinutes = getMaximumDataGapInMinutes(lightData);

        if (maxDataGapInMinutes > MAXIMUM_ALLOWABLE_DATAGAP) {
            LOGGER.warn("action=invalidating-timeline reason=max-data-gap-greater-than-limit account_id={} data-gap-minutes={} limit={} sleep_period={} ", accountId, maxDataGapInMinutes, MAXIMUM_ALLOWABLE_DATAGAP, period);
            return TimelineError.DATA_GAP_TOO_LARGE;
        }

        if (sleepEvents.wakeUp.isPresent() && sleepEvents.fallAsleep.isPresent()) {
            final boolean motionDuringSleep = motionDuringSleepCheck(minMotionCountThreshold, processedTrackerMotions, sleepEvents.fallAsleep.get().getStartTimestamp(), sleepEvents.wakeUp.get().getStartTimestamp());
            if (!motionDuringSleep) {
                LOGGER.warn("action=invalidating-timeline reason=insufficient-motion-during-sleeptime account_id={} sleep_period={}", accountId, period);
                return TimelineError.NO_MOTION_DURING_SLEEP;
            }
        }

        //check if we need to check inBedTime is valid for three sleep period support
        if (checkSleepPeriod.isPresent()) {
            //if checking all three sleep periods, require motion gap check:
            final int maxMotionGapInMinutes = getMaximumMotionGapInMinutes(processedTrackerMotions, sleepEvents.fallAsleep.get().getStartTimestamp(), sleepEvents.wakeUp.get().getStartTimestamp());
            if (maxMotionGapInMinutes > maxMotionGapThreshold) {
                LOGGER.warn("action=invalidating-timeline reason=max=motion-gap-too-large motion_gap={} threshold={} sleep_period={}", maxMotionGapInMinutes, maxMotionGapThreshold, period);
                return TimelineError.MOTION_GAP_TOO_LARGE;
            }
            if (sleepEvents.goToBed.isPresent() && !isValidInBedTime(checkSleepPeriod.get(), sleepEvents.goToBed.get())) {
                    return TimelineError.IN_BED_EVENT_OUTSIDE_SLEEP_PERIOD;
            }

        }
        return TimelineError.NO_ERROR;

    }

    private boolean isValidInBedTime(final SleepPeriod sleepPeriod, final Event inBedEvent){

        if(inBedEvent.getStartTimestamp() >=  sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.END_IN_BED, inBedEvent.getTimezoneOffset()).getMillis()){
            return false;
        }
        if(inBedEvent.getStartTimestamp() <  sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.START, inBedEvent.getTimezoneOffset()).getMillis()){
            return false;
        }
        return true;
    }

    public static boolean isProbableNight(final Long accountId, final boolean daySleeper, final SleepPeriod sleepPeriod, final OneDaysSensorData oneDaysSensorData){
        // thresholds based on estimated prevalence of day sleepers
        final boolean hasPartner = oneDaysSensorData.oneDaysPartnerMotion.originalTrackerMotions.size() > 0;

        if(!oneDaysSensorData.allSensorSampleList.getAvailableSensors().contains(Sensor.LIGHT) || !oneDaysSensorData.allSensorSampleList.getAvailableSensors().contains(Sensor.SOUND_PEAK_ENERGY)){
            return false;
        }

        final List<Sample> lightDataPeriod = oneDaysSensorData.allSensorSampleList.get(Sensor.LIGHT) ;
        final List<Sample> soundDataPeriod = oneDaysSensorData.allSensorSampleList.get(Sensor.SOUND_PEAK_DISTURBANCE) ;
        final int offset = lightDataPeriod.get(lightDataPeriod.size() -1).offsetMillis;

        //if inbed period not over or user is not a night sleeper, default to day sleeper
        double[] validNightProbabilityThreshold = VALID_NIGHT_PROB_THRESHOLD_DAYSLEEPER;

        if(hasPartner){
            if(daySleeper){
                validNightProbabilityThreshold = VALID_NIGHT_PROB_PARTNERED_THRESHOLD_DAYSLEEPER;
            }else {
                validNightProbabilityThreshold = VALID_NIGHT_PROB_PARTNERED_THRESHOLD;
            }
        }else {
            if (daySleeper) {
                validNightProbabilityThreshold = VALID_NIGHT_PROB_THRESHOLD_DAYSLEEPER;
            }
            else{
                validNightProbabilityThreshold = VALID_NIGHT_PROB_THRESHOLD;
            }
        }

        final ImmutableList<TrackerMotion> processedTrackerMotionsPeriod = ImmutableList.copyOf(oneDaysSensorData.oneDaysTrackerMotion.getMotionsForTimeWindow(sleepPeriod.getSleepPeriodMillis(SleepPeriod.Boundary.START, offset), sleepPeriod.getSleepPeriodMillis(SleepPeriod.Boundary.END_DATA, offset),false).processedtrackerMotions);


        //features: inbed period broken in to 4 2-hour windows (plus following window). max inter motion gap, motion count, avg light for last 15 minutes and avg sound of last 15 minutes calculated
        final List<Integer> maxGaps = Lists.newArrayListWithExpectedSize(NUM_WINDOWS);
        final List<Integer> motionCounts = Lists.newArrayListWithExpectedSize(NUM_WINDOWS);
        final List<Double> avgEndLight= Lists.newArrayListWithExpectedSize(NUM_WINDOWS);
        final List<Double> avgEndSound= Lists.newArrayListWithExpectedSize(NUM_WINDOWS);

        final DateTime sleepPeriodStartTime = sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.START, oneDaysSensorData.timezoneOffsetMillis);
        for (int i = 0; i < NUM_WINDOWS; i ++){
            final DateTime startTimeMotionWindow = sleepPeriodStartTime.plusHours(FEATURE_WINDOW_TIME_OFFSETS[i][0][0]).plusMinutes(FEATURE_WINDOW_TIME_OFFSETS[i][0][1]);
            int j = FEATURE_WINDOW_TIME_OFFSETS[i][0][1];
            final DateTime startTimeSensorWindow =sleepPeriodStartTime.plusHours(FEATURE_WINDOW_TIME_OFFSETS[i][1][0]).plusMinutes(FEATURE_WINDOW_TIME_OFFSETS[i][1][1]);
            final DateTime endTimeWindow =         sleepPeriodStartTime.plusHours(FEATURE_WINDOW_TIME_OFFSETS[i][2][0]).plusMinutes(FEATURE_WINDOW_TIME_OFFSETS[i][2][1]);
            final int maxMotionGapForWindow = getMaximumMotionGapInMinutes(processedTrackerMotionsPeriod, startTimeMotionWindow.getMillis(), endTimeWindow.getMillis());
            final int motionCountForWindow = getMotionCount(processedTrackerMotionsPeriod, startTimeMotionWindow.getMillis(), endTimeWindow.getMillis());
            final double avgLightEndOfWindow = getSensorAvg(Sensor.LIGHT, lightDataPeriod, startTimeSensorWindow.getMillis(), endTimeWindow.getMillis());
            final double avgSoundEndOfWindow = getSensorAvg(Sensor.SOUND_PEAK_DISTURBANCE, soundDataPeriod, startTimeSensorWindow.getMillis(), endTimeWindow.getMillis());
            maxGaps.add(maxMotionGapForWindow);
            motionCounts.add(motionCountForWindow);
            avgEndLight.add(avgLightEndOfWindow);
            avgEndSound.add(avgSoundEndOfWindow);
        }

        final double prob = getProbability(sleepPeriod.period, maxGaps, motionCounts, avgEndLight, avgEndSound);
        if (prob > validNightProbabilityThreshold[sleepPeriod.period.getValue()]) {
            return true;
        }
       return false;
    }
    private static double getProbability(final SleepPeriod.Period period, final List<Integer> maxGaps, final List<Integer> motionCounts, final List<Double> avgEndSound, final List<Double> avgEndLight){
        final double z = LOG_REG_COEFS[0] +
                LOG_REG_COEFS[ 1]*motionCounts.get(0) + LOG_REG_COEFS[2]*maxGaps.get(0) + LOG_REG_COEFS[ 3]*avgEndLight.get(0) + LOG_REG_COEFS[4]*avgEndSound.get(0) +
                LOG_REG_COEFS[ 5]*motionCounts.get(1) + LOG_REG_COEFS[6]*maxGaps.get(1) + LOG_REG_COEFS[ 7]*avgEndLight.get(1) + LOG_REG_COEFS[8]*avgEndSound.get(1) +
                LOG_REG_COEFS[ 9]*motionCounts.get(2) + LOG_REG_COEFS[10]*maxGaps.get(2) + LOG_REG_COEFS[11]*avgEndLight.get(2) + LOG_REG_COEFS[12]*avgEndSound.get(2) +
                LOG_REG_COEFS[ 13]*motionCounts.get(3) + LOG_REG_COEFS[14]*maxGaps.get(3) + LOG_REG_COEFS[15]*avgEndLight.get(3) + LOG_REG_COEFS[16]*avgEndSound.get(3) +
                LOG_REG_COEFS[ 17]*motionCounts.get(4) + LOG_REG_COEFS[18]*maxGaps.get(4) + LOG_REG_COEFS[19]*avgEndLight.get(4) + LOG_REG_COEFS[20]*avgEndSound.get(4) +
                LOG_REG_COEFS[21]*period.getValue();

        final double exp = Math.exp(z);
        final double prob = exp / (1 + exp);
      return prob;

    }

}
