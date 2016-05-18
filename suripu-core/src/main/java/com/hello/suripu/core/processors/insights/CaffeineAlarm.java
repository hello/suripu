package com.hello.suripu.core.processors.insights;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.CaffeineAlarmMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.processors.AccountInfoProcessor;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.InsightUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by jyfan on 5/11/16.
 */
public class CaffeineAlarm {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaffeineAlarm.class);

    public static final Integer COFFEE_SPACE_MINUTES = 6 * 60; //As per National Sleep Foundation https://sleep.org/articles/how-much-caffeine-should-i-have/
    public static final Integer SIX_AM_MINUTES = 6 * 60;

    public static final Integer NUM_DAYS = 14;
    public static final Integer MAX_ALLOWED_RANGE = 3 * 60; //3 hrs
    public static final Integer LATEST_ALLOWED_SLEEP_TIME = (4 + 24) * 60; //4AM
    public static final Integer EARLIEST_ALLOWED_SLEEP_TIME = 20 * 60; //8PM

    public static Optional<InsightCard> getInsights(final AccountInfoProcessor accountInfoProcessor, final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final Long accountId) {

        final Boolean drinksCoffee = accountInfoProcessor.checkUserDrinksCaffeine(accountId);
        if (!drinksCoffee) {
            return Optional.absent();
        }

        //get sleep variance data for the past NUM_DAYS
        final DateTime queryEndDate = DateTime.now().withTimeAtStartOfDay();
        final DateTime queryStartDate = queryEndDate.minusDays(NUM_DAYS);

        final String queryEndDateString = DateTimeUtil.dateToYmdString(queryEndDate);
        final String queryStartDateString = DateTimeUtil.dateToYmdString(queryStartDate);

        final List<AggregateSleepStats> sleepStats = sleepStatsDAODynamoDB.getBatchStats(accountId, queryStartDateString, queryEndDateString);
        LOGGER.debug("Account id {} length of sleep stats is {} and sleepStats is {} ", accountId, sleepStats.size(), sleepStats);
        final List<Integer> sleepTimeList = Lists.newArrayList();
        for (final AggregateSleepStats stat : sleepStats) {

            final Integer dayOfWeek = stat.dateTime.getDayOfWeek(); //Do not pull weekends (Fri & Sat)
            if (dayOfWeek == 5) {
                continue;
            } else if (dayOfWeek == 6) {
                continue;
            }

            final Long sleepTimeStamp = stat.sleepStats.sleepTime;
            final DateTime sleepTimeDateTime = new DateTime(sleepTimeStamp, DateTimeZone.forOffsetMillis(stat.offsetMillis));
            final int sleepTime = sleepTimeDateTime.getMinuteOfDay();
            sleepTimeList.add(sleepTime);
        }

        final Optional<InsightCard> card = processCaffeineAlarm(accountId, sleepTimeList);
        return card;
    }

    @VisibleForTesting
    public static Optional<InsightCard> processCaffeineAlarm(final Long accountId, final List<Integer> sleepTimeList) {

        if (sleepTimeList.isEmpty()) {
            LOGGER.debug("Got nothing in sleepTimeList");
            return Optional.absent();
        }
        else if (sleepTimeList.size() <= 2) {
            LOGGER.debug("Size of sleepTimeList is less than 2 for accountId {}", accountId);
            return Optional.absent(); //not big enough to calculate mean meaningfully har har
        }

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final int sleepTime : sleepTimeList) {
            if (sleepTime < SIX_AM_MINUTES) { //If sleep time is after midnight, add 24 hrs to avoid messing up stats
                stats.addValue(sleepTime + InsightUtils.DAY_MINUTES);
            } else {
                stats.addValue(sleepTime);
            }
        }

        final Boolean passSafeGuards = checkSafeGuards(stats);
        if (!passSafeGuards) {
            return Optional.absent();
        }

        final Double sleepAvgDouble = stats.getMean();
        final int sleepAvg = (int) Math.round(sleepAvgDouble);

        final int recommendedCoffeeMinutesTime = getRecommendedCoffeeMinutesTime(sleepAvg);

        final String sleepTime = InsightUtils.timeConvert(sleepAvg);
        final String coffeeTime = InsightUtils.timeConvert(recommendedCoffeeMinutesTime);

        final Text text = CaffeineAlarmMsgEN.getCaffeineAlarmMessage(sleepTime, coffeeTime);

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.CAFFEINE, InsightCard.TimePeriod.MONTHLY,
                DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT));
    }

    @VisibleForTesting
    public static Boolean checkSafeGuards(final DescriptiveStatistics stats) {

        //Safeguard for anomalous sleeptime encountered last week
        final Double sleepMax = stats.getMax();
        final Double sleepMin = stats.getMin();
        final Double sleepRange = sleepMax - sleepMin;
        if (sleepRange > MAX_ALLOWED_RANGE) { //If Range in sleep times is too big, average sleep time is less meaningful
            return Boolean.FALSE;
        }

        //Sleep time sanity check, should be between 8PM and 4AM
        final Double sleepAvg = stats.getMean();
        if (sleepAvg > LATEST_ALLOWED_SLEEP_TIME) {
            return Boolean.FALSE;
        }
        if (sleepAvg < EARLIEST_ALLOWED_SLEEP_TIME) {
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    private static Integer getRecommendedCoffeeMinutesTime(final int sleepTimeMinutes) {
        //Should never encounter sleepTimeHour < 6 because we added 24 hrs to those times before averaging
        return (sleepTimeMinutes - COFFEE_SPACE_MINUTES);
    }

}
