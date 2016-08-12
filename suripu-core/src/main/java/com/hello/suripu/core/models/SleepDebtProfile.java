package com.hello.suripu.core.models;

import java.util.List;

/**
 * Created by jarredheinrich on 8/12/16.
 */
public class SleepDebtProfile {
    public final int minSleepDurationMins;
    public final int idealSleepDurationHours;
    public final List<AggregateSleepStats> sleepStatsLastWeek;
    public final List<AggregateSleepStats> sleepStatsLastMonth;

    public SleepDebtProfile(final int minSleepDurationMins, final int idealSleepDurationHours, final List<AggregateSleepStats> sleepStatsLastWeek, final List<AggregateSleepStats> sleepStatsLastMonth) {
        this.minSleepDurationMins = minSleepDurationMins;
        this.idealSleepDurationHours = idealSleepDurationHours;
        this.sleepStatsLastWeek = sleepStatsLastWeek;
        this.sleepStatsLastMonth = sleepStatsLastMonth;
    }
}