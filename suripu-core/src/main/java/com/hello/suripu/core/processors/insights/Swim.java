package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.MarketingMsgEN;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by jyfan on 3/3/16.
 */
public class Swim {
    public static Optional<InsightCard> getMarketingInsights(final Long accountId) {
        final String categoryName = "Performance";
        return Optional.of(new InsightCard(accountId, MarketingMsgEN.SWIM_TITLE, MarketingMsgEN.SWIM_MSG, InsightCard.Category.SWIM, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), categoryName, InsightCard.InsightType.BASIC));
    }
}
