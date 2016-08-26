package com.hello.suripu.core.insights;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.MarketingInsightsSeenDAODynamoDB;
import com.hello.suripu.core.insights.schedulers.HighPriorityScheduler;
import com.hello.suripu.core.insights.schedulers.InsightScheduler;
import com.hello.suripu.core.insights.schedulers.MarketingInsightsScheduler;
import com.hello.suripu.core.insights.schedulers.RandomInsightScheduler;
import com.hello.suripu.core.insights.schedulers.WeeklyScheduler;
import com.librato.rollout.RolloutClient;

import java.util.List;

public class SchedulerFactory {

    private final MarketingInsightsSeenDAODynamoDB marketingInsightsSeenDAODynamoDB;
    private final RolloutClient featureFlipper;

    public SchedulerFactory(final MarketingInsightsSeenDAODynamoDB marketingInsightsSeenDAODynamoDB, final RolloutClient featureFlipper) {
        this.marketingInsightsSeenDAODynamoDB = marketingInsightsSeenDAODynamoDB;
        this.featureFlipper = featureFlipper;
    }

    public InsightScheduler marketingScheduler() {
        return new MarketingInsightsScheduler(marketingInsightsSeenDAODynamoDB);
    }

    public InsightScheduler weeklyScheduler() {
        return new WeeklyScheduler();
    }

    public InsightScheduler randomScheduler() {
        return new RandomInsightScheduler(featureFlipper);
    }

    public InsightScheduler highPriorityScheduler() {
        return new HighPriorityScheduler(featureFlipper);
    }

    public List<InsightScheduler> all() {

        final List<InsightScheduler> schedulers = Lists.newArrayList(
                weeklyScheduler(),
                highPriorityScheduler(),
                randomScheduler(),
                marketingScheduler()
        );
        return ImmutableList.copyOf(schedulers);
    }
}
