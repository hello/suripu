package com.hello.suripu.core.models;

import org.joda.time.DateTime;

/**
 * Created by ksg on 5/24/16
 */
public class SleepScoreParameters {
    public static final Integer MISSING_THRESHOLD = 0; // return this if we don't have a personalized threshold


    public final Long accountId;
    public final DateTime dateTime;
    public final Integer durationThreshold;
    public final Integer motionFrequencyThreshold;


    public SleepScoreParameters(final Long accountId, final DateTime dateTime, final Integer durationThreshold, final Integer motionFrequencyThreshold) {
        this.accountId = accountId;
        this.dateTime = dateTime;
        this.durationThreshold = durationThreshold;
        this.motionFrequencyThreshold = motionFrequencyThreshold;
    }

    public SleepScoreParameters(final Long accountId, final DateTime dateTime, final Integer durationThreshold) {
        this.accountId = accountId;
        this.dateTime = dateTime;
        this.durationThreshold = durationThreshold;
        this.motionFrequencyThreshold = MISSING_THRESHOLD;
    }

    public SleepScoreParameters(final Long accountId, final DateTime dateTime) {
        this.accountId = accountId;
        this.dateTime = dateTime;
        this.durationThreshold = MISSING_THRESHOLD;
        this.motionFrequencyThreshold = MISSING_THRESHOLD;

    }

}
