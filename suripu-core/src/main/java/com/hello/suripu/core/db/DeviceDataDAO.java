package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.binders.BindDeviceData;
import com.hello.suripu.core.db.mappers.DeviceDataBucketMapper;
import com.hello.suripu.core.db.mappers.DeviceDataMapper;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class DeviceDataDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceDataDAO.class);

    @SqlUpdate("INSERT INTO device_sensors_master (account_id, device_id, ts, local_utc_ts, offset_millis, ambient_temp, ambient_light, " +
            "ambient_humidity, ambient_air_quality) VALUES(:account_id, :device_id, :ts, :local_utc_ts, :offset_millis, :ambient_temp, :ambient_light, :ambient_humidity, :ambient_air_quality)")
    public abstract void insert(@BindDeviceData final DeviceData deviceData);

    @RegisterMapper(DeviceDataMapper.class)
    @SqlQuery("SELECT * FROM device_sensors_master WHERE account_id = :account_id AND ts >= :start_timestamp AND ts <= :end_timestamp ORDER BY ts ASC")
    public abstract ImmutableList<DeviceData> getBetweenByUTCTime(
            @Bind("account_id") Long accountId,
            @Bind("start_timestamp") DateTime startTimestampUTC,
            @Bind("end_timestamp") DateTime endTimestampUTC);


    @RegisterMapper(DeviceDataMapper.class)
    @SqlQuery("SELECT * FROM device_sensors_master WHERE account_id = :account_id AND local_utc_ts >= :start_timestamp AND local_utc_ts <= :end_timestamp ORDER BY ts ASC")
    public abstract ImmutableList<DeviceData> getBetweenByLocalTime(
            @Bind("account_id") Long accountId,
            @Bind("start_timestamp") DateTime startTimestampLocalSetToUTC,
            @Bind("end_timestamp") DateTime endTimestampLocalSetToUTC);



    @RegisterMapper(DeviceDataBucketMapper.class)
    @SqlQuery("SELECT\n" +
            "MAX(account_id) AS account_id," +
            "MAX(device_id) AS device_id," +
            "ROUND(AVG(ambient_temp)) as ambient_temp," +
            "ROUND(AVG(ambient_light)) as ambient_light," +
            "ROUND(AVG(ambient_humidity)) as ambient_humidity," +
            "ROUND(AVG(ambient_air_quality)) as ambient_air_quality," +
            "ROUND(MIN(offset_millis)) as offset_millis," +
            "date_trunc('hour', ts) + (CAST(date_part('minute', ts) AS integer) / :slot_duration) * :slot_duration * interval '1 min' AS ts_bucket " +
            "FROM device_sensors_master " +
            "WHERE account_id = :account_id AND device_id = :device_id " +
            "AND local_utc_ts >= :start_ts AND local_utc_ts < :end_ts " +
            "GROUP BY ts_bucket " +
            "ORDER BY ts_bucket ASC")
    public abstract ImmutableList<DeviceData> getBetweenByLocalTimeAggregateBySlotDuration(
            @Bind("account_id") Long accountId,
            @Bind("device_id") Long deviceId,
            @Bind("start_ts") DateTime start,
            @Bind("end_ts") DateTime end,
            @Bind("slot_duration") Integer slotDuration);

    @SqlUpdate("INSERT INTO device_sound (device_id, amplitude, ts, offset_millis) VALUES(:device_id, :amplitude, :ts, :offset);")
    public abstract void insertSound(@Bind("device_id") Long deviceId, @Bind("amplitude") float amplitude, @Bind("ts") DateTime ts, @Bind("offset") int offset);

    @RegisterMapper(DeviceDataMapper.class)
    @SingleValueResult(DeviceData.class)
    @SqlQuery("SELECT * FROM device_sensors_master WHERE account_id = :account_id ORDER BY ts DESC LIMIT 1;")
    public abstract Optional<DeviceData> getMostRecent(@Bind("account_id") final Long accountId);


    @Timed
    public List<Sample> generateTimeSerie(final Long clientUtcTimestamp, final Long accountId, final Long deviceId, final int slotDurationInMinutes, final int queryDurationInHours, final String sensor) {

        // queryEndTime is in UTC. If local now is 8:04pm in PDT, we create a utc timestamp in 8:04pm UTC
        final DateTime queryEndTime = new DateTime(clientUtcTimestamp, DateTimeZone.UTC);
        final DateTime queryStartTime = queryEndTime.minusHours(queryDurationInHours);

        final List<DeviceData> rows = getBetweenByLocalTimeAggregateBySlotDuration(accountId, deviceId, queryStartTime, queryEndTime, slotDurationInMinutes);
        LOGGER.debug("Retrieved {} rows from database", rows.size());

        if(rows.size() == 0) {
            return new ArrayList<>();
        }

        // create buckets with keys in UTC-Time
        final int currentOffsetMillis = rows.get(0).offsetMillis;
        final DateTime now = queryEndTime.withSecondOfMinute(0).withMillisOfSecond(0).minusMillis(currentOffsetMillis);
        final int remainder = now.getMinuteOfHour() % slotDurationInMinutes;
        final int minuteBucket = now.getMinuteOfHour() - remainder;
        // if 4:36 -> bucket = 4:35

        final DateTime nowRounded = now.minusMinutes(remainder);
        LOGGER.debug("Now (rounded) = {}", nowRounded);



//        int numberOfBuckets= (12 * 24) + 1;
        final int numberOfBuckets= (queryDurationInHours * 60 / slotDurationInMinutes) + 1;

        final Map<Long, Sample> map = new HashMap<>();
        for(int i = 0; i < numberOfBuckets; i++) {
            LOGGER.trace("Inserting {}", nowRounded.minusMinutes(i * slotDurationInMinutes));
            // TODO: try to remove the null value. Use optional maybe?
            map.put(nowRounded.minusMinutes(i * slotDurationInMinutes).getMillis(), new Sample(nowRounded.minusMinutes(i * slotDurationInMinutes).getMillis(), 0, null));
        }

        LOGGER.debug("Map size = {}", map.size());

        for(final DeviceData deviceData: rows) {
            if(!map.containsKey(deviceData.dateTimeUTC.getMillis())) {
                LOGGER.debug("NOT IN MAP: {}", deviceData.dateTimeUTC);
                continue;
            }

            // TODO: refactor this

            int sensorValue = 0;
            if(sensor.equals("humidity")) {
                sensorValue = deviceData.ambientHumidity;
            } else if(sensor.equals("temperature")) {
                sensorValue = deviceData.ambientTemperature;
            } else if (sensor.equals("particulates")) {
                sensorValue = deviceData.ambientAirQuality;
            } else if (sensor.equals("light")) {
                sensorValue = deviceData.ambientLight;
            } else {
                LOGGER.warn("Sensor {} is not supported for account_id: {}. Returning early", sensor, accountId);
                return new ArrayList<>();
            }

            map.put(deviceData.dateTimeUTC.getMillis(), new Sample(deviceData.dateTimeUTC.getMillis(), sensorValue, deviceData.offsetMillis));
        }

        LOGGER.debug("New map size = {}", map.size());

        final Sample[] samples = map.values().toArray(new Sample[0]);

        Arrays.sort(samples, new Comparator<Sample>() {
            @Override
            public int compare(Sample o1, Sample o2) {
                return Long.compare(o1.dateTime, o2.dateTime);
            }
        });

        int lastOffsetMillis = -1;
        for(final Sample sample : samples) {
            if(sample.offsetMillis == null) {
                if(lastOffsetMillis == -1) {
                    sample.offsetMillis = currentOffsetMillis;
                } else {
                    sample.offsetMillis = lastOffsetMillis;
                }
            }

            lastOffsetMillis = sample.offsetMillis;
        }
        return Lists.newArrayList(samples);
    }
}
