package com.hello.suripu.core.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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
    public final Integer sleepDurationScore;
    public final Integer environmentalScore;
    public final Integer timesAwakePenaltyScore;

    public final SleepStats sleepStats;

    public AggregateSleepStats(final Long accountId, final DateTime dateTime, final Integer offsetMillis,
                               final Integer sleepScore, final String version,
                               final MotionScore motionScore,
                               final Integer sleepDurationScore,
                               final Integer environmentalScore,
                               final Integer timesAwakePenaltyScore,
                               final SleepStats sleepStats) {
        this.accountId = accountId;
        this.dateTime = dateTime;
        this.offsetMillis = offsetMillis;
        this.sleepScore = sleepScore;
        this.version = version;
        this.motionScore = motionScore;
        this.sleepDurationScore = sleepDurationScore;
        this.environmentalScore = environmentalScore;
        this.timesAwakePenaltyScore = timesAwakePenaltyScore;
        this.sleepStats = sleepStats;
    }

    @Override
    public int compareTo(AggregateSleepStats o) {
        final AggregateSleepStats compareObject = o;
        final long compareTimestamp = compareObject.dateTime.getMillis()/1000L;
        final long objectTimestamp = this.dateTime.getMillis()/1000L;
        return (int) (objectTimestamp - compareTimestamp);  // ascending
    }

    public static class Builder {
        private Long accountId;
        private DateTime dateTime;
        private Integer offsetMillis;
        private Integer sleepScore;
        private String version;
        private MotionScore motionScore;
        private Integer sleepDurationScore;
        private Integer environmentalScore;
        private Integer timesAwakePenaltyScore;
        private SleepStats sleepStats;

        public Builder() {
            this.accountId = 0L;
            this.dateTime = DateTime.now(DateTimeZone.UTC);
            this.offsetMillis = 0;
            this.sleepScore = 0;
            this.version = "";
            this.motionScore = null;
            this.sleepDurationScore = 0;
            this.environmentalScore = 0;
            this.timesAwakePenaltyScore = 0;
            this.sleepStats = null;
        }

        public Builder withAccountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withDateTime(final DateTime dateTime) {
            this.dateTime = dateTime;
            return this;
        }

        public Builder withOffsetMillis(final Integer offsetMillis) {
            this.offsetMillis = offsetMillis;
            return this;
        }

        public Builder withSleepScore(final Integer sleepScore) {
            this.sleepScore = sleepScore;
            return this;
        }

        public Builder withVersion(final String version) {
            this.version = version;
            return this;
        }

        public Builder withMotionScore(final MotionScore motionScore) {
            this.motionScore = motionScore;
            return this;
        }

        public Builder withSleepDurationScore(final Integer sleepDurationScore) {
            this.sleepDurationScore = sleepDurationScore;
            return this;
        }

        public Builder withEnvironmentalScore(final Integer environmentalScore) {
            this.environmentalScore = environmentalScore;
            return this;
        }

        public Builder withTimesAwakePenaltyScore(final Integer timesAwakePenaltyScore) {
            this.timesAwakePenaltyScore = timesAwakePenaltyScore;
            return this;
        }

        public Builder withSleepStats(final SleepStats sleepStats) {
            this.sleepStats = sleepStats;
            return this;
        }

        public AggregateSleepStats build() {
            return new AggregateSleepStats(
                    accountId,
                    dateTime,
                    offsetMillis,
                    sleepScore,
                    version,
                    motionScore,
                    sleepDurationScore,
                    environmentalScore,
                    timesAwakePenaltyScore,
                    sleepStats);
        }
    }
}
