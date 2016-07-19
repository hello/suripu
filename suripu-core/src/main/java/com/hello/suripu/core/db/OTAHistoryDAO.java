package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.OTAHistory;
import org.joda.time.DateTime;

import java.util.List;

public interface OTAHistoryDAO {
    Optional<OTAHistory> insertOTAEvent(OTAHistory historyEntry);

    List<OTAHistory> getOTAEvents(String deviceId, DateTime startTime, DateTime endTime);
}
