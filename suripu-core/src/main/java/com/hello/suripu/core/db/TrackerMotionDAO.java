package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.TrackerMotionMapper;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

/**
 * Created by pangwu on 5/8/14.
 */
@RegisterMapper(TrackerMotionMapper.class)
public interface TrackerMotionDAO {


    @SqlQuery("SELECT * FROM motion WHERE " +
            "account_id = :account_id AND ts >= :start_timestamp AND ts <= :end_timestamp " +
            "ORDER BY ts ASC;"
    )
    public ImmutableList<TrackerMotion> getBetween(@Bind("account_id") long accountId,
                                                   @Bind("start_timestamp") DateTime startTimestampUTC,
                                                   @Bind("end_timestamp") DateTime endTimestampUTC);


    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO motion (account_id, tracker_id, svm_no_gravity, ts, offset_millis) " +
            "VALUES(:account_id, :tracker_id, :svm_no_gravity, :ts, :offset_millis);")
    Long insertTrackerMotion(@Bind("account_id") Long accountId,
                             @Bind("tracker_id") String trackerId,
                             @Bind("svm_no_gravity") int value,
                             @Bind("ts") DateTime timestampUTC,
                             @Bind("offset_millis") int offset);
}
