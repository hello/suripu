package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindTrackerMotion;
import com.hello.suripu.core.db.mappers.DeviceAccountPairMapper;
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
@RegisterMapper(TrackerMotionMapper.class)
public interface TrackerMotionDAO {


    @SqlQuery("SELECT * FROM tracker_motion_master WHERE " +
            "account_id = :account_id AND ts >= :start_timestamp AND ts <= :end_timestamp " +
            "ORDER BY ts ASC;"
    )
    public ImmutableList<TrackerMotion> getBetween(@Bind("account_id") long accountId,
                                                   @Bind("start_timestamp") DateTime startTimestampUTC,
                                                   @Bind("end_timestamp") DateTime endTimestampUTC);


    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO tracker_motion_master (account_id, tracker_id, svm_no_gravity, ts, offset_millis, local_utc_ts) " +
            "VALUES(:account_id, :tracker_id, :svm_no_gravity, :ts, :offset_millis, :local_utc_ts);")
    Long insertTrackerMotion(@BindTrackerMotion TrackerMotion trackerMotion);

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE account_id = :account_id;")
    List<DeviceAccountPair> getTrackerIds(@Bind("account_id") Long accountId);
}
