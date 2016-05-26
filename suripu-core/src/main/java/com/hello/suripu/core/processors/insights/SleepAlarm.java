package com.hello.suripu.core.processors.insights;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.SleepAlarmMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.InsightUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Years;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by jyfan on 5/18/16.
 */
public class SleepAlarm {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepAlarm.class);

    public static final Integer PRE_SLEEP_TIME = 30;

    public static final Integer NUM_DAYS = 14;
    public static final Integer MAX_ALLOWED_RANGE = 3 * 60; //3 hrs
    public static final Integer LATEST_ALLOWED_WAKE_TIME = (11) * 60; //11 AM
    public static final Integer EARLIEST_ALLOWED_WAKE_TIME = 4 * 60; //4 AM

    public static Optional<InsightCard> getInsights(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final AccountReadDAO accountReadDAO, final Long accountId) {

        //get sleep variance data for the past NUM_DAYS
        final DateTime queryEndDate = DateTime.now().withTimeAtStartOfDay();
        final DateTime queryStartDate = queryEndDate.minusDays(NUM_DAYS);

        final String queryEndDateString = DateTimeUtil.dateToYmdString(queryEndDate);
        final String queryStartDateString = DateTimeUtil.dateToYmdString(queryStartDate);

        final List<AggregateSleepStats> sleepStats = sleepStatsDAODynamoDB.getBatchStats(accountId, queryStartDateString, queryEndDateString);
        LOGGER.debug("insight=sleep_alarm-account_id={}-sleep_stat_len={}", accountId, sleepStats.size());
        final List<Integer> wakeTimeList = Lists.newArrayList();
        for (final AggregateSleepStats stat : sleepStats) {

            final Integer dayOfWeek = stat.dateTime.getDayOfWeek(); //Do not pull weekends (Sat & Sun)
            if (dayOfWeek == 6) {
                continue;
            } else if (dayOfWeek == 7) {
                continue;
            }

            final Long wakeTimeStamp = stat.sleepStats.wakeTime;
            final DateTime wakeTimeDateTime = new DateTime(wakeTimeStamp, DateTimeZone.forOffsetMillis(stat.offsetMillis));
            final int wakeTime = wakeTimeDateTime.getMinuteOfDay();
            wakeTimeList.add(wakeTime);
        }

        final DateTime dob;
        final Optional<Account> account = accountReadDAO.getById(accountId);
        if (account.isPresent()) {
            dob = account.get().DOB;
        } else {
            dob = DateTime.now();
        }

        final Integer userAge = Years.yearsBetween(dob, DateTime.now()).toPeriod().getYears();

        final Optional<InsightCard> card = processSleepAlarm(accountId, wakeTimeList, userAge);
        return card;
    }

    @VisibleForTesting
    public static Optional<InsightCard> processSleepAlarm(final Long accountId, final List<Integer> wakeTimeList, final Integer userAge) {

        if (wakeTimeList.isEmpty()) {
            LOGGER.info("account_id={}-insight=sleep_alarm-action=wake_time_list_empty", accountId);
            return processSleepAlarmFallBack(accountId);
        }
        else if (wakeTimeList.size() <= 2) {
            LOGGER.info("account_id={}-insight=sleep_alarm-action=wake_time_list_too_small", accountId);
            return processSleepAlarmFallBack(accountId); //not big enough to calculate mean meaningfully har har
        }

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final int wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        final Boolean passSafeGuards = checkSafeGuards(stats);
        if (!passSafeGuards) {
            LOGGER.info("insight=sleep_alarm-account_id={}-action=fail_safe_guard");
            return processSleepAlarmFallBack(accountId);
        }

        final Double wakeAvgDouble = stats.getMean();
        final int wakeAvg = (int) Math.round(wakeAvgDouble);

        final int recSleepDurationMins = getRecommendedSleepDurationMinutes(userAge);
        final int recommendedSleepMinutesTime = wakeAvg - recSleepDurationMins;

        final String wakeTime = InsightUtils.timeConvert(wakeAvg);
        final String preSleepTime = InsightUtils.timeConvert((recommendedSleepMinutesTime - PRE_SLEEP_TIME));
        final String sleepTime = InsightUtils.timeConvert(recommendedSleepMinutesTime);

        final Text text = SleepAlarmMsgEN.getSleepAlarmMessage(wakeTime, recSleepDurationMins, preSleepTime, sleepTime);

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.SLEEP_TIME, InsightCard.TimePeriod.MONTHLY,
                DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT));
    }

    @VisibleForTesting
    public static Optional<InsightCard> processSleepAlarmFallBack(final Long accountId) {
        final Text text = SleepAlarmMsgEN.getSleepAlarmFallBackMessage();

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.SLEEP_TIME, InsightCard.TimePeriod.MONTHLY,
                DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT));
    }

    @VisibleForTesting
    public static Boolean checkSafeGuards(final DescriptiveStatistics stats) {

        //Safeguard for anomalous sleeptime encountered last week
        final Double wakeMax = stats.getMax();
        final Double wakeMin = stats.getMin();
        final Double wakeRange = wakeMax - wakeMin;
        if (wakeRange > MAX_ALLOWED_RANGE) { //If Range in sleep times is too big, average sleep time is less meaningful
            return Boolean.FALSE;
        }

        //Wake time sanity check, should be between 4AM and 11AM
        final Double wakeAvg = stats.getMean();
        if (wakeAvg > LATEST_ALLOWED_WAKE_TIME) {
            return Boolean.FALSE;
        }
        if (wakeAvg < EARLIEST_ALLOWED_WAKE_TIME) {
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    @VisibleForTesting
    public static Integer getRecommendedSleepDurationMinutes(final Integer userAge) {
        final SleepDuration.recommendation sleepDurationRecommendation = SleepDuration.getSleepDurationRecommendation(userAge);
        final Integer recommendation = (sleepDurationRecommendation.maxHours * 60 + sleepDurationRecommendation.minHours * 60) / 2;

        return recommendation;
    }
}
