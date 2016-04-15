package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.GoalMsgEN;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by jyfan on 4/14/16.
 */
public class GoalWakeVariance {
    public static Optional<InsightCard> getInsights(final Long accountId) {
        return Optional.of(new InsightCard(accountId, GoalMsgEN.GOAL_WAKE_VARIANCE_TITLE, GoalMsgEN.GOAL_WAKE_VARIANCE_MSG, InsightCard.Category.GOAL_WAKE_VARIANCE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), GoalMsgEN.GOAL_WEEKLONG_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }
}
