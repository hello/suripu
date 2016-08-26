package com.hello.suripu.core.insights.schedulers;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.MarketingInsightsSeenDAODynamoDB;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.insights.InsightProfile;
import com.hello.suripu.core.insights.MarketingInsightsSeen;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MarketingInsightsScheduler implements InsightScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketingInsightsScheduler.class);

    private static final Random RANDOM = new Random();

    private static final ImmutableSet<InsightCard.Category> marketingInsightPool = ImmutableSet.copyOf(Sets.newHashSet(
            InsightCard.Category.DRIVE,
            InsightCard.Category.EAT,
            InsightCard.Category.LEARN,
            InsightCard.Category.LOVE,
            InsightCard.Category.PLAY,
            InsightCard.Category.RUN,
            InsightCard.Category.SWIM,
            InsightCard.Category.WORK
    ));

    private final MarketingInsightsSeenDAODynamoDB marketingInsightsSeenDAODynamoDB;

    public MarketingInsightsScheduler(MarketingInsightsSeenDAODynamoDB marketingInsightsSeenDAODynamoDB) {
        this.marketingInsightsSeenDAODynamoDB = marketingInsightsSeenDAODynamoDB;
    }

    private Optional<InsightCard.Category> selectMarketingInsightToGenerate(final Long accountId, final DateTime currentTime) {
        //Get all historical insight categories
        final Optional<MarketingInsightsSeen> marketingInsightsSeenOptional = marketingInsightsSeenDAODynamoDB.getSeenCategories(accountId);
        if (!marketingInsightsSeenOptional.isPresent()) {
            return selectMarketingInsightToGenerate(currentTime, new HashSet<InsightCard.Category>(), RANDOM, currentTime.minusDays(1));
        }

        return selectMarketingInsightToGenerate(currentTime, marketingInsightsSeenOptional.get().seenCategories, RANDOM, marketingInsightsSeenOptional.get().updated);
    }

    private Optional<InsightCard.Category> selectMarketingInsightToGenerate(final DateTime currentTime, final Set<InsightCard.Category> marketingSeenCategories, final Random random, final DateTime lastUpdate) {
        final DateTime today = currentTime.withTimeAtStartOfDay(); //currentTime is DateTime.now() - UTC
        final DateTime lastMarketingUpdate = lastUpdate.withTimeAtStartOfDay(); //parameter is updated_utc

        //Already generated marketing insight today. skip
        if (today.isEqual(lastMarketingUpdate)) {
            return Optional.absent();
        }

        final Integer dayOfMonth = currentTime.getDayOfMonth();
        LOGGER.debug("The day of the month is {}", dayOfMonth);

        //Check date condition
        switch (dayOfMonth) {
            case 1:
            case 4:
            case 7:
            case 10:
            case 13:
            case 16:
            case 19:

                //Pull random insight out of set of allowed marketing insights
                final Optional<InsightCard.Category> pickedRandomInsight = pickRandomInsightCategory(marketingInsightPool, marketingSeenCategories, random);
                return pickedRandomInsight;
        }

        return Optional.absent();
    }


    private Optional<InsightCard.Category> pickRandomInsightCategory(final Set<InsightCard.Category> insightPool, final Set<InsightCard.Category> seenPool, final Random random) {
        //For category in seen pool, if it is in insight pool, remove from insight pool
        final Set<InsightCard.Category> allowedPool = Sets.newHashSet();

        for (InsightCard.Category category : insightPool) {
            if (!seenPool.contains(category)) {
                allowedPool.add(category);
            }
        }

        //Pick random category out of allowed pool
        if (allowedPool.isEmpty()) {
            return Optional.absent();
        }

        final InsightCard.Category[] allowedPoolList = allowedPool.toArray(new InsightCard.Category[allowedPool.size()]);
        final Integer randomIndex = random.nextInt(allowedPool.size());

        return Optional.of(allowedPoolList[randomIndex]);
    }

    @Override
    public void update(InsightProfile profile, InsightCard.Category category) {
        if (marketingInsightPool.contains(category)) {
            marketingInsightsSeenDAODynamoDB.updateSeenCategories(profile.pill().accountId, category);
        }
    }

    @Override
    public Optional<InsightCard.Category> schedule(Map<InsightCard.Category, DateTime> recentCategories, InsightProfile profile) {
        return selectMarketingInsightToGenerate(profile.accountId(), profile.utcnow());
    }
}
