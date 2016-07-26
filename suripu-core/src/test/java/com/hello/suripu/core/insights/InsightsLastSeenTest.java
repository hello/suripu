package com.hello.suripu.core.insights;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by jarredheinrich on 7/26/16.
 */

public class InsightsLastSeenTest {

    @Test
    public void test_recentCategories() {
        final long fakeAccountId = 1L;
        //Turn on feature flip for marketing schedule
        final InsightsLastSeen fakeInsightLastSeen1 = new InsightsLastSeen(fakeAccountId, InsightCard.Category.AIR_QUALITY, DateTime.now().minusDays(14));
        final InsightsLastSeen fakeInsightLastSeen2 = new InsightsLastSeen(fakeAccountId, InsightCard.Category.ALCOHOL, DateTime.now().minusDays(1));
        final List<InsightsLastSeen> fakeInsightsLastSeen = Lists.newArrayList();;
        fakeInsightsLastSeen.add(fakeInsightLastSeen1);
        fakeInsightsLastSeen.add(fakeInsightLastSeen2);
        Map<InsightCard.Category, DateTime> recentCategories = InsightsLastSeen.getLastSeenInsights(fakeInsightsLastSeen);

        //TEST - Incorrect date for weekly insight - get nothing

        //Look for marketing insight - can't spy on private random, so do assert
        assertThat(recentCategories.containsKey(InsightCard.Category.AIR_QUALITY), is(true));
        assertThat(recentCategories.containsKey(InsightCard.Category.ALCOHOL), is(true));
        assertThat(recentCategories.containsKey(InsightCard.Category.BED_LIGHT_DURATION), is(false));

        final Boolean checkBedLight = InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.BED_LIGHT_DURATION, 13);
        final Boolean checkAlcohol = InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.ALCOHOL, 13);
        final Boolean checkAirQuality = InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.AIR_QUALITY, 13);
        final int numRecentInsights = InsightsLastSeen.getNumRecentInsights(recentCategories, 13);

        assertThat(checkBedLight, is(true));
        assertThat(checkAirQuality, is(true));
        assertThat(checkAlcohol, is(false));
        assertThat(numRecentInsights, is(1));
    }

}

