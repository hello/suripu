package com.hello.suripu.core.models;

import org.joda.time.DateTime;

/**
 * Created by kingshy on 3/13/15.
 */
public class AggregateSleepStats implements Comparable<AggregateSleepStats>{
    public final Long accountId;
    public final DateTime dateTime;
    public final Integer offsetMillis;

    public final Integer sleepScore;
    public final String version;

    public final MotionScore motionScore;
    public final SleepStats sleepStats;

    public AggregateSleepStats(final Long accountId, final DateTime dateTime, final Integer offsetMillis,
                               final Integer sleepScore, final String version,
                               final MotionScore motionScore,
                               final SleepStats sleepStats) {
        this.accountId = accountId;
        this.dateTime = dateTime;
        this.offsetMillis = offsetMillis;
        this.sleepScore = sleepScore;
        this.version = version;
        this.motionScore = motionScore;
        this.sleepStats = sleepStats;
    }

    @Override
    public int compareTo(AggregateSleepStats o) {
        final AggregateSleepStats compareObject = o;
        final long compareTimestamp = compareObject.dateTime.getMillis();
        final long objectTimestamp = this.dateTime.getMillis();
        return (int) (objectTimestamp - compareTimestamp);  // ascending

    }

}
