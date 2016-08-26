package com.hello.suripu.core.insights.schedulers;

import com.google.common.base.Optional;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.insights.InsightProfile;
import com.hello.suripu.core.insights.InsightsLastSeen;
import com.librato.rollout.RolloutClient;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

public class RandomInsightScheduler implements InsightScheduler {

    private static final int LAST_TWO_WEEKS = 13; //last 2 weeks

    private static final Logger LOGGER = LoggerFactory.getLogger(RandomInsightScheduler.class);
    private final RolloutClient featureFlipper;

    public RandomInsightScheduler(final RolloutClient featureFlipper) {
        this.featureFlipper = featureFlipper;
    }

    @Override
    public Optional<InsightCard.Category> schedule(Map<InsightCard.Category, DateTime> recentCategories, InsightProfile profile) {
        final Optional<InsightCard.Category> toGenerateRandomCategory = selectRandomOldInsightsToGenerate(profile.accountId(), recentCategories, profile.utcnow());
        return toGenerateRandomCategory;
    }


    public Optional<InsightCard.Category> selectRandomOldInsightsToGenerate(final Long accountId, final Map<InsightCard.Category, DateTime> recentCategories, final DateTime currentTime) {

        /* randomly select a card that hasn't been generated recently - TODO when we have all categories
        final List<InsightCard.Category> eligibleCatgories = new ArrayList<>();
        for (final InsightCard.Category category : InsightCard.Category.values()) {
            if (!recentCategories.contains(category)) {
                eligibleCategories.add(category);
            }
        }
        */

        //Generate some Insights based on day of month - once every 9 days TODO: randomly generate old Insight on day of week if has not been generated in a while
        final Integer dayOfMonth = currentTime.getDayOfMonth();
        LOGGER.debug("The day of the month is {}", dayOfMonth);

        switch (dayOfMonth) {
            case 1:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.HUMIDITY, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.HUMIDITY);
            case 4:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.BED_LIGHT_DURATION, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.BED_LIGHT_DURATION);
            case 7:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.BED_LIGHT_INTENSITY_RATIO, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.BED_LIGHT_INTENSITY_RATIO);
            case 10:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.TEMPERATURE, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.TEMPERATURE);

            case 13:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.LIGHT, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.LIGHT);
            case 18:
                if (!featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_CAFFEINE, accountId, Collections.EMPTY_LIST)) {
                    return Optional.absent();
                }
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.CAFFEINE, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.CAFFEINE);
            case 19:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.SLEEP_QUALITY, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.SLEEP_QUALITY);
            case 22:
                if (!featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_SLEEP_TIME, accountId, Collections.EMPTY_LIST)) {
                    return Optional.absent();
                }
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.SLEEP_TIME, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.SLEEP_TIME);
            case 25:
                if (!featureFlipper.userFeatureActive(FeatureFlipper.INSIGHTS_AIR_QUALITY, accountId, Collections.EMPTY_LIST)) {
                    return Optional.absent();
                }
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.AIR_QUALITY, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.AIR_QUALITY);
        }
        return Optional.absent();
    }

    @Override
    public void update(InsightProfile profile, InsightCard.Category category) {

    }
}
