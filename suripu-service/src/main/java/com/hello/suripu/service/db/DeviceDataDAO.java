package com.hello.suripu.service.db;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import java.util.List;

public interface DeviceDataDAO {

    @SqlBatch("INSERT INTO sensor_samples (sensor_id, device_id, ts, val) VALUES(:sensor_id, :device_id, :ts, :val);")
    void insertBatch(
            @Bind("sensor_id") List<Integer> sensorIds,
            @Bind("device_id") Long deviceId,
            @Bind("ts") List<DateTime> timestamps,
            @Bind("val") List<Integer> value);

    @SqlUpdate("INSERT INTO device_sensors (device_id, ts, offset_millis, ambient_temp, ambient_light, " +
            "ambient_humidity, ambient_air_quality) VALUES(:device_id, :ts, :offset_millis, :ambient_temp, :ambient_light, :ambient_humidity, :ambient_air_quality)")
    void insert(
            @Bind("device_id") Long deviceId,
            @Bind("ts") DateTime ts,
            @Bind("offset_millis") int offsetMillis,
            @Bind("ambient_temp") float temp,
            @Bind("ambient_light") float light,
            @Bind("ambient_humidity") float humidity,
            @Bind("ambient_air_quality") float air_quality);


    @SqlUpdate("INSERT INTO device_sound (device_id, amplitude, ts, offset_millis) VALUES(:device_id, :amplitude, :ts, :offset);")
    void insertSound(@Bind("device_id") Long deviceId, @Bind("amplitude") float amplitude, @Bind("ts") DateTime ts, @Bind("offset") int offset);


}
