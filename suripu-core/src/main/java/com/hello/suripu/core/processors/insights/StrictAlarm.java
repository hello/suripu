package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.QuestionCard;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.util.DateTimeUtil;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by jyfan on 10/31/16.
 */
public class StrictAlarm {

    private static final Logger LOGGER = LoggerFactory.getLogger(WakeVariance.class);
    private static final int NUM_DAYS_ONE_WEEK = 7;

    public static Optional<QuestionCard> getQuestion(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final Long accountId, final DateTime queryEndDate, final int numDays, final Map<String, Integer> strictAlarmTextQidMap) {

        //get wake variance data for the past n=numDays days
        final DateTime queryStartDate = queryEndDate.minusDays(numDays);
        final String queryEndDateString = DateTimeUtil.dateToYmdString(queryEndDate);
        final String queryStartDateString = DateTimeUtil.dateToYmdString(queryStartDate);

        final List<AggregateSleepStats> sleepStats = sleepStatsDAODynamoDB.getBatchStats(accountId, queryStartDateString, queryEndDateString);
        LOGGER.debug("insight=wake-variance sleep_stat_len={} account_id={}", sleepStats.size(), accountId);
        final List<Integer> wakeTimeList = Lists.newArrayList();
        final List<Integer> weekdayWakeTimeList = Lists.newArrayList();
        for (final AggregateSleepStats stat : sleepStats) {

            final Long wakeTimeStamp = stat.sleepStats.wakeTime;
            final DateTime wakeTimeDateTime = new DateTime(wakeTimeStamp, DateTimeZone.forOffsetMillis(stat.offsetMillis));
            final int wakeTime = wakeTimeDateTime.getMinuteOfDay();
            wakeTimeList.add(wakeTime);

            final int dayOfWeek = wakeTimeDateTime.getDayOfWeek();
            if (dayOfWeek < 5) { //if weekday (exclude friday night/sat morning)
                weekdayWakeTimeList.add(wakeTime);
            }

        }

        final Boolean qualifyInsight = qualifyStrictAlarm(wakeTimeList);
        if (!qualifyInsight) {
            return Optional.absent();
        }

        if (weekdayWakeTimeList.isEmpty()) {
            return Optional.absent();
        }

        //calculate target wake time
        final Integer recWeekdayWakeMins = getRecommendedWakeMinutes(weekdayWakeTimeList);
        final Optional<Integer> recQidOptional = mapWakeTimeToQid(recWeekdayWakeMins, strictAlarmTextQidMap);

        if (!recQidOptional.isPresent()) {
            return Optional.absent();
        }

        final Integer offsetMillis = sleepStats.get(0).offsetMillis;
        final DateTime todayLocal = DateTime.now(DateTimeZone.UTC).plusMillis(offsetMillis).withTimeAtStartOfDay();
        final DateTime expireDate = todayLocal.plusDays(NUM_DAYS_ONE_WEEK);

        return Optional.of(QuestionCard.createQuestionCard(accountId, recQidOptional.get(), todayLocal, expireDate));
    }

    public static Boolean qualifyStrictAlarm(final List<Integer> wakeTimeList) {

        if (wakeTimeList.isEmpty() || wakeTimeList.size() <=2) {
            return Boolean.FALSE; //not big enough to calculate variance usefully
        }

        // compute variance
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final int wakeTime : wakeTimeList) {
            stats.addValue(wakeTime);
        }

        final Double wakeStdDevDouble = stats.getStandardDeviation();
        final int wakeStdDev = (int) Math.round(wakeStdDevDouble);

        if (wakeStdDev >= 90) { //1.5 hours
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    public static Integer getRecommendedWakeMinutes(final List<Integer> weekdayWakeTimeList) {

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final int wakeTime : weekdayWakeTimeList) {
            stats.addValue(wakeTime);
        }

        final int avgWeekdayWake = (int) stats.getMean();

        return avgWeekdayWake;
    }

    public static Optional<Integer> mapWakeTimeToQid(final Integer recommendedWakeTimeMins, final Map<String, Integer> textQidMap) {

        final String questionText;

        if (recommendedWakeTimeMins < 360) { //6AM
            questionText = "nonexistent question";
        } else if (recommendedWakeTimeMins < 375) { //6:30AM
            questionText = "You could benefit from a consistent wake time. Set a recurring alarm for 6:00 AM? Existing alarms will be deleted.";
        } else if (recommendedWakeTimeMins < 405) { //6:45AM
            questionText = "You could benefit from a consistent wake time. Set a recurring alarm for 6:30 AM? Existing alarms will be deleted."; //TODO edit to real time
        } else if (recommendedWakeTimeMins < 435) { //7:15AM
            questionText = "You could benefit from a consistent wake time. Set a recurring alarm for 7:00 AM? Existing alarms will be deleted.";
        } else if (recommendedWakeTimeMins < 465) { //7:45AM
            questionText = "You could benefit from a consistent wake time. Set a recurring alarm for 7:30 AM? Existing alarms will be deleted.";
        } else if (recommendedWakeTimeMins < 495) { //8:15AM
            questionText = "You could benefit from a consistent wake time. Set a recurring alarm for 8:00 AM? Existing alarms will be deleted.";
        } else if (recommendedWakeTimeMins < 525) { //8:45AM
            questionText = "You could benefit from a consistent wake time. Set a recurring alarm for 8:30 AM? Existing alarms will be deleted.";
        } else if (recommendedWakeTimeMins < 555) { //9:15AM
            questionText = "You could benefit from a consistent wake time. Set a recurring alarm for 9:00 AM? Existing alarms will be deleted.";
        } else if (recommendedWakeTimeMins < 585) { //9:45AM
            questionText = "You could benefit from a consistent wake time. Set a recurring alarm for 9:30 AM? Existing alarms will be deleted.";
        } else if (recommendedWakeTimeMins < 630) { //10:30AM
            questionText = "You could benefit from a consistent wake time. Set a recurring alarm for 10:00 AM? Existing alarms will be deleted.";
        } else {
            questionText = "nonexistent question";
        }

        final Integer qid = textQidMap.get(questionText);
        if (qid == null) {
            return Optional.absent();
        }

        return Optional.of(qid);
    }


}
