package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindBatchSensorData;
import com.hello.suripu.core.db.mappers.BatchSensorDataMapper;
import com.hello.suripu.core.models.BatchSensorData;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;


@RegisterMapper(BatchSensorDataMapper.class)
public interface DeviceDataDAO {

    @SqlUpdate("INSERT INTO device_sensors_batch (account_id, ambient_temp, ambient_light, ambient_humidity, ambient_air_quality, ts, offset_millis) " +
            "VALUES(:account_id, :ambient_temp, :ambient_light, :ambient_humidity, :ambient_air_quality, :ts, :offset_millis);")
    void insertBatch(@BindBatchSensorData final BatchSensorData batchSensorData);

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



    @SqlQuery("SELECT * FROM device_sensors_batch WHERE account_id = :account_id AND ts >= :start_timestamp AND ts <= :end_timestamp ORDER BY id ASC")
    ImmutableList<BatchSensorData> getBatchSensorDataBetween(
            @Bind("account_id") Long accountId,
            @Bind("start_timestamp") DateTime startTimestampUTC,
            @Bind("end_timestamp") DateTime endTimestampUTC);


    @SingleValueResult(BatchSensorData.class)
    @SqlQuery("SELECT * FROM device_sensors_batch WHERE account_id = :account_id ORDER BY id DESC LIMIT 1;")
    Optional<BatchSensorData> getMostRecent(@Bind("account_id") final Long accountId);
}
