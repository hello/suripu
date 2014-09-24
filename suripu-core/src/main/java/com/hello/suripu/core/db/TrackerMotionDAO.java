package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindTrackerMotion;
import com.hello.suripu.core.db.mappers.DeviceAccountPairMapper;
import com.hello.suripu.core.db.mappers.GroupedTrackerMotionMapper;
import com.hello.suripu.core.db.mappers.TrackerMotionMapper;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.TrackerMotion;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 5/8/14.
 */
public abstract class TrackerMotionDAO {

    @RegisterMapper(TrackerMotionMapper.class)
    @SqlQuery("SELECT * FROM tracker_motion_master WHERE " +
            "account_id = :account_id AND ts >= :start_timestamp AND ts <= :end_timestamp " +
            "ORDER BY ts ASC;"
    )
    public abstract ImmutableList<TrackerMotion> getBetween(@Bind("account_id") long accountId,
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
            "GROUP BY ts_bucket, tracker_id " +
            "ORDER BY tracker_id DESC, ts_bucket ASC;"
    )
    public abstract ImmutableList<TrackerMotion> getBetweenGrouped(@Bind("account_id") long accountId,
                                                   @Bind("start_timestamp") DateTime startTimestampUTC,
                                                   @Bind("end_timestamp") DateTime endTimestampUTC,
                                                   @Bind("slot_duration") Integer slotDuration);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO tracker_motion_master (account_id, tracker_id, svm_no_gravity, ts, offset_millis, local_utc_ts) " +
            "VALUES(:account_id, :tracker_id, :svm_no_gravity, :ts, :offset_millis, :local_utc_ts);")
    public abstract Long insertTrackerMotion(@BindTrackerMotion TrackerMotion trackerMotion);


    @SqlBatch("INSERT INTO tracker_motion_master (account_id, tracker_id, svm_no_gravity, ts, offset_millis, local_utc_ts) " +
            "VALUES(:account_ids, :tracker_ids, :svm_no_gravity, :ts, :offset_millis, :local_utc_ts);")
    public abstract Long batchInsert(
            @Bind("account_ids") List<Long> accountIDs,
            @Bind("tracker_ids") List<Long> trackerIDs,
            @Bind("svm_no_gravity") List<Integer> pillValues,
            @Bind("ts") List<DateTime> timestamps,
            @Bind("offset_millis") List<Integer> offsets,
            @Bind("local_utc_ts") List<DateTime> local_utc
            );

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE account_id = :account_id;")
    public abstract List<DeviceAccountPair> getTrackerIds(@Bind("account_id") Long accountId);

    @Timed public int batchInsertTrackerMotionData(List<TrackerMotion> trackerMotionData, int batchSize) {

        List<Long> accountIDs = new ArrayList<>();
        List<Long> trackerIDs = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        List<Long> rawTimestamps = new ArrayList<>();
        List<DateTime> timestamps = new ArrayList<>();
        List<Integer> offsets = new ArrayList<>();
        List<DateTime> local_utc_ts = new ArrayList<>();

        int totalInserted = 0;
        for (final TrackerMotion trackerMotion : trackerMotionData) {
            DateTime ts = new DateTime(trackerMotion.timestamp, DateTimeZone.UTC);
            DateTime local_ts = new DateTime(trackerMotion.timestamp, DateTimeZone.UTC).plusMillis(trackerMotion.offsetMillis);

            accountIDs.add(trackerMotion.accountId);
            trackerIDs.add(trackerMotion.trackerId);
            values.add(trackerMotion.value);
            rawTimestamps.add(trackerMotion.timestamp);
            timestamps.add(ts);
            offsets.add(trackerMotion.offsetMillis);
            local_utc_ts.add(local_ts);


            if (accountIDs.size() < batchSize) {
                continue;
            }
            Long inserted = this.batchInsert(accountIDs, trackerIDs, values, timestamps, offsets, local_utc_ts);
            if (inserted != accountIDs.size()) {
                // do individual inserts
                inserted = 0L;
                for (int i = 0; i < accountIDs.size(); i++) {
                    final TrackerMotion singleTrackerMotion = new TrackerMotion(
                            0L,
                            accountIDs.get(i),
                            trackerIDs.get(i),
                            rawTimestamps.get(i),
                            values.get(i),
                            offsets.get(i)
                    );
                    Long id = this.insertTrackerMotion(singleTrackerMotion);
                    if (id > 0L) {
                        inserted++;
                    }
                }
            }
            totalInserted += inserted;

        }

        return totalInserted;

    }
}
