package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Insights.GenericInsightCards;
import com.hello.suripu.core.models.Insights.SleepInsight;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by kingshy on 10/24/14.
 */
public class InsightProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsightProcessor.class);

    @Timed
    public static List<SleepInsight> getInsights(final Long accountId) {
        Random rnd = new Random();
        LOGGER.debug("Generating insights for account {}", accountId);
        final DateTime created_utc = new DateTime(DateTime.now(), DateTimeZone.UTC).withSecondOfMinute(0).withMillisOfSecond(0);

        final List<SleepInsight> generated = new ArrayList<>();
        final List<SleepInsight> availableInsights = new ArrayList<>();

        availableInsights.add(new SleepInsight(1, Optional.<Long>absent(), "Lately", "[placeholder] You've been going to bed way **too late**.", GenericInsightCards.Category.SLEEP_TIME, created_utc));
        availableInsights.add(new SleepInsight(2, Optional.<Long>absent(), "Lately", "[placeholder] Your sleep has improved since you started working out.\n\nGood job!", GenericInsightCards.Category.WORKOUT, created_utc));
        availableInsights.add(new SleepInsight(3, Optional.<Long>absent(), "Lately", "[placeholder] You've been drinking **too much** coffee, it's affecting your sleep.", GenericInsightCards.Category.CAFFEINE, created_utc));
        availableInsights.add(new SleepInsight(4, Optional.<Long>absent(), "Lately", "[placeholder] You're not drinking enough juice, it's affecting your sleep.", GenericInsightCards.Category.DIET, created_utc));
        int pick = rnd.nextInt(4);
        generated.add(availableInsights.get(pick));

        availableInsights.add(new SleepInsight(5, Optional.<Long>absent(), "Last night", "[placeholder] You slept soundly for **4 hours**.", GenericInsightCards.Category.SLEEP_DURATION, created_utc));
        availableInsights.add(new SleepInsight(6, Optional.<Long>absent(), "Last night", "[placeholder] You fell asleep quicker than usual.", GenericInsightCards.Category.SLEEP_TIME, created_utc));
        availableInsights.add(new SleepInsight(7, Optional.<Long>absent(), "Last night", "[placeholder] You went to bed **1 hour later** than usual", GenericInsightCards.Category.SLEEP_TIME, created_utc));
        availableInsights.add(new SleepInsight(8, Optional.<Long>absent(), "Last night", "[placeholder] Your sleep was more agitated than usual.", GenericInsightCards.Category.SLEEP_QUALITY, created_utc));
        pick = rnd.nextInt(4) + 4;
        generated.add(availableInsights.get(pick));

        availableInsights.add(new SleepInsight(9, Optional.<Long>absent(), "This week", "[placeholder] You've been sleeping one hour less on average.", GenericInsightCards.Category.SLEEP_DURATION, created_utc));
        availableInsights.add(new SleepInsight(10, Optional.<Long>absent(), "This week", "[placeholder] You've been sleeping earlier than usual.", GenericInsightCards.Category.SLEEP_TIME, created_utc));
        availableInsights.add(new SleepInsight(11, Optional.<Long>absent(), "This week", "[placeholder] You've been getting up 30 minutes earlier than usual.", GenericInsightCards.Category.WAKEUP_TIME, created_utc));
        availableInsights.add(new SleepInsight(12, Optional.<Long>absent(), "This week", "[placeholder] Your sleep score is 10% lower than last week's.", GenericInsightCards.Category.SLEEP_QUALITY, created_utc));
        pick = rnd.nextInt(4) + 8;
        generated.add(availableInsights.get(pick));

        return generated;
    }
}
