package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindTrackerMotion;
import com.hello.suripu.core.db.mappers.DeviceAccountPairMapper;
import com.hello.suripu.core.db.mappers.GroupedTrackerMotionMapper;
import com.hello.suripu.core.db.mappers.TrackerMotionMapper;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

/**
 * Created by pangwu on 5/8/14.
 */
public interface TrackerMotionDAO {

    @RegisterMapper(TrackerMotionMapper.class)
    @SqlQuery("SELECT * FROM tracker_motion_master WHERE " +
            "account_id = :account_id AND ts >= :start_timestamp AND ts <= :end_timestamp " +
            "ORDER BY ts ASC;"
    )
    public ImmutableList<TrackerMotion> getBetween(@Bind("account_id") long accountId,
                                                   @Bind("start_timestamp") DateTime startTimestampUTC,
                                                   @Bind("end_timestamp") DateTime endTimestampUTC);

    @RegisterMapper(GroupedTrackerMotionMapper.class)
    @SqlQuery("SELECT MAX(account_id) as account_id, " +
            "MIN(id) as id, " +
            "MAX(tracker_id) as tracker_id, " +
            "ROUND(AVG(svm_no_gravity)) as svm_no_gravity, " +
            "date_trunc('hour', ts) + (CAST(date_part('minute', ts) AS integer) / :slot_duration) * :slot_duration * interval '1 min' AS ts_bucket, " +
            "MAX(offset_millis) as offset_millis " +
            "FROM tracker_motion_master " +
            "WHERE account_id = :account_id AND local_utc_ts >= :start_timestamp AND local_utc_ts <= :end_timestamp " +
            "GROUP BY ts_bucket " +
            "ORDER BY ts_bucket ASC;"
    )
    public ImmutableList<TrackerMotion> getBetweenGrouped(@Bind("account_id") long accountId,
                                                   @Bind("start_timestamp") DateTime startTimestampUTC,
                                                   @Bind("end_timestamp") DateTime endTimestampUTC,
                                                   @Bind("slot_duration") Integer slotDuration);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO tracker_motion_master (account_id, tracker_id, svm_no_gravity, ts, offset_millis, local_utc_ts) " +
            "VALUES(:account_id, :tracker_id, :svm_no_gravity, :ts, :offset_millis, :local_utc_ts);")
    Long insertTrackerMotion(@BindTrackerMotion TrackerMotion trackerMotion);

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE account_id = :account_id;")
    List<DeviceAccountPair> getTrackerIds(@Bind("account_id") Long accountId);
}
