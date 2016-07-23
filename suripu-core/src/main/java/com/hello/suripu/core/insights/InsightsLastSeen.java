package com.hello.suripu.core.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Insights.*;
import org.joda.time.*;

import java.util.Set;

/**
 * Created by jarredheinrich on 7/21/16.
 */
public class InsightsLastSeen {
    public final Long accountId;
    public final InsightCard.Category seenCategory;
    public final DateTime updatedUTC;

    public InsightsLastSeen(final Long accountId, final InsightCard.Category seenCategory, final DateTime updatedUTC) {
        this.accountId = accountId;
        this.seenCategory = seenCategory;
        this.updatedUTC = updatedUTC;
    }
    public InsightsLastSeen(final Long accountId, final InsightCard.Category seenCategory, final Long timestampUTC) {
        this.accountId = accountId;
        this.seenCategory = seenCategory;
        this.updatedUTC = new DateTime(timestampUTC, DateTimeZone.UTC);
    }
}


