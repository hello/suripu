package com.hello.suripu.core.insights;

import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Insights.InsightCard;

/**
 * Created by jarredheinrich on 7/21/16.
 */
public interface InsightsLastSeenDAO {

    ImmutableList<InsightsLastSeen> getAll (final Long accountId);
    Optional<InsightsLastSeen> getFor (final Long accountId, final InsightCard.Category category);
    Boolean markLastSeen (final InsightsLastSeen insightLastSeen);
}
