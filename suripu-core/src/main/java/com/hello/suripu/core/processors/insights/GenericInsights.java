package com.hello.suripu.core.processors.insights;

import com.hello.suripu.core.models.Insights.InsightCard;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by kingshy on 1/9/15.
 */
public class GenericInsights {

    public static InsightCard getIntroductionCard(final Long accountId) {
        final String title = "Nice To Meet You";
        final String message = "Welcome to Sense. " +
                "This is where you'll see personalized **Sleep Insights** " +
                "related to your sleep patterns and behavior.\n\n" +
                "From time to time, you'll also be shown **Questions** in this space. " +
                "These questions will help us provide more accurate and detailed " +
                "insights about what's affecting your sleep. Let's get started.";
        return new InsightCard(accountId, title, message, InsightCard.Category.GENERIC, InsightCard.TimePeriod.NONE, DateTime.now(DateTimeZone.UTC));
    }
}