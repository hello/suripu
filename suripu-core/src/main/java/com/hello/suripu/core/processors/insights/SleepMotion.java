package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.SleepMotionMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.models.Insights.SleepStatsSample;
import com.hello.suripu.core.models.SleepScore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kingshy on 1/5/15.
 */
public class SleepMotion {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepMotion.class);

    // last computed 01/19/2015
    // SELECT SUM(agitation_num) / CAST((15 * COUNT(*)) AS FLOAT) AS perc FROM sleep_score;
    private static float AVERAGE_SLEEP_PERC = 0.1673f;

    private static float SIGNIFICANT_DIFF = 3.0f; // only differences greater than 3% is worth reporting (TODO)

    private static int MIN_DAYS_REQUIRED = 3;

    public static Optional<InsightCard> getInsights(final Long accountId, final Long deviceId, final TrendsInsightsDAO trendsInsightsDAO, final SleepScoreDAO scoreDAO, final Boolean isNewUser) {

        int numDays = 14; // 2 weeks comparison
        if (isNewUser) {
            // get last 5 nights data
            numDays = 5;
        }

        final ImmutableList<SleepStatsSample> sleepStats = trendsInsightsDAO.getAccountRecentSleepStats(accountId, numDays);

        final int size = sleepStats.size();
        if (size == 0) {
            LOGGER.warn("No sleep stats for account {}", accountId);
            return Optional.absent();
        }

        DateTime queryEndTime = DateTime.now(DateTimeZone.UTC);
        DateTime queryStartTime = queryEndTime.minusDays(numDays);
        Boolean foundStart = false;
        Boolean foundEnd = false;
        for (int i = 0; i < size; i++) {
            if (!foundEnd) {
                final long endWakeTime = sleepStats.get(size - (i+1)).stats.wakeTime;
                if (endWakeTime > 0) {
                    queryEndTime = new DateTime(endWakeTime, DateTimeZone.UTC);
                    foundEnd = true;
                }
            }

            if (!foundStart) {
                final long startSleepTime = sleepStats.get(i).stats.sleepTime;
                if (startSleepTime > 0) {
                    queryStartTime = new DateTime(startSleepTime, DateTimeZone.UTC);
                    foundStart = true;
                }
            }
            if (foundStart && foundEnd) {
                break;
            }
        }

        final List<SleepScore> motionData = scoreDAO.getByAccountBetweenDateBucket(accountId,queryStartTime, queryEndTime);

        // get sleep movement from sleep_score
        final Optional<InsightCard> card = processData(accountId, sleepStats, motionData, isNewUser);
        return card;
    }

    public static Optional<InsightCard> processData(final Long accountId, final List<SleepStatsSample> sleepStats, final List<SleepScore> motionData, final Boolean isNewUser) {

        if (motionData.isEmpty()) {
            return Optional.absent();
        }

        // TODO compare with users of same age and gender once we have enough users
        // for now, compare with ALL user
        // compare % of sleep where there's movement

        // Track summary for each night
        final int dataSize = motionData.size();
        final List<Float> percentages = new ArrayList<>();

        int totDuration = 0;
        int totMotion = 0;

        for (SleepStatsSample sample : sleepStats) {
            int index = 0;
            final DateTime sleepTime = new DateTime(sample.stats.sleepTime, DateTimeZone.UTC);
            final DateTime wakeTime = new DateTime(sample.stats.wakeTime, DateTimeZone.UTC);

            int numBuckets = 0;
            int numMotion = 0;
            for (int i = index; i < dataSize; i++, index++ ) {
                final SleepScore scoreData = motionData.get(i);
                if (scoreData.dateBucketUTC.isBefore(sleepTime)) {
                    continue;
                }
                if (scoreData.dateBucketUTC.isAfter(wakeTime)) {
                    break;
                }
                numBuckets++;
                numMotion += scoreData.agitationNum;
            }
            final Duration sleepDuration = new Duration(sleepTime, wakeTime);
            if (sleepDuration.getMillis() > 0) {
                final float perc = (float) numMotion / (float) sleepDuration.getStandardMinutes();
                percentages.add((perc - AVERAGE_SLEEP_PERC) / AVERAGE_SLEEP_PERC * 100.0f);
                totDuration += sleepDuration.getStandardMinutes();
                totMotion += numMotion;
            }
        }

        if (totMotion == 0) {
            return Optional.absent();
        }

        final int numDays = percentages.size();

        if (numDays < MIN_DAYS_REQUIRED) {
            return  Optional.absent();
        }

        final float averageMotionPercentage = (float) totMotion / (float) totDuration;
        final float overallDiff = (averageMotionPercentage - AVERAGE_SLEEP_PERC) / AVERAGE_SLEEP_PERC * 100.0f;

        int greater = 0;
        int lesser = 0;
        for (final Float value : percentages) {
            if (Math.abs(value ) >= SIGNIFICANT_DIFF) {
                if (value > 0.0f) {
                    greater++;
                } else {
                    lesser++;
                }
            }
        }

        Text text;
        if (Math.abs(overallDiff) >= SIGNIFICANT_DIFF) {
            if (overallDiff > 0) {
                text = SleepMotionMsgEN.moreMovement(numDays, greater, overallDiff, averageMotionPercentage * 100.0f);
            } else {
                text = SleepMotionMsgEN.lessMovement(numDays, lesser, overallDiff, averageMotionPercentage * 100.0f);
            }
        } else {
            text = SleepMotionMsgEN.equalMovement(numDays, overallDiff, averageMotionPercentage * 100.0f);
        }

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.SLEEP_QUALITY, InsightCard.TimePeriod.RECENTLY,
                DateTime.now(DateTimeZone.UTC)));
    }
}
