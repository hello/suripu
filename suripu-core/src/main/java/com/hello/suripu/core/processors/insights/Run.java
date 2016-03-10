package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.MarketingMsgEN;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by jyfan on 3/3/16.
 */
public class Run {
    public static Optional<InsightCard> getMarketingInsights(final Long accountId) {
        final String categoryName = "Performance";
        return Optional.of(new InsightCard(accountId, MarketingMsgEN.RUN_TITLE, MarketingMsgEN.RUN_MSG, InsightCard.Category.RUN, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), categoryName, InsightCard.InsightType.BASIC));
    }
}
