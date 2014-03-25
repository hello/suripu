package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.Record;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(RecordMapper.class)
public interface TimeSerieDAO {

    @SqlQuery("SELECT * FROM device_sensors WHERE device_id = :device_id AND ts > :start_ts AND ts < :end_ts")
    public ImmutableList<Record> getHistoricalData(@Bind("device_id") Long deviceId, @Bind("start_ts") DateTime start, @Bind("end_ts") DateTime end);
}
