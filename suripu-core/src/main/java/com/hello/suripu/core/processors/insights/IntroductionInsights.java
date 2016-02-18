package com.hello.suripu.core.processors.insights;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.IntroductionMsgEn;
import com.hello.suripu.core.models.Insights.Message.Text;
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
        return new InsightCard(accountId, text.title, text.message, InsightCard.Category.GENERIC, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC).plusMillis(50), categoryName, InsightCard.InsightType.GENERIC);
    }

    public static InsightCard getIntroSleepTipsCard(final Long accountId) {
        final Text text = IntroductionMsgEn.getSleepTipsMessage();
        final String categoryName = "Sleep Tips";
        return new InsightCard(accountId, text.title, text.message, InsightCard.Category.SLEEP_HYGIENE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), categoryName, InsightCard.InsightType.GENERIC);
    }

    public static InsightCard getIntroSleepDurationCard(final Long accountId) {

        final Text text = IntroductionMsgEn.getSleepDurationMessage();

        final String categoryName = "Sleep Tips";

        return new InsightCard(accountId, text.title, text.message, InsightCard.Category.SLEEP_DURATION, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC).plusMillis(10), categoryName, InsightCard.InsightType.GENERIC);
    }

}