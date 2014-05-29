package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.db.mappers.EventMapper;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

/**
 * Created by pangwu on 5/8/14.
 */
@RegisterMapper(EventMapper.class)
public interface EventDAO {

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO event (account_id, event_type, start_time_utc, end_time_utc, offset_millis) " +
            "VALUES (:account_id, :event_type, :start_time_utc, :end_time_utc, :offset_millis);")
    Long create(@Bind("account_id") long accountId,
                   @Bind("event_type") int eventType,
                   @Bind("start_time_utc") DateTime startTime,
                   @Bind("end_time_utc") DateTime endTime,
                   @Bind("offset_millis") int offsetMillis);


    @SqlQuery("SELECT * FROM event " +
            "WHERE account_id = :account_id AND event_type = :event_type AND offset_millis = :offset_millis AND start_time_utc >= :start_time_utc AND start_time_utc <= :end_time_utc " +
            "ORDER BY start_time_utc"
    )
    ImmutableList<Event> getByTypeAndTimeRange(@Bind("account_id") long accountId,
                                                @Bind("event_type") int type,
                                                @Bind("start_time_utc") DateTime startTimeUTC,
                                                @Bind("end_time_utc") DateTime endTimeUTC,
                                                @Bind("offset_millis") int timeZoneOffset);

    @SqlQuery("SELECT * FROM event WHERE account_id = :account_id AND event_type = :type ORDER BY start_time_utc DESC limit :n;")
    public ImmutableList<Event> getLast(@Bind("n") int numberOfRecords,
                                        @Bind("type") int eventType,
                                        @Bind("account_id") long accountId);
}
