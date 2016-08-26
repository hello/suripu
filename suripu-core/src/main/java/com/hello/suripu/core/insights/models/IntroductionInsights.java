package com.hello.suripu.core.insights.models;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.insights.models.text.IntroductionMsgEn;
import com.hello.suripu.core.insights.models.text.Text;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

/**
 * Created by kingshy on 1/9/15.
 */
public class IntroductionInsights {

    public static List<InsightCard> getIntroCards(final Long accountId) {
        final List<InsightCard> cards = ImmutableList.of(getIntroductionCard(accountId),
                getIntroSleepTipsCard(accountId),
                getIntroSleepDurationCard(accountId));
        return cards;
    }

    // TODO Pull all of these from DB
    public static InsightCard getIntroductionCard(final Long accountId) {
        final Text text = IntroductionMsgEn.getWelcomeMessage();
        final String categoryName = "Sense";
        return InsightCard.createIntroductionInsightCardsWithId(accountId, text.title, text.message, InsightCard.Category.GENERIC, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC).plusMillis(50), categoryName, InsightCard.InsightType.BASIC);
    }

    public static InsightCard getIntroSleepTipsCard(final Long accountId) {
        final Text text = IntroductionMsgEn.getSleepTipsMessage();
        final String categoryName = "Sleep Tips";
        return InsightCard.createIntroductionInsightCardsWithId(accountId, text.title, text.message, InsightCard.Category.SLEEP_HYGIENE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), categoryName, InsightCard.InsightType.BASIC);
    }

    public static InsightCard getIntroSleepDurationCard(final Long accountId) {

        final Text text = IntroductionMsgEn.getSleepDurationMessage();

        final String categoryName = "Sleep Tips";

        return InsightCard.createIntroductionInsightCardsWithId(accountId, text.title, text.message, InsightCard.Category.SLEEP_DURATION, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC).plusMillis(10), categoryName, InsightCard.InsightType.BASIC);
    }

}