package com.hello.suripu.core.util;

import com.hello.suripu.core.processors.insights.SleepDuration;

/**
 * Created by kingshy on 2/25/15.
 */
public class SleepScoreUtils {

    public static Integer DURATION_MAX_SCORE = 80;
    public static Integer DURATION_MIN_SCORE = 10;
    public static Float DURATION_SCORE_SCALE = (float) (DURATION_MAX_SCORE - DURATION_MIN_SCORE);
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

        if (sleepDurationHours < (float) (idealHours.absoluteMinHours - 1) || sleepDurationHours > (float) (idealHours.absoluteMaxHours + TOO_MUCH_SLEEP_ALLOWANCE)) {
            return DURATION_MIN_SCORE;
        }

        float diffMinutes = 0;
        float scaleMinutes = 0;
        if (sleepDurationHours < (float) idealHours.minHours) {
            diffMinutes = (float) (idealHours.minHours * 60  - sleepDurationMinutes);
            scaleMinutes = (idealHours.minHours - idealHours.absoluteMinHours + 1) * 60.0f;
        } else {
            diffMinutes = (float) (sleepDurationMinutes - idealHours.maxHours * 60);
            scaleMinutes = (idealHours.absoluteMaxHours + TOO_MUCH_SLEEP_ALLOWANCE - idealHours.maxHours) * 60.0f;
        }
        final int score = DURATION_MAX_SCORE - Math.round((diffMinutes / scaleMinutes) * DURATION_SCORE_SCALE);
        return score;
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