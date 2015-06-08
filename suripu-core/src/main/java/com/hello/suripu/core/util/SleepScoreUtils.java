package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.processors.insights.SleepDuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by kingshy on 2/25/15.
 */
public class SleepScoreUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepScoreUtils.class);

    public static Integer DURATION_MAX_SCORE = 80;
    public static Integer DURATION_MIN_SCORE = 10;
    private static float MOTION_SCORE_MIN = 10.0f;
    private static float MOTION_SCORE_RANGE = 80.0f; // max score is 90

    public static Float DURATION_SCORE_SCALE = (float) (DURATION_MAX_SCORE - DURATION_MIN_SCORE);
    public static Integer TOO_LITTLE_SLEEP_ALLOWANCE = 1;
    public static Integer TOO_MUCH_SLEEP_ALLOWANCE = 4; // allow too much sleep recommendation to exceed by 4 hours

    /**
     * compute a score based on sleep duration.
     * if duration is within recommended range, return max score
     * if duration is way out of the min/max + allowance recommended range, return min score
     * everything else is a linear decrease in score
     * @param userAgeInYears
     * @param sleepDurationMinutes
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

        Float diffMinutes = 0.0f;
        Float scaleMinutes = 0.0f;
        if (sleepDurationHours < (float) idealHours.minHours) {
            // under-sleep
            diffMinutes = (float) (idealHours.minHours * 60  - sleepDurationMinutes);
            scaleMinutes = (idealHours.minHours - idealHours.absoluteMinHours + 1) * 60.0f;
        } else {
            // over-slept
            diffMinutes = (float) (sleepDurationMinutes - idealHours.maxHours * 60);
            scaleMinutes = (idealHours.absoluteMaxHours + TOO_MUCH_SLEEP_ALLOWANCE - idealHours.maxHours) * 60.0f;
        }
        final Integer score = DURATION_MAX_SCORE - Math.round((diffMinutes / scaleMinutes) * DURATION_SCORE_SCALE);
        return score;
    }

    /**
     * Compute sleep score based on average number of agitation during sleep
     * @param targetDate
     * @param trackerMotions
     * @param fallAsleepTimestamp
     * @param wakeUpTimestamp
     * @return
     */
    public static MotionScore getSleepMotionScore(final DateTime targetDate, final List<TrackerMotion> trackerMotions, final Long fallAsleepTimestamp, final Long wakeUpTimestamp) {
        float numAgitations = 0.0f;
        Float avgMotionAmplitude = 0.0f;
        Integer maxMotionAmplitude = 0;

        final Integer offsetMillis = trackerMotions.get(0).offsetMillis;

        Long sleepStartMillis = fallAsleepTimestamp;
        if (sleepStartMillis == 0L) {
            sleepStartMillis = targetDate.withHourOfDay(22).minusMillis(offsetMillis).getMillis();
        }

        Long sleepStopMillis = wakeUpTimestamp;
        if (sleepStopMillis == 0L) {
            sleepStopMillis = sleepStartMillis + 12 * 3600000L;
        }

        Long firstMotionTime = 0L;
        Long lastMotionTime = 0L;

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
        float totalScore =  (numAgitations / numAsleepMinutes) * 100.0f;
        float score = 100.0f - totalScore;

        // TODO: factor in motion amplitude
        avgMotionAmplitude = avgMotionAmplitude / numAgitations;

        final MotionScore motionScore = new MotionScore((int) numAgitations, (int) numAsleepMinutes,
                avgMotionAmplitude, maxMotionAmplitude,
                Math.round((score / 100.0f) * MOTION_SCORE_RANGE + MOTION_SCORE_MIN));


        LOGGER.trace("NEW SCORING - Mins asleep: {}, num_agitations: {}, total Score: {}, final score {}, avg Amplitude: {}, max: {}",
                numAsleepMinutes, numAgitations, totalScore, motionScore.score, avgMotionAmplitude, maxMotionAmplitude);


        return motionScore;
    }

    public static Optional<MotionScore> getSleepMotionScoreMaybe(final DateTime targetDate, final List<TrackerMotion> trackerMotions, final Long fallAsleepTimestamp, final Long wakeUpTimestamp) {
        try {
            return Optional.of(getSleepMotionScore(targetDate, trackerMotions, fallAsleepTimestamp, wakeUpTimestamp));
        } catch (Exception e) {
            LOGGER.error("Unexpected error: {}", e.getMessage());
        }

        return Optional.absent();
    }



    /**
     * Computes an aggregated score
     * @param motionScore 70%
     * @param durationScore 20%
     * @param environmentScore 10%
     * @return
     */
    public static Integer aggregateSleepScore(final Integer motionScore, final Integer durationScore, final Integer environmentScore) {
        return Math.round(0.7f * motionScore + 0.2f * durationScore + 0.1f * environmentScore);
    }
}