package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.PlayMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by jyfan on 3/3/16.
 */
public class Play {
    public static Optional<InsightCard> getMarketingInsights(final Long accountId) {
        final Text text = PlayMsgEN.getPlayMarketing();
        final String categoryName = "Performance";
        return Optional.of(new InsightCard(accountId, text.title, text.message, InsightCard.Category.PLAY, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), categoryName, InsightCard.InsightType.BASIC));
    }
}
