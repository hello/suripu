package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.SleepMotionMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kingshy on 1/5/15.
 */
public class SleepMotion {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepMotion.class);

    // computed 01/19/2015 = 0.1673f
    // computed 03/19/2015, prod = 0.2249f
    // SELECT SUM(agitation_num) / CAST((15 * COUNT(*)) AS FLOAT) AS perc FROM sleep_score;
    // sleep scores are now saved in DynamoDB
    // computed 04/08/2015, 7632 scores, prod_sleep_stats_v_0_2 = 0.121882824828
    private static float AVERAGE_SLEEP_PERC = 12.0f;

    private static float SIGNIFICANT_DIFF = 20.0f; // only differences greater than 30% is worth reporting (TODO)

    private static int MIN_DAYS_REQUIRED = 3;

    public static Optional<InsightCard> getInsights(final Long accountId, final DeviceId deviceId, final TrendsInsightsDAO trendsInsightsDAO, final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final Boolean isNewUser) {

        int numDays = 14; // 2 weeks comparison
        if (isNewUser) {
            // get last 5 nights data
            numDays = 5;
        }

        DateTime queryEndTime = DateTime.now(DateTimeZone.UTC);
        DateTime queryStartTime = queryEndTime.minusDays(numDays);

        final ImmutableList<AggregateSleepStats> sleepStats = sleepStatsDAODynamoDB.getBatchStats(accountId,
                DateTimeUtil.dateToYmdString(queryStartTime),
                DateTimeUtil.dateToYmdString(queryEndTime));

        // get sleep movement from sleep_score
        final Optional<InsightCard> card = processData(accountId, sleepStats, isNewUser);
        return card;
    }

    public static Optional<InsightCard> processData(final Long accountId, final ImmutableList<AggregateSleepStats> sleepStats, final Boolean isNewUser) {
        if (sleepStats.isEmpty()) {
            return Optional.absent();
        }

        float totDuration = 0.0f;
        float totMotion = 0.0f;
        int numDays = 0;

        for (final AggregateSleepStats stat : sleepStats) {
            if (stat.motionScore.motionPeriodMinutes == 0) {
                continue;
            }

            totMotion += (float) stat.motionScore.numMotions;
            totDuration += (float) stat.motionScore.motionPeriodMinutes;
            numDays++;
        }

        if (totMotion == 0 || numDays < MIN_DAYS_REQUIRED) {
            return Optional.absent();
        }

        final float averageMotionPercentage = ((totMotion / totDuration) * 100.0f);
        final float overallDiff = (( averageMotionPercentage - AVERAGE_SLEEP_PERC) / AVERAGE_SLEEP_PERC ) * 100.0f;

        Text text;
        if (Math.abs(overallDiff) >= SIGNIFICANT_DIFF) {
            if (overallDiff > 0) {
                text = SleepMotionMsgEN.moreMovement(numDays, (int) overallDiff, (int) averageMotionPercentage);
            } else {
                text = SleepMotionMsgEN.lessMovement(numDays, (int) overallDiff, (int) averageMotionPercentage);
            }
        } else {
            if ((int) overallDiff != 0) {
                text = SleepMotionMsgEN.equalMovement(numDays, (int) overallDiff, (int) averageMotionPercentage);
            }
            else {
                text = SleepMotionMsgEN.reallyEqualMovement(numDays, (int) overallDiff, (int) averageMotionPercentage);
            }
        }

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.SLEEP_QUALITY, InsightCard.TimePeriod.RECENTLY,
                DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT));

    }

}
