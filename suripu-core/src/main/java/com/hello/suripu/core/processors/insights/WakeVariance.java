package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.*;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.WakeVarianceMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.util.DateTimeUtil;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jingyun on 7/25/15.
 */

public class WakeVariance {
    private static final Logger LOGGER = LoggerFactory.getLogger(WakeVariance.class);

    public static Optional<InsightCard> getInsights(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final Long accountId, final WakeStdDevData wakeStdDevData, final DateTime queryEndDate, final int numDays) {

        //get wake variance data for the past n=numDays days
        final DateTime queryStartDate = queryEndDate.minusDays(numDays);
        final String queryEndDateString = DateTimeUtil.dateToYmdString(queryEndDate);
        final String queryStartDateString = DateTimeUtil.dateToYmdString(queryStartDate);

        final List<AggregateSleepStats> sleepStats = sleepStatsDAODynamoDB.getBatchStats(accountId, queryStartDateString, queryEndDateString);
        LOGGER.debug("Account id {} length of sleep stats is {} and sleepStats is {} ", accountId, sleepStats.size(), sleepStats);
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

    public static Optional<InsightCard> processWakeVarianceData(final Long accountId, final List<Integer> wakeTimeList, final WakeStdDevData wakeStdDevData) {

        if (wakeTimeList.isEmpty()) {
            LOGGER.debug("Got nothing in wakeTimeList");
            return Optional.absent();
        }
        else if (wakeTimeList.size() <= 2) {
            LOGGER.debug("Size of wakeTimeList is less than 2 for accountId {}", accountId);
            return Optional.absent(); //not big enough to calculate variance usefully
        }

        // compute variance
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final int wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        final Double wakeStdDevDouble = stats.getStandardDeviation();
        final int wakeStdDev = (int) Math.round(wakeStdDevDouble);

        LOGGER.debug("Wake Std Dev for accountId {} is {}", accountId, wakeStdDev);
        final Integer percentile = wakeStdDevData.getWakeStdDevPercentile(wakeStdDev);

        Text text;
        if (wakeStdDev <= 50) { //25 percentile
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

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.WAKE_VARIANCE, InsightCard.TimePeriod.RECENTLY,
                DateTime.now(DateTimeZone.UTC)));

    }

}
