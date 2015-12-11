package com.hello.suripu.core.processors.insights;

import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.IntroductionMsgEn;
import com.hello.suripu.core.models.Insights.Message.Text;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kingshy on 1/9/15.
 */
public class IntroductionInsights {

    public static List<InsightCard> getIntroCards(final Long accountId, final int userAgeInYears) {
        final List<InsightCard> cards = new ArrayList<>();
        cards.add(getIntroductionCard(accountId));
        cards.add(getIntroSleepTipsCard(accountId));
        cards.add(getIntroSleepDurationCard(accountId, userAgeInYears));
        return cards;
    }

    // TODO Pull all of these from DB
    public static InsightCard getIntroductionCard(final Long accountId) {
        final Text text = IntroductionMsgEn.getWelcomeMessage();
        final String categoryName = "Sense";
        return new InsightCard(accountId, text.title, text.message, InsightCard.Category.GENERIC, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC).plusMillis(50), categoryName);
    }

    public static InsightCard getIntroSleepTipsCard(final Long accountId) {
        final Text text = IntroductionMsgEn.getSleepTipsMessage();
        final String categoryName = "Sleep Tips";
        return new InsightCard(accountId, text.title, text.message, InsightCard.Category.SLEEP_HYGIENE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC), categoryName);
    }

    public static InsightCard getIntroSleepDurationCard(final Long accountId, final int userAgeInYears) {
        final SleepDuration.recommendation recommendation = SleepDuration.getSleepDurationRecommendation(userAgeInYears);

        final Text text = IntroductionMsgEn.getSleepDurationMessage(recommendation.minHours, recommendation.maxHours,
                recommendation.absoluteMinHours, recommendation.absoluteMaxHours);

        final String categoryName = "Sleep Tips";

        return new InsightCard(accountId, text.title, text.message, InsightCard.Category.SLEEP_DURATION, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC).plusMillis(10), categoryName);
    }

}