package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.TimeZoneHistory;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

/**
 * Created by jarredheinrich on 11/1/16.
 */
public interface TimeZoneHistoryDAO {
    Optional<TimeZoneHistory> updateTimeZone(final long accountId, final DateTime updatedTime, final String clientTimeZoneId, int clientTimeZoneOffsetMillis);
    List<TimeZoneHistory> getTimeZoneHistory(final long accountId, final DateTime start);
    List<TimeZoneHistory> getMostRecentTimeZoneHistory(final long accountId, final DateTime start, int limit);
    List<TimeZoneHistory> getTimeZoneHistory(final long accountId, final DateTime start, final DateTime end);
    List<TimeZoneHistory> getTimeZoneHistory(final long accountId, final DateTime start, final DateTime end, int limit);
    Optional<TimeZoneHistory> getCurrentTimeZone(final long accountId);
    Map<DateTime, TimeZoneHistory> getAllTimeZones(final long accountId);
}
