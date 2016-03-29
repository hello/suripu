package com.hello.suripu.core.db.util;

import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.DynamoDBIT;
import com.hello.suripu.core.db.MarketingInsightsSeenDAODynamoDB;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.MarketingInsightsSeen;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by ksg on 3/25/16
 */

public class MarketingInsightsSeenDynamoDBIT extends DynamoDBIT<MarketingInsightsSeenDAODynamoDB> {
    @Override
    protected CreateTableResult createTable()  {
        return dao.createTable(2L, 2L);
    }

    @Override
    protected MarketingInsightsSeenDAODynamoDB createDAO() {
        return new MarketingInsightsSeenDAODynamoDB(amazonDynamoDBClient, TABLE_NAME);
    }

    @Test
    public void testInsertNewItem() {
        final Long accountId = 10L;
        final InsightCard.Category category = InsightCard.Category.ALCOHOL;

        final boolean result = dao.updateSeenCategories(accountId, category);
        assertThat(result, is(true));

        final Optional<MarketingInsightsSeen> seen = dao.getSeenCategories(accountId);
        assertThat(seen.isPresent(), is(true));

        final Set<InsightCard.Category> categorySet = seen.get().seenCategories;
        assertThat(categorySet.contains(category), is(true));

        // add a second category
        // DOES ANYONE ELSE HATE MUTABILITY, OR AM I A SPECIAL SNOWFLAKE?!!!!
        final InsightCard.Category category2 = InsightCard.Category.PARTNER_MOTION;
        final boolean result2 = dao.updateSeenCategories(accountId, category2);
        assertThat(result2, is(true));

        final Optional<MarketingInsightsSeen> seen2 = dao.getSeenCategories(accountId);
        assertThat(seen2.isPresent(), is(true));

        final Set<InsightCard.Category> categorySet2 = seen2.get().seenCategories;
        assertThat(categorySet2.size(), is(2));
        assertThat(categorySet2.contains(category2), is(true));

        // add same category
        final boolean result3 = dao.updateSeenCategories(accountId, category2);
        assertThat(result3, is(true));

        final Optional<MarketingInsightsSeen> seen3 = dao.getSeenCategories(accountId);
        assertThat(seen3.isPresent(), is(true));
        assertThat(seen3.get().seenCategories.size(), is(2));
        assertThat(seen3.get().seenCategories.contains(category2), is(true));


    }
}
