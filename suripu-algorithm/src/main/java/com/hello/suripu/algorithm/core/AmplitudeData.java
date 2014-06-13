package com.hello.suripu.algorithm.core;

/**
 * Created by pangwu on 4/22/14.
 */
public class AmplitudeData {
    public final long timestamp;
    public final double amplitude;
    public final int offsetMillis;

    public AmplitudeData(final long timestamp, final double amplitude, final int offsetMillis){
        this.timestamp = timestamp;
        this.amplitude = amplitude;
        this.offsetMillis = offsetMillis;
    }
}
