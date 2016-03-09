package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.SleepScoreMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by jyfan on 3/3/16.
 */
public class SleepScore {
    public static Optional<InsightCard> getMarketingInsights(final Long accountId) {
        final Text text = SleepScoreMsgEN.getSleepScoreMarketing();
        final String categoryName = "Sleep Score";
        return Optional.of(new InsightCard(accountId, text.title, text.message, InsightCard.Category.SLEEP_SCORE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), categoryName, InsightCard.InsightType.BASIC));
    }
}
