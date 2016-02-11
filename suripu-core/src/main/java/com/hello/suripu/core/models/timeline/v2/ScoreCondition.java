package com.hello.suripu.core.models.timeline.v2;

import com.hello.suripu.core.models.DataCompleteness;
import com.hello.suripu.core.models.SleepScore;

public enum ScoreCondition {
    UNAVAILABLE,
    INCOMPLETE,
    IDEAL,
    WARNING,
    ALERT,
    NO_DATA;

    public static ScoreCondition fromScore(final int score,
                                           final DataCompleteness dataCompleteness) {
        if (dataCompleteness == DataCompleteness.NOT_ENOUGH_DATA) {
            return INCOMPLETE;
        } else if (dataCompleteness == DataCompleteness.NO_DATA) {
            return NO_DATA;
        } else if (score == SleepScore.NO_SCORE) {
            return UNAVAILABLE;
        } else if (score < SleepScore.WARNING_SCORE_THRESHOLD) {
            return ALERT;
        } else if (score < SleepScore.IDEAL_SCORE_THRESHOLD) {
            return WARNING;
        }

        return IDEAL;
    }
}
