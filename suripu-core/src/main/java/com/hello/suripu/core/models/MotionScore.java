package com.hello.suripu.core.models;

/**
 * Created by kingshy on 3/19/15.
 */
public class MotionScore {
    public final Integer numMotions;
    public final Integer motionPeriodMinutes;
    public final Float avgAmplitude;
    public final Integer maxAmplitude;
    public final Integer score;

    public MotionScore(final Integer numMotions, final Integer motionPeriodMinutes, final Float avgAmplitude, final Integer maxAmplitude, final Integer score) {
        this.numMotions = numMotions;
        this.motionPeriodMinutes = motionPeriodMinutes;
        this.avgAmplitude = avgAmplitude;
        this.maxAmplitude = maxAmplitude;
        this.score = score;
    }
}
