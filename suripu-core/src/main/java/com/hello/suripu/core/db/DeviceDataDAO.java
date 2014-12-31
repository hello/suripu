package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.binders.BindDeviceData;
import com.hello.suripu.core.db.mappers.DeviceDataBucketMapper;
import com.hello.suripu.core.db.mappers.DeviceDataMapper;
import com.hello.suripu.core.db.util.Bucketing;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;


public abstract class DeviceDataDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceDataDAO.class);

    // TODO: Add wave_count, hold_count into table.
    @SqlUpdate("INSERT INTO device_sensors_master (account_id, device_id, ts, local_utc_ts, offset_millis, " +
            "ambient_temp, ambient_light, ambient_light_variance, ambient_light_peakiness, ambient_humidity, " +
            "ambient_air_quality, ambient_air_quality_raw, ambient_dust_variance, ambient_dust_min, ambient_dust_max, " +
            "firmware_version, wave_count, hold_count) VALUES " +
            "(:account_id, :device_id, :ts, :local_utc_ts, :offset_millis, " +
            ":ambient_temp, :ambient_light, :ambient_light_variance, :ambient_light_peakiness, :ambient_humidity, " +
            ":ambient_air_quality, :ambient_air_quality_raw, :ambient_dust_variance, :ambient_dust_min, :ambient_dust_max, " +
            ":firmware_version, :wave_count, :hold_count)")
    public abstract void insert(@BindDeviceData final DeviceData deviceData);

    @SqlBatch("INSERT INTO device_sensors_master (account_id, device_id, ts, local_utc_ts, offset_millis, " +
            "ambient_temp, ambient_light, ambient_light_variance, ambient_light_peakiness, ambient_humidity, " +
            "ambient_air_quality, ambient_air_quality_raw, ambient_dust_variance, ambient_dust_min, ambient_dust_max, " +
            "firmware_version, wave_count, hold_count) VALUES " +
            "(:account_id, :device_id, :ts, :local_utc_ts, :offset_millis, " +
            ":ambient_temp, :ambient_light, :ambient_light_variance, :ambient_light_peakiness, :ambient_humidity, " +
            ":ambient_air_quality, :ambient_air_quality_raw, :ambient_dust_variance, :ambient_dust_min, :ambient_dust_max, " +
            ":firmware_version, :wave_count, :hold_count);")
    public abstract void batchInsert(@BindDeviceData Iterator<DeviceData> deviceDataList);

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
            "ROUND(MIN(ambient_temp)) as ambient_temp," +
            "ROUND(AVG(ambient_light)) as ambient_light," +
            "ROUND(AVG(ambient_light_variance)) as ambient_light_variance," +
            "ROUND(AVG(ambient_light_peakiness)) as ambient_light_peakiness," +
            "ROUND(AVG(ambient_humidity)) as ambient_humidity," +
            "ROUND(AVG(ambient_air_quality)) as ambient_air_quality," +
            "ROUND(AVG(ambient_air_quality_raw)) as ambient_air_quality_raw," +
            "ROUND(AVG(ambient_dust_variance)) as ambient_dust_variance," +
            "ROUND(AVG(ambient_dust_min)) as ambient_dust_min," +
            "ROUND(MAX(ambient_dust_max)) as ambient_dust_max," +
            "ROUND(MIN(offset_millis)) as offset_millis," +
            "ROUND(MAX(firmware_version)) as firmware_version," +
            "ROUND(MAX(wave_count)) as wave_count," +
            "ROUND(MAX(hold_count)) as hold_count," +
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


    @RegisterMapper(DeviceDataBucketMapper.class)
    @SqlQuery("SELECT " +
            "MAX(account_id) AS account_id," +
            "MAX(device_id) AS device_id," +
            "ROUND(MIN(ambient_temp)) as ambient_temp," +
            "ROUND(AVG(ambient_light)) as ambient_light," +
            "ROUND(AVG(ambient_light_variance)) as ambient_light_variance," +
            "ROUND(AVG(ambient_light_peakiness)) as ambient_light_peakiness," +
            "ROUND(AVG(ambient_humidity)) as ambient_humidity," +
            "ROUND(AVG(ambient_air_quality)) as ambient_air_quality," +
            "ROUND(AVG(ambient_air_quality_raw)) as ambient_air_quality_raw," +
            "ROUND(AVG(ambient_dust_variance)) as ambient_dust_variance," +
            "ROUND(AVG(ambient_dust_min)) as ambient_dust_min," +
            "ROUND(MAX(ambient_dust_max)) as ambient_dust_max," +
            "ROUND(MIN(offset_millis)) as offset_millis," +
            "ROUND(MAX(firmware_version)) as firmware_version," +
            "ROUND(MAX(wave_count)) as wave_count," +
            "ROUND(MAX(hold_count)) as hold_count," +
            "date_trunc('hour', ts) + (CAST(date_part('minute', ts) AS integer) / :slot_duration) * :slot_duration * interval '1 min' AS ts_bucket " +
            "FROM device_sensors_master " +
            "WHERE account_id = :account_id AND device_id = :device_id " +
            "AND ts >= :start_ts AND ts < :end_ts " +
            "GROUP BY ts_bucket " +
            "ORDER BY ts_bucket ASC")
    public abstract ImmutableList<DeviceData> getBetweenByAbsoluteTimeAggregateBySlotDuration(
            @Bind("account_id") Long accountId,
            @Bind("device_id") Long deviceId,
            @Bind("start_ts") DateTime start,
            @Bind("end_ts") DateTime end,
            @Bind("slot_duration") Integer slotDuration);

    @SqlUpdate("INSERT INTO device_sound (device_id, amplitude, ts, offset_millis) VALUES(:device_id, :amplitude, :ts, :offset);")
    public abstract void insertSound(@Bind("device_id") Long deviceId, @Bind("amplitude") float amplitude, @Bind("ts") DateTime ts, @Bind("offset") int offset);

    @RegisterMapper(DeviceDataMapper.class)
    @SingleValueResult(DeviceData.class)
    @SqlQuery("SELECT * FROM device_sensors_master WHERE account_id = :account_id AND device_id = :device_id AND ts < :utc_ts_limit ORDER BY ts DESC LIMIT 1;")
    public abstract Optional<DeviceData> getMostRecent(@Bind("account_id") final Long accountId, @Bind("device_id") Long deviceId, @Bind("utc_ts_limit") final DateTime tsLimit);



    public int batchInsertWithFailureFallback(final List<DeviceData> data){
        int inserted = 0;
        try {
            this.batchInsert(data.iterator());
            return data.size();
        } catch (UnableToExecuteStatementException exception) {
            LOGGER.error("Failed to insert batch data for sense: {}", exception.getMessage());
            LOGGER.error("Failed to insert batch data for sense internal id= {}", data.get(0).deviceId);
        }

        for(final DeviceData datum:data){
            try {
                this.insert(datum);
                inserted++;
            } catch (UnableToExecuteStatementException exception) {
                final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());
                if(matcher.find())
                {
                    LOGGER.warn("Duplicate device sensor value for device {}, account {}, timestamp {}",
                            datum.deviceId,
                            datum.accountId,
                            datum.dateTimeUTC.withZone(DateTimeZone.forOffsetMillis(datum.offsetMillis)));
                }else{
                    LOGGER.error("Cannot insert data for device {}, account {}, timestamp {}, error {}",
                            datum.deviceId,
                            datum.accountId,
                            datum.dateTimeUTC.withZone(DateTimeZone.forOffsetMillis(datum.offsetMillis)),
                            exception.getMessage());
                }

            }
        }



        return inserted;
    }

    /**
     * Generate time serie for given sensor. Return empty list if no data
     * @param queryStartTimestampInLocalUTC
     * @param queryEndTimestampInLocalUTC
     * @param accountId
     * @param deviceId
     * @param slotDurationInMinutes
     * @param sensor
     * @return
     */
    @Timed
    public List<Sample> generateTimeSeriesByLocalTime(
            final Long queryStartTimestampInLocalUTC,
            final Long queryEndTimestampInLocalUTC,
            final Long accountId,
            final Long deviceId,
            final int slotDurationInMinutes,
            final String sensor) {

        // queryEndTime is in UTC. If local now is 8:04pm in PDT, we create a utc timestamp in 8:04pm UTC
        final DateTime queryEndTime = new DateTime(queryEndTimestampInLocalUTC, DateTimeZone.UTC);
        final DateTime queryStartTime = new DateTime(queryStartTimestampInLocalUTC, DateTimeZone.UTC);

        LOGGER.debug("Client utcTimeStamp : {} ({})", queryEndTimestampInLocalUTC, new DateTime(queryEndTimestampInLocalUTC));
        LOGGER.debug("QueryEndTime: {} ({})", queryEndTime, queryEndTime.getMillis());
        LOGGER.debug("QueryStartTime: {} ({})", queryStartTime, queryStartTime.getMillis());

        final List<DeviceData> rows = getBetweenByLocalTimeAggregateBySlotDuration(accountId, deviceId, queryStartTime, queryEndTime, slotDurationInMinutes);
        LOGGER.debug("Retrieved {} rows from database", rows.size());

        if(rows.size() == 0) {
            return new ArrayList<>();
        }

        // create buckets with keys in UTC-Time
        final int endOffsetMillis = rows.get(rows.size() - 1).offsetMillis;
        final int startOffsetMillis = rows.get(0).offsetMillis;


        // final int numberOfBuckets= (queryDurationInHours * 60 / slotDurationInMinutes) + 1;   // This is wrong, duration in hours must come from actual data

        // We cannot estimate time duration by from local time, because time zone can change between
        // local start time and local end time. The only way to get interval is compute from absolute time.


        final DateTime nowLocal = new DateTime(queryEndTime.getYear(),
                queryEndTime.getMonthOfYear(),
                queryEndTime.getDayOfMonth(),
                queryEndTime.getHourOfDay(),
                queryEndTime.getMinuteOfHour(),
                DateTimeZone.forOffsetMillis(endOffsetMillis));

        final DateTime startLocal = new DateTime(queryStartTime.getYear(),
                queryStartTime.getMonthOfYear(),
                queryStartTime.getDayOfMonth(),
                queryStartTime.getHourOfDay(),
                queryStartTime.getMinuteOfHour(),
                DateTimeZone.forOffsetMillis(startOffsetMillis));

        final long absoluteIntervalMS = nowLocal.getMillis() - startLocal.getMillis();
        final DateTime now = new DateTime(nowLocal.getMillis(), DateTimeZone.UTC);

        final int remainder = now.getMinuteOfHour() % slotDurationInMinutes;
        final int minuteBucket = now.getMinuteOfHour() - remainder;
        // if 4:36 -> bucket = 4:35

        final DateTime nowRounded = now.minusMinutes(remainder);
        LOGGER.debug("Current Offset Milis = {}", startOffsetMillis);
        LOGGER.debug("Remainder = {}", remainder);
        LOGGER.debug("Now (rounded) = {} ({})", nowRounded, nowRounded.getMillis());

        final int numberOfBuckets= (int) ((absoluteIntervalMS / DateTimeConstants.MILLIS_PER_MINUTE) / slotDurationInMinutes + 1);

        final Map<Long, Sample> map = Bucketing.generateEmptyMap(numberOfBuckets, nowRounded, slotDurationInMinutes);

        LOGGER.debug("Map size = {}", map.size());


        final Optional<Map<Long, Sample>> optionalPopulatedMap = Bucketing.populateMap(rows, sensor);

        if(!optionalPopulatedMap.isPresent()) {
            return Collections.EMPTY_LIST;
        }

        // Override map with values from DB
        final Map<Long, Sample> merged = Bucketing.mergeResults(map, optionalPopulatedMap.get());

        LOGGER.debug("New map size = {}", merged.size());

        final List<Sample> sortedList = Bucketing.sortResults(merged, startOffsetMillis);
        return sortedList;
    }


    @Timed
    public List<Sample> generateTimeSeriesByUTCTime(
            final Long queryStartTimestampInUTC,
            final Long queryEndTimestampInUTC,
            final Long accountId,
            final Long deviceId,
            final int slotDurationInMinutes,
            final String sensor) {

        // queryEndTime is in UTC. If local now is 8:04pm in PDT, we create a utc timestamp in 8:04pm UTC
        final DateTime queryEndTime = new DateTime(queryEndTimestampInUTC, DateTimeZone.UTC);
        final DateTime queryStartTime = new DateTime(queryStartTimestampInUTC, DateTimeZone.UTC);

        LOGGER.debug("Client utcTimeStamp : {} ({})", queryEndTimestampInUTC, new DateTime(queryEndTimestampInUTC));
        LOGGER.debug("QueryEndTime: {} ({})", queryEndTime, queryEndTime.getMillis());
        LOGGER.debug("QueryStartTime: {} ({})", queryStartTime, queryStartTime.getMillis());

        final List<DeviceData> rows = getBetweenByAbsoluteTimeAggregateBySlotDuration(accountId, deviceId, queryStartTime, queryEndTime, slotDurationInMinutes);
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


        final long absoluteIntervalMS = queryEndTimestampInUTC - queryStartTimestampInUTC;
        final int numberOfBuckets= (int) ((absoluteIntervalMS / DateTimeConstants.MILLIS_PER_MINUTE) / slotDurationInMinutes + 1);

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
            "AVG(ambient_air_quality_raw) as ambient_air_quality_raw, " +
            "AVG(ambient_dust_variance) as ambient_dust_variance, " +
            "AVG(ambient_dust_min) as ambient_dust_min, " +
            "AVG(ambient_dust_max) as ambient_dust_max, " +
            "AVG(ambient_light) as ambient_light," +
            "AVG(ambient_light_variance) as ambient_light_variance," +
            "AVG(ambient_light_peakiness) as ambient_light_peakiness," +
            "MIN(ts) as ts, " +
            "MIN(offset_millis) as offset_millis " +
            "ROUND(MAX(firmware_version)) as firmware_version," +
            "ROUND(MAX(wave_count)) as wave_count," +
            "ROUND(MAX(hold_count)) as hold_count," +
            "FROM device_sensors_master " +
            "WHERE account_id = :account_id " +
            "AND local_utc_ts >= :start_ts AND local_utc_ts < :end_ts;")
    public abstract Optional<DeviceData> getAverageForNight(@Bind("account_id") final Long accountId,
                                                            @Bind("start_ts") final DateTime targetDate,
                                                            @Bind("end_ts") final DateTime endDate);
}
