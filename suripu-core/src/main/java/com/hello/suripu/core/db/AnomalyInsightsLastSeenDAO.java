package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Insights.AnomalyInsightsLastSeen;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.joda.time.DateTime;

/**
 * Created by jarredheinrich on 7/21/16.
 */
public interface AnomalyInsightsLastSeenDAO {

    ImmutableList<AnomalyInsightsLastSeen> getAnomalyInsightsByAccountId (final Long accountId);
    AnomalyInsightsLastSeen getAnomalyInsightsByAccountIdAndCategory (final Long accountId, final InsightCard.Category category);
    Boolean upsertAnomalyInsightsLastSeen (final AnomalyInsightsLastSeen anomalyInsightsLastSeen);
}
