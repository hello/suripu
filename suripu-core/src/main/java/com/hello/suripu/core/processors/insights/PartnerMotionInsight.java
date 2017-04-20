package com.hello.suripu.core.processors.insights;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.PartnerMotionMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jyfan on 10/8/15.
 */
public class PartnerMotionInsight {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartnerMotionInsight.class);

    public static Optional<InsightCard> getInsights(final Long accountId, final DeviceReadDAO deviceReadDAO, final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final DateTime dateVisibleUTC) {

        //Get partner id
        final Optional<Long> optionalPartnerAccountId = deviceReadDAO.getPartnerAccountId(accountId);
        if (!optionalPartnerAccountId.isPresent()) {
            LOGGER.debug("action=insight_absent-insight=partner_motion-reason=no_partner-account_id={}", accountId);
            return Optional.absent();
        }
        LOGGER.debug("action=found-partner account_id={} partner_id={}", accountId, optionalPartnerAccountId.get());

        //Query partner data
        final String queryStartTime = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(8).toString("yyyy-MM-dd");
        final String queryEndTime = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay().toString("yyyy-MM-dd");

        final ImmutableList<AggregateSleepStats> mySleepStats = sleepStatsDAODynamoDB.getBatchStats(accountId, queryStartTime, queryEndTime);
        final ImmutableList<AggregateSleepStats> partnerSleepStats = sleepStatsDAODynamoDB.getBatchStats(optionalPartnerAccountId.get(), queryStartTime, queryEndTime);

        if (mySleepStats.isEmpty() || partnerSleepStats.isEmpty()) {
            LOGGER.debug("action=insight-absent insight=partner-motion reason=sleep-stats-absent account_id={} partner_id={}", accountId, optionalPartnerAccountId.get());
            return Optional.absent();
        }

        final int numComparisons = Math.min(mySleepStats.size(), partnerSleepStats.size());

        Float myMotionTtl = 0f;
        Float partnerMotionTtl = 0f;
        for (AggregateSleepStats mySleepStat : mySleepStats.subList(0, numComparisons)) {
            myMotionTtl = myMotionTtl + mySleepStat.motionScore.numMotions;
        }
        for (AggregateSleepStats partnerSleepStat : partnerSleepStats.subList(0, numComparisons)) {
            partnerMotionTtl = partnerMotionTtl + partnerSleepStat.motionScore.numMotions;
        }

        if (myMotionTtl == 0f || partnerMotionTtl ==0f) {
            LOGGER.debug("action=insight-absent insight=partner-motion reason=motion-ttl-zero account_id={} partner_id={}", accountId, optionalPartnerAccountId.get());
            return Optional.absent();
        }

        final Text text = getInsightText(myMotionTtl, partnerMotionTtl);

        return Optional.of(InsightCard.createBasicInsightCard(accountId, text.title, text.message,
                InsightCard.Category.PARTNER_MOTION, InsightCard.TimePeriod.MONTHLY,
                dateVisibleUTC, InsightCard.InsightType.DEFAULT));
    }

    @VisibleForTesting
    public static Text getInsightText(final Float myMotionTtl, final Float partnerMotionTtl) {
        final Float motionDifference = myMotionTtl - partnerMotionTtl;

        final Text text;
        if (motionDifference < -1) {
            final Integer percentage = (int) ((motionDifference / myMotionTtl) * -100);

            if (percentage < 100) {
                text = PartnerMotionMsgEN.getBadPartner(percentage);
            } else if (percentage == 100) {
                text = PartnerMotionMsgEN.getTwiceBadPartner();
            } else {
                text = PartnerMotionMsgEN.getVeryBadPartner();
            }

        } else if (motionDifference <= 1) {
            text = PartnerMotionMsgEN.getEgalitarian();
        } else {
            final Integer percentage = (int) ((motionDifference / partnerMotionTtl) * 100);

            if (percentage < 100) {
                text = PartnerMotionMsgEN.getBadMe(percentage);
            } else if (percentage == 100) {
                text = PartnerMotionMsgEN.getTwiceBadMe();
            } else {
                text = PartnerMotionMsgEN.getVeryBadMe();
            }

        }

        return text;
    }
}
