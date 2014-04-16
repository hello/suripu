package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.Record;
import com.hello.suripu.core.SoundRecord;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(RecordMapper.class)
public interface TimeSerieDAO {

    @SqlQuery("SELECT * FROM device_sensors WHERE device_id = :device_id AND ts > :start_ts AND ts < :end_ts")
    public ImmutableList<Record> getHistoricalData(@Bind("device_id") Long deviceId, @Bind("start_ts") DateTime start, @Bind("end_ts") DateTime end);


    // TODO: normalize start/end datetime queries, eg always looking backwars or something like that
    @RegisterMapper(SoundRecordMapper.class)
    @SqlQuery("SELECT device_id, date_trunc('hour', ts) as ts_trunc, avg(amplitude) as avg_max_amplitude FROM device_sound WHERE device_id = :device_id AND ts > :start AND ts < :end GROUP BY ts_trunc, device_id ORDER BY ts_trunc asc;")
    public ImmutableList<SoundRecord> getAvgSoundData(@Bind("device_id") Long deviceId, @Bind("start") DateTime start, @Bind("end") DateTime end);
}
