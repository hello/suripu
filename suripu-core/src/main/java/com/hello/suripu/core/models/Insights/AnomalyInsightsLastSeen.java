package com.hello.suripu.core.models.Insights;

import com.google.common.base.Optional;
import org.joda.time.DateTime;

import java.util.Set;

/**
 * Created by jarredheinrich on 7/21/16.
 */
public class AnomalyInsightsLastSeen {
    public final Long accountId;
    public final Optional<InsightCard.Category> seenCategory;
    public final Optional<DateTime> updatedUTC;

    public AnomalyInsightsLastSeen(final Long accountId, final Optional<InsightCard.Category> seenCategory, final Optional<DateTime> updatedUTC) {
        this.accountId = accountId;
        this.seenCategory = seenCategory;
        this.updatedUTC = updatedUTC;
    }

    public AnomalyInsightsLastSeen(final Long accountId){
        this.accountId = accountId;
        this.seenCategory = Optional.absent();
        this.updatedUTC = Optional.absent();
    }
}


