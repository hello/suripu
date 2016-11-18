package com.hello.suripu.core.models;

/**
 * Created by jarredheinrich on 11/17/16.
 */
public class ProgressiveAlarmThresholds {
    public final int amplitudeThreshold;
    public final int amplitudeThresholdCountLimit;
    public final int kickoffCountThreshold;
    public final int onDurationThreshold;

    public ProgressiveAlarmThresholds(final int amplitudeThreshold, final int amplitudeThresholdCountLimit, final int kickoffCountThreshold, final int onDurationThreshold) {
        this.amplitudeThreshold = amplitudeThreshold;
        this.amplitudeThresholdCountLimit = amplitudeThresholdCountLimit;
        this.kickoffCountThreshold = kickoffCountThreshold;
        this.onDurationThreshold = onDurationThreshold;
    }
}
