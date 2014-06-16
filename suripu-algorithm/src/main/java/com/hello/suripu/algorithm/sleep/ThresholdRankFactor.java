package com.hello.suripu.algorithm.sleep;

/**
 * Created by pangwu on 6/11/14.
 */
class ThresholdRankFactor {
    public final double errorDiff;
    public final SleepThreshold threshold;

    public ThresholdRankFactor(final double errorDiff, final SleepThreshold threshold){
        this.errorDiff = errorDiff;
        this.threshold = threshold;
    }
}
