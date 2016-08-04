package com.hello.suripu.core.analytics;

import com.google.common.base.Optional;
import org.joda.time.DateTime;

public interface AnalyticsTrackingDAO {

    boolean putIfAbsent(TrackingEvent event, Long accountId);
    boolean putIfAbsent(TrackingEvent event, Long accountId, DateTime createdAt);
    Optional<TrackingEvent> get(Long accountId);
}
