package com.hello.suripu.core.processors.insights;

import com.hello.suripu.core.models.Insights.InsightCard;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kingshy on 1/9/15.
 */
public class GenericInsights {

    public static List<InsightCard> getIntroCards(final Long accountId, final int userAgeInYears) {
        final List<InsightCard> cards = new ArrayList<>();
        cards.add(getIntroductionCard(accountId));
        cards.add(getIntroSleepTipsCard(accountId));
        cards.add(getIntroSleepDurationCard(accountId, userAgeInYears));
        return cards;
    }

    public static InsightCard getIntroductionCard(final Long accountId) {
        final String title = "Nice To Meet You";
        final String message = "Welcome to Sense. " +
                "This is where you'll see personalized **Sleep Insights** " +
                "related to your sleep patterns and behavior.\n\n" +
                "From time to time, you'll also be shown **Questions** in this space. " +
                "These questions will help us provide more accurate and detailed " +
                "insights about what's affecting your sleep. Let's get started.";

        return new InsightCard(accountId, title, message, InsightCard.Category.GENERIC, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC).plusMillis(50));
    }

    public static InsightCard getIntroSleepTipsCard(final Long accountId) {
        final String title = "Sleep Tips";
        final String message = "Having **healthy sleep habits** is crucial to a good night's sleep. Try to:\n\n" +
                "- Stick to the same sleep and wake up time.\n" +
                "- Avoid naps, caffeine drinks and heavy meals close to bedtime.\n" +
                "- Exercise frequently.\n" +
                "- Wind down about an hour before sleep.\n" +
                "- Manage your circadian rhythms.\n";


        return new InsightCard(accountId, title, message, InsightCard.Category.SLEEP_HYGIENE, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC));
    }

    public static InsightCard getIntroSleepDurationCard(final Long accountId, final int userAgeInYears) {

        final SleepDuration.recommendation recommendation = SleepDuration.getSleepDurationRecommendation(userAgeInYears);

        final String title = "How Much Sleep Do We Need?";
        final String message = "The National Sleep Foundation recently published a recommendation of " +
                String.format("%d to %d", recommendation.minHours, recommendation.maxHours) +
                " hours for your age group. While there may be some individual variability, you should sleep " +
                String.format("**no less than %d**", recommendation.absoluteMinHours) + " and " +
                String.format("**no more than %d**", recommendation.absoluteMaxHours) + " hours.";

        return new InsightCard(accountId, title, message, InsightCard.Category.SLEEP_DURATION, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC).plusMillis(10));
    }

}