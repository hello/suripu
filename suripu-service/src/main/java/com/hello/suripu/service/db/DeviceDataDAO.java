package com.hello.suripu.service.db;

import com.hello.suripu.core.db.binders.BindDeviceBatch;
import com.hello.suripu.core.db.binders.DeviceBatch;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface DeviceDataDAO {

    @SqlUpdate("INSERT INTO device_sensors_batch (account_id, ambient_temp, ambient_light, ambient_humidity, ambient_air_quality, ts, offset_millis) " +
            "VALUES(:account_id, :ambient_temp, :ambient_light, :ambient_humidity, :ambient_air_quality, :ts, :offset_millis);")
    void insertBatch(@BindDeviceBatch final DeviceBatch deviceBatch);

    @SqlUpdate("INSERT INTO device_sensors (device_id, account_id, ts, offset_millis, ambient_temp, ambient_light, " +
            "ambient_humidity, ambient_air_quality) VALUES(:device_id, :account_id, :ts, :offset_millis, :ambient_temp, :ambient_light, :ambient_humidity, :ambient_air_quality)")
    void insert(
            @Bind("device_id") Long deviceId,
            @Bind("account_id") Long accountId,
            @Bind("ts") DateTime ts,
            @Bind("offset_millis") int offsetMillis,
            @Bind("ambient_temp") float temp,
            @Bind("ambient_light") float light,
            @Bind("ambient_humidity") float humidity,
            @Bind("ambient_air_quality") float air_quality);


    @SqlUpdate("INSERT INTO device_sound (device_id, amplitude, ts, offset_millis) VALUES(:device_id, :amplitude, :ts, :offset);")
    void insertSound(@Bind("device_id") Long deviceId, @Bind("amplitude") float amplitude, @Bind("ts") DateTime ts, @Bind("offset") int offset);


}
