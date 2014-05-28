package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.TrackerMotion;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

/**
 * Created by pangwu on 5/8/14.
 */
@RegisterMapper(TrackerMotionMapper.class)
public interface TrackerMotionDAO {

    @SqlQuery("SELECT * " +
            "FROM motion " +
            // TODO: Test changing trackerId for the same account
            "WHERE account_id = :account_id AND ts > :ts " +  // We ignore the tracker id when retrieving data for processing
            "ORDER BY ts ASC;"
    )
    public ImmutableList<TrackerMotion> getAllTrackerMotionAfter(@Bind("account_id") long accountId,
                                                                       @Bind("ts") DateTime timestampUTC);

    @SqlQuery("SELECT * FROM motion WHERE account_id = :account_id ORDER BY ts DESC limit :n;")
    public ImmutableList<TrackerMotion> getLast(@Bind("n") int numberOfRecords,
                                                      @Bind("account_id") long accountId);

    @SqlQuery("SELECT * FROM motion WHERE " +
            "account_id = :account_id AND ts >= :start_timestamp AND ts <= :end_timestamp " +
            "ORDER BY ts ASC;"
    )
    public ImmutableList<TrackerMotion> getBetween(@Bind("account_id") long accountId,
                                                   @Bind("start_timestamp") DateTime startTimestampUTC,
                                                   @Bind("end_timestamp") DateTime endTimestampUTC);

    @SqlBatch("INSERT INTO motion (account_id, tracker_id, svm_no_gravity, ts, offset_millis) " +
            "VALUES(:account_id, :tracker_id, :svm_no_gravity, :ts, :offset_millis);")
    void insertTrackerMotionBatch(@Bind("account_id") Long accountId,
                                  @Bind("tracker_id") List<String> trackerIds,
                                  @Bind("svm_no_gravity") List<Integer> values,
                                  @Bind("ts") List<DateTime> timestamps,
                                  @Bind("offset_millis") List<Integer> offsets);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO motion (account_id, tracker_id, svm_no_gravity, ts, offset_millis) " +
            "VALUES(:account_id, :tracker_id, :svm_no_gravity, :ts, :offset_millis);")
    Long insertTrackerMotion(@Bind("account_id") Long accountId,
                             @Bind("tracker_id") String trackerId,
                             @Bind("svm_no_gravity") int value,
                             @Bind("ts") DateTime timestampUTC,
                             @Bind("offset_millis") int offset);
}
