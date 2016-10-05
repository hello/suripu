package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.AgitatedSleep;
import com.hello.suripu.core.models.MotionFrequency;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.processors.insights.Lights;
import com.hello.suripu.core.processors.insights.Particulates;
import com.hello.suripu.core.processors.insights.SleepDuration;
import com.hello.suripu.core.processors.insights.TemperatureHumidity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
/**
 * Created by ksg on 02/25/15
 */
public class SleepScoreUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepScoreUtils.class);

    public static final Integer DURATION_MAX_SCORE = 80;
    public static final Integer DURATION_MIN_SCORE = 10;
    public static final float MOTION_SCORE_MIN = 10.0f;
    private static final float MOTION_SCORE_RANGE = 80.0f; // max score is 90
    private static final float MIN_ASLEEP_MINUTES_REQUIRED = 60.0f; // need to be asleep for at least 60 minutes

    public static final int PENALTY_PER_SOUND_EVENT = 20;
    public static final int SENSOR_IDEAL_SCORE = 100;
    public static final int SENSOR_WARNING_SCORE = 75;
    public static final int SENSOR_ALERT_SCORE = 50;

    public static final Float DURATION_SCORE_SCALE = (float) (DURATION_MAX_SCORE - DURATION_MIN_SCORE);
    public static final Integer TOO_LITTLE_SLEEP_ALLOWANCE = 1;
    public static final Integer TOO_MUCH_SLEEP_ALLOWANCE = 4; // allow too much sleep recommendation to exceed by 4 hours

    public static final Integer DURATION_V2_MIN_SCORE_HOURS = 2;
    public static final Integer DURATION_TOO_MUCH_HOURS = 15;
    public static final Integer DURATION_TOO_MUCH_SCORE = 50;

    public static final Integer MAX_TIMES_AWAKE_PENALTY_SCORE = -30;
    public static final Integer AWAKE_PENALTY_SCORE = -5; // minus 5 for each time-awake

    public static final int DURATION_MAX_V3 = 720; //12 hours
    public static final int DURATION_POP_IDEAL = 480; //8 hours
    public static final float RAW_SCORE_MAX_DUR_V3 = 52.866f;//raw score if sleep > 12 hours
    public static final float[] DURATION_WEIGHTS_V3 = new float[]{14.8027f, 4.3001e-01f, -2.7177e-03f, 8.2262e-06f, -1.1033e-08f, 5.333e-12f};
    public static final float[] DURATION_WEIGHTS_V4 = new float[]{ -112.81f, 3.30f, -0.19f, -19.40f, -54.14f, 37.71f,-3.77f};
    public static final float[] DURATION_WEIGHTS_V5 = new float[]{ -107.20f, 3.22f, -.10f, 1.0f,-1.99f};

    public static final float[] MOTION_FREQUENCY_PENALTY = new float[]{ -50.42f, -84.60f, 5.724f};
    public static final float MOTION_FREQUENCY_THRESHOLD_DEFAULT = 0.084f;
    public static final float MOTION_FREQUENCY_THRESHOLD_MIN = 0.01f;
    public static final float MOTION_FREQUENCY_THRESHOLD_MAX = 0.16f;
    public static final float RELATIVE_MOTION_FREQUENCY_MAX = .25f;

    public static final long SLEEP_SCORE_V2_V4_TRANSITION_EPOCH = 1470009600000L; //2016-08-01 utc
    public static final float SLEEP_SCORE_V2_V4_TRANSITION_WEIGHTING = 0.0333f; // full transition in 30 days

    public static final long SLEEP_SCORE_V4_V5_TRANSITION_EPOCH = 1475971200000L; //2016-10-09 utc
    public static final float SLEEP_SCORE_V4_V5_TRANSITION_WEIGHTING = 0.0715f; // full transition in 14 days


    /**
     * compute a score based on sleep duration.
     * if duration is within recommended range, return max score
     * if duration is way out of the min/max + allowance recommended range, return min score
     * everything else is a linear decrease in score
     * @param userAgeInYears age
     * @param sleepDurationMinutes sleep duration
     * @return duration score
     */
    public static Integer getSleepDurationScore(final int userAgeInYears, final Integer sleepDurationMinutes) {
        final SleepDuration.recommendation idealHours = SleepDuration.getSleepDurationRecommendation(userAgeInYears);

        final Float sleepDurationHours = (float) sleepDurationMinutes / 60.0f;
        if (sleepDurationHours >= (float) idealHours.minHours && sleepDurationHours <= idealHours.maxHours) {
            return DURATION_MAX_SCORE;
        }

        if (sleepDurationHours < (float) (idealHours.absoluteMinHours - TOO_LITTLE_SLEEP_ALLOWANCE) ||
                sleepDurationHours > (float) (idealHours.absoluteMaxHours + TOO_MUCH_SLEEP_ALLOWANCE)) {
            return DURATION_MIN_SCORE;
        }

        Float diffMinutes;
        Float scaleMinutes;
        if (sleepDurationHours < (float) idealHours.minHours) {
            // under-sleep
            diffMinutes = (float) (idealHours.minHours * 60  - sleepDurationMinutes);
            scaleMinutes = (idealHours.minHours - idealHours.absoluteMinHours + 1) * 60.0f;
        } else {
            // over-slept
            diffMinutes = (float) (sleepDurationMinutes - idealHours.maxHours * 60);
            scaleMinutes = (idealHours.absoluteMaxHours + TOO_MUCH_SLEEP_ALLOWANCE - idealHours.maxHours) * 60.0f;
        }
        return DURATION_MAX_SCORE - Math.round((diffMinutes / scaleMinutes) * DURATION_SCORE_SCALE);
    }

    public static Integer getSleepDurationScoreV2(final int userAgeInYears,  final Integer sleepDurationMinutes) {
        final SleepDuration.recommendation idealHours = SleepDuration.getSleepDurationRecommendation(userAgeInYears);
        final Float sleepDurationHours = (float) sleepDurationMinutes / 60.0f;

        final float baseScore;
        final float topScore;
        final float diffMinutes;
        final float bucketTotalMinutes;

        if (sleepDurationHours < DURATION_V2_MIN_SCORE_HOURS) {
            // if you sleep less than 2 hours, you're doomed.
            return DURATION_MIN_SCORE;
        }

        else if (sleepDurationHours > DURATION_TOO_MUCH_HOURS) {
            // sleep too much, doomed as well.
            return DURATION_TOO_MUCH_SCORE;
        }

        else if (sleepDurationHours < idealHours.absoluteMinHours) {
            // between 2 hours to ideal-min
            // score is between 10 - 60
            // this awards the user 1 extra point for every ~5 minutes of sleep
            baseScore = (float) DURATION_MIN_SCORE;
            topScore = 70.0f;
            bucketTotalMinutes = idealHours.absoluteMinHours - DURATION_V2_MIN_SCORE_HOURS;
            diffMinutes = sleepDurationMinutes - (DURATION_V2_MIN_SCORE_HOURS * 60.0f);
        }

        else if (sleepDurationHours < idealHours.maxHours) {
            // between absolute min and ideal-max, score between 60 - 95
            baseScore = 70.0f;
            topScore = 95.0f;
            bucketTotalMinutes = idealHours.maxHours - idealHours.absoluteMinHours;
            diffMinutes = sleepDurationMinutes - (idealHours.absoluteMinHours * 60.0f);
        }

        // between ideal-max and absolute max, score between 70 - 80
        else if (sleepDurationHours < idealHours.absoluteMaxHours) {
            baseScore = 90.0f;
            topScore = 70.0f;
            bucketTotalMinutes = idealHours.absoluteMaxHours - idealHours.maxHours;
            diffMinutes = sleepDurationMinutes - (idealHours.maxHours * 60.0f);
        }

        else {
            // between absolute-max and way-too-much sleep, reduce score
            baseScore = 70.0f;
            topScore = DURATION_TOO_MUCH_SCORE;
            bucketTotalMinutes = DURATION_TOO_MUCH_HOURS - idealHours.absoluteMaxHours;
            diffMinutes = sleepDurationMinutes - (idealHours.absoluteMaxHours) * 60.0f;
        }

        return Math.round(baseScore + diffMinutes * ((topScore - baseScore) / (bucketTotalMinutes * 60.0f)));
    }


    public static float getSleepScoreDurationV3(final int userAgeInYears, final Integer sleepDurationThreshold, final Integer sleepDurationMinutes) {
        final SleepDuration.recommendation idealHours = SleepDuration.getSleepDurationRecommendation(userAgeInYears);
        final float rawScoreV3;
        final long adjSleepDurationV3p2, adjSleepDurationV3p3, adjSleepDurationV3p4, adjSleepDurationV3p5;
        final Integer adjSleepDurationV3, sleepDurationTargetV3;
        //Sets sleep duration target to individualized ideal within age-specific range
        if (sleepDurationThreshold == 0){

            if (userAgeInYears < 18){
                sleepDurationTargetV3 = (idealHours.minHours +idealHours.maxHours)/2 * 60;
            }else {
                sleepDurationTargetV3 = DURATION_POP_IDEAL;
            }

        }else if (sleepDurationThreshold > idealHours.maxHours*60) {
            sleepDurationTargetV3 = idealHours.maxHours*60;
        }else if (sleepDurationThreshold < idealHours.minHours*60) {
            sleepDurationTargetV3 = idealHours.minHours*60;
        }else {
            sleepDurationTargetV3 = sleepDurationThreshold;
        }

        //Adjusted sleep duration based on deviations from population mean, reduced magnitude of delta
        adjSleepDurationV3 = sleepDurationMinutes + (DURATION_POP_IDEAL - sleepDurationTargetV3)/2;

        if (adjSleepDurationV3 > DURATION_MAX_V3) {
            rawScoreV3 = RAW_SCORE_MAX_DUR_V3;
        }else{
            //rawScore calculated using 5th degree polynomial model to extrapolate change in sleep quality with sleep duration
            adjSleepDurationV3p2 = adjSleepDurationV3 * adjSleepDurationV3;
            adjSleepDurationV3p3 = adjSleepDurationV3p2 * adjSleepDurationV3;
            adjSleepDurationV3p4 = adjSleepDurationV3p3 * adjSleepDurationV3;
            adjSleepDurationV3p5 = adjSleepDurationV3p4 * adjSleepDurationV3;

            rawScoreV3 = DURATION_WEIGHTS_V3[0]+ DURATION_WEIGHTS_V3[1] * adjSleepDurationV3 + DURATION_WEIGHTS_V3[2] * adjSleepDurationV3p2 + DURATION_WEIGHTS_V3[3] * adjSleepDurationV3p3 + DURATION_WEIGHTS_V3[4] * adjSleepDurationV3p4 + DURATION_WEIGHTS_V3[5] * adjSleepDurationV3p5;
        }

        return rawScoreV3;
    }

    public static Integer getSleepScoreDurationV4(final long accountId, final float sleepDurationScoreV3, final MotionFrequency motionFrequency, final Integer timesAwake, final int agitatedSleepDuration) {
        final float maxMotionFreq = 0.25f;
        final int maxTimesAwake = 6;
        final int maxAgitatedSleep = 45;
        final float rawScore = DURATION_WEIGHTS_V4[0] + DURATION_WEIGHTS_V4[1] * sleepDurationScoreV3 + DURATION_WEIGHTS_V4[2] * Math.min(agitatedSleepDuration, maxAgitatedSleep) + DURATION_WEIGHTS_V4[3] * Math.min(motionFrequency.motionFrequencyFirstPeriod, maxMotionFreq)  + DURATION_WEIGHTS_V4[4] * Math.min( motionFrequency.motionFrequencyMiddlePeriod, maxMotionFreq) + DURATION_WEIGHTS_V4[5] * Math.min(motionFrequency.motionFrequencyLastPeriod, maxMotionFreq) + DURATION_WEIGHTS_V4[6] * Math.min(timesAwake, maxTimesAwake);
        final int durationScorev4 = (int) Math.max(Math.min(rawScore * .95 + 21, 90), 0);
        LOGGER.trace("action=calculated-durationscore-v4 account_id={} sleep_duration_score_v3={} motion_frequency={} awake_times={} agitated_sleep_duration={} durationscore_v4={}", accountId, sleepDurationScoreV3, motionFrequency.motionFrequency, timesAwake, agitatedSleepDuration, durationScorev4);
        return durationScorev4;
    }


    public static Integer getSleepScoreDurationV5(final long accountId, final float sleepDurationScoreV3, final float motionFreqPenalty, final Integer timesAwake, final AgitatedSleep agitatedSleep) {
        final int maxTimesAwake = 6;
        final int maxAgitatedSleep = 90;
        final float rawScore = DURATION_WEIGHTS_V5[0] + DURATION_WEIGHTS_V5[1] * sleepDurationScoreV3 + DURATION_WEIGHTS_V5[2] * Math.min(agitatedSleep.agitatedSleepMins, maxAgitatedSleep) + DURATION_WEIGHTS_V5[3] * motionFreqPenalty + DURATION_WEIGHTS_V5[4] * Math.min(timesAwake, maxTimesAwake);
        final int durationScorev5 = (int) Math.max(Math.min(rawScore * .95 + 18, 90), 0);
        LOGGER.trace("action=calculated-durationscore-v5 account_id={} sleep_duration_score_v3={} motion_frequency_penalty={} awake_times={} agitated_sleep_duration={} durationscore_v5={}", accountId, sleepDurationScoreV3, motionFreqPenalty, timesAwake, agitatedSleep.agitatedSleepMins, durationScorev5);
        return durationScorev5;
    }

        /**
         * Compute motion score based on average number of agitation during sleep.
         * score ranges from 10 to 90. A ZERO score actually means no score is computed.
         * @param targetDate nightdate
         * @param trackerMotions pill data
         * @param fallAsleepTimestamp detected fell asleep time
         * @param wakeUpTimestamp detected woke up time
         * @return a score for motion during the night
         */

    public static MotionScore getSleepMotionScore(final DateTime targetDate, final List<TrackerMotion> trackerMotions, final Long fallAsleepTimestamp, final Long wakeUpTimestamp) {
        float numAgitations = 0.0f;
        Float avgMotionAmplitude = 0.0f;
        Integer maxMotionAmplitude = 0;

        final Integer offsetMillis = trackerMotions.get(0).offsetMillis;

        // check if sleep time is valid
        Long sleepStartMillis = fallAsleepTimestamp;
        if (sleepStartMillis == 0L) {
            sleepStartMillis = targetDate.withHourOfDay(22).minusMillis(offsetMillis).getMillis();
        }

        // check if awake time is valid, sleep-duration needs to be >= 60 mins
        Long sleepStopMillis = wakeUpTimestamp;
        if (sleepStopMillis == 0L || (sleepStopMillis - sleepStartMillis) < MIN_ASLEEP_MINUTES_REQUIRED * 60000) {
            sleepStopMillis = sleepStartMillis + 12 * 3600000L;
        }

        Long firstMotionTime = 0L;
        Long lastMotionTime = 0L;

        // Compute average motion per hour
        for (final TrackerMotion motion : trackerMotions) {
            if (motion.timestamp > sleepStopMillis) {
                break;
            }

            if (motion.timestamp > sleepStartMillis) {
                if (firstMotionTime == 0L) {
                    firstMotionTime = motion.timestamp;
                }
                lastMotionTime = motion.timestamp;
                numAgitations += 1.0f;
                avgMotionAmplitude += (float) motion.value;
                if (motion.value > maxMotionAmplitude) {
                    maxMotionAmplitude = motion.value;
                }
            }
        }

        float numAsleepMinutes = (float) ((double) (lastMotionTime - firstMotionTime) / 60000.0);
        float totalScore = 0.0f;
        float score = 0.0f;
        if (numAsleepMinutes > MIN_ASLEEP_MINUTES_REQUIRED) {
            totalScore = (numAgitations / numAsleepMinutes) * 100.0f;
            score = ((100.0f - totalScore) / 100.0f) * MOTION_SCORE_RANGE + MOTION_SCORE_MIN;
        }


        // TODO: factor in motion amplitude
        if (numAgitations > 0.0f) {
            avgMotionAmplitude = avgMotionAmplitude / numAgitations;
        }

        final MotionScore motionScore = new MotionScore((int) numAgitations, (int) numAsleepMinutes,
                avgMotionAmplitude, maxMotionAmplitude, Math.round(score));


        LOGGER.trace("NEW SCORING - Mins asleep: {}, num_agitations: {}, total Score: {}, final score {}, avg Amplitude: {}, max: {}",
                numAsleepMinutes, numAgitations, totalScore, motionScore.score, avgMotionAmplitude, maxMotionAmplitude);


        return motionScore;
    }

    public static MotionFrequency getMotionFrequency(final List<TrackerMotion> trackerMotions, final Integer sleepDurationMinutes, final Long fallAsleepTimestamp,final Long wakeUpTimestamp){
        final long confineTimeWindowEnd = 7200000L;
        int numMotionsFirstPeriod = 0;
        int numMotionsMiddlePeriod = 0;
        int numMotionsLastPeriod = 0;
        float motionFrequencyMiddlePeriod = 0.0f;
        //first segment
        if (sleepDurationMinutes ==0){
            return new MotionFrequency(0.0f, 0.0f, 0.0f, 0.0f);
        }
        for (TrackerMotion trackerMotion : trackerMotions) {
            if (trackerMotion.timestamp < fallAsleepTimestamp) {
                continue;
            }else if (trackerMotion.timestamp <= fallAsleepTimestamp + confineTimeWindowEnd) {
                if (trackerMotion.value > 0){
                    numMotionsFirstPeriod += 1;
                }
            } else if (trackerMotion.timestamp <= wakeUpTimestamp - confineTimeWindowEnd) {
                if (trackerMotion.value > 0){
                    numMotionsMiddlePeriod += 1;
                }
            } else if (trackerMotion.timestamp <= wakeUpTimestamp) {
                if (trackerMotion.value > 0) {
                    numMotionsLastPeriod += 1;
                }
            }else{
                break;
            }
        }
        final float motionFrequency = (float) (numMotionsFirstPeriod + numMotionsMiddlePeriod + numMotionsLastPeriod) / sleepDurationMinutes;
        final float motionFrequencyFirstPeriod = (float) numMotionsFirstPeriod/ 120;
        if (sleepDurationMinutes > 240) {
            motionFrequencyMiddlePeriod = (float) numMotionsMiddlePeriod/ (sleepDurationMinutes -240);
        }
        final float motionFrequencyLastPeriod = (float) numMotionsLastPeriod/ 120;
        return new MotionFrequency(motionFrequency, motionFrequencyFirstPeriod, motionFrequencyMiddlePeriod, motionFrequencyLastPeriod);
    }

    public static float getMotionFrequencyPenalty(final MotionFrequency motionFrequency, float motionFrequencyThreshold){
        if (motionFrequencyThreshold < 0.0f){
            LOGGER.error("action=invalid-motion-frequency-threshold motionFrequencyThreshold ={}", motionFrequencyThreshold);
            return 0.0f;
        }

        //bounds motionFrequencyThreshold range.
        if (motionFrequencyThreshold == 0.0f){
            motionFrequencyThreshold = MOTION_FREQUENCY_THRESHOLD_DEFAULT;
        } else if (motionFrequencyThreshold < MOTION_FREQUENCY_THRESHOLD_MIN){
            motionFrequencyThreshold = MOTION_FREQUENCY_THRESHOLD_MIN;
        } else if (motionFrequencyThreshold > MOTION_FREQUENCY_THRESHOLD_MAX){
            motionFrequencyThreshold = MOTION_FREQUENCY_THRESHOLD_MAX;
        }

        //bounds relative motion frequency range
        final float motionFrequencyFirstPeriodAdjusted = Math.max(Math.min(motionFrequency.motionFrequencyFirstPeriod - motionFrequencyThreshold, RELATIVE_MOTION_FREQUENCY_MAX), 0);
        final float motionFrequencyMiddlePeriodAdjusted = Math.max(Math.min(motionFrequency.motionFrequencyMiddlePeriod - motionFrequencyThreshold, RELATIVE_MOTION_FREQUENCY_MAX), 0);
        final float motionFrequencyLastPeriodAdjusted = Math.max(Math.min(motionFrequency.motionFrequencyLastPeriod - motionFrequencyThreshold, RELATIVE_MOTION_FREQUENCY_MAX), 0);
        final float motionFrequencyPenalty= MOTION_FREQUENCY_PENALTY[0] * motionFrequencyFirstPeriodAdjusted + MOTION_FREQUENCY_PENALTY[1] * motionFrequencyMiddlePeriodAdjusted+ MOTION_FREQUENCY_PENALTY[2] * motionFrequencyLastPeriodAdjusted;

        return motionFrequencyPenalty;
    }

    public static AgitatedSleep getAgitatedSleep(final List<TrackerMotion> trackerMotions, final Long fallAsleepTimestamp, final Long wakeUpTimestamp) {
        // computes periods of agitated sleep  using on duration. Over 16 seconds of movement within a two minute window initiates a state of agitated sleep that persists until there is a 4 minute window with no motion
        final int onDurationThreshold = 9; //secs
        final int minOnDuration = 1;
        final int uninterruptedSleepThreshold = 60; // represents full sleep cycle - if sleep segment < 1 sleep cycle, then do not count that sleep as uninterrupted. After ~1 sleep cycle was probably completed, it is not possible to truncate sleep duration by cycle count
        final long noMotionThreshold = DateTimeConstants.MILLIS_PER_MINUTE * 4; //4 mins
        final long timeWindow = 90000L; //1.5 minutes - finds two consecutive minutes with some flexibility
        int agitatedSleepMins = 0;
        int uninterruptedSleepMins = 0;

        long currentOnDuration = 0;
        long currentOnDurationTS = 0L;
        long lastMotionTS = fallAsleepTimestamp;
        long agitatedIntervalStartTS = 0L;
        boolean agitatedSleep = false;
        long uninterruptedSleepSegment;
        for (TrackerMotion trackerMotion : trackerMotions) {

            if (trackerMotion.timestamp < fallAsleepTimestamp) {
                continue;
            } else if (trackerMotion.timestamp > wakeUpTimestamp) {
                break;
            }
            final long previousOnDuration = currentOnDuration;
            final long previousODurationTS = currentOnDurationTS;
            currentOnDuration = trackerMotion.onDurationInSeconds;
            currentOnDurationTS = trackerMotion.timestamp;
            final long totalOnDuration;
            if (currentOnDurationTS - previousODurationTS < timeWindow){
                totalOnDuration = currentOnDuration + previousOnDuration;
            } else {
                totalOnDuration = currentOnDuration;
            }
            //check for consecutive but distinct agitated sleep windows
            if (agitatedSleep & noMotionThreshold < currentOnDurationTS - lastMotionTS){
                final long agitatedSegmentDuration = (lastMotionTS - agitatedIntervalStartTS ) / DateTimeConstants.MILLIS_PER_MINUTE;
                agitatedSleepMins = agitatedSleepMins + (int) agitatedSegmentDuration + 1;
                agitatedSleep = false;
             }

            if (totalOnDuration > onDurationThreshold){
                if (!agitatedSleep){
                    agitatedIntervalStartTS = currentOnDurationTS;
                    //captures first uninterrupted interval
                    if ((agitatedIntervalStartTS - lastMotionTS)/DateTimeConstants.MILLIS_PER_MINUTE >= uninterruptedSleepThreshold){
                        uninterruptedSleepSegment = (agitatedIntervalStartTS - lastMotionTS) / DateTimeConstants.MILLIS_PER_MINUTE;
                        uninterruptedSleepMins += (int) uninterruptedSleepSegment;
                    }
                }

                agitatedSleep = true;
                lastMotionTS = currentOnDurationTS;


            } else if(totalOnDuration >= minOnDuration & agitatedSleep ){
                lastMotionTS = currentOnDurationTS;
            }


        }
        if ((wakeUpTimestamp - lastMotionTS )/ DateTimeConstants.MILLIS_PER_MINUTE > uninterruptedSleepThreshold ){
            uninterruptedSleepSegment = (wakeUpTimestamp - lastMotionTS) / DateTimeConstants.MILLIS_PER_MINUTE;
            uninterruptedSleepMins += (int) uninterruptedSleepSegment;
        }

        return new AgitatedSleep(agitatedSleepMins, uninterruptedSleepMins);
    }

    //for v4, old
    public static int getAgitatedSleepDuration(final List<TrackerMotion> trackerMotions, final Long fallAsleepTimestamp, final Long wakeUpTimestamp) {
        // computes periods of agitated sleep  3 or more minutes in length with a mean motion amplitude above the median motion amplitude and at least one motion above the mean motion amplitude
        final int motionMinThreshold = 250;
        final int motionMaxThreshold = 1250;
        final int consecutiveMotionMinsThreshold = 3;
        final long noMotionThreshold = 180000L; //3 mins (total 5 minute window of no motion)
        final long confineTimeWindowStart = 900000L; //15 mins
        final long confineTimeWindowEnd = 7200000L; //2 hrs
        final long rollingTimeWindow = 120000L; // 2 mins
        boolean sufficientMotionAmplitude = false;
        boolean sufficientMotion = false;
        int consecutiveMotionMins = 0;
        int agitatedSleepMins = 0;
        long previousMotionTime = 0L;
        long noMotionTime = 0L;
        for (TrackerMotion trackerMotion : trackerMotions) {
            //ignores tracker motion  during first 15 mins and last 120 mins of sleep
            if (trackerMotion.timestamp < fallAsleepTimestamp + confineTimeWindowStart) {
                continue;
            } else if (trackerMotion.timestamp > wakeUpTimestamp - confineTimeWindowEnd) {
                break;
            }
            //condition 1: Satisfactory motion event with either no previous satisfactory motion event or a satisfactory motion event within the last 2 minutes
            if (trackerMotion.motionRange > motionMinThreshold && (consecutiveMotionMins == 0 || trackerMotion.timestamp < previousMotionTime + rollingTimeWindow + noMotionTime )) {
                if (trackerMotion.motionRange > motionMaxThreshold) {
                    sufficientMotionAmplitude = true;
                }
                if (consecutiveMotionMins == 0){
                    consecutiveMotionMins = 1;
                }else {
                    consecutiveMotionMins += Math.round((trackerMotion.timestamp - previousMotionTime) / 60000);
                }
                if (consecutiveMotionMins >=consecutiveMotionMinsThreshold & sufficientMotionAmplitude){
                    sufficientMotion = true;
                    noMotionTime = noMotionThreshold;
                }
                previousMotionTime = trackerMotion.timestamp;

                // condition 2: Unsatisfactory motion event  more than 2 minutes after previous satisfactory motion event
            } else if (trackerMotion.motionRange <= motionMinThreshold & trackerMotion.timestamp >= previousMotionTime + rollingTimeWindow + noMotionTime ) {
                if (sufficientMotion) {
                    agitatedSleepMins += consecutiveMotionMins;
                }
                consecutiveMotionMins = 0;
                sufficientMotionAmplitude = false;
                sufficientMotion = false;
                noMotionTime = 0L;

                // condition 3: Satisfactory motion event more than 2 minutes after previous satisfactory motion event
            }else if (trackerMotion.motionRange > motionMinThreshold & trackerMotion.timestamp >= previousMotionTime + rollingTimeWindow + noMotionTime) {
                if (sufficientMotion) {
                    agitatedSleepMins += consecutiveMotionMins;
                }
                consecutiveMotionMins = 1;
                previousMotionTime = trackerMotion.timestamp;
                sufficientMotion = false;
                noMotionTime = 0L;

                if (trackerMotion.motionRange > motionMaxThreshold){
                    sufficientMotionAmplitude = true;
                }
            }
        }
        // condition 4: final agitated sleep event not followed by additional motion event before wake time - 15 minutes
        if (sufficientMotionAmplitude & consecutiveMotionMins > consecutiveMotionMinsThreshold) {
            agitatedSleepMins +=  consecutiveMotionMins;
        }

        return agitatedSleepMins;
    }

    public static int calculateSoundScore(final int numberSoundEvents) {
        return Math.max(0, 100 - (numberSoundEvents * PENALTY_PER_SOUND_EVENT));
    }

    public static float calculateSensorAverageInTimeRange(final List<Sample> samples, final long startTime, final long endTime) {
        float sum = 0;
        int total = 0;
        for (Sample sample : samples) {
            final long dateTime = sample.dateTime;
            if (dateTime >= startTime && dateTime <= endTime) {
                sum += sample.value;
                total++;
            }
        }
        return sum / total;
    }

    public static int calculateTemperatureScore(final List<Sample> samples, final long fallAsleepTimestamp, final long wakeUpTimestamp) {
        final float average = calculateSensorAverageInTimeRange(samples, fallAsleepTimestamp, wakeUpTimestamp);
        if (average > TemperatureHumidity.ALERT_TEMP_MAX_CELSIUS) {
            return SENSOR_ALERT_SCORE;
        } else if (average > TemperatureHumidity.IDEAL_TEMP_MAX_CELSIUS) {
            return SENSOR_WARNING_SCORE;
        } else if (average < TemperatureHumidity.ALERT_TEMP_MIN_CELSIUS) {
            return SENSOR_ALERT_SCORE;
        } else if (average < TemperatureHumidity.IDEAL_TEMP_MIN_CELSIUS) {
            return SENSOR_WARNING_SCORE;
        } else {
            return SENSOR_IDEAL_SCORE;
        }
    }

    public static int calculateHumidityScore(final List<Sample> samples, final long fallAsleepTimestamp, final long wakeUpTimestamp) {
        final float average = calculateSensorAverageInTimeRange(samples, fallAsleepTimestamp, wakeUpTimestamp);
        if (average < TemperatureHumidity.ALERT_HUMIDITY_LOW) {
            return SENSOR_ALERT_SCORE;
        } else if (average < TemperatureHumidity.IDEAL_HUMIDITY_MIN) {
            return SENSOR_WARNING_SCORE;
        } else if (average > TemperatureHumidity.ALERT_HUMIDITY_HIGH) {
            return SENSOR_ALERT_SCORE;
        } else if (average > TemperatureHumidity.IDEAL_HUMIDITY_MAX) {
            return SENSOR_WARNING_SCORE;
        } else {
            return SENSOR_IDEAL_SCORE;
        }
    }

    public static int calculateLightScore(final List<Sample> samples, final long fallAsleepTimestamp, final long wakeUpTimestamp) {
        final float average = calculateSensorAverageInTimeRange(samples, fallAsleepTimestamp, wakeUpTimestamp);
        if (average > Lights.LIGHT_LEVEL_ALERT) {
            return SENSOR_ALERT_SCORE;
        } else if (average > Lights.LIGHT_LEVEL_WARNING) {
            return SENSOR_WARNING_SCORE;
        } else {
            return SENSOR_IDEAL_SCORE;
        }
    }

    public static int calculateParticulateScore(final List<Sample> samples, final long fallAsleepTimestamp, final long wakeUpTimestamp) {
        final float average = calculateSensorAverageInTimeRange(samples, fallAsleepTimestamp, wakeUpTimestamp);
        if (average > Particulates.PARTICULATE_DENSITY_MAX_WARNING) {
            return SENSOR_ALERT_SCORE;
        } else if (average > Particulates.PARTICULATE_DENSITY_MAX_IDEAL) {
            return SENSOR_WARNING_SCORE;
        } else {
            return SENSOR_IDEAL_SCORE;
        }
    }

    public static int calculateAggregateEnvironmentScore(final int soundScore, final int temperatureScore, final int humidityScore, final int lightScore, final int particulateScore) {
        return Math.round((0.2f * temperatureScore) + (0.2f * humidityScore) + (0.2f * soundScore) + (0.2f * lightScore) + (0.2f * particulateScore));
    }


    public static Optional<MotionScore> getSleepMotionScoreMaybe(final DateTime targetDate, final List<TrackerMotion> trackerMotions, final Long fallAsleepTimestamp, final Long wakeUpTimestamp) {
        try {
            return Optional.of(getSleepMotionScore(targetDate, trackerMotions, fallAsleepTimestamp, wakeUpTimestamp));
        } catch (Exception e) {
            LOGGER.error("Unexpected error: {}", e.getMessage());
        }

        return Optional.absent();
    }


    public static Integer calculateTimesAwakePenaltyScore(final int timesAwake) {
        // penalty, returns a negative score
        final int penalty = timesAwake * AWAKE_PENALTY_SCORE;
        return (penalty < MAX_TIMES_AWAKE_PENALTY_SCORE) ? MAX_TIMES_AWAKE_PENALTY_SCORE : penalty;
    }


    public static float getSleepScoreV2V4Weighting(final long targetDateEpoch){
        if (targetDateEpoch < SLEEP_SCORE_V2_V4_TRANSITION_EPOCH){
            return 0.0f;

        } else {
            return Math.min((targetDateEpoch - SLEEP_SCORE_V2_V4_TRANSITION_EPOCH) / 82400000 * SLEEP_SCORE_V2_V4_TRANSITION_WEIGHTING , 1.0f);

        }
    }

    public static float getSleepScoreV4V5Weighting(final long targetDateEpoch){
        if (targetDateEpoch < SLEEP_SCORE_V4_V5_TRANSITION_EPOCH){
            return 0.0f;

        } else {
            return Math.min((targetDateEpoch - SLEEP_SCORE_V4_V5_TRANSITION_EPOCH) / 82400000 * SLEEP_SCORE_V4_V5_TRANSITION_WEIGHTING , 1.0f);

        }
    }

}