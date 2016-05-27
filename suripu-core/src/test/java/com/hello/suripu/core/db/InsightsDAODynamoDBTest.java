package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class InsightsDAODynamoDBTest {


    private InsightCard generate(InsightCard.Category category) {
        return InsightCard.createBasicInsightCard(13L, "title", "message", category, InsightCard.TimePeriod.DAILY, DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT);
    }

    @Test
    public void testEmptyCardsAndEmptyCategoryNames() {
        final List<InsightCard> cards = Lists.newArrayList();
        final Map<InsightCard.Category, String> categoryNames =Maps.newHashMap();

        final List<InsightCard> processed = InsightsDAODynamoDB.backfillImagesBasedOnCategory(cards, categoryNames);
        assertThat(processed.isEmpty(), is(true));
    }

    @Test
    public void testEmptyCategoryNames() {
        final List<InsightCard> cards = Lists.newArrayList(generate(InsightCard.Category.LIGHT));
        final Map<InsightCard.Category, String> categoryNames =Maps.newHashMap();

        final List<InsightCard> processed = InsightsDAODynamoDB.backfillImagesBasedOnCategory(cards, categoryNames);
        assertThat(processed.size(), is(1));
        assertThat(processed.get(0).categoryName(), equalTo(InsightCard.Category.LIGHT.name().toLowerCase()));
    }

    @Test
    public void testIncompleteCategoryNames() {
        final List<InsightCard> cards = Lists.newArrayList(generate(InsightCard.Category.LIGHT), generate(InsightCard.Category.AIR_QUALITY));
        final String categoryName = "Light!";
        final Map<InsightCard.Category, String> categoryNames = ImmutableMap.of(InsightCard.Category.LIGHT, "Light!");

        final List<InsightCard> processed = InsightsDAODynamoDB.backfillImagesBasedOnCategory(cards, categoryNames);
        assertThat(processed.size(), is(2));
        assertThat(processed.get(0).categoryName(), equalTo(categoryName));
        assertThat(processed.get(1).categoryName(), equalTo(InsightCard.Category.AIR_QUALITY.name().toLowerCase()));
    }

    @Test
    public void testHasImage() {
        final List<InsightCard> cards = Lists.newArrayList(generate(InsightCard.Category.LIGHT));

        final Map<InsightCard.Category, String> categoryNames = ImmutableMap.of(InsightCard.Category.LIGHT, "Light!");

        final List<InsightCard> processed = InsightsDAODynamoDB.backfillImagesBasedOnCategory(cards, categoryNames);
        assertThat(processed.size(), is(1));
        assertThat(processed.get(0).image.isPresent(), is(true));
    }

    @Test
    public void testGenerateInsightType() {

        final Set<InsightCard.Category> basicCategories = Sets.newHashSet(
                InsightCard.Category.SLEEP_DURATION,
                InsightCard.Category.SLEEP_HYGIENE,
                InsightCard.Category.GENERIC
        );

        for (final InsightCard.Category category : basicCategories) {
            final InsightCard.InsightType insightType = InsightsDAODynamoDB.generateInsightType(
                    category,
                    Optional.<String>absent()
            );
            assertThat(insightType, equalTo(InsightCard.InsightType.BASIC));
        }

        for(InsightCard.Category category : InsightCard.Category.values()) {
            final InsightCard.InsightType expected = (basicCategories.contains(category))
                    ? InsightCard.InsightType.BASIC
                    : InsightCard.InsightType.DEFAULT;

                final InsightCard.InsightType insightType = InsightsDAODynamoDB.generateInsightType(
                        category,
                        Optional.<String>absent()
                );
                assertThat(insightType, equalTo(expected));
        }
    }

    @Test
    public void testGenerateInsightTypeCustom() {

        InsightCard.InsightType insightType = InsightsDAODynamoDB.generateInsightType(
                InsightCard.Category.SLEEP_HYGIENE,
                Optional.of("BASIC")
        );
        assertThat(insightType, equalTo(InsightCard.InsightType.BASIC));

        assertThat(insightType, equalTo(InsightCard.InsightType.BASIC));

        insightType = InsightsDAODynamoDB.generateInsightType(
                InsightCard.Category.SLEEP_HYGIENE,
                Optional.of("DEFAULT")
        );

        assertThat(insightType, equalTo(InsightCard.InsightType.DEFAULT));
    }


    @Test(expected = IllegalArgumentException.class)
    public void testGenerateInsightTypeMalformed() {

        InsightsDAODynamoDB.generateInsightType(
                InsightCard.Category.SLEEP_HYGIENE,
                Optional.of("BASIC!")
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateInsightTypeLowercase() {
        InsightsDAODynamoDB.generateInsightType(
                InsightCard.Category.SLEEP_HYGIENE,
                Optional.of("basic")
        );
    }
}
