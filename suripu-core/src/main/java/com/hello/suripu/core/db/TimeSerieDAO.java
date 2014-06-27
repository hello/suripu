package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;

import com.hello.suripu.core.db.mappers.DeviceDataMapper;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.SoundRecord;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.db.mappers.SoundRecordMapper;
import com.hello.suripu.core.db.mappers.TrackerMotionMapper;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;


public interface TimeSerieDAO {
    // FIXME: I think this should be get by account_id since we copy data to different accounts that bundle to the same device.
    @Deprecated
    @RegisterMapper(DeviceDataMapper.class)
    @SqlQuery("SELECT * FROM device_sensors_master WHERE device_id = :device_id AND ts > :start_ts AND ts < :end_ts")
    public ImmutableList<DeviceData> getHistoricalData(@Bind("device_id") Long deviceId, @Bind("start_ts") DateTime start, @Bind("end_ts") DateTime end);

    @RegisterMapper(DeviceDataMapper.class)
    @SqlQuery("SELECT * FROM device_sensors_master WHERE account_id = :account_id AND local_utc_ts >= :start_timestamp AND local_utc_ts <= :end_timestamp ORDER BY ts ASC")
    ImmutableList<DeviceData> getDeviceDataBetweenByLocalTime(
            @Bind("account_id") Long accountId,
            @Bind("start_timestamp") DateTime startTimestampLocalSetToUTC,
            @Bind("end_timestamp") DateTime endTimestampLocalSetToUTC);

    @RegisterMapper(DeviceDataMapper.class)
    @SqlQuery("SELECT * FROM device_sensors_master WHERE account_id = :account_id AND ts >= :start_timestamp AND ts <= :end_timestamp ORDER BY ts ASC")
    ImmutableList<DeviceData> getDeviceDataBetweenByUTCTime(
            @Bind("account_id") Long accountId,
            @Bind("start_timestamp") DateTime startTimestampLocalSetToUTC,
            @Bind("end_timestamp") DateTime endTimestampLocalSetToUTC);

    // TODO: normalize start/end datetime queries, eg always looking backwars or something like that
    @RegisterMapper(SoundRecordMapper.class)
    @SqlQuery("SELECT device_id, date_trunc('hour', ts) as ts_trunc, round(avg(amplitude)) as max_amplitude FROM device_sound WHERE device_id = :device_id AND ts > :start AND ts < :end GROUP BY ts_trunc, device_id ORDER BY ts_trunc asc;")
    public ImmutableList<SoundRecord> getAvgSoundData(@Bind("device_id") Long deviceId, @Bind("start") DateTime start, @Bind("end") DateTime end);


    // FIXME: Should query by account_id instead of device_id
    @RegisterMapper(SoundRecordMapper.class)
    @SqlQuery("SELECT device_id, date_trunc('minute', ts) AS ts_trunc, max(amplitude) AS max_amplitude " +
            "FROM device_sound " +
            "WHERE device_id = :device_id AND ts >= :start AND ts <= :end " +
            "GROUP BY ts_trunc, device_id " +
            "ORDER BY ts_trunc ASC;")
    public ImmutableList<SoundRecord> getSoundDataBetween(@Bind("device_id") Long deviceId,
                                                          @Bind("start") DateTime start,
                                                          @Bind("end") DateTime end);


    @RegisterMapper(TrackerMotionMapper.class)
    @SqlQuery("SELECT * FROM motion WHERE " +
            "account_id = :account_id AND ts >= :start_timestamp AND ts <= :end_timestamp " +
            "ORDER BY ts ASC;")
    public ImmutableList<TrackerMotion> getTrackerDataBetween(@Bind("account_id") Long accountId,
                                                   @Bind("start_timestamp") DateTime startTimestampUTC,
                                                   @Bind("end_timestamp") DateTime endTimestampUTC);

}
