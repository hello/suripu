package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.CorrelationTemperatureMsgEN;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jyfan on 6/8/16.
 */
public class CorrelationTemperature {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationTemperature.class);

    public static Optional<InsightCard> getInsights(final Long accountId, final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final DateTime publicationDateLocal) {

        //Get dateVisibleUTC
        final Optional<Integer> timeZoneOffsetOptional = sleepStatsDAODynamoDB.getTimeZoneOffset(accountId);
        if (!timeZoneOffsetOptional.isPresent()) {
            LOGGER.debug("action=insight-absent insight=correlation_temperature reason=timezoneoffset-absent account_id={}", accountId);
            return Optional.absent(); //cannot compute insight without timezone info
        }

        final Integer timeZoneOffset = timeZoneOffsetOptional.get();
        final DateTime publicationDateUTC = publicationDateLocal.minusMillis(timeZoneOffset);

        return Optional.of(InsightCard.createBasicInsightCard(accountId,
                CorrelationTemperatureMsgEN.TEMP_CORR_MARKETING_TITLE,
                CorrelationTemperatureMsgEN.TEMP_CORR_MARKETING_MSG,
                InsightCard.Category.CORRELATION_TEMP,
                InsightCard.TimePeriod.WEEKLY,
                publicationDateUTC,
                InsightCard.InsightType.DEFAULT));
    }

}
