package com.hello.suripu.core.insights.models;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.insights.InsightProfile;
import com.hello.suripu.core.insights.models.text.Text;
import com.hello.suripu.core.insights.models.text.WakeVarianceMsgEN;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.util.DateTimeUtil;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by jingyun on 7/25/15.
 */

public class WakeVariance implements InsightModel{
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final WakeStdDevData wakeStdDevData;
    private static final int DAYS_ONE_WEEK = 7;

    private WakeVariance(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final WakeStdDevData wakeStdDevData) {
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.wakeStdDevData = wakeStdDevData;
    }

    public static WakeVariance create(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {
        return new WakeVariance(sleepStatsDAODynamoDB, new WakeStdDevData());
    }

    public static WakeVariance create(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final WakeStdDevData wakeStdDevData) {
        return new WakeVariance(sleepStatsDAODynamoDB, wakeStdDevData);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WakeVariance.class);

    public static Optional<InsightCard> processWakeVarianceData(final Long accountId, final List<Integer> wakeTimeList, final WakeStdDevData wakeStdDevData) {

        if (wakeTimeList.isEmpty()) {
            LOGGER.debug("action=insight-absent insight=wake-variance reason=wake-time-list-empty account_id={}", accountId);
            return Optional.absent();
        }
        else if (wakeTimeList.size() <= 2) {
            LOGGER.debug("action=insight-absent insight=wake-variance reason=wake-time-list-small account_id={}", accountId);
            return Optional.absent(); //not big enough to calculate variance usefully
        }

        // compute variance
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final int wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        final Double wakeStdDevDouble = stats.getStandardDeviation();
        final int wakeStdDev = (int) Math.round(wakeStdDevDouble);

        LOGGER.debug("insight=wake-variance account_id={} wake_std_dev={}", accountId, wakeStdDev);
        final Integer percentile = wakeStdDevData.getWakeStdDevPercentile(wakeStdDev);

        Text text;
        if (wakeStdDev <= 50) { //25 percentile based on data from 07/25/2015
            text = WakeVarianceMsgEN.getWakeVarianceLow(wakeStdDev, percentile);
        }
        else if (wakeStdDev <= 79) { //50 percentile
            text = WakeVarianceMsgEN.getWakeVarianceNotLowEnough(wakeStdDev, percentile);
        }
        else if (wakeStdDev <= 108) { //75 percentile
            text = WakeVarianceMsgEN.getWakeVarianceHigh(wakeStdDev, percentile);
        }
        else {
            text = WakeVarianceMsgEN.getWakeVarianceTooHigh(wakeStdDev, percentile);
        }

        return Optional.of(InsightCard.createBasicInsightCard(accountId, text.title, text.message,
                InsightCard.Category.WAKE_VARIANCE, InsightCard.TimePeriod.WEEKLY,
                DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT));

    }

    @Override
    public Optional<InsightCard> generate(final InsightProfile insightProfile) {
        //get wake variance data for the past n=numDays days
        final DateTime queryEndDate = insightProfile.utcnow().withTimeAtStartOfDay();
        final DateTime queryStartDate = queryEndDate.minusDays(DAYS_ONE_WEEK);
        final String queryEndDateString = DateTimeUtil.dateToYmdString(queryEndDate);
        final String queryStartDateString = DateTimeUtil.dateToYmdString(queryStartDate);

        final Long accountId = insightProfile.pill().accountId;

        final List<AggregateSleepStats> sleepStats = sleepStatsDAODynamoDB.getBatchStats(accountId, queryStartDateString, queryEndDateString);
        LOGGER.debug("insight=wake-variance sleep_stat_len={} account_id={}", sleepStats.size(), accountId);
        final List<Integer> wakeTimeList = Lists.newArrayList();
        for (final AggregateSleepStats stat : sleepStats) {
            final Long wakeTimeStamp = stat.sleepStats.wakeTime;
            final DateTime wakeTimeDateTime = new DateTime(wakeTimeStamp, DateTimeZone.forOffsetMillis(stat.offsetMillis));
            final int wakeTime = wakeTimeDateTime.getMinuteOfDay();
            wakeTimeList.add(wakeTime);
        }

        final Optional<InsightCard> card = processWakeVarianceData(accountId, wakeTimeList, wakeStdDevData);
        return card;
    }
}
