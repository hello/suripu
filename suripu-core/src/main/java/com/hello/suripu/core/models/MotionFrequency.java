
package com.hello.suripu.core.models;

/**
 * Created by jarredheinrich on 6/30/16.
 */
public class MotionFrequency {
    public final Float motionFrequency;
    public final Float motionFrequencyFirstPeriod;
    public final Float motionFrequencyMiddlePeriod;
    public final Float motionFrequencyLastPeriod;

    public MotionFrequency(final Float motionFrequency, final Float motionFrequencyFirstPeriod, final Float motionFrequencyMiddlePeriod, final Float motionFrequencyLastPeriod) {
        this.motionFrequency = motionFrequency;
        this.motionFrequencyFirstPeriod = motionFrequencyFirstPeriod;
        this.motionFrequencyMiddlePeriod = motionFrequencyMiddlePeriod;
        this.motionFrequencyLastPeriod = motionFrequencyLastPeriod;
    }
}