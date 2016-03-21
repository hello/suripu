package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.GoalMsgEN;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by jyfan on 3/21/16.
 */
public class GoalGoOutside {
    public static Optional<InsightCard> getInsights(final Long accountId) {
        final String categoryName = "Goal Go Outside";
        return Optional.of(new InsightCard(accountId, GoalMsgEN.GOAL_GO_OUTSIDE_MARKETING_TITLE, GoalMsgEN.GOAL_GO_OUTSIDE_MARKETING_MSG, InsightCard.Category.GOAL_GO_OUTSIDE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), categoryName, InsightCard.InsightType.BASIC));
    }
}
