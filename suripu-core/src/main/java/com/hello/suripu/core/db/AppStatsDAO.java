package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import org.joda.time.DateTime;

/**
 * Created by km on 9/16/15.
 */
public interface AppStatsDAO {
    void putInsightsLastViewed(Long accountId, DateTime lastViewed);
    Optional<DateTime> getInsightsLastViewed(Long accountId);
}
