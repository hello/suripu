package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindTrackerMotionBatch;
import com.hello.suripu.core.db.mappers.TrackerMotionBatchMapper;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

/**
 * Created by pangwu on 6/23/14.
 */
public interface TrackerMotionBatchDAO {

    @SqlUpdate("INSERT INTO motion_batch (account_id, amplitudes, ts, offset_millis) " +
            "VALUES(:account_id, :amplitudes, :ts, :offset_millis)")
    void insert(@BindTrackerMotionBatch final TrackerMotion.Batch batchMotion);


    @RegisterMapper(TrackerMotionBatchMapper.class)
    @SqlQuery("SELECT * FROM motion_batch WHERE account_id = :account_id AND ts >= :start_timestamp AND ts <= :end_timestamp ORDER BY ts ASC")
    ImmutableList<TrackerMotion.Batch> getBetween(
            @Bind("account_id") final Long accountId,
            @Bind("start_timestamp") final DateTime startTimestampUTC,
            @Bind("end_timestamp") final DateTime endTimestampUTC);
}
