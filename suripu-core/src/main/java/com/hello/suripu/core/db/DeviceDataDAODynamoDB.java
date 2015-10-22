package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.util.Bucketing;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.db.util.DynamoDBItemAggregator;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.yammer.metrics.annotation.Timed;
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
 */
public class DeviceDataDAODynamoDB implements DeviceDataIngestDAO {
    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceDataDAODynamoDB.class);

    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public static final class AttributeNames {
        public static final String DEVICE_ID                = "device_id";
        public static final String TIMESTAMP                = "timestamp";
        public static final String ACCOUNT_ID               = "account_id";
        public static final String AMBIENT_TEMP             = "ambient_temp";
        public static final String AMBIENT_LIGHT            = "ambient_light";
        public static final String AMBIENT_LIGHT_VARIANCE   = "ambient_light_variance";
        public static final String AMBIENT_LIGHT_PEAKINESS  = "ambient_light_peakiness";
        public static final String AMBIENT_HUMIDITY         = "ambient_humidity";
        public static final String AMBIENT_AIR_QUALITY      = "ambient_air_quality";
        public static final String AMBIENT_AIR_QUALITY_RAW  = "ambient_air_quality_raw";
        public static final String AMBIENT_DUST_VARIANCE    = "ambient_dust_variance";
        public static final String AMBIENT_DUST_MIN         = "ambient_dust_min";
        public static final String AMBIENT_DUST_MAX         = "ambient_dust_max";
        public static final String LOCAL_UTC_TIMESTAMP      = "local_utc_ts";
        public static final String OFFSET_MILLIS            = "offset_millis";
    }

    private static final int MAX_PUT_ITEMS = 25;
    private static final int MAX_BATCH_WRITE_ATTEMPTS = 5;
    private static final int MAX_QUERY_ATTEMPTS = 5;

    private static final String RANGE_KEY_NAME = "account_id:timestamp";

    // Store everything to the minute level
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");

    private static final ImmutableMap<String, String> ATTRIBUTE_TYPES = new ImmutableMap.Builder<String, String>()
            .put(AttributeNames.DEVICE_ID, "N")
            .put(AttributeNames.TIMESTAMP, "S")
            .put(AttributeNames.ACCOUNT_ID, "N")
            .put(AttributeNames.AMBIENT_TEMP, "N")
            .put(AttributeNames.AMBIENT_LIGHT, "N")
            .put(AttributeNames.AMBIENT_LIGHT_VARIANCE, "N")
            .put(AttributeNames.AMBIENT_LIGHT_PEAKINESS, "N")
            .put(AttributeNames.AMBIENT_HUMIDITY, "N")
            .put(AttributeNames.AMBIENT_AIR_QUALITY, "N")
            .put(AttributeNames.AMBIENT_AIR_QUALITY_RAW, "N")
            .put(AttributeNames.AMBIENT_DUST_VARIANCE, "N")
            .put(AttributeNames.AMBIENT_DUST_MIN, "N")
            .put(AttributeNames.AMBIENT_DUST_MAX, "N")
            .put(AttributeNames.LOCAL_UTC_TIMESTAMP, "S")
            .put(AttributeNames.OFFSET_MILLIS, "N")
            .build();


    public DeviceDataDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient) {
        // attributes
        ArrayList<AttributeDefinition> attributes = Lists.newArrayList();
        attributes.add(new AttributeDefinition().withAttributeName(AttributeNames.DEVICE_ID).withAttributeType("N"));
        attributes.add(new AttributeDefinition().withAttributeName(RANGE_KEY_NAME).withAttributeType("S"));

        // keys
        ArrayList<KeySchemaElement> keySchema = Lists.newArrayList();
        keySchema.add(new KeySchemaElement().withAttributeName(AttributeNames.DEVICE_ID).withKeyType(KeyType.HASH));
        keySchema.add(new KeySchemaElement().withAttributeName(RANGE_KEY_NAME).withKeyType(KeyType.RANGE));

        // throughput provision
        ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L);

        final CreateTableRequest request = new CreateTableRequest()
                .withTableName(tableName)
                .withAttributeDefinitions(attributes)
                .withKeySchema(keySchema)
                .withProvisionedThroughput(provisionedThroughput);

        return dynamoDBClient.createTable(request);
    }

    private AttributeValue dateTimeToAttributeValue(final DateTime dateTime) {
        return new AttributeValue(dateTime.toString(DATE_TIME_FORMATTER));
    }

    private AttributeValue getRangeKey(final Long accountId, final DateTime dateTime) {
        return new AttributeValue(String.valueOf(accountId) + ":" + dateTime.toString(DATE_TIME_FORMATTER));
    }

    private HashMap<String, AttributeValue> deviceDataToAttributeMap(final DeviceData data) {
        final HashMap<String, AttributeValue> item = Maps.newHashMap();
        item.put(AttributeNames.DEVICE_ID, new AttributeValue().withN(String.valueOf(data.deviceId)));
        item.put(RANGE_KEY_NAME, getRangeKey(data.accountId, data.dateTimeUTC));
        item.put(AttributeNames.TIMESTAMP, dateTimeToAttributeValue(data.dateTimeUTC));
        item.put(AttributeNames.ACCOUNT_ID, new AttributeValue().withN(String.valueOf(data.accountId)));
        item.put(AttributeNames.AMBIENT_TEMP, new AttributeValue().withN(String.valueOf(data.ambientTemperature)));
        item.put(AttributeNames.AMBIENT_LIGHT, new AttributeValue().withN(String.valueOf(data.ambientLight)));
        item.put(AttributeNames.AMBIENT_LIGHT_VARIANCE, new AttributeValue().withN(String.valueOf(data.ambientLightVariance)));
        item.put(AttributeNames.AMBIENT_LIGHT_PEAKINESS, new AttributeValue().withN(String.valueOf(data.ambientLightPeakiness)));
        item.put(AttributeNames.AMBIENT_HUMIDITY, new AttributeValue().withN(String.valueOf(data.ambientHumidity)));
        item.put(AttributeNames.AMBIENT_AIR_QUALITY, new AttributeValue().withN(String.valueOf(data.ambientAirQuality)));
        item.put(AttributeNames.AMBIENT_AIR_QUALITY_RAW, new AttributeValue().withN(String.valueOf(data.ambientAirQualityRaw)));
        item.put(AttributeNames.AMBIENT_DUST_VARIANCE, new AttributeValue().withN(String.valueOf(data.ambientDustVariance)));
        item.put(AttributeNames.AMBIENT_DUST_MIN, new AttributeValue().withN(String.valueOf(data.ambientDustMin)));
        item.put(AttributeNames.AMBIENT_DUST_MAX, new AttributeValue().withN(String.valueOf(data.ambientDustMax)));
        item.put(AttributeNames.LOCAL_UTC_TIMESTAMP, dateTimeToAttributeValue(data.dateTimeUTC.plusMillis(data.offsetMillis)));
        item.put(AttributeNames.OFFSET_MILLIS, new AttributeValue().withN(String.valueOf(data.offsetMillis)));
        return item;
    }

    private DeviceData attributeMapToDeviceData(final Map<String, AttributeValue> item) {
        return new DeviceData.Builder()
                .withDeviceId(Long.valueOf(item.get(AttributeNames.DEVICE_ID).getN()))
                .withDateTimeUTC(DateTime.parse(item.get(AttributeNames.TIMESTAMP).getS(), DATE_TIME_FORMATTER))
                .withAccountId(Long.valueOf(item.get(AttributeNames.ACCOUNT_ID).getN()))
                .withAmbientTemperature(Integer.valueOf(item.get(AttributeNames.AMBIENT_TEMP).getN()))
                .withAmbientLight(Integer.valueOf(item.get(AttributeNames.AMBIENT_LIGHT).getN()))
                .withAmbientLightVariance(Integer.valueOf(item.get(AttributeNames.AMBIENT_LIGHT_VARIANCE).getN()))
                .withAmbientLightPeakiness(Integer.valueOf(item.get(AttributeNames.AMBIENT_LIGHT_PEAKINESS).getN()))
                .withAmbientHumidity(Integer.valueOf(item.get(AttributeNames.AMBIENT_HUMIDITY).getN()))
                .withAmbientAirQualityRaw(Integer.valueOf(item.get(AttributeNames.AMBIENT_AIR_QUALITY_RAW).getN()))
                .withAmbientDustVariance(Integer.valueOf(item.get(AttributeNames.AMBIENT_DUST_VARIANCE).getN()))
                .withAmbientDustMin(Integer.valueOf(item.get(AttributeNames.AMBIENT_DUST_MIN).getN()))
                .withAmbientDustMax(Integer.valueOf(item.get(AttributeNames.AMBIENT_DUST_MAX).getN()))
                .withOffsetMillis(Integer.valueOf(item.get(AttributeNames.OFFSET_MILLIS).getN()))
                .build();
    }

    private void backoff(int numberOfAttempts) {
        try {
            long sleepMillis = (long) Math.pow(2, numberOfAttempts) * 50;
            LOGGER.warn("Throttled by DynamoDB, sleeping for {} ms.", sleepMillis);
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while attempting exponential backoff.");
        }
    }

    /**
     * Batch insert list of DeviceData objects.
     * Subject to DynamoDB's maximum BatchWriteItem size.
     * @param deviceDataList
     * @return The number of successfully inserted elements.
     */
    public int batchInsert(final List<DeviceData> deviceDataList) {
        // Create a map with hash+range as the key to deduplicate and avoid DynamoDB exceptions
        final Map<String, WriteRequest> writeRequestMap = Maps.newHashMap();
        for (final DeviceData data: deviceDataList) {
            final Map<String, AttributeValue> item = deviceDataToAttributeMap(data);
            final String hashAndRangeKey = item.get(AttributeNames.DEVICE_ID).getN() + item.get(RANGE_KEY_NAME).getS();
            final WriteRequest request = new WriteRequest().withPutRequest(new PutRequest().withItem(item));
            writeRequestMap.put(hashAndRangeKey, request);
        }

        Map<String, List<WriteRequest>> requestItems = Maps.newHashMapWithExpectedSize(1);
        requestItems.put(this.tableName, Lists.newArrayList(writeRequestMap.values()));

        int numAttempts = 0;

        do {
            if (numAttempts > 0) {
                // Being throttled! Back off, buddy.
                backoff(numAttempts);
            }

            numAttempts++;
            final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest().withRequestItems(requestItems);
            final BatchWriteItemResult result = this.dynamoDBClient.batchWriteItem(batchWriteItemRequest);
            // check for unprocessed items
            requestItems = result.getUnprocessedItems();
        } while (!requestItems.isEmpty() && (numAttempts < MAX_BATCH_WRITE_ATTEMPTS));

        if (!requestItems.isEmpty()) {
            LOGGER.warn("Exceeded {} attempts to batch write to Dynamo. {} items left over.",
                    MAX_BATCH_WRITE_ATTEMPTS, requestItems.get(tableName).size());
            return writeRequestMap.size() - requestItems.get(tableName).size();
        }

        return writeRequestMap.size();
    }

    /**
     * Partitions and inserts list of DeviceData objects.
     * @param deviceDataList
     * @return The number of items that were successfully inserted
     */
    public int batchInsertAll(final List<DeviceData> deviceDataList) {
        final List<List<DeviceData>> deviceDataLists = Lists.partition(deviceDataList, MAX_PUT_ITEMS);
        int successfulInsertions = 0;

        // Insert each chunk
        for (final List<DeviceData> deviceDataListToWrite: deviceDataLists) {
            try {
                successfulInsertions += batchInsert(deviceDataListToWrite);
            } catch (AmazonClientException e) {
                LOGGER.error("Got exception while attempting to batchInsert to DynamoDB: {}", e);
            }

        }

        return successfulInsertions;
    }

    private DateTime getFloorOfDateTime(final DateTime dateTime, final Integer toMinutes) {
        return new DateTime(dateTime).withMinuteOfHour(dateTime.getMinuteOfHour() - (dateTime.getMinuteOfHour() % toMinutes));
    }

    private DateTime timestampFromDDBItem(final Map<String, AttributeValue> item) {
        return DateTime.parse(item.get(AttributeNames.TIMESTAMP).getS(), DATE_TIME_FORMATTER);
    }

    private DeviceData aggregateDynamoDBItemsToDeviceData(final List<Map<String, AttributeValue>> items, final DeviceData template) {
        final DynamoDBItemAggregator aggregator = new DynamoDBItemAggregator(items);
        return new DeviceData.Builder()
                .withAccountId(template.accountId)
                .withDeviceId(template.deviceId)
                .withDateTimeUTC(template.dateTimeUTC)
                .withOffsetMillis(template.offsetMillis)
                .withAmbientTemperature((int) aggregator.min(AttributeNames.AMBIENT_TEMP))
                .withAmbientLight((int) aggregator.roundedMean(AttributeNames.AMBIENT_LIGHT))
                .withAmbientLightVariance((int) aggregator.roundedMean(AttributeNames.AMBIENT_LIGHT_VARIANCE))
                .withAmbientLightPeakiness((int) aggregator.roundedMean(AttributeNames.AMBIENT_LIGHT_PEAKINESS))
                .withAmbientHumidity((int) aggregator.roundedMean(AttributeNames.AMBIENT_HUMIDITY))
                .withAmbientAirQualityRaw((int) aggregator.roundedMean(AttributeNames.AMBIENT_AIR_QUALITY_RAW))
                .withAmbientDustVariance((int) aggregator.roundedMean(AttributeNames.AMBIENT_DUST_VARIANCE))
                .withAmbientDustMin((int) aggregator.roundedMean(AttributeNames.AMBIENT_DUST_MIN))
                .withAmbientDustMax((int) aggregator.max(AttributeNames.AMBIENT_DUST_MAX))
                .build();
    }

    private List<DeviceData> aggregateDynamoDBItemsToDeviceData(final List<Map<String, AttributeValue>> items, final Integer slotDuration) {
        final List<DeviceData> resultList = Lists.newLinkedList();
        LinkedList<Map<String, AttributeValue>> currentWorkingList = Lists.newLinkedList();
        final DeviceData.Builder templateBuilder = new DeviceData.Builder();
        for (final Map<String, AttributeValue> item: items) {
            final DateTime itemDateTime = timestampFromDDBItem(item);
            if (currentWorkingList.isEmpty()) {
                // First iteration
                currentWorkingList.add(item);
            } else if (timestampFromDDBItem(currentWorkingList.getLast()).isAfter(itemDateTime)) {
                // Unsorted list
                throw new IllegalArgumentException("Input DeviceDatas must be sorted.");
            } else if (getFloorOfDateTime(timestampFromDDBItem(currentWorkingList.getLast()), slotDuration)
                    .plusMinutes(slotDuration)
                    .isAfter(itemDateTime)) {
                // Within the window of our current working set.
                currentWorkingList.add(item);
            } else {
                // Outside the window, aggregate working set to single value.
                resultList.add(aggregateDynamoDBItemsToDeviceData(currentWorkingList, templateBuilder.build()));

                // Create new working sets
                currentWorkingList = Lists.newLinkedList();
                currentWorkingList.add(item);
            }
            templateBuilder
                    .withDateTimeUTC(getFloorOfDateTime(itemDateTime, slotDuration))
                    .withAccountId(Long.valueOf(item.get(AttributeNames.ACCOUNT_ID).getN()))
                    .withOffsetMillis(Integer.valueOf(item.get(AttributeNames.OFFSET_MILLIS).getN()))
                    .withDeviceId(Long.valueOf(item.get(AttributeNames.DEVICE_ID).getN()));
        }
        if (!currentWorkingList.isEmpty()) {
            resultList.add(aggregateDynamoDBItemsToDeviceData(currentWorkingList, templateBuilder.build()));
        }
        return resultList;
    }

    private List<Map<String, AttributeValue>> query(final Map<String, Condition> queryConditions, final Collection<String> targetAttributes) {
        final List<Map<String, AttributeValue>> results = Lists.newArrayList();

        Map<String, AttributeValue> lastEvaluatedKey = null;
        int numAttempts = 0;
        boolean keepTrying = true;

        do {
            numAttempts++;
            final QueryRequest queryRequest = new QueryRequest()
                    .withTableName(this.tableName)
                    .withKeyConditions(queryConditions)
                    .withAttributesToGet(targetAttributes)
                    .withExclusiveStartKey(lastEvaluatedKey);

            final QueryResult queryResult;
            try {
                queryResult = this.dynamoDBClient.query(queryRequest);
            } catch (ProvisionedThroughputExceededException e) {
                backoff(numAttempts);
                continue;
            }
            final List<Map<String, AttributeValue>> items = queryResult.getItems();

            if (queryResult.getItems() != null) {
                for (final Map<String, AttributeValue> item : items) {
                    if (!item.keySet().containsAll(targetAttributes)) {
                        LOGGER.warn("Missing field in item {}", item);
                        continue;
                    }
                    results.add(item);
                }
            }

            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
            keepTrying = (lastEvaluatedKey != null);

        } while (keepTrying && (numAttempts < MAX_QUERY_ATTEMPTS));

        // TODO should actually probably throw an error or return a flag here if your query could not complete
        if (lastEvaluatedKey != null) {
            LOGGER.warn("Exceeded {} attempts while querying. Stopping with last evaluated key: {}",
                    MAX_QUERY_ATTEMPTS, lastEvaluatedKey);
        }

        return results;
    }

    public ImmutableList<DeviceData> getBetweenByAbsoluteTimeAggregateBySlotDuration(
            final Long deviceId,
            final Long accountId,
            final DateTime start,
            final DateTime end,
            final Integer slotDuration,
            final Collection<String> targetAttributes)
    {

        final Condition selectByDeviceId  = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(deviceId)));

        final Condition selectByTimestamp = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                .withAttributeValueList(
                        getRangeKey(accountId, start),
                        getRangeKey(accountId, end));

        final Map<String, Condition> queryConditions = Maps.newHashMap();
        queryConditions.put(AttributeNames.DEVICE_ID, selectByDeviceId);
        queryConditions.put(RANGE_KEY_NAME, selectByTimestamp);

        final List<Map<String, AttributeValue>> results = query(queryConditions, targetAttributes);

        return ImmutableList.copyOf(aggregateDynamoDBItemsToDeviceData(results, slotDuration));
    }


    public ImmutableList<DeviceData> getBetweenByAbsoluteTimeAggregateBySlotDuration(
            final Long deviceId,
            final Long accountId,
            final DateTime start,
            final DateTime end,
            final Integer slotDuration)
    {
        return getBetweenByAbsoluteTimeAggregateBySlotDuration(deviceId, accountId, start, end, slotDuration, ATTRIBUTE_TYPES.keySet());
    }

    private final static ImmutableSet<String> BASE_ATTRIBUTES = new ImmutableSet.Builder<String>()
            .add(AttributeNames.DEVICE_ID)
            .add(AttributeNames.ACCOUNT_ID)
            .add(AttributeNames.OFFSET_MILLIS)
            .add(AttributeNames.LOCAL_UTC_TIMESTAMP)
            .build();

    private final static Map<String, Set<String>> SENSOR_NAME_TO_ATTR_NAMES = new ImmutableMap.Builder<String, Set<String>>()
            .put("humidity", ImmutableSet.of(AttributeNames.AMBIENT_TEMP, AttributeNames.AMBIENT_HUMIDITY))
            .put("temperature", ImmutableSet.of(AttributeNames.AMBIENT_TEMP))
            // TODO store firmware version, audioPeakBackgroundDB, audioPeakDisturbancesDB
//            .put("particulates", ImmutableSet.of(AttributeNames.AMBIENT_AIR_QUALITY_RAW, AttributeNames.FIRMWARE_VERSION))
            .put("light", ImmutableSet.of(AttributeNames.AMBIENT_LIGHT))
//            .put("sound", ImmutableSet.of(AttributeNames.AUDIO_PEAK_BACKGROUND_DB, AttributeNames.AUDIO_PEAK_DISTURBANCES_DB))
            .build();

    private static Set<String> sensorNameToAttributeNames(final String sensorName) {
        final Set<String> sensorAttributes = SENSOR_NAME_TO_ATTR_NAMES.get(sensorName);
        if (sensorAttributes == null) {
            throw new IllegalArgumentException("Unknown sensor name: '" + sensorName + "'");
        }
        return new ImmutableSet.Builder<String>().addAll(BASE_ATTRIBUTES).addAll(sensorAttributes).build();
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
            final Optional<Calibration> calibrationOptional) throws IllegalArgumentException {

        final DateTime queryEndTime = new DateTime(queryEndTimestampInUTC, DateTimeZone.UTC);
        final DateTime queryStartTime = new DateTime(queryStartTimestampInUTC, DateTimeZone.UTC);

        LOGGER.trace("Client utcTimeStamp : {} ({})", queryEndTimestampInUTC, new DateTime(queryEndTimestampInUTC));
        LOGGER.trace("QueryEndTime: {} ({})", queryEndTime, queryEndTime.getMillis());
        LOGGER.trace("QueryStartTime: {} ({})", queryStartTime, queryStartTime.getMillis());

        final List<DeviceData> rows = getBetweenByAbsoluteTimeAggregateBySlotDuration(accountId, deviceId, queryStartTime, queryEndTime, slotDurationInMinutes, sensorNameToAttributeNames(sensor));
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
}
