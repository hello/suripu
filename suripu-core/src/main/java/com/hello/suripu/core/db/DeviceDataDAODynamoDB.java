package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Expressions;
import com.hello.suripu.core.db.dynamo.expressions.Expression;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.db.util.Bucketing;
import com.hello.suripu.core.db.util.DynamoDBItemAggregator;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.AllSensorSampleMap;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jakepiccolo on 10/9/15.
 *
 * Schema:
 *  account_id(HK)  |  timestamp|external_device_id(RK)  |  ambient_temp  |  (rest of the values...)
 *
 * The hash key is the account_id, so it is required for getting device data and all data is attached to an account.
 *
 * The range key is the UTC timestamp, concatenated with the (external) device id. This is so that we can always query by time.
 * Why add on the external device ID? To ensure uniqueness of hash key + range key if you are paired to multiple Senses.
 * The device ID is added to the end of the range key so that using it in a range query is optional, i.e. it is possible to get
 * data for all devices for a given account and time range. Getting data for a specific device requires additional in-memory filtering.
 * Example range key: "2015-10-22 10:00|ABCDEF"
 *
 * We shard tables monthly based on the UTC timestamp, in order to better utilize our throughput on recent time series data.
 * See http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GuidelinesForTables.html#GuidelinesForTables.TimeSeriesDataAccessPatterns
 * and http://stackoverflow.com/a/30200359
 */
