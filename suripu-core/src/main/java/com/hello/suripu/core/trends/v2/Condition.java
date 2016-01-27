package com.hello.suripu.core.trends.v2;

import com.hello.suripu.core.models.SleepScore;

/**
 * Created by kingshy on 1/21/16.
 */
public enum Condition {
    UNKNOWN("UNKNOWN"),
    IDEAL("IDEAL"),
    WARNING("WARNING"),
    ALERT("ALERT");

    private String value;

    private Condition(final String value) { this.value = value; }

    public static Condition getScoreCondition(final float value) {
        if (value >= SleepScore.IDEAL_SCORE_THRESHOLD) {
            return Condition.IDEAL;
        } else if (value >= SleepScore.WARNING_SCORE_THRESHOLD) {
            return Condition.WARNING;
        } else if (value >= SleepScore.ALERT_SCORE_THRESHOLD) {
            return Condition.ALERT;
        }
        return Condition.UNKNOWN;
    }
}