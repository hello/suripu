package com.hello.suripu.core.models.timeline.v2;

public enum ScoreCondition {
    UNAVAILABLE,
    INCOMPLETE,
    IDEAL,
    WARNING,
    ALERT;

    public static ScoreCondition fromScore(final int score,
                                           final boolean notEnoughData) {
        if (notEnoughData) {
            return INCOMPLETE;
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