public class DeviceDataDAODynamoDB extends TimeSeriesDAODynamoDB<DeviceData> implements DeviceDataIngestDAO, DeviceDataInsightQueryDAO, DeviceDataReadAllSensorsDAO {
    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceDataDAODynamoDB.class);

    public enum DeviceDataAttribute implements Attribute {
        ACCOUNT_ID ("aid", "N"),
        RANGE_KEY ("ts|dev", "S"),  // <utc_timestamp>|<external_device_id>
        AMBIENT_TEMP ("tmp", "N"),
        AMBIENT_LIGHT ("lite", "N"),
        AMBIENT_LIGHT_VARIANCE ("litevar", "N"),
        AMBIENT_HUMIDITY ("hum", "N"),
        AMBIENT_AIR_QUALITY_RAW ("aqr", "N"),
        AUDIO_PEAK_BACKGROUND_DB ("apbg", "N"),
        AUDIO_PEAK_ENERGY_DB ("apedb", "N"),
        AUDIO_PEAK_DISTURBANCES_DB ("apd", "N"),
        AUDIO_NUM_DISTURBANCES ("and", "N"),
        OFFSET_MILLIS ("om", "N"),
        LOCAL_UTC_TIMESTAMP ("lutcts", "S"),
        WAVE_COUNT ("wc", "N"),
        HOLD_COUNT ("hc", "N");

        private final String name;
        private final String type;

        DeviceDataAttribute(String name, String type) {
            this.name = name;
            this.type = type;
        }

        /**
         * Useful instead of item.get(DeviceDataAttribute.<DeviceDataAttribute>.name) to avoid NullPointerException
         */
        private AttributeValue get(final Map<String, AttributeValue> item) {
            return item.get(this.name);
        }

        private Integer getInteger(final Map<String, AttributeValue> item) {
            if (item.containsKey(this.name)) {
                return Integer.valueOf(get(item).getN());
            }
            return 0;
        }

        public String sanitizedName() {
            return toString();
        }

        public String shortName() {
            return name;
        }

        @Override
        public String type() {
            return type;
        }
    }

    public final static ImmutableSet<DeviceDataAttribute> BASE_ATTRIBUTES = new ImmutableSet.Builder<DeviceDataAttribute>()
            .add(DeviceDataAttribute.ACCOUNT_ID)
            .add(DeviceDataAttribute.OFFSET_MILLIS)
            .add(DeviceDataAttribute.LOCAL_UTC_TIMESTAMP)
            .add(DeviceDataAttribute.RANGE_KEY)
            .build();

    private final static Map<String, Set<DeviceDataAttribute>> SENSOR_NAME_TO_ATTRIBUTES = new ImmutableMap.Builder<String, Set<DeviceDataAttribute>>()
            .put("humidity", ImmutableSet.of(DeviceDataAttribute.AMBIENT_TEMP, DeviceDataAttribute.AMBIENT_HUMIDITY))
            .put("temperature", ImmutableSet.of(DeviceDataAttribute.AMBIENT_TEMP))
            .put("particulates", ImmutableSet.of(DeviceDataAttribute.AMBIENT_AIR_QUALITY_RAW))
            .put("light", ImmutableSet.of(DeviceDataAttribute.AMBIENT_LIGHT))
            .put("sound", ImmutableSet.of(DeviceDataAttribute.AUDIO_PEAK_BACKGROUND_DB,
                                          DeviceDataAttribute.AUDIO_PEAK_DISTURBANCES_DB,
                                          DeviceDataAttribute.AUDIO_PEAK_ENERGY_DB))
            .build();

    public final static ImmutableSet<DeviceDataAttribute> ALL_ATTRIBUTES = ImmutableSet.copyOf(DeviceDataAttribute.values());

    // Store everything to the minute level
    private static final DateTimeFormatter DATE_TIME_READ_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:00Z");
    private static final String DATE_TIME_STRING_TEMPLATE = "yyyy-MM-dd HH:mm";
    private static final DateTimeFormatter DATE_TIME_WRITE_FORMATTER = DateTimeFormat.forPattern(DATE_TIME_STRING_TEMPLATE);

    public DeviceDataDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tablePrefix) {
        super(dynamoDBClient, tablePrefix);
    }


    //region Override abstract methods
    @Override
    protected Logger logger() {
        return LOGGER;
    }

    @Override
    protected Integer maxQueryAttempts() {
        return 5;
    }

    @Override
    protected Integer maxBatchWriteAttempts() {
        return 5;
    }

    @Override
    protected String hashKeyName() {
        return DeviceDataAttribute.ACCOUNT_ID.name;
    }

    @Override
    protected String rangeKeyName() {
        return DeviceDataAttribute.RANGE_KEY.name;
    }

    @Override
    protected String hashKeyType() {
        return DeviceDataAttribute.ACCOUNT_ID.type;
    }

    @Override
    protected String rangeKeyType() {
        return DeviceDataAttribute.RANGE_KEY.type;
    }

    @Override
    protected String getHashKey(AttributeValue attributeValue) {
        return attributeValue.getN();
    }

    @Override
    protected String getRangeKey(AttributeValue attributeValue) {
        return attributeValue.getS();
    }

    @Override
    protected DateTime getTimestamp(DeviceData model) {
        return model.dateTimeUTC;
    }

    @Override
    public String getTableName(final DateTime dateTime) {
        final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy_MM");
        return tablePrefix + "_" + dateTime.toString(formatter);
    }

    @Override
    public List<String> getTableNames(final DateTime start, final DateTime end) {
        final LinkedList<String> names = Lists.newLinkedList();
        for (final DateTime dateTime: DateTimeUtil.dateTimesForStartOfMonthBetweenDates(start, end)) {
            final String tableName = getTableName(dateTime);
            names.add(tableName);
        }
        return names;
    }

    @Override
    protected Map<String, AttributeValue> toAttributeMap(DeviceData data) {
        final HashMap<String, AttributeValue> item = Maps.newHashMap();
        item.put(DeviceDataAttribute.ACCOUNT_ID.name, toAttributeValue(data.accountId));
        item.put(DeviceDataAttribute.RANGE_KEY.name, getRangeKey(data.dateTimeUTC, data.externalDeviceId));
        item.put(DeviceDataAttribute.AMBIENT_TEMP.name, toAttributeValue(data.ambientTemperature));
        item.put(DeviceDataAttribute.AMBIENT_LIGHT.name, toAttributeValue(data.ambientLight));
        item.put(DeviceDataAttribute.AMBIENT_LIGHT_VARIANCE.name, toAttributeValue(data.ambientLightVariance));
        item.put(DeviceDataAttribute.AMBIENT_HUMIDITY.name, toAttributeValue(data.ambientHumidity));
        item.put(DeviceDataAttribute.AMBIENT_AIR_QUALITY_RAW.name, toAttributeValue(data.ambientAirQualityRaw));
        item.put(DeviceDataAttribute.AUDIO_PEAK_BACKGROUND_DB.name, toAttributeValue(data.audioPeakBackgroundDB));
        item.put(DeviceDataAttribute.AUDIO_PEAK_ENERGY_DB.name, toAttributeValue(data.audioPeakEnergyDB));
        item.put(DeviceDataAttribute.AUDIO_PEAK_DISTURBANCES_DB.name, toAttributeValue(data.audioPeakDisturbancesDB));
        item.put(DeviceDataAttribute.AUDIO_NUM_DISTURBANCES.name, toAttributeValue(data.audioNumDisturbances));
        item.put(DeviceDataAttribute.WAVE_COUNT.name, toAttributeValue(data.waveCount));
        item.put(DeviceDataAttribute.HOLD_COUNT.name, toAttributeValue(data.holdCount));
        item.put(DeviceDataAttribute.LOCAL_UTC_TIMESTAMP.name, dateTimeToAttributeValue(data.dateTimeUTC.plusMillis(data.offsetMillis)));
        item.put(DeviceDataAttribute.OFFSET_MILLIS.name, toAttributeValue(data.offsetMillis));
        return item;
    }
    //endregion


    private static AttributeValue dateTimeToAttributeValue(final DateTime dateTime) {
        return new AttributeValue(dateTime.toString(DATE_TIME_WRITE_FORMATTER));
    }

    private static AttributeValue getRangeKey(final DateTime dateTime, final String senseId) {
        return new AttributeValue(dateTime.toString(DATE_TIME_WRITE_FORMATTER) + "|" + senseId);
    }

    private static AttributeValue toAttributeValue(final Integer value) {
        return new AttributeValue().withN(String.valueOf(value));
    }

    private static AttributeValue toAttributeValue(final Long value) {
        return new AttributeValue().withN(String.valueOf(value));
    }


    //region DeviceDataIngestDAO implementation
    @Override
    public int batchInsertAll(List<DeviceData> allDeviceData) {
        return batchInsertAllPartitions(allDeviceData);
    }

    @Override
    public Class name() {
        return  DeviceDataDAODynamoDB.class;
    }
    //endregion


    private DateTime getFloorOfDateTime(final DateTime dateTime, final Integer toMinutes) {
        final int minute = dateTime.getMinuteOfHour();
        return dateTime.withMinuteOfHour(minute - (minute % toMinutes));
    }

    private DateTime timestampFromDDBItem(final Map<String, AttributeValue> item) {
        final String dateString = DeviceDataAttribute.RANGE_KEY.get(item).getS().substring(0, DATE_TIME_STRING_TEMPLATE.length());
        return DateTime.parse(dateString + ":00Z", DATE_TIME_READ_FORMATTER).withZone(DateTimeZone.UTC);
    }

    private String externalDeviceIdFromDDBItem(final Map<String, AttributeValue> item) {
        return item.get(DeviceDataAttribute.RANGE_KEY.name).getS().substring(DATE_TIME_STRING_TEMPLATE.length() + 1);
    }


    //region Aggregation
    private DeviceData aggregateDynamoDBItemsToDeviceData(final List<Map<String, AttributeValue>> items, final DeviceData template) {
        final DynamoDBItemAggregator aggregator = new DynamoDBItemAggregator(items);
        return new DeviceData.Builder()
                .withAccountId(template.accountId)
                .withExternalDeviceId(template.externalDeviceId)
                .withDateTimeUTC(template.dateTimeUTC)
                .withOffsetMillis(template.offsetMillis)
                .withAmbientTemperature((int) aggregator.min(DeviceDataAttribute.AMBIENT_TEMP.name))
                .calibrateAmbientLight((int) aggregator.roundedMean(DeviceDataAttribute.AMBIENT_LIGHT.name))
                .withAmbientLightVariance((int) aggregator.roundedMean(DeviceDataAttribute.AMBIENT_LIGHT_VARIANCE.name))
                .withAmbientHumidity((int) aggregator.roundedMean(DeviceDataAttribute.AMBIENT_HUMIDITY.name))
                .withWaveCount((int) aggregator.sum(DeviceDataAttribute.WAVE_COUNT.name))
                .withHoldCount((int) aggregator.sum(DeviceDataAttribute.HOLD_COUNT.name))
                .withAudioNumDisturbances((int) aggregator.max(DeviceDataAttribute.AUDIO_NUM_DISTURBANCES.name))
                .withAudioPeakBackgroundDB((int) aggregator.max(DeviceDataAttribute.AUDIO_PEAK_BACKGROUND_DB.name))
                .withAudioPeakEnergyDB((int) aggregator.max(DeviceDataAttribute.AUDIO_PEAK_ENERGY_DB.name))
                .withAudioPeakDisturbancesDB((int) aggregator.max(DeviceDataAttribute.AUDIO_PEAK_DISTURBANCES_DB.name))
                .withAmbientAirQualityRaw((int) aggregator.roundedMean(DeviceDataAttribute.AMBIENT_AIR_QUALITY_RAW.name))
                .build();
    }

    List<DeviceData> aggregateDynamoDBItemsToDeviceData(final List<Map<String, AttributeValue>> items, final Integer slotDuration) {
        final List<DeviceData> resultList = Lists.newArrayListWithExpectedSize(items.size() / slotDuration);
        if (items.isEmpty()) {
            return resultList;
        }

        List<Map<String, AttributeValue>> currentWorkingList = Lists.newArrayListWithExpectedSize(slotDuration);
        final Map<String, AttributeValue> firstItem = items.get(0);
        final DeviceData.Builder templateBuilder = new DeviceData.Builder()
                .withAccountId(Long.valueOf(firstItem.get(DeviceDataAttribute.ACCOUNT_ID.name).getN()))
                .withExternalDeviceId(externalDeviceIdFromDDBItem(firstItem));
        DateTime currSlotTime = getFloorOfDateTime(timestampFromDDBItem(firstItem), slotDuration);

        for (final Map<String, AttributeValue> item: items) {
            final DateTime itemDateTime = getFloorOfDateTime(timestampFromDDBItem(item), slotDuration);
            if (currSlotTime.equals(itemDateTime)) {
                // Within the window of our current working set.
                currentWorkingList.add(item);
            } else if (itemDateTime.isAfter(currSlotTime)) {
                // Outside the window, aggregate working set to single value.
                templateBuilder.withDateTimeUTC(currSlotTime);
                templateBuilder.withOffsetMillis(DeviceDataAttribute.OFFSET_MILLIS.getInteger(currentWorkingList.get(0)));
                resultList.add(aggregateDynamoDBItemsToDeviceData(currentWorkingList, templateBuilder.build()));

                // Create new working sets
                currentWorkingList = Lists.newArrayListWithExpectedSize(slotDuration);
                currentWorkingList.add(item);
            } else {
                // Unsorted list
                throw new IllegalArgumentException("Input DeviceDatas must be sorted.");
            }
            currSlotTime = itemDateTime;
        }

        templateBuilder.withDateTimeUTC(currSlotTime);
        templateBuilder.withOffsetMillis(DeviceDataAttribute.OFFSET_MILLIS.getInteger(currentWorkingList.get(0)));
        resultList.add(aggregateDynamoDBItemsToDeviceData(currentWorkingList, templateBuilder.build()));
        return resultList;
    }
    //endregion


    /**
     * Aggregate DeviceDatas to the given slotDuration in minutes.
     * @param accountId Account ID
     * @param externalDeviceId Device ID
     * @param start - Start timestamp, inclusive
     * @param end - End timestamp, exclusive
     * @param slotDuration - Duration of each aggregated bucket in minutes
     * @param targetAttributes - Attributes to include in output DeviceDatas
     * @return DeviceDatas matching the above filter criteria
     */
    public Response<ImmutableList<DeviceData>> getBetweenByAbsoluteTimeAggregateBySlotDuration(
            final Long accountId,
            final String externalDeviceId,
            final DateTime start,
            final DateTime end,
            final Integer slotDuration,
            final Collection<DeviceDataAttribute> targetAttributes)
    {
        final DateTime endExclusive = end.minusMinutes(1);
        final Expression keyConditionExpression = Expressions.and(
                Expressions.equals(DeviceDataAttribute.ACCOUNT_ID, toAttributeValue(accountId)),
                Expressions.between(DeviceDataAttribute.RANGE_KEY, getRangeKey(start, externalDeviceId), getRangeKey(endExclusive, externalDeviceId)));

        final Response<List<Map<String, AttributeValue>>> results = queryTables(getTableNames(start, endExclusive), keyConditionExpression, targetAttributes);
        final List<Map<String, AttributeValue>> filteredResults = Lists.newLinkedList();
        for (final Map<String, AttributeValue> item : results.data) {
            if (externalDeviceIdFromDDBItem(item).equals(externalDeviceId)) {
                filteredResults.add(item);
            }
        }

        final List<DeviceData> aggregated = aggregateDynamoDBItemsToDeviceData(filteredResults, slotDuration);

        return Response.into(ImmutableList.copyOf(aggregated), results);
    }

    /**
     * Same as the method that accepts targetAttributes, but defaults to getting all attributes.
     */
    public Response<ImmutableList<DeviceData>> getBetweenByAbsoluteTimeAggregateBySlotDuration(
            final Long accountId,
            final String externalDeviceId,
            final DateTime start,
            final DateTime end,
            final Integer slotDuration) {
        return getBetweenByAbsoluteTimeAggregateBySlotDuration(accountId, externalDeviceId, start, end, slotDuration, ALL_ATTRIBUTES);
    }

    private static Set<DeviceDataAttribute> sensorNameToAttributeNames(final String sensorName) {
        final Set<DeviceDataAttribute> sensorAttributes = SENSOR_NAME_TO_ATTRIBUTES.get(sensorName);
        if (sensorAttributes == null) {
            throw new IllegalArgumentException("Unknown sensor name: '" + sensorName + "'");
        }
        return new ImmutableSet.Builder<DeviceDataAttribute>().addAll(BASE_ATTRIBUTES).addAll(sensorAttributes).build();
    }

    private DateTime timestampToDateTimeUTC(final long timestampUTC) {
        return new DateTime(timestampUTC, DateTimeZone.UTC);
    }

    public List<Sample> generateTimeSeriesByUTCTime(
            final Long queryStartTimestampInUTC,
            final Long queryEndTimestampInUTC,
            final Long accountId,
            final String externalDeviceId,
            final int slotDurationInMinutes,
            final String sensor,
            final Integer missingDataDefaultValue,
            final Optional<Device.Color> color,
            final Optional<Calibration> calibrationOptional,
            final Boolean useAudioPeakEnergy) throws IllegalArgumentException {

        final DateTime queryEndTime = timestampToDateTimeUTC(queryEndTimestampInUTC);
        final DateTime queryStartTime = timestampToDateTimeUTC(queryStartTimestampInUTC);

        LOGGER.trace("Client utcTimeStamp : {} ({})", queryEndTimestampInUTC, queryEndTime);
        LOGGER.trace("QueryEndTime: {} ({})", queryEndTime, queryEndTime.getMillis());
        LOGGER.trace("QueryStartTime: {} ({})", queryStartTime, queryStartTime.getMillis());

        LOGGER.debug("Calling getBetweenByAbsoluteTimeAggregateBySlotDuration with arguments: ({}, {}, {}, {}, {}, {})", accountId, externalDeviceId, queryStartTime, queryEndTime, slotDurationInMinutes, sensorNameToAttributeNames(sensor));
        final Response<ImmutableList<DeviceData>> response = getBetweenByAbsoluteTimeAggregateBySlotDuration(
                accountId, externalDeviceId, queryStartTime, queryEndTime, slotDurationInMinutes, sensorNameToAttributeNames(sensor));
        final List<DeviceData> rows = response.data;
        LOGGER.debug("Retrieved {} rows from database", rows.size());

        if(rows.isEmpty() || response.status != Response.Status.SUCCESS) {
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

        final Optional<Map<Long, Sample>> optionalPopulatedMap = Bucketing.populateMap(rows, sensor, color, calibrationOptional, useAudioPeakEnergy);

        if(!optionalPopulatedMap.isPresent()) {
            LOGGER.debug("Map not populated, returning empty list of samples.");
            return Collections.EMPTY_LIST;
        }

        // Override map with values from DB
        final Map<Long, Sample> merged = Bucketing.mergeResults(map, optionalPopulatedMap.get());

        LOGGER.trace("New map size = {}", merged.size());

        final List<Sample> sortedList = Bucketing.sortResults(merged, currentOffsetMillis);
        return sortedList;

    }

    @Override
    public AllSensorSampleList generateTimeSeriesByUTCTimeAllSensors(
            final Long queryStartTimestampInUTC,
            final Long queryEndTimestampInUTC,
            final Long accountId,
            final String externalDeviceId,
            final int slotDurationInMinutes,
            final Integer missingDataDefaultValue,
            final Optional<Device.Color> color,
            final Optional<Calibration> calibrationOptional,
            final Boolean useAudioPeakEnergy) {

        // queryEndTime is in UTC. If local now is 8:04pm in PDT, we create a utc timestamp in 8:04pm UTC
        final DateTime queryEndTime = timestampToDateTimeUTC(queryEndTimestampInUTC);
        final DateTime queryStartTime = timestampToDateTimeUTC(queryStartTimestampInUTC);

        LOGGER.trace("Client utcTimeStamp : {} ({})", queryEndTimestampInUTC, queryEndTime);
        LOGGER.trace("QueryEndTime: {} ({})", queryEndTime, queryEndTime.getMillis());
        LOGGER.trace("QueryStartTime: {} ({})", queryStartTime, queryStartTime.getMillis());

        final Response<ImmutableList<DeviceData>> response = getBetweenByAbsoluteTimeAggregateBySlotDuration(accountId, externalDeviceId, queryStartTime, queryEndTime, slotDurationInMinutes);
        final List<DeviceData> rows = response.data;
        LOGGER.trace("Retrieved {} rows from database", rows.size());

        final AllSensorSampleList sensorDataResults = new AllSensorSampleList();

        if(rows.isEmpty() || response.status != Response.Status.SUCCESS) {
            return sensorDataResults;
        }

        final AllSensorSampleMap allSensorSampleMap = Bucketing.populateMapAll(rows, color, calibrationOptional, useAudioPeakEnergy);

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

    final DeviceData attributeMapToDeviceData(final Map<String, AttributeValue> item) {
        return new DeviceData.Builder()
                .withDateTimeUTC(timestampFromDDBItem(item))
                .withAccountId(Long.valueOf(DeviceDataAttribute.ACCOUNT_ID.get(item).getN()))
                .withExternalDeviceId(externalDeviceIdFromDDBItem(item))
                .withOffsetMillis(DeviceDataAttribute.OFFSET_MILLIS.getInteger(item))
                .withAmbientTemperature(DeviceDataAttribute.AMBIENT_TEMP.getInteger(item))
                .calibrateAmbientLight(DeviceDataAttribute.AMBIENT_LIGHT.getInteger(item))
                .withAmbientLightVariance(DeviceDataAttribute.AMBIENT_LIGHT_VARIANCE.getInteger(item))
                .withAmbientHumidity(DeviceDataAttribute.AMBIENT_HUMIDITY.getInteger(item))
                .withWaveCount(DeviceDataAttribute.WAVE_COUNT.getInteger(item))
                .withHoldCount(DeviceDataAttribute.HOLD_COUNT.getInteger(item))
                .withAudioNumDisturbances(DeviceDataAttribute.AUDIO_NUM_DISTURBANCES.getInteger(item))
                .withAudioPeakBackgroundDB(DeviceDataAttribute.AUDIO_PEAK_BACKGROUND_DB.getInteger(item))
                .withAudioPeakEnergyDB(DeviceDataAttribute.AUDIO_PEAK_ENERGY_DB.getInteger(item))
                .withAudioPeakDisturbancesDB(DeviceDataAttribute.AUDIO_PEAK_DISTURBANCES_DB.getInteger(item))
                .withAmbientAirQualityRaw(DeviceDataAttribute.AMBIENT_AIR_QUALITY_RAW.getInteger(item))
                .build();
    }

    final List<DeviceData> attributeMapsToDeviceDataList(final List<Map<String, AttributeValue>> items) {
        final List<DeviceData> dataList = Lists.newArrayListWithCapacity(items.size());
        for (final Map<String, AttributeValue> item: items) {
            dataList.add(attributeMapToDeviceData(item));
        }
        return dataList;
    }


    //region Most recent
    /**
     * Get the most recent DeviceData for the given accountId and externalDeviceId.
     *
     * The query will only search from minTsLimit to maxTsLimit, so these are used to bound the search.
     */
    public Optional<DeviceData> getMostRecent(final Long accountId,
                                              final String externalDeviceId,
                                              final DateTime maxTsLimit,
                                              final DateTime minTsLimit)
    {
        final Expression keyConditionExpression = Expressions.and(
                Expressions.equals(DeviceDataAttribute.ACCOUNT_ID, toAttributeValue(accountId)),
                Expressions.between(DeviceDataAttribute.RANGE_KEY, getRangeKey(minTsLimit, externalDeviceId), getRangeKey(maxTsLimit, externalDeviceId)));
        final Collection<DeviceDataAttribute> attributes = ALL_ATTRIBUTES;

        final Optional<Map<String, AttributeValue>> result = getLatest(getTableName(maxTsLimit), keyConditionExpression, attributes);
        if (result.isPresent()) {
            final DeviceData deviceData = attributeMapToDeviceData(result.get());
            if (deviceData.externalDeviceId.equals(externalDeviceId)) {
                return Optional.of(deviceData);
            }
        }

        // Getting the absolute most recent didn't work, so try querying relevant tables.
        final Response<List<Map<String, AttributeValue>>> response = queryTables(getTableNames(minTsLimit, maxTsLimit), keyConditionExpression, attributes);

        // Iterate through results in reverse order (most recent first)
        for (final Map<String, AttributeValue> item: Lists.reverse(response.data)) {
            final DeviceData deviceData = attributeMapToDeviceData(item);
            if (deviceData.externalDeviceId.equals(externalDeviceId)) {
                return Optional.of(deviceData);
            }
        }

        return Optional.absent();
    }
    //endregion


    private DynamoDBItemAggregator aggregatorForAttribute(final Long accountId,
                                                          final String externalDeviceId,
                                                          final DateTime startTime,
                                                          final DateTime endTime,
                                                          final DeviceDataAttribute attribute)
    {
        final Expression keyConditionExp = Expressions.and(
                Expressions.equals(DeviceDataAttribute.ACCOUNT_ID, toAttributeValue(accountId)),
                Expressions.between(DeviceDataAttribute.RANGE_KEY, getRangeKey(startTime, externalDeviceId), getRangeKey(endTime, externalDeviceId)));
        final ImmutableSet<Attribute> attributes = ImmutableSet.<Attribute>builder()
                .add(attribute)
                .addAll(BASE_ATTRIBUTES)
                .build();
        final Response<List<Map<String, AttributeValue>>> response = queryTables(getTableNames(startTime, endTime), keyConditionExp, attributes);
        return new DynamoDBItemAggregator(response.data);
    }

    private Integer getAverageForTimeRange(final Long accountId,
                                           final String externalDeviceId,
                                           final DateTime startTime,
                                           final DateTime endTime,
                                           final DeviceDataAttribute attribute)
    {
        final DynamoDBItemAggregator aggregator = aggregatorForAttribute(accountId, externalDeviceId, startTime, endTime, attribute);
        return ((int) aggregator.roundedMean(attribute.name));
    }

    public Integer getAverageDustForLast10Days(final Long accountId,
                                               final String externalDeviceId,
                                               final DateTime endTime)
    {
        final DateTime startTime = endTime.minusDays(10);
        final DeviceDataAttribute attribute = DeviceDataAttribute.AMBIENT_AIR_QUALITY_RAW;
        return getAverageForTimeRange(accountId, externalDeviceId, startTime, endTime, attribute);
    }


    //region DeviceDataInsightQueryDAO implementation
    /**
     *
     * @param accountId Account ID
     * @param deviceId Device ID
     * @param minLightLevel Only return DeviceDatas whose ambientLight is > this value.
     * @param startTime Earliest UTC time to retrieve (inclusive)
     * @param endTime Latest UTC time to retrieve (inclusive)
     * @param startLocalTime Earliest local time to retrieve (inclusive)
     * @param endLocalTime Latest local time to retrieve (inclusive)
     * @param startHour The start hour of the "night before"
     * @param endHour The end hour of the "morning after"
     * @return DeviceDatas matching the above criteria
     */
    @Override
    public Response<ImmutableList<DeviceData>> getLightByBetweenHourDateByTS(final Long accountId,
                                                                             final DeviceId deviceId,
                                                                             final int minLightLevel,
                                                                             final DateTime startTime,
                                                                             final DateTime endTime,
                                                                             final DateTime startLocalTime,
                                                                             final DateTime endLocalTime,
                                                                             final int startHour,
                                                                             final int endHour)
    {
        final String externalDeviceId = deviceId.externalDeviceId.get();

        final Expression filterExp = Expressions.and(
                Expressions.between(DeviceDataAttribute.LOCAL_UTC_TIMESTAMP, dateTimeToAttributeValue(startLocalTime), dateTimeToAttributeValue(endLocalTime)),
                Expressions.compare(DeviceDataAttribute.AMBIENT_LIGHT, ">", toAttributeValue(minLightLevel)));
        final Expression keyConditionExp = Expressions.and(
                Expressions.equals(DeviceDataAttribute.ACCOUNT_ID, toAttributeValue(accountId)),
                Expressions.between(DeviceDataAttribute.RANGE_KEY, getRangeKey(startTime, externalDeviceId), getRangeKey(endTime, externalDeviceId)));

        final List<DeviceData> results = Lists.newArrayList();
        final Response<List<Map<String, AttributeValue>>> response = queryTables(getTableNames(startTime, endTime), keyConditionExp, filterExp, ALL_ATTRIBUTES);
        for (final Map<String, AttributeValue> result : response.data) {
            final DeviceData data = attributeMapToDeviceData(result);
            final int hourOfDay = data.localTime().getHourOfDay();
            if (data.externalDeviceId.equals(externalDeviceId) &&
                    (hourOfDay >= startHour || hourOfDay < endHour)) {
                results.add(data);
            }
        }


        return Response.into(ImmutableList.copyOf(results), response);
    }

    private Response<List<Map<String, AttributeValue>>> getItemsBetweenLocalTime(
            final Long accountId,
            final DeviceId deviceId,
            final DateTime startUTCTime,
            final DateTime endUTCTime,
            final DateTime startLocalTime,
            final DateTime endLocalTime,
            final Collection<DeviceDataAttribute> attributes)
    {
        final String externalDeviceId = deviceId.externalDeviceId.get();
        final Expression keyConditionExpression = Expressions.and(
                Expressions.equals(DeviceDataAttribute.ACCOUNT_ID, toAttributeValue(accountId)),
                Expressions.between(DeviceDataAttribute.RANGE_KEY, getRangeKey(startUTCTime, externalDeviceId), getRangeKey(endUTCTime, externalDeviceId)));
        final Expression filterExpression = Expressions.between(DeviceDataAttribute.LOCAL_UTC_TIMESTAMP, dateTimeToAttributeValue(startLocalTime), dateTimeToAttributeValue(endLocalTime));

        final List<Map<String, AttributeValue>> results = Lists.newArrayList();
        final Response<List<Map<String, AttributeValue>>> response = queryTables(getTableNames(startUTCTime, endUTCTime), keyConditionExpression, filterExpression, attributes);
        for (final Map<String, AttributeValue> result : response.data) {
            if (externalDeviceIdFromDDBItem(result).equals(externalDeviceId)) {
                results.add(result);
            }
        }

        return Response.into(results, response);
    }

    /**
     *
     * @param accountId Account ID
     * @param deviceId Sense ID
     * @param startUTCTime Earliest UTC time to retrieve (inclusive)
     * @param endUTCTime Latest UTC time to retrieve (inclusive)
     * @param startLocalTime Earliest local time to retrieve (inclusive)
     * @param endLocalTime Latest local time to retrieve (inclusive)
     * @param attributes Attributes to be included in the response DeviceDatas.
     * @return DeviceDatas matching the above criteria
     */
    public Response<ImmutableList<DeviceData>> getBetweenLocalTime(final Long accountId,
                                                  final DeviceId deviceId,
                                                  final DateTime startUTCTime,
                                                  final DateTime endUTCTime,
                                                  final DateTime startLocalTime,
                                                  final DateTime endLocalTime,
                                                  final Collection<DeviceDataAttribute> attributes)
    {
        final Response<List<Map<String, AttributeValue>>> response = getItemsBetweenLocalTime(accountId, deviceId, startUTCTime, endUTCTime, startLocalTime, endLocalTime, attributes);
        ImmutableList<DeviceData> data = ImmutableList.copyOf(attributeMapsToDeviceDataList(response.data));
        return Response.into(data, response);
    }

    private List<Integer> averageDailyAirQualityRaw(final List<DeviceData> deviceDataList) {
        if (deviceDataList.isEmpty()) {
            return ImmutableList.of();
        }

        // Daily aggregate items
        final LinkedList<LinkedList<Integer>> airQuality = Lists.newLinkedList();
        DateTime lastLocalTime = deviceDataList.get(0).localTime().withTimeAtStartOfDay();
        for (final DeviceData data: deviceDataList) {
            if (!data.localTime().withTimeAtStartOfDay().equals(lastLocalTime) || airQuality.isEmpty()) {
                airQuality.add(Lists.<Integer>newLinkedList());
            }
            airQuality.getLast().add(data.ambientAirQualityRaw);
            lastLocalTime = data.localTime().withTimeAtStartOfDay();
        }

        final List<Integer> aggregated = Lists.newArrayListWithCapacity(airQuality.size());
        for (final List<Integer> currList : airQuality) {
            double sum = 0;
            for (final Integer x : currList) {
                sum += x;
            }
            aggregated.add((int) sum / currList.size());
        }

        return aggregated;
    }

    /**
     * Get daily (based on local time) average air quality raw.
     */
    @Override
    public Response<ImmutableList<Integer>> getAirQualityRawList(final Long accountId,
                                                                 final DeviceId deviceId,
                                                                 final DateTime startUTCTime,
                                                                 final DateTime endUTCTime,
                                                                 final DateTime startLocalTime,
                                                                 final DateTime endLocalTime)
    {
        final Set<DeviceDataAttribute> attributes = new ImmutableSet.Builder<DeviceDataAttribute>().addAll(BASE_ATTRIBUTES).add(DeviceDataAttribute.AMBIENT_AIR_QUALITY_RAW).build();
        final Response<ImmutableList<DeviceData>> response = getBetweenLocalTime(accountId, deviceId, startUTCTime, endUTCTime, startLocalTime, endLocalTime, attributes);

        final List<Integer> aggregated = averageDailyAirQualityRaw(response.data);
        return new Response<>(ImmutableList.copyOf(aggregated), response.status, response.exception);
    }

    private Response<List<Map<String, AttributeValue>>> getBetweenHourDateByTS(final Long accountId,
                                                                               final DeviceId deviceId,
                                                                               final DateTime startUTCTime,
                                                                               final DateTime endUTCTime,
                                                                               final DateTime startLocalTime,
                                                                               final DateTime endLocalTime,
                                                                               final int startHour,
                                                                               final int endHour,
                                                                               final boolean sameDay)
    {
        final Response<List<Map<String, AttributeValue>>> response = getItemsBetweenLocalTime(accountId, deviceId, startUTCTime, endUTCTime, startLocalTime, endLocalTime, ALL_ATTRIBUTES);
        final List<Map<String, AttributeValue>> filtered = Lists.newArrayList();
        for (final Map<String, AttributeValue> result: response.data) {
            final int hourOfDay = timestampFromDDBItem(result).plusMillis(DeviceDataAttribute.OFFSET_MILLIS.getInteger(result)).getHourOfDay();
            if (sameDay && (hourOfDay >= startHour && hourOfDay < endHour) ||
                    (!sameDay && (hourOfDay >= startHour || hourOfDay < endHour))) {
                filtered.add(result);
            }
        }
        return Response.into(filtered, response);
    }

    /**
     * Get DeviceDatas that fall within [startHour, endHour) where endHour > startHour.
     */
    @Override
    public Response<ImmutableList<DeviceData>> getBetweenHourDateByTSSameDay(final Long accountId,
                                                                             final DeviceId deviceId,
                                                                             final DateTime startUTCTime,
                                                                             final DateTime endUTCTime,
                                                                             final DateTime startLocalTime,
                                                                             final DateTime endLocalTime,
                                                                             final int startHour,
                                                                             final int endHour)
    {
        final Response<List<Map<String, AttributeValue>>> response = getBetweenHourDateByTS(accountId, deviceId, startUTCTime, endUTCTime, startLocalTime, endLocalTime, startHour, endHour, true);
        final ImmutableList<DeviceData> data = ImmutableList.copyOf(attributeMapsToDeviceDataList(response.data));
        return Response.into(data, response);
    }

    /**
     *  Get DeviceDatas that are >= startHour or < endHour, where startHour > endHour (because it's from the prev night)
     */
    @Override
    public Response<ImmutableList<DeviceData>> getBetweenHourDateByTS(final Long accountId,
                                                                      final DeviceId deviceId,
                                                                      final DateTime startUTCTime,
                                                                      final DateTime endUTCTime,
                                                                      final DateTime startLocalTime,
                                                                      final DateTime endLocalTime,
                                                                      final int startHour,
                                                                      final int endHour)
    {
        final Response<List<Map<String, AttributeValue>>> response = getBetweenHourDateByTS(accountId, deviceId, startUTCTime, endUTCTime, startLocalTime, endLocalTime, startHour, endHour, false);
        final ImmutableList<DeviceData> data = ImmutableList.copyOf(attributeMapsToDeviceDataList(response.data));
        return Response.into(data, response);
    }

    /**
     * Same as getBetweenHourDateByTS, but aggregated to slotDuration minutes.
     */
    @Override
    public Response<ImmutableList<DeviceData>> getBetweenByLocalHourAggregateBySlotDuration(final Long accountId,
                                                                                            final DeviceId deviceId,
                                                                                            final DateTime start,
                                                                                            final DateTime end,
                                                                                            final DateTime startLocal,
                                                                                            final DateTime endLocal,
                                                                                            int startHour,
                                                                                            int endHour,
                                                                                            final Integer slotDuration) {
        final Response<List<Map<String, AttributeValue>>> response = getBetweenHourDateByTS(accountId, deviceId, start, end, startLocal, endLocal, startHour, endHour, false);
        final ImmutableList<DeviceData> data = ImmutableList.copyOf(aggregateDynamoDBItemsToDeviceData(response.data, slotDuration));
        return Response.into(data, response);
    }
    //endregion

    //region migration
    /**
     * Write batch without retries, simply return unsuccessful inserts. (used by data migration)
     * @param deviceDataList -- always match batch write item size
     */
    public List<DeviceData> batchInsertReturnsRemaining(final List<DeviceData> deviceDataList) {
        final List<Map<String, AttributeValue>> remainingItems = batchInsertNoRetryReturnsRemaining(deviceDataList);
        final List<DeviceData> remainingDeviceData = Lists.newArrayListWithCapacity(remainingItems.size());
        for (final Map<String, AttributeValue> item : remainingItems) {
            remainingDeviceData.add(dynamoItemToRawDeviceData(item));
        }
        return remainingDeviceData;
    }

    private DeviceData dynamoItemToRawDeviceData(final Map<String, AttributeValue> item) {
        final String externalDeviceId = externalDeviceIdFromDDBItem(item);
        final DateTime dateTimeUTC = timestampFromDDBItem(item);

        final int temp = DeviceDataAttribute.AMBIENT_TEMP.getInteger(item);
        final int humid = DeviceDataAttribute.AMBIENT_HUMIDITY.getInteger(item);
        final int dust = DeviceDataAttribute.AMBIENT_AIR_QUALITY_RAW.getInteger(item);
        final int light = DeviceDataAttribute.AMBIENT_LIGHT.getInteger(item);
        final int lightVar = DeviceDataAttribute.AMBIENT_LIGHT_VARIANCE.getInteger(item);

        final Integer offsetMillis = DeviceDataAttribute.OFFSET_MILLIS.getInteger(item);
        final Integer waveCount = DeviceDataAttribute.WAVE_COUNT.getInteger(item);
        final Integer holdCount = DeviceDataAttribute.HOLD_COUNT.getInteger(item);
        final Integer audioNum = DeviceDataAttribute.AUDIO_NUM_DISTURBANCES.getInteger(item);
        final Integer audioPeak = DeviceDataAttribute.AUDIO_PEAK_DISTURBANCES_DB.getInteger(item);
        final Integer audioBG = DeviceDataAttribute.AUDIO_PEAK_BACKGROUND_DB.getInteger(item);
        final Integer audioPeakEnergy = DeviceDataAttribute.AUDIO_PEAK_ENERGY_DB.getInteger(item);

        return new DeviceData.Builder()
                .withAccountId(Long.valueOf(DeviceDataAttribute.ACCOUNT_ID.get(item).getN()))
                .withExternalDeviceId(externalDeviceId)
                .withAmbientTemperature(temp)
                .withAmbientLight(light)
                .withAmbientLightVariance(lightVar)
                .withAmbientHumidity(humid)
                .withAmbientAirQualityRaw(dust)
                .withAlreadyCalibratedAudioPeakBackgroundDB(audioBG)    // use data as-is
                .withAlreadyCalibratedAudioPeakDisturbancesDB(audioPeak)
                .withAlreadyCalibratedPeakEnergyDB(audioPeakEnergy)
                .withAudioNumDisturbances(audioNum)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(dateTimeUTC)
                .withWaveCount(waveCount)
                .withHoldCount(holdCount)
                .build();

    }
    //endregion
}