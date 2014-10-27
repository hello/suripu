package com.hello.suripu.core.processors;

import com.hello.suripu.core.models.SleepInsight;
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
        availableInsights.add(new SleepInsight("Lately", "You've been going to bed way too late.", created_utc));
        availableInsights.add(new SleepInsight("Lately", "Your sleep has improved since you started working out. Good job!", created_utc));
        availableInsights.add(new SleepInsight("Lately", "You've been drinking too much coffee, it's destroying your sleep.", created_utc));
        availableInsights.add(new SleepInsight("Lately", "You're not drinking enough juice, it's affecting your sleep.", created_utc));
        int pick = rnd.nextInt(4);
        generated.add(availableInsights.get(pick));

        availableInsights.add(new SleepInsight("Last night", "You slept soundly for 4 hours.", created_utc));
        availableInsights.add(new SleepInsight("Last night", "You fell asleep quicker than usual.", created_utc));
        availableInsights.add(new SleepInsight("Last night", "You went to bed 1 hour later than usual", created_utc));
        availableInsights.add(new SleepInsight("Last night", "Your sleep was more agitated than usual.", created_utc));
        pick = rnd.nextInt(4) + 4;
        generated.add(availableInsights.get(pick));

        availableInsights.add(new SleepInsight("This week", "You've been sleeping one hour less on average.", created_utc));
        availableInsights.add(new SleepInsight("This week", "You've been sleeping earlier than usual.", created_utc));
        availableInsights.add(new SleepInsight("This week", "You've been getting up 30 minutes earlier than usual.", created_utc));
        availableInsights.add(new SleepInsight("This week", "Your sleep score is 10% lower than last week's.", created_utc));
        pick = rnd.nextInt(4) + 8;
        generated.add(availableInsights.get(pick));

        return generated;
    }
}
