package com.hello.suripu.core.processors.insights;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.CaffeineAlarmMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.preferences.TimeFormat;
import com.hello.suripu.core.processors.AccountInfoProcessor;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.InsightUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
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
    public static final Integer MAX_ALLOWED_RANGE = 6 * 60; //6 hrs
    public static final Integer LATEST_ALLOWED_SLEEP_TIME = (4 + 24) * 60; //4AM
    public static final Integer EARLIEST_ALLOWED_SLEEP_TIME = 20 * 60; //8PM

    public static Optional<InsightCard> getInsights(final AccountInfoProcessor accountInfoProcessor, final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final Long accountId, final TimeFormat timeFormat) {

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
        LOGGER.debug("insight=caffeine-alarm account_id={} sleep_stat_len={}", accountId, sleepStats.size());
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

        final Optional<InsightCard> card = processCaffeineAlarm(accountId, sleepTimeList, timeFormat);
        return card;
    }

    @VisibleForTesting
    public static Optional<InsightCard> processCaffeineAlarm(final Long accountId, final List<Integer> sleepTimeList, final TimeFormat timeFormat) {

        if (sleepTimeList.isEmpty()) {
            LOGGER.info("account_id={} insight=caffeine-alarm action=sleep-time-list-empty", accountId);
            return Optional.absent();
        }
        else if (sleepTimeList.size() <= 2) {
            LOGGER.info("account_id={} insight=caffeine-alarm action=sleep-time-list-too-small", accountId);
            return Optional.absent(); //not big enough to calculate mean meaningfully har har
        }

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final int sleepTime : sleepTimeList) {
            if (sleepTime < SIX_AM_MINUTES) { //If sleep time is after midnight, add 24 hrs to avoid messing up stats
                stats.addValue(sleepTime + DateTimeConstants.MINUTES_PER_DAY);
            } else {
                stats.addValue(sleepTime);
            }
        }

        final Boolean passSafeGuards = checkSafeGuards(stats);
        if (!passSafeGuards) {
            LOGGER.info("insight=caffeine-alarm account_id={} action=fail-safe-guard");
            return Optional.absent();
        }

        final Double sleepMedDouble = stats.getPercentile(50);
        final int sleepMed = (int) Math.round(sleepMedDouble);

        final int recommendedCoffeeMinutesTime = getRecommendedCoffeeMinutesTime(sleepMed);

        final String sleepTime = InsightUtils.timeConvertRound(sleepMed, timeFormat);
        final String coffeeTime = InsightUtils.timeConvertRound(recommendedCoffeeMinutesTime, timeFormat);

        final Text text = CaffeineAlarmMsgEN.getCaffeineAlarmMessage(sleepTime, coffeeTime);

        return Optional.of(InsightCard.createBasicInsightCard(accountId, text.title, text.message,
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
        final Double sleepMed = stats.getPercentile(50);
        if (sleepMed > LATEST_ALLOWED_SLEEP_TIME) {
            return Boolean.FALSE;
        }
        if (sleepMed < EARLIEST_ALLOWED_SLEEP_TIME) {
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    private static Integer getRecommendedCoffeeMinutesTime(final int sleepTimeMinutes) {
        //Should never encounter sleepTimeHour < 6 because we added 24 hrs to those times before averaging
        return (sleepTimeMinutes - COFFEE_SPACE_MINUTES);
    }

}
