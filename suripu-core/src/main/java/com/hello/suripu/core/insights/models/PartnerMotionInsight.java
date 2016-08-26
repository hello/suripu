package com.hello.suripu.core.insights.models;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.insights.models.text.PartnerMotionMsgEN;
import com.hello.suripu.core.insights.models.text.Text;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.insights.InsightCard;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jyfan on 10/8/15.
 */
public class PartnerMotionInsight {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartnerMotionInsight.class);

    public static Optional<InsightCard> getInsights(final Long accountId, final DeviceReadDAO deviceReadDAO, final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {
        final Optional<Long> optionalPartnerAccountId = deviceReadDAO.getPartnerAccountId(accountId);
        if (!optionalPartnerAccountId.isPresent()) {
            LOGGER.debug("action=insight_absent-insight=partner_motion-reason=no_partner-account_id={}", accountId);
            return Optional.absent();
        }
        LOGGER.debug("action=found-partner account_id={} partner_id={}", accountId, optionalPartnerAccountId.get());

        final String queryLastNightTime = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1).toString("yyyy-MM-dd"); //Note tz for trigger time

        final Optional<AggregateSleepStats> mySleepStat = sleepStatsDAODynamoDB.getSingleStat(accountId, queryLastNightTime);
        final Optional<AggregateSleepStats> partnerSleepStat = sleepStatsDAODynamoDB.getSingleStat(optionalPartnerAccountId.get(), queryLastNightTime);

        if (!mySleepStat.isPresent() || !partnerSleepStat.isPresent()) {
            LOGGER.debug("action=insight-absent insight=partner-motion reason=sleep-stats-absent account_id={} partner_id={}", accountId, optionalPartnerAccountId.get());
            return Optional.absent();
        }

        // I do not check that mySleepStat.get().motionScore.motionPeriodMinutes!=0 b/c
        // Checked prod_sleep_stats 11/4/2015 only values of numMotions when period=0 is 0,1
        // So biggest anomalous difference is 0 (after motionTtl check) \in egalitarian class
        final Float myMotionTtl = (float) mySleepStat.get().motionScore.numMotions;
        final Float partnerMotionTtl = (float) partnerSleepStat.get().motionScore.numMotions;

        if (myMotionTtl == 0f || partnerMotionTtl ==0f) {
            LOGGER.debug("action=insight-absent insight=partner-motion reason=motion-ttl-zero account_id={} partner_id={}", accountId, optionalPartnerAccountId.get());
            return Optional.absent();
        }

        final Text text = getInsightText(myMotionTtl, partnerMotionTtl);

        return Optional.of(InsightCard.createBasicInsightCard(accountId, text.title, text.message,
                InsightCard.Category.PARTNER_MOTION, InsightCard.TimePeriod.MONTHLY,
                DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT));
    }

    @VisibleForTesting
    public static Text getInsightText(final Float myMotionTtl, final Float partnerMotionTtl) {
        final Float motionDifference = myMotionTtl - partnerMotionTtl;

        final Text text;
        if (motionDifference < -1) {
            final Integer percentage = (int) ((motionDifference / myMotionTtl) * -100);
            text = PartnerMotionMsgEN.getBadPartner(percentage);
        } else if (motionDifference <= 1) {
            text = PartnerMotionMsgEN.getEgalitarian();
        } else {
            final Integer percentage = (int) ((motionDifference / partnerMotionTtl) * 100);
            text = PartnerMotionMsgEN.getBadMe(percentage);
        }

        return text;
    }
}
