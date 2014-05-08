package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.TrackerMotionSample;
import com.hello.suripu.core.Record;
import com.hello.suripu.core.SoundRecord;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;


public interface TimeSerieDAO {

    @RegisterMapper(RecordMapper.class)
    @SqlQuery("SELECT * FROM device_sensors WHERE device_id = :device_id AND ts > :start_ts AND ts < :end_ts")
    public ImmutableList<Record> getHistoricalData(@Bind("device_id") Long deviceId, @Bind("start_ts") DateTime start, @Bind("end_ts") DateTime end);


    // TODO: normalize start/end datetime queries, eg always looking backwars or something like that
    @RegisterMapper(SoundRecordMapper.class)
    @SqlQuery("SELECT device_id, date_trunc('hour', ts) as ts_trunc, avg(amplitude) as avg_max_amplitude FROM device_sound WHERE device_id = :device_id AND ts > :start AND ts < :end GROUP BY ts_trunc, device_id ORDER BY ts_trunc asc;")
    public ImmutableList<SoundRecord> getAvgSoundData(@Bind("device_id") Long deviceId, @Bind("start") DateTime start, @Bind("end") DateTime end);

    @RegisterMapper(TrackerMotionMapper.class)
    @SqlQuery("SELECT * " +
            "FROM motion " +
            // TODO: Test changing trackerId for the same account
            "WHERE account_id = :account_id AND ts > :ts AND offset_millis = :offset_millis " +  // We ignore the tracker id when retrieving data for processing
            "ORDER BY ts ASC;"
    )
    public ImmutableList<TrackerMotionSample> getAllTrackerMotionAfter(@Bind("account_id") long accountId,
                                                   @Bind("ts") DateTime timestampUTC,
                                                   @Bind("offset_millis") int timezoneOffset);
}
