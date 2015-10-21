package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.binders.BindDeviceData;
import com.hello.suripu.core.db.mappers.DeviceDataBucketMapper;
import com.hello.suripu.core.db.mappers.DeviceDataMapper;
import com.hello.suripu.core.db.mappers.SenseDeviceStatusMapper;
import com.hello.suripu.core.db.util.Bucketing;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.AllSensorSampleMap;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
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


public abstract class DeviceDataDAO implements DeviceDataIngestDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceDataDAO.class);

    // Amount of records to attempt to insert at once.
    // Higher numbers reduce database queries but increase likelihood of failure due to uniqueness constraint failure.
    private static final int MAX_BATCH_INSERT_SIZE = 30;

    private static final String AGGREGATE_SELECT_STRING_GROUPBY_TSBUCKET = "SELECT " +  // for queries using DeviceDataBucketMapper
            "MAX(account_id) AS account_id," +
            "MAX(device_id) AS device_id," +
            "ROUND(MIN(ambient_temp)) AS ambient_temp," +
            "ROUND(AVG(ambient_light)) AS ambient_light," +
            "ROUND(AVG(ambient_light_variance)) AS ambient_light_variance," +
            "ROUND(AVG(ambient_light_peakiness)) AS ambient_light_peakiness," +
            "ROUND(AVG(ambient_humidity)) AS ambient_humidity," +
            "ROUND(AVG(ambient_air_quality)) AS ambient_air_quality," +
            "ROUND(AVG(ambient_air_quality_raw)) AS ambient_air_quality_raw," +
            "ROUND(AVG(ambient_dust_variance)) AS ambient_dust_variance," +
            "ROUND(AVG(ambient_dust_min)) AS ambient_dust_min," +
            "ROUND(MAX(ambient_dust_max)) AS ambient_dust_max," +
            "ROUND(MIN(offset_millis)) AS offset_millis," +
            "ROUND(MAX(firmware_version)) AS firmware_version," +
            "ROUND(SUM(wave_count)) AS wave_count," +
            "ROUND(SUM(hold_count)) AS hold_count," +
            "ROUND(MAX(audio_num_disturbances)) AS audio_num_disturbances," +
            "ROUND(MAX(audio_peak_disturbances_db)) AS audio_peak_disturbances_db," +
            "ROUND(MAX(audio_peak_background_db)) AS audio_peak_background_db," +
            "date_trunc('hour', ts) + (CAST(date_part('minute', ts) AS integer) / :slot_duration) * :slot_duration * interval '1 min' AS ts_bucket ";

    // TODO: Add wave_count, hold_count into table.
    @SqlUpdate("INSERT INTO device_sensors_master (account_id, device_id, ts, local_utc_ts, offset_millis, " +
            "ambient_temp, ambient_light, ambient_light_variance, ambient_light_peakiness, ambient_humidity, " +
            "ambient_air_quality, ambient_air_quality_raw, ambient_dust_variance, ambient_dust_min, ambient_dust_max, " +
            "firmware_version, wave_count, hold_count, " +
            "audio_num_disturbances, audio_peak_disturbances_db, audio_peak_background_db) VALUES " +
            "(:account_id, :device_id, :ts, :local_utc_ts, :offset_millis, " +
            ":ambient_temp, :ambient_light, :ambient_light_variance, :ambient_light_peakiness, :ambient_humidity, " +
            ":ambient_air_quality, :ambient_air_quality_raw, :ambient_dust_variance, :ambient_dust_min, :ambient_dust_max, " +
            ":firmware_version, :wave_count, :hold_count, " +
            ":audio_num_disturbances, :audio_peak_disturbances_db, :audio_peak_background_db)")
    public abstract void insert(@BindDeviceData final DeviceData deviceData);

    @SqlBatch("INSERT INTO device_sensors_master (account_id, device_id, ts, local_utc_ts, offset_millis, " +
            "ambient_temp, ambient_light, ambient_light_variance, ambient_light_peakiness, ambient_humidity, " +
            "ambient_air_quality, ambient_air_quality_raw, ambient_dust_variance, ambient_dust_min, ambient_dust_max, " +
            "firmware_version, wave_count, hold_count, " +
            "audio_num_disturbances, audio_peak_disturbances_db, audio_peak_background_db) VALUES " +
            "(:account_id, :device_id, :ts, :local_utc_ts, :offset_millis, " +
            ":ambient_temp, :ambient_light, :ambient_light_variance, :ambient_light_peakiness, :ambient_humidity, " +
            ":ambient_air_quality, :ambient_air_quality_raw, :ambient_dust_variance, :ambient_dust_min, :ambient_dust_max, " +
            ":firmware_version, :wave_count, :hold_count, " +
            ":audio_num_disturbances, :audio_peak_disturbances_db, :audio_peak_background_db);")
    public abstract void batchInsert(@BindDeviceData Iterator<DeviceData> deviceDataList);

    @RegisterMapper(DeviceDataMapper.class)
    @SqlQuery("SELECT * FROM device_sensors_master WHERE account_id = :account_id AND ts >= :start_timestamp AND ts <= :end_timestamp ORDER BY ts ASC")
    public abstract ImmutableList<DeviceData> getBetweenByUTCTime(
            @Bind("account_id") Long accountId,
            @Bind("start_timestamp") DateTime startTimestampUTC,
            @Bind("end_timestamp") DateTime endTimestampUTC);

    @RegisterMapper(SenseDeviceStatusMapper.class)
    @SingleValueResult(DeviceStatus.class)
    @SqlQuery("SELECT id, device_id, firmware_version, ts AS last_seen from device_sensors_master WHERE device_id = :sense_id AND ts > now() - interval '7 days'  ORDER BY ts DESC LIMIT 1;")
    public abstract Optional<DeviceStatus> senseStatusLastWeek(@Bind("sense_id") final Long senseId);


    @RegisterMapper(SenseDeviceStatusMapper.class)
    @SingleValueResult(DeviceStatus.class)
    @SqlQuery("SELECT id, device_id, firmware_version, ts AS last_seen from device_sensors_master WHERE device_id = :sense_id and ts > now() - interval '1 hours' ORDER BY ts DESC LIMIT 1;")
    public abstract Optional<DeviceStatus> senseStatusLastHour(@Bind("sense_id") final Long senseId);

    @Deprecated
    @RegisterMapper(DeviceDataMapper.class)
    @SqlQuery("SELECT * FROM device_sensors_master WHERE account_id = :account_id AND local_utc_ts >= :start_timestamp AND local_utc_ts <= :end_timestamp ORDER BY ts ASC")
    public abstract ImmutableList<DeviceData> getBetweenByLocalTime(
            @Bind("account_id") Long accountId,
            @Bind("start_timestamp") DateTime startTimestampLocalSetToUTC,
            @Bind("end_timestamp") DateTime endTimestampLocalSetToUTC);



    @Deprecated
    @RegisterMapper(DeviceDataBucketMapper.class)
    @SqlQuery(AGGREGATE_SELECT_STRING_GROUPBY_TSBUCKET +
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
    @SqlQuery(AGGREGATE_SELECT_STRING_GROUPBY_TSBUCKET +
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

    @RegisterMapper(DeviceDataMapper.class)
    @SqlQuery("SELECT * FROM device_sensors_master " +
            "WHERE account_id = :account_id AND device_id = :device_id AND ambient_light > :light_level " +
            "AND local_utc_ts >= :start_ts AND local_utc_ts <= :end_ts " +
            "AND (CAST(date_part('hour', local_utc_ts) AS integer) >= :start_hour " +
            "OR CAST(date_part('hour', local_utc_ts) AS integer) < :end_hour) " +
            "ORDER BY ts")
    public abstract ImmutableList<DeviceData> getLightByBetweenHourDate(@Bind("account_id") Long accountId,
                                                                        @Bind("device_id") Long deviceId,
                                                                        @Bind("light_level") int lightLevel,
                                                                        @Bind("start_ts") DateTime startTimestamp,
                                                                        @Bind("end_ts") DateTime endTimestamp,
                                                                        @Bind("start_hour") int startHour,
                                                                        @Bind("end_hour") int endHour);

    @RegisterMapper(DeviceDataMapper.class)
    @SqlQuery("SELECT * FROM device_sensors_master " +
            "WHERE account_id = :account_id AND device_id = :device_id " +
            "AND ambient_light > :light_level " +
            "AND ts >= :start_ts AND ts <= :end_ts " +
            "AND local_utc_ts >= :start_local_utc_ts AND local_utc_ts <= :end_local_utc_ts " +
            "AND (CAST(date_part('hour', local_utc_ts) AS integer) >= :start_hour " +
            "OR CAST(date_part('hour', local_utc_ts) AS integer) < :end_hour) " +
            "ORDER BY ts")
    public abstract ImmutableList<DeviceData> getLightByBetweenHourDateByTS(@Bind("account_id") Long accountId,
                                                                            @Bind("device_id") Long deviceId,
                                                                            @Bind("light_level") int lightLevel,
                                                                            @Bind("start_ts") DateTime startTimestamp,
                                                                            @Bind("end_ts") DateTime endTimestamp,
                                                                            @Bind("start_local_utc_ts") DateTime startLocalTimeStamp,
                                                                            @Bind("end_local_utc_ts") DateTime endLocalTimeStamp,
                                                                            @Bind("start_hour") int startHour,
                                                                            @Bind("end_hour") int endHour);

    @SqlQuery("SELECT AVG(ambient_air_quality), EXTRACT(day FROM local_utc_ts) AS date FROM device_sensors_master " +
            "WHERE account_id = :account_id AND device_id = :device_id " +
            "AND ts >= :start_ts AND ts <= :end_ts " +
            "AND local_utc_ts >= :start_local_utc_ts AND local_utc_ts <= :end_local_utc_ts " +
            "GROUP BY date")
    public abstract ImmutableList<Float> getAirQualityList(@Bind("account_id") Long accountId,
                                                             @Bind("device_id") Long deviceId,
                                                             @Bind("start_ts") DateTime startTimestamp,
                                                             @Bind("end_ts") DateTime endTimestamp,
                                                             @Bind("start_local_utc_ts") DateTime startLocalTimeStamp,
                                                             @Bind("end_local_utc_ts") DateTime endLocalTimeStamp);


    @RegisterMapper(DeviceDataMapper.class)
    @SqlQuery("SELECT * FROM device_sensors_master " +
            "WHERE account_id = :account_id AND device_id = :device_id " +
            "AND ts >= :start_ts AND ts <= :end_ts " +
            "AND local_utc_ts >= :start_local_utc_ts AND local_utc_ts <= :end_local_utc_ts " +
            "AND (CAST(date_part('hour', local_utc_ts) AS INTEGER) >= :start_hour " +
            "AND CAST(date_part('hour', local_utc_ts) AS INTEGER) < :end_hour)")
    public abstract ImmutableList<DeviceData> getBetweenHourDateByTSSameDay(@Bind("account_id") Long accountId,
                                                                            @Bind("device_id") Long deviceId,
                                                                            @Bind("start_ts") DateTime startTimestamp,
                                                                            @Bind("end_ts") DateTime endTimestamp,
                                                                            @Bind("start_local_utc_ts") DateTime startLocalTimeStamp,
                                                                            @Bind("end_local_utc_ts") DateTime endLocalTimeStamp,
                                                                            @Bind("start_hour") int startHour,
                                                                            @Bind("end_hour") int endHour);

    @RegisterMapper(DeviceDataMapper.class)
    @SqlQuery("SELECT * FROM device_sensors_master " +
            "WHERE account_id = :account_id AND device_id = :device_id " +
            "AND ts >= :start_ts AND ts <= :end_ts " +
            "AND local_utc_ts >= :start_local_utc_ts AND local_utc_ts <= :end_local_utc_ts " +
            "AND (CAST(date_part('hour', local_utc_ts) AS INTEGER) >= :start_hour " +
            "OR CAST(date_part('hour', local_utc_ts) AS INTEGER) < :end_hour)")
    public abstract ImmutableList<DeviceData> getBetweenHourDateByTS(@Bind("account_id") Long accountId,
                                                                     @Bind("device_id") Long deviceId,
                                                                     @Bind("start_ts") DateTime startTimestamp,
                                                                     @Bind("end_ts") DateTime endTimestamp,
                                                                     @Bind("start_local_utc_ts") DateTime startLocalTimeStamp,
                                                                     @Bind("end_local_utc_ts") DateTime endLocalTimeStamp,
                                                                     @Bind("start_hour") int startHour,
                                                                     @Bind("end_hour") int endHour);

    @RegisterMapper(DeviceDataBucketMapper.class)
    @SqlQuery(AGGREGATE_SELECT_STRING_GROUPBY_TSBUCKET +
            "FROM device_sensors_master " +
            "WHERE account_id = :account_id AND device_id = :device_id " +
            "AND local_utc_ts >= :start_ts AND local_utc_ts < :end_ts " +
            "AND (CAST(date_part('hour', local_utc_ts) AS integer) >= :start_hour " +
            "OR CAST(date_part('hour', local_utc_ts) AS integer) < :end_hour) " +
            "GROUP BY ts_bucket " +
            "ORDER BY ts_bucket ASC")
    public abstract ImmutableList<DeviceData> getBetweenByLocalHourAggregateBySlotDuration(@Bind("account_id") Long accountId,
                                                                                           @Bind("device_id") Long deviceId,
                                                                                           @Bind("start_ts") DateTime start,
                                                                                           @Bind("end_ts") DateTime end,
                                                                                           @Bind("start_hour") int startHour,
                                                                                           @Bind("end_hour") int endHour,
                                                                                           @Bind("slot_duration") Integer slotDuration);

    @RegisterMapper(DeviceDataMapper.class)
    @SingleValueResult(DeviceData.class)
    @SqlQuery("SELECT * FROM device_sensors_master " +
            "WHERE account_id = :account_id AND device_id = :device_id " +
            "AND ts < :max_utc_ts_limit and ts > :min_utc_ts_limit ORDER BY ts DESC LIMIT 1;")
    public abstract Optional<DeviceData> getMostRecent(@Bind("account_id") final Long accountId,
                                                       @Bind("device_id") Long deviceId,
                                                       @Bind("max_utc_ts_limit") final DateTime maxTsLimit,
                                                       @Bind("min_utc_ts_limit") final DateTime minTsLimit);

    public int batchInsertAll(final List<DeviceData> allDeviceData){
        final List<List<DeviceData>> batches = Lists.partition(allDeviceData, MAX_BATCH_INSERT_SIZE);

        int inserted = 0;
        for (final List<DeviceData> batchedData: batches) {

            try {
                this.batchInsert(batchedData.iterator());
                inserted += batchedData.size();
                continue;
            } catch (UnableToExecuteStatementException exception) {
                LOGGER.error("Failed to insert batch data: {}", exception.getMessage());
            }

            for(final DeviceData datum:batchedData){
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
        }

        return inserted;
    }

    @Timed
    public List<Sample> generateTimeSeriesByUTCTime(
            final Long queryStartTimestampInUTC,
            final Long queryEndTimestampInUTC,
            final Long accountId,
            final Long deviceId,
            final int slotDurationInMinutes,
            final String sensor,
            final Integer missingDataDefaultValue,
            final Optional<Device.Color> color,
            final Optional<Calibration> calibrationOptional) {

        final DateTime queryEndTime = new DateTime(queryEndTimestampInUTC, DateTimeZone.UTC);
        final DateTime queryStartTime = new DateTime(queryStartTimestampInUTC, DateTimeZone.UTC);

        LOGGER.trace("Client utcTimeStamp : {} ({})", queryEndTimestampInUTC, new DateTime(queryEndTimestampInUTC));
        LOGGER.trace("QueryEndTime: {} ({})", queryEndTime, queryEndTime.getMillis());
        LOGGER.trace("QueryStartTime: {} ({})", queryStartTime, queryStartTime.getMillis());

        final List<DeviceData> rows = getBetweenByAbsoluteTimeAggregateBySlotDuration(accountId, deviceId, queryStartTime, queryEndTime, slotDurationInMinutes);
        LOGGER.debug("Retrieved {} rows from database", rows.size());

        if(rows.size() == 0) {
            return new ArrayList<>();
        }

        // create buckets with keys in UTC-Time
        final int currentOffsetMillis = rows.get(0).offsetMillis;
        final DateTime now = queryEndTime.withSecondOfMinute(0).withMillisOfSecond(0);
        final int remainder = now.getMinuteOfHour() % slotDurationInMinutes;
        // if 4:36 -> bucket = 4:35

        final DateTime nowRounded = now.minusMinutes(remainder);
        LOGGER.trace("Current Offset Milis = {}", currentOffsetMillis);
        LOGGER.trace("Remainder = {}", remainder);
        LOGGER.trace("Now (rounded) = {} ({})", nowRounded, nowRounded.getMillis());


        final long absoluteIntervalMS = queryEndTimestampInUTC - queryStartTimestampInUTC;
        final int numberOfBuckets= (int) ((absoluteIntervalMS / DateTimeConstants.MILLIS_PER_MINUTE) / slotDurationInMinutes + 1);

        final Map<Long, Sample> map = Bucketing.generateEmptyMap(numberOfBuckets, nowRounded, slotDurationInMinutes, missingDataDefaultValue);

        LOGGER.trace("Map size = {}", map.size());

        final Optional<Map<Long, Sample>> optionalPopulatedMap = Bucketing.populateMap(rows, sensor, color, calibrationOptional);

        if(!optionalPopulatedMap.isPresent()) {
            return Collections.EMPTY_LIST;
        }

        // Override map with values from DB
        final Map<Long, Sample> merged = Bucketing.mergeResults(map, optionalPopulatedMap.get());

        LOGGER.trace("New map size = {}", merged.size());

        final List<Sample> sortedList = Bucketing.sortResults(merged, currentOffsetMillis);
        return sortedList;

    }

    // used by timeline, query by local_utc_ts
    @Deprecated
    @Timed
    public AllSensorSampleList generateTimeSeriesByLocalTimeAllSensors(
            final Long queryStartTimestampInLocalUTC,
            final Long queryEndTimestampInLocalUTC,
            final Long accountId,
            final Long deviceId,
            final int slotDurationInMinutes,
            final Integer missingDataDefaultValue,
            final Optional<Device.Color> color,
            final Optional<Calibration> calibrationOptional) {

        // queryEndTime is in UTC. If local now is 8:04pm in PDT, we create a utc timestamp in 8:04pm UTC
        final DateTime queryEndTime = new DateTime(queryEndTimestampInLocalUTC, DateTimeZone.UTC);
        final DateTime queryStartTime = new DateTime(queryStartTimestampInLocalUTC, DateTimeZone.UTC);

        LOGGER.trace("Client utcTimeStamp : {} ({})", queryEndTimestampInLocalUTC, new DateTime(queryEndTimestampInLocalUTC));
        LOGGER.trace("QueryEndTime: {} ({})", queryEndTime, queryEndTime.getMillis());
        LOGGER.trace("QueryStartTime: {} ({})", queryStartTime, queryStartTime.getMillis());

        final List<DeviceData> rows = getBetweenByLocalTimeAggregateBySlotDuration(accountId, deviceId, queryStartTime, queryEndTime, slotDurationInMinutes);
        LOGGER.debug("Retrieved {} rows from database", rows.size());

        final AllSensorSampleList sensorDataResults = new AllSensorSampleList();
        if(rows.size() == 0) {
            return sensorDataResults;
        }

        final AllSensorSampleMap allSensorSampleMap = Bucketing.populateMapAll(rows, color, calibrationOptional);

        if(allSensorSampleMap.isEmpty()) {
            return sensorDataResults;
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
        LOGGER.trace("Current Offset Milis = {}", startOffsetMillis);
        LOGGER.trace("Remainder = {}", remainder);
        LOGGER.trace("Now (rounded) = {} ({})", nowRounded, nowRounded.getMillis());

        final int numberOfBuckets= (int) ((absoluteIntervalMS / DateTimeConstants.MILLIS_PER_MINUTE) / slotDurationInMinutes + 1);



        for (final Sensor sensor : Sensor.values()) {
            LOGGER.trace("Processing sensor {}", sensor.toString());
            final Map<Long, Sample> sensorMap = allSensorSampleMap.get(sensor);
            if (!sensorMap.isEmpty()) {

                final Map<Long, Sample> map = Bucketing.generateEmptyMap(numberOfBuckets, nowRounded, slotDurationInMinutes, missingDataDefaultValue);
                LOGGER.trace("Empty Map size = {}", map.size());

                // Override map with values from DB
                final Map<Long, Sample> merged = Bucketing.mergeResults(map, sensorMap);

                LOGGER.trace("New map size = {}", merged.size());

                final List<Sample> sortedList = Bucketing.sortResults(merged, startOffsetMillis);
                sensorDataResults.add(sensor, sortedList);
            }
        }

        return sensorDataResults;
    }

    // used by room conditions, query by utc_ts
    @Timed
    public  AllSensorSampleList generateTimeSeriesByUTCTimeAllSensors(
            final Long queryStartTimestampInUTC,
            final Long queryEndTimestampInUTC,
            final Long accountId,
            final Long deviceId,
            final int slotDurationInMinutes,
            final Integer missingDataDefaultValue,
            final Optional<Device.Color> color,
            final Optional<Calibration> calibrationOptional) {

        // queryEndTime is in UTC. If local now is 8:04pm in PDT, we create a utc timestamp in 8:04pm UTC
        final DateTime queryEndTime = new DateTime(queryEndTimestampInUTC, DateTimeZone.UTC);
        final DateTime queryStartTime = new DateTime(queryStartTimestampInUTC, DateTimeZone.UTC);

        LOGGER.trace("Client utcTimeStamp : {} ({})", queryEndTimestampInUTC, new DateTime(queryEndTimestampInUTC));
        LOGGER.trace("QueryEndTime: {} ({})", queryEndTime, queryEndTime.getMillis());
        LOGGER.trace("QueryStartTime: {} ({})", queryStartTime, queryStartTime.getMillis());

        final List<DeviceData> rows = getBetweenByAbsoluteTimeAggregateBySlotDuration(accountId, deviceId, queryStartTime, queryEndTime, slotDurationInMinutes);
        LOGGER.trace("Retrieved {} rows from database", rows.size());

        final AllSensorSampleList sensorDataResults = new AllSensorSampleList();

        if(rows.size() == 0) {
            return sensorDataResults;
        }

        final AllSensorSampleMap allSensorSampleMap = Bucketing.populateMapAll(rows,color, calibrationOptional);

        if(allSensorSampleMap.isEmpty()) {
            return sensorDataResults;
        }

        // create buckets with keys in UTC-Time
        final int currentOffsetMillis = rows.get(0).offsetMillis;
        final DateTime now = queryEndTime.withSecondOfMinute(0).withMillisOfSecond(0);
        final int remainder = now.getMinuteOfHour() % slotDurationInMinutes;
        // if 4:36 -> bucket = 4:35

        final DateTime nowRounded = now.minusMinutes(remainder);
        LOGGER.trace("Current Offset Milis = {}", currentOffsetMillis);
        LOGGER.trace("Remainder = {}", remainder);
        LOGGER.trace("Now (rounded) = {} ({})", nowRounded, nowRounded.getMillis());


        final long absoluteIntervalMS = queryEndTimestampInUTC - queryStartTimestampInUTC;
        final int numberOfBuckets= (int) ((absoluteIntervalMS / DateTimeConstants.MILLIS_PER_MINUTE) / slotDurationInMinutes + 1);


        final AllSensorSampleMap mergedMaps = new AllSensorSampleMap();

        for (final Sensor sensor : Sensor.values()) {
            LOGGER.trace("Processing sensor {}", sensor.toString());

            final Map<Long, Sample> sensorMap = allSensorSampleMap.get(sensor);

            if (sensorMap.isEmpty()) {
                continue;
            }

            final Map<Long, Sample> map = Bucketing.generateEmptyMap(numberOfBuckets, nowRounded, slotDurationInMinutes, missingDataDefaultValue);
            LOGGER.trace("Map size = {}", map.size());

            // Override map with values from DB
            mergedMaps.setSampleMap(sensor, Bucketing.mergeResults(map, sensorMap));

            if (!mergedMaps.get(sensor).isEmpty()) {
                LOGGER.trace("New map size = {}", mergedMaps.get(sensor).size());
                final List<Sample> sortedList = Bucketing.sortResults(mergedMaps.get(sensor), currentOffsetMillis);

                sensorDataResults.add(sensor, sortedList);

            }
        }

        return sensorDataResults;
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
            "ROUND(MAX(audio_num_disturbances)) AS audio_num_disturbances," +
            "ROUND(MAX(audio_peak_disturbances_db)) AS audio_peak_disturbances_db," +
            "ROUND(MAX(audio_peak_background_db)) AS audio_peak_background_db " +
            "FROM device_sensors_master " +
            "WHERE account_id = :account_id " +
            "AND local_utc_ts >= :start_ts AND local_utc_ts < :end_ts;")
    public abstract Optional<DeviceData> getAverageForNight(@Bind("account_id") final Long accountId,
                                                            @Bind("start_ts") final DateTime targetDate,
                                                            @Bind("end_ts") final DateTime endDate);


    @SqlQuery("SELECT ROUND(AVG(ambient_air_quality_raw)) FROM device_sensors_master WHERE device_id = :device_id AND account_id := :account_id AND ts > :then;")
    protected abstract Integer getAverageDustForLastNDays(@Bind("account_id") final Long accountId, @Bind("device_id") final Long deviceId, @Bind("then") final DateTime then);

    public Integer getAverageDustForLast10Days(final Long accountId, final Long deviceId) {
        return this.getAverageDustForLastNDays(accountId, deviceId, DateTime.now(DateTimeZone.UTC).minusDays(10));
    }
}
