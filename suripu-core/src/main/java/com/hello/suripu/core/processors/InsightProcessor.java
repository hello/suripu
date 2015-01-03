package com.hello.suripu.core.processors;

import com.hello.suripu.core.models.Insights.GenericInsightCards;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by kingshy on 10/24/14.
 */
public class InsightProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsightProcessor.class);

    @Timed
    public static List<InsightCard> getInsights(final Long accountId) {
        Random rnd = new Random();
        LOGGER.debug("Generating insights for account {}", accountId);
        final DateTime createdUTC = new DateTime(DateTime.now(), DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);
        final DateTime localUTC = createdUTC.withTimeAtStartOfDay();
        final int offset = -28800000;

        final List<InsightCard> generated = new ArrayList<>();
        final List<InsightCard> availableInsights = new ArrayList<>();
        final List<GenericInsightCards> genericInsightCards = Collections.emptyList();

        availableInsights.add(new InsightCard(1L, accountId, "Lately", "[placeholder] You've been going to bed way **too late**.", InsightCard.Category.SLEEP_TIME, InsightCard.TimePeriod.RECENTLY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(2L, accountId, "Lately", "[placeholder] Your sleep has improved since you started working out.\n\nGood job!", InsightCard.Category.WORKOUT, InsightCard.TimePeriod.RECENTLY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(3L, accountId, "Lately", "[placeholder] You've been drinking **too much** coffee, it's affecting your sleep.", InsightCard.Category.CAFFEINE, InsightCard.TimePeriod.RECENTLY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(4L, accountId, "Lately", "[placeholder] You're not drinking enough juice, it's affecting your sleep.", InsightCard.Category.DIET, InsightCard.TimePeriod.RECENTLY, createdUTC, genericInsightCards));
        int pick = rnd.nextInt(4);
        generated.add(availableInsights.get(pick));

        availableInsights.add(new InsightCard(5L, accountId, "Last night", "[placeholder] You slept soundly for **4 hours**.", InsightCard.Category.SLEEP_DURATION, InsightCard.TimePeriod.DAILY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(6L, accountId, "Last night", "[placeholder] You fell asleep quicker than usual.", InsightCard.Category.SLEEP_TIME, InsightCard.TimePeriod.DAILY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(7L, accountId, "Last night", "[placeholder] You went to bed **1 hour later** than usual", InsightCard.Category.SLEEP_TIME, InsightCard.TimePeriod.DAILY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(8L, accountId, "Last night", "[placeholder] Your sleep was more agitated than usual.", InsightCard.Category.SLEEP_QUALITY, InsightCard.TimePeriod.DAILY, createdUTC, genericInsightCards));
        pick = rnd.nextInt(4) + 4;
        generated.add(availableInsights.get(pick));

        availableInsights.add(new InsightCard(9L, accountId, "This week", "[placeholder] You've been sleeping one hour less on average.", InsightCard.Category.SLEEP_DURATION, InsightCard.TimePeriod.WEEKLY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(10L, accountId, "This week", "[placeholder] You've been sleeping earlier than usual.", InsightCard.Category.SLEEP_TIME, InsightCard.TimePeriod.WEEKLY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(11L, accountId, "This week", "[placeholder] You've been getting up 30 minutes earlier than usual.", InsightCard.Category.WAKE_TIME, InsightCard.TimePeriod.WEEKLY, createdUTC, genericInsightCards));
        availableInsights.add(new InsightCard(12L, accountId, "This week", "[placeholder] Your sleep score is 10% lower than last week's.", InsightCard.Category.SLEEP_QUALITY, InsightCard.TimePeriod.WEEKLY, createdUTC, genericInsightCards));
        pick = rnd.nextInt(4) + 8;
        generated.add(availableInsights.get(pick));

        return generated;
    }
}
