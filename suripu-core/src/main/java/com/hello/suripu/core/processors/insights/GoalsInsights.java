package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.GoalMsgEN;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by ksg on 5/26/16
 */
public class GoalsInsights {
    public static Optional<InsightCard> getCoffeeInsight(final Long accountId) {
        return Optional.of(new InsightCard(accountId, GoalMsgEN.GOAL_COFFEE_TITLE, GoalMsgEN.GOAL_COFFEE_MSG, InsightCard.Category.GOAL_COFFEE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), GoalMsgEN.GOAL_WEEKLONG_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

    public static Optional<InsightCard> getGoOutsideInsight(final Long accountId) {
        return Optional.of(new InsightCard(accountId, GoalMsgEN.GOAL_GO_OUTSIDE_TITLE, GoalMsgEN.GOAL_GO_OUTSIDE_MSG, InsightCard.Category.GOAL_GO_OUTSIDE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), GoalMsgEN.GOAL_WEEKLONG_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

    public static Optional<InsightCard> getScheduleThoughtsInsight(final Long accountId) {
        return Optional.of(new InsightCard(accountId, GoalMsgEN.GOAL_SCHEDULE_THOUGHTS_TITLE, GoalMsgEN.GOAL_SCHEDULE_THOUGHTS_MSG, InsightCard.Category.GOAL_SCHEDULE_THOUGHTS, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), GoalMsgEN.GOAL_WEEKLONG_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

    public static Optional<InsightCard> getScreensInsight(final Long accountId) {
        return Optional.of(new InsightCard(accountId, GoalMsgEN.GOAL_SCREENS_TITLE, GoalMsgEN.GOAL_SCREENS_MSG, InsightCard.Category.GOAL_SCREENS, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), GoalMsgEN.GOAL_WEEKLONG_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

    public static Optional<InsightCard> getWakeVarianceInsight(final Long accountId) {
        return Optional.of(new InsightCard(accountId, GoalMsgEN.GOAL_WAKE_VARIANCE_TITLE, GoalMsgEN.GOAL_WAKE_VARIANCE_MSG, InsightCard.Category.GOAL_WAKE_VARIANCE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), GoalMsgEN.GOAL_WEEKLONG_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

}
