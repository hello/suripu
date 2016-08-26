package com.hello.suripu.core.insights.models;

import com.google.common.base.Optional;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.insights.models.text.MarketingMsgEN;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by ksg on 5/26/16
 */
public class MarketingInsights {
    public static Optional<InsightCard> getDriveInsight(final Long accountId) {
        return Optional.of(InsightCard.createInsightCardWithCategoryName(accountId, MarketingMsgEN.DRIVE_TITLE, MarketingMsgEN.DRIVE_MSG, InsightCard.Category.DRIVE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), MarketingMsgEN.DRIVE_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

    public static Optional<InsightCard> getEatInsight(final Long accountId) {
        return Optional.of(InsightCard.createInsightCardWithCategoryName(accountId, MarketingMsgEN.EAT_TITLE, MarketingMsgEN.EAT_MSG, InsightCard.Category.EAT, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), MarketingMsgEN.EAT_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

    public static Optional<InsightCard> getLearnInsight(final Long accountId) {
        return Optional.of(InsightCard.createInsightCardWithCategoryName(accountId, MarketingMsgEN.LEARN_TITLE, MarketingMsgEN.LEARN_MSG, InsightCard.Category.LEARN, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), MarketingMsgEN.LEARN_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

    public static Optional<InsightCard> getLoveInsight(final Long accountId) {
        return Optional.of(InsightCard.createInsightCardWithCategoryName(accountId, MarketingMsgEN.LOVE_TITLE, MarketingMsgEN.LOVE_MSG, InsightCard.Category.LOVE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), MarketingMsgEN.LOVE_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

    public static Optional<InsightCard> getPlayInsight(final Long accountId) {
        return Optional.of(InsightCard.createInsightCardWithCategoryName(accountId, MarketingMsgEN.PLAY_TITLE, MarketingMsgEN.PLAY_MSG, InsightCard.Category.PLAY, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), MarketingMsgEN.PERFORMANCE_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

    public static Optional<InsightCard> getRunInsight(final Long accountId) {
        return Optional.of(InsightCard.createInsightCardWithCategoryName(accountId, MarketingMsgEN.RUN_TITLE, MarketingMsgEN.RUN_MSG, InsightCard.Category.RUN, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), MarketingMsgEN.PERFORMANCE_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

    public static Optional<InsightCard> getMarketingSleepScoreInsight(final Long accountId) {
        return Optional.of(InsightCard.createInsightCardWithCategoryName(accountId, MarketingMsgEN.SLEEP_SCORE_MARKETING_TITLE, MarketingMsgEN.SLEEP_SCORE_MARKETING_MSG, InsightCard.Category.SLEEP_SCORE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), MarketingMsgEN.SLEEPSCORE_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

    public static Optional<InsightCard> getSwimInsight(final Long accountId) {
        return Optional.of(InsightCard.createInsightCardWithCategoryName(accountId, MarketingMsgEN.SWIM_TITLE, MarketingMsgEN.SWIM_MSG, InsightCard.Category.SWIM, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), MarketingMsgEN.PERFORMANCE_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

    public static Optional<InsightCard> getWorkInsight(final Long accountId) {
        return Optional.of(InsightCard.createInsightCardWithCategoryName(accountId, MarketingMsgEN.WORK_TITLE, MarketingMsgEN.WORK_MSG, InsightCard.Category.WORK, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), MarketingMsgEN.PERFORMANCE_CATEGORY_NAME, InsightCard.InsightType.BASIC));
    }

}
