package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.SleepScore;
import com.hello.suripu.core.models.SleepStats;
import org.joda.time.DateTime;

/**
 * Created by benjo on 1/21/16.
 */
public interface SleepStatsDAO {
    Boolean updateStat(Long accountId, DateTime date, Integer overallSleepScore, SleepScore sleepScore, SleepStats stats, Integer offsetMillis);

    Optional<Integer> getTimeZoneOffset(Long accountId);

    Optional<Integer> getTimeZoneOffset(Long accountId, DateTime queryDate);

    Optional<AggregateSleepStats> getSingleStat(Long accountId, String date);

    ImmutableList<AggregateSleepStats> getBatchStats(Long accountId, String startDate, String endDate);
}
