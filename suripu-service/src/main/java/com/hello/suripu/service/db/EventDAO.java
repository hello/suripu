package com.hello.suripu.service.db;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;

import java.sql.Timestamp;
import java.util.List;

public interface EventDAO {

    @SqlBatch("INSERT INTO sensor_samples (sensor_id, device_id, ts, val) VALUES(:sensor_id, :device_id, :ts, :val);")
    void insertBatch(@Bind("sensor_id") List<Integer> sensorIds, @Bind("device_id") Long deviceId, @Bind("ts") List<DateTime> timestamps, @Bind("val") List<Integer> value);
}
