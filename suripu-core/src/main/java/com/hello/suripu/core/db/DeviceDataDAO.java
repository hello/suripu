package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindDeviceData;
import com.hello.suripu.core.db.mappers.DeviceDataBucketMapper;
import com.hello.suripu.core.db.mappers.DeviceDataMapper;
import com.hello.suripu.core.db.util.Bucketing;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;


public abstract class DeviceDataDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceDataDAO.class);

    @SqlUpdate("INSERT INTO device_sensors_master (account_id, device_id, ts, local_utc_ts, offset_millis, " +
            "ambient_temp, ambient_light, ambient_light_variance, ambient_light_peakiness " +
            "ambient_humidity, ambient_air_quality) VALUES(:account_id, :device_id, :ts, :local_utc_ts, :offset_millis, " +
            ":ambient_temp, :ambient_light, :ambient_light_variance, :ambient_light_peakiness, :ambient_humidity, :ambient_air_quality)")
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
    @SqlQuery("SELECT " +
            "MAX(account_id) AS account_id," +
            "MAX(device_id) AS device_id," +
            "ROUND(AVG(ambient_temp)) as ambient_temp," +
            "ROUND(AVG(ambient_light)) as ambient_light," +
            "ROUND(AVG(ambient_light_variance)) as ambient_light_variance," +
            "ROUND(AVG(ambient_light_peakiness)) as ambient_light_peakiness," +
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


    /**
     * Generate time serie for given sensor. Return empty list if no data
     * @param clientUtcTimestamp
     * @param accountId
     * @param deviceId
     * @param slotDurationInMinutes
     * @param queryDurationInHours
     * @param sensor
     * @return
     */
    @Timed
    public List<Sample> generateTimeSerie(
            final Long clientUtcTimestamp,
            final Long accountId,
            final Long deviceId,
            final int slotDurationInMinutes,
            final int queryDurationInHours,
            final String sensor) {

        // queryEndTime is in UTC. If local now is 8:04pm in PDT, we create a utc timestamp in 8:04pm UTC
        final DateTime queryEndTime = new DateTime(clientUtcTimestamp, DateTimeZone.UTC);
        final DateTime queryStartTime = queryEndTime.minusHours(queryDurationInHours);

        LOGGER.debug("Client utcTimeStamp : {} ({})", clientUtcTimestamp, new DateTime(clientUtcTimestamp));
        LOGGER.debug("QueryEndTime: {} ({})", queryEndTime, queryEndTime.getMillis());
        LOGGER.debug("QueryStartTime: {} ({})", queryStartTime, queryStartTime.getMillis());

        final List<DeviceData> rows = getBetweenByLocalTimeAggregateBySlotDuration(accountId, deviceId, queryStartTime, queryEndTime, slotDurationInMinutes);
        LOGGER.debug("Retrieved {} rows from database", rows.size());

        if(rows.size() == 0) {
            return new ArrayList<>();
        }

        // create buckets with keys in UTC-Time
        final int currentOffsetMillis = rows.get(0).offsetMillis;
        final DateTime now = queryEndTime.withSecondOfMinute(0).withMillisOfSecond(0);
        final int remainder = now.getMinuteOfHour() % slotDurationInMinutes;
        final int minuteBucket = now.getMinuteOfHour() - remainder;
        // if 4:36 -> bucket = 4:35

        final DateTime nowRounded = now.minusMinutes(remainder);
        LOGGER.debug("Current Offset Milis = {}", currentOffsetMillis);
        LOGGER.debug("Remainder = {}", remainder);
        LOGGER.debug("Now (rounded) = {} ({})", nowRounded, nowRounded.getMillis());


//        int numberOfBuckets= (12 * 24) + 1;
        final int numberOfBuckets= (queryDurationInHours * 60 / slotDurationInMinutes) + 1;

        final Map<Long, Sample> map = Bucketing.generateEmptyMap(numberOfBuckets, nowRounded, slotDurationInMinutes);

        LOGGER.debug("Map size = {}", map.size());


        final Optional<Map<Long, Sample>> optionalPopulatedMap = Bucketing.populateMap(rows, sensor);

        if(!optionalPopulatedMap.isPresent()) {
            return Collections.EMPTY_LIST;
        }

        // Override map with values from DB
        final Map<Long, Sample> merged = Bucketing.mergeResults(map, optionalPopulatedMap.get());

        LOGGER.debug("New map size = {}", merged.size());

        final List<Sample> sortedList = Bucketing.sortResults(merged, currentOffsetMillis);
        return sortedList;
    }


    @RegisterMapper(DeviceDataMapper.class)
    @SingleValueResult(DeviceData.class)
    @SqlQuery("SELECT " +
            "MAX(account_id) as account_id, " +
            "MAX(device_id) as device_id, " +
            "AVG(ambient_temp) as ambient_temp, " +
            "AVG(ambient_humidity) as ambient_humidity, " +
            "AVG(ambient_air_quality) as ambient_air_quality, " +
            "AVG(ambient_light) as ambient_light," +
            "AVG(ambient_light_variance) as ambient_light_variance," +
            "AVG(ambient_light_peakiness) as ambient_light_peakiness," +
            "MIN(ts) as ts, " +
            "MIN(offset_millis) as offset_millis " +
            "FROM device_sensors_master " +
            "WHERE account_id = :account_id " +
            "AND local_utc_ts >= :start_ts AND local_utc_ts < :end_ts;")
    public abstract Optional<DeviceData> getAverageForNight(@Bind("account_id") final Long accountId,
                                                            @Bind("start_ts") final DateTime targetDate,
                                                            @Bind("end_ts") final DateTime endDate);
}
