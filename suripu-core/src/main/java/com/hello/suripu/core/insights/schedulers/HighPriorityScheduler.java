package com.hello.suripu.core.insights.schedulers;

import com.google.common.base.Optional;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.insights.InsightProfile;
import com.librato.rollout.RolloutClient;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class HighPriorityScheduler implements InsightScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HighPriorityScheduler.class);

    private final RolloutClient featureFlipper;

    public HighPriorityScheduler(final RolloutClient featureFlipper) {
        this.featureFlipper = featureFlipper;
    }

    @Override
    public Optional<InsightCard.Category> schedule(final Map<InsightCard.Category, DateTime> recentCategories, final InsightProfile profile) {
        //logic for generating current high-priority Insight
        return  selectHighPriorityInsightToGenerate(profile.accountId(), recentCategories, profile.utcnow());
    }

    @Override
    public void update(InsightProfile profile, InsightCard.Category category) {

    }

    private Optional<InsightCard.Category> selectHighPriorityInsightToGenerate(final Long accountId, final Map<InsightCard.Category, DateTime> recentCategories, final DateTime currentTime) {

        //TODO: Read category to generate off of an external file to allow for most flexibility
        return Optional.absent();
    }
}
