package com.hello.suripu.core.insights;

import org.joda.time.DateTime;

import java.util.Set;

/**
 * Created by kingshy on 3/28/16
 */
public class MarketingInsightsSeen {
    public final Set<InsightCard.Category> seenCategories;
    public final DateTime updated;

    public MarketingInsightsSeen(final Set<InsightCard.Category> seenCategories, final DateTime updated) {
        this.seenCategories = seenCategories;
        this.updated = updated;
    }
}
