package com.hello.suripu.core.models.timeline.v2;

public enum ScoreCondition {
    UNAVAILABLE,
    IDEAL,
    WARNING,
    ALERT;

    public static ScoreCondition fromScore(final int score) {
        if (score == 0) {
            return UNAVAILABLE;
        } else if (score < 50) {
            return ALERT;
        } else if (score < 80) {
            return WARNING;
        }

        return IDEAL;
    }
}
