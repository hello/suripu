package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.GoalMsgEN;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by jyfan on 4/5/16.
 */
public class GoalCoffee {
    public static Optional<InsightCard> getInsights(final Long accountId) {
        return Optional.of(new InsightCard(accountId, GoalMsgEN.GOAL_COFFEE_TITLE, GoalMsgEN.GOAL_COFFEE_MSG, InsightCard.Category.GOAL_COFFEE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), GoalMsgEN.GOAL_WEEKLONG_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }
}
