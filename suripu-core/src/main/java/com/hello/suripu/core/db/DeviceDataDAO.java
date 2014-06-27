package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindDeviceData;
import com.hello.suripu.core.db.mappers.DeviceDataMapper;
import com.hello.suripu.core.models.DeviceData;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;


public interface DeviceDataDAO {

    @SqlUpdate("INSERT INTO device_sensors_master (account_id, device_id, ts, local_utc_ts, offset_millis, ambient_temp, ambient_light, " +
            "ambient_humidity, ambient_air_quality) VALUES(:account_id, :device_id, :ts, :local_utc_ts, :offset_millis, :ambient_temp, :ambient_light, :ambient_humidity, :ambient_air_quality)")
    void insert(@BindDeviceData final DeviceData deviceData);

    @RegisterMapper(DeviceDataMapper.class)
    @SqlQuery("SELECT * FROM device_sensors_master WHERE account_id = :account_id AND ts >= :start_timestamp AND ts <= :end_timestamp ORDER BY ts ASC")
    ImmutableList<DeviceData> getBetweenByUTCTime(
            @Bind("account_id") Long accountId,
            @Bind("start_timestamp") DateTime startTimestampUTC,
            @Bind("end_timestamp") DateTime endTimestampUTC);


    @RegisterMapper(DeviceDataMapper.class)
    @SqlQuery("SELECT * FROM device_sensors_master WHERE account_id = :account_id AND local_utc_ts >= :start_timestamp AND local_utc_ts <= :end_timestamp ORDER BY ts ASC")
    ImmutableList<DeviceData> getBetweenByLocalTime(
            @Bind("account_id") Long accountId,
            @Bind("start_timestamp") DateTime startTimestampLocalSetToUTC,
            @Bind("end_timestamp") DateTime endTimestampLocalSetToUTC);


    @SqlUpdate("INSERT INTO device_sound (device_id, amplitude, ts, offset_millis) VALUES(:device_id, :amplitude, :ts, :offset);")
    void insertSound(@Bind("device_id") Long deviceId, @Bind("amplitude") float amplitude, @Bind("ts") DateTime ts, @Bind("offset") int offset);

    @RegisterMapper(DeviceDataMapper.class)
    @SingleValueResult(DeviceData.class)
    @SqlQuery("SELECT * FROM device_sensors_master WHERE account_id = :account_id ORDER BY ts DESC LIMIT 1;")
    Optional<DeviceData> getMostRecent(@Bind("account_id") final Long accountId);
}
