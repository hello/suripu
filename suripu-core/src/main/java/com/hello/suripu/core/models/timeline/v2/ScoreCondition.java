package com.hello.suripu.core.models.timeline.v2;

import com.hello.suripu.core.models.DataCompleteness;

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
        } else if (score == 0) {
            return UNAVAILABLE;
        } else if (score < 60) {
            return ALERT;
        } else if (score < 80) {
            return WARNING;
        }

        return IDEAL;
    }
}
