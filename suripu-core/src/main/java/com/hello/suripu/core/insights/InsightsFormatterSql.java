package com.hello.suripu.core.insights;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.TrendsInsightsDAO;

import java.util.Map;

public class InsightsFormatterSql {

    private final TrendsInsightsDAO trendsInsightsDAO;
    public InsightsFormatterSql(TrendsInsightsDAO trendsInsightsDAO) {
        this.trendsInsightsDAO = trendsInsightsDAO;
    }

    public static Optional<String> getInsightPreviewForCategory(final InsightCard.Category category,
                                                                final TrendsInsightsDAO trendsInsightsDAO)
    {
        final Map<String, String> insightInfoPreview = Maps.newHashMap();
        final ImmutableList<InfoInsightCards> infoInsightCards = trendsInsightsDAO.getAllGenericInsightCards();

        for (final InfoInsightCards card : infoInsightCards) {
            // only grab the first title for a category, if multiple exists
            final String categoryString = card.category.toCategoryString();
            if (!insightInfoPreview.containsKey(categoryString)) {
                insightInfoPreview.put(categoryString, card.title);
            }
        }
        return Optional.fromNullable(insightInfoPreview.get(category.toCategoryString()));
    }

    public Optional<String> getInsightPreviewForCategory(final InsightCard.Category category) {
        return getInsightPreviewForCategory(category, trendsInsightsDAO);
    }

    public static ImmutableMap<InsightCard.Category, String> categoryNames(final TrendsInsightsDAO trendsInsightsDAO) {
        final Map<InsightCard.Category, String> categoryNames = Maps.newHashMap();

        final ImmutableList<InfoInsightCards> infoInsightCards = trendsInsightsDAO.getAllGenericInsightCards();

        for (final InfoInsightCards card : infoInsightCards) {
            categoryNames.put(card.category, card.categoryName);
        }
        return ImmutableMap.copyOf(categoryNames);
    }

    public ImmutableMap<InsightCard.Category, String> categoryNames() {
        return categoryNames(trendsInsightsDAO);
    }
}
