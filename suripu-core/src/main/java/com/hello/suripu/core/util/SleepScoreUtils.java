package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.processors.insights.Lights;
import com.hello.suripu.core.processors.insights.Particulates;
import com.hello.suripu.core.processors.insights.SleepDuration;
import com.hello.suripu.core.processors.insights.TemperatureHumidity;
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
    public static float MOTION_SCORE_MIN = 10.0f;
    private static float MOTION_SCORE_RANGE = 80.0f; // max score is 90
    private static float MIN_ASLEEP_MINUTES_REQUIRED = 60.0f; // need to be asleep for at least 60 minutes

    public static final int PENALTY_PER_SOUND_EVENT = 20;
    public static final int SENSOR_IDEAL_SCORE = 100;
    public static final int SENSOR_WARNING_SCORE = 75;
    public static final int SENSOR_ALERT_SCORE = 50;

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
     * Compute motion score based on average number of agitation during sleep.
     * score ranges from 10 to 90. A ZERO score actually means no score is computed.
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
        if (average > Particulates.PARTICULATE_AQI_LEVEL_MAX_WARNING) {
            return SENSOR_ALERT_SCORE;
        } else if (average > Particulates.PARTICULATE_AQI_LEVEL_MAX_IDEAL) {
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