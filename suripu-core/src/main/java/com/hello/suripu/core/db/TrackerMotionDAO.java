package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindTrackerMotion;
import com.hello.suripu.core.db.mappers.DeviceStatusMapper;
import com.hello.suripu.core.db.mappers.GroupedTrackerMotionMapper;
import com.hello.suripu.core.db.mappers.TrackerMotionMapper;
import com.hello.suripu.core.db.mappers.TrackerMotionOffsetMillisMapper;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pangwu on 5/8/14.
 */
public abstract class TrackerMotionDAO implements PillDataIngestDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackerMotionDAO.class);
    private static final Pattern PG_UNIQ_PATTERN = Pattern.compile("ERROR: duplicate key value violates unique constraint \"(\\w+)\"");

    @RegisterMapper(TrackerMotionMapper.class)
    @SqlQuery("SELECT * FROM tracker_motion_master WHERE " +
            "account_id = :account_id AND ts >= :start_timestamp AND ts <= :end_timestamp " +
            "ORDER BY ts ASC;"
    )
    public abstract ImmutableList<TrackerMotion> getBetween(@Bind("account_id") long accountId,
                                                   @Bind("start_timestamp") final DateTime startTimestampUTC,
                                                   @Bind("end_timestamp") final DateTime endTimestampUTC);

    @RegisterMapper(TrackerMotionMapper.class)
    @SqlQuery("SELECT * FROM tracker_motion_master WHERE " +
            "account_id = :account_id AND local_utc_ts >= :start_timestamp_local_utc AND local_utc_ts <= :end_timestamp_local_utc " +
            "ORDER BY ts ASC;"
    )
    public abstract ImmutableList<TrackerMotion> getBetweenLocalUTC(@Bind("account_id") long accountId,
                                                   @Bind("start_timestamp_local_utc") final DateTime startTimestampLocalUTC,
                                                   @Bind("end_timestamp_local_utc") final DateTime endTimestampLocalUTC);

    @SqlQuery("SELECT COUNT(*) FROM tracker_motion_master WHERE " +
            "account_id = :account_id AND local_utc_ts >= :start_timestamp_local_utc AND local_utc_ts <= :end_timestamp_local_utc;"
    )
    public abstract Integer getDataCountBetweenLocalUTC(@Bind("account_id") long accountId,
                                                                    @Bind("start_timestamp_local_utc") final DateTime startTimestampLocalUTC,
                                                                    @Bind("end_timestamp_local_utc") final DateTime endTimestampLocalUTC);

    @RegisterMapper(DeviceStatusMapper.class)
    @SingleValueResult(DeviceStatus.class)
    @SqlQuery("SELECT id, tracker_id AS pill_id, '1' AS firmware_version, 100 AS battery_level, ts AS last_seen, 0 AS uptime " +
            "FROM tracker_motion_master WHERE local_utc_ts > now() - interval '15 days' AND " +
            "tracker_id = :pill_id AND " +
            "account_id = :account_id ORDER BY local_utc_ts DESC LIMIT 1;")
    public abstract Optional<DeviceStatus> pillStatus(@Bind("pill_id") final Long pillId,
                                                      @Bind("account_id") final Long accountId);


    @RegisterMapper(GroupedTrackerMotionMapper.class)
    @SqlQuery("SELECT MAX(account_id) as account_id, " +
            "MIN(id) as id, " +
            "MAX(tracker_id) as tracker_id, " +
            "ROUND(AVG(svm_no_gravity)) as svm_no_gravity, " +
            "ROUND(AVG(motion_range)) AS motion_range, " +
            "ROUND(AVG(kickoff_counts)) AS kickoff_counts, " +
            "ROUND(AVG(on_duration)) AS on_duration, " +
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

    @SingleValueResult(Integer.class)
    @SqlUpdate("INSERT INTO tracker_motion_master (account_id, tracker_id, svm_no_gravity, ts, offset_millis, local_utc_ts, motion_range, kickoff_counts, on_duration_seconds) " +
            "VALUES(:account_id, :tracker_id, :svm_no_gravity, :ts, :offset_millis, :local_utc_ts, :motion_range, :kickoff_counts, :on_duration_seconds);")
    public abstract Integer insertTrackerMotion(@BindTrackerMotion TrackerMotion trackerMotion);


    @SqlBatch("INSERT INTO tracker_motion_master (account_id, tracker_id, svm_no_gravity, ts, offset_millis, local_utc_ts, motion_range, kickoff_counts, on_duration_seconds) " +
            "VALUES(:account_ids, :tracker_ids, :svm_no_gravity, :ts, :offset_millis, :local_utc_ts, :motion_range, :kickoff_counts, :on_duration_seconds);")
    public abstract void batchInsert(
            @Bind("account_ids") List<Long> accountIDs,
            @Bind("tracker_ids") List<Long> trackerIDs,
            @Bind("svm_no_gravity") List<Integer> pillValues,
            @Bind("ts") List<DateTime> timestamps,
            @Bind("offset_millis") List<Integer> offsets,
            @Bind("local_utc_ts") List<DateTime> local_utc,
            @Bind("motion_range") List<Long> motionRanges,
            @Bind("kickoff_counts") List<Long> kickoffCountsList,
            @Bind("on_duration_seconds") List<Long> onDurationSecondsList

            );

    @SqlUpdate("DELETE FROM tracker_motion_master WHERE tracker_id = :tracker_id")
    public abstract Integer deleteDataTrackerID(@Bind("tracker_id") Long trackerID);

    @RegisterMapper(TrackerMotionOffsetMillisMapper.class)
    @SqlQuery("SELECT MAX(id) AS id, " +
            "MAX(account_id) AS account_id, " +
            "MAX(tracker_id) AS tracker_id, " +
            "MIN(ts) AS ts, " +
            "offset_millis, " +
            "MIN(local_utc_ts) AS ts_bucket from tracker_motion_master " +
            "WHERE account_id = :account_id and local_utc_ts >= :start_date and local_utc_ts <= :end_date " +
            "GROUP BY offset_millis")
    public abstract ImmutableList<TrackerMotion> getTrackerOffsetMillis(
            @Bind("account_id") long accountId,
            @Bind("start_date") DateTime startDate,
            @Bind("end_date") DateTime endDate);

    public int batchInsertTrackerMotionData(final List<TrackerMotion> trackerMotionData, final int batchSize) {

        final List<Long> accountIDs = new ArrayList<>();
        final List<Long> trackerIDs = new ArrayList<>();
        final List<Integer> values = new ArrayList<>();
        final List<Long> rawTimestamps = new ArrayList<>();
        final List<DateTime> timestamps = new ArrayList<>();
        final List<Integer> offsets = new ArrayList<>();
        final List<DateTime> local_utc_ts = new ArrayList<>();
        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        final List<Long> motionRanges = new ArrayList<>();
        final List<Long> kickoffCounts = new ArrayList<>();
        final List<Long> onDurationSeconds = new ArrayList<>();

        int totalInserted = 0;
        int numIterations = 0;
        int numProcessed = 0;
        final int dataSize = trackerMotionData.size();
        LOGGER.debug("Dataset Size {}", dataSize);

        for (final TrackerMotion trackerMotion : trackerMotionData) {

            trackerMotions.add(trackerMotion);
            accountIDs.add(trackerMotion.accountId);
            trackerIDs.add(trackerMotion.trackerId);
            values.add(trackerMotion.value);
            rawTimestamps.add(trackerMotion.timestamp);
            offsets.add(trackerMotion.offsetMillis);
            motionRanges.add(trackerMotion.motionRange);
            kickoffCounts.add(trackerMotion.kickOffCounts);
            onDurationSeconds.add(trackerMotion.onDurationInSeconds);

            final DateTime ts = new DateTime(trackerMotion.timestamp, DateTimeZone.UTC);
            timestamps.add(ts);
            local_utc_ts.add(ts.plusMillis(trackerMotion.offsetMillis));

            numProcessed++;


            if (accountIDs.size() < batchSize && numProcessed != dataSize) {
                continue;
            }

            numIterations++;
            int inserted = 0;
            try {
                this.batchInsert(accountIDs, trackerIDs, values, timestamps, offsets, local_utc_ts, motionRanges, kickoffCounts, onDurationSeconds);
                inserted = accountIDs.size();
            } catch (UnableToExecuteStatementException exception) {
                LOGGER.warn("Batch insert fails, duplicate records!");
            }

            if (inserted != accountIDs.size()) {
                inserted = this.insertSingleTrackerMotion(trackerMotions); // batch insert fail, do individual inserts
            }

            LOGGER.debug("Round {}, inserted {} / {}", numIterations, inserted, accountIDs.size());

            totalInserted += inserted;

            accountIDs.clear();
            trackerIDs.clear();
            values.clear();
            rawTimestamps.clear();
            timestamps.clear();
            offsets.clear();
            local_utc_ts.clear();
            trackerMotions.clear();
            motionRanges.clear();
            kickoffCounts.clear();
            onDurationSeconds.clear();
        }

        LOGGER.debug("expected iteration: {}, actual: {}", Math.round(dataSize / (float) batchSize), numIterations);
        return totalInserted;
    }

    public int insertSingleTrackerMotion(final List<TrackerMotion> trackerMotions) {
        int inserted = 0;
        for (final TrackerMotion trackerMotion : trackerMotions) {
            try {
                LOGGER.debug("individual insert {}", trackerMotion);
                final Integer id = this.insertTrackerMotion(trackerMotion);

                if (id == null) {
                    LOGGER.warn("id is null");
                    continue;
                }
            } catch (UnableToExecuteStatementException exception) {
                Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                if (matcher.find()) {
                    LOGGER.debug("Dupe: Account {} Pill {} ts {}", trackerMotion.accountId, trackerMotion.trackerId, trackerMotion.timestamp);
                }
                LOGGER.error("Insert data for pill {}, account {}, ts {} failed, error {}",
                        trackerMotion.trackerId,
                        trackerMotion.accountId,
                        trackerMotion.timestamp,
                        exception.getMessage());
            }
            inserted++;
        }
        return inserted;
    }

    public Map<DateTime, Integer> getOffsetMillisForDates(final long accountId, final List<DateTime> dates) {

        Collections.sort(dates);

        final ImmutableList<TrackerMotion> trackerMotions = this.getTrackerOffsetMillis(accountId,
                dates.get(0).minusDays(1), dates.get(dates.size() - 1).plusDays(1));

        if (trackerMotions.size() == 0) {
            // no data for user in this date range
            return Collections.emptyMap();
        }

        final Map<DateTime, Integer> offsets = new HashMap<>();

        if (trackerMotions.size() == 1) {
            for (final DateTime date : dates) {
                offsets.put(date, trackerMotions.get(0).offsetMillis);
            }
            return offsets;
        }

        int offsetIndex = 0;
        for (final DateTime date: dates) {
            final long dateTimestamp = date.getMillis();
            for (int i = offsetIndex; i < trackerMotions.size(); i++) {
                final long compareStartDate = trackerMotions.get(offsetIndex).timestamp;
                long compareEndDate = new DateTime(DateTime.now(), DateTimeZone.UTC).getMillis();
                if (offsetIndex + 1 < trackerMotions.size()) {
                    compareEndDate = trackerMotions.get(offsetIndex + 1).timestamp;
                }

                if (dateTimestamp >= compareStartDate && dateTimestamp < compareEndDate) {
                    offsetIndex = i;
                    break;
                }
            }
            offsets.put(date, trackerMotions.get(offsetIndex).offsetMillis);
        }
        return offsets;
    }

    @Override
    public Class name() {
        return  TrackerMotionDAO.class;
    }

}
