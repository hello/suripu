package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.AttributeUtils;
import com.hello.suripu.core.db.dynamo.Expressions;
import com.hello.suripu.core.db.dynamo.expressions.Expression;
import com.hello.suripu.core.db.util.Bucketing;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.AllSensorSampleMap;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.db.util.DynamoDBItemAggregator;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.util.DateTimeUtil;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
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
public class DeviceDataDAODynamoDB implements DeviceDataIngestDAO, DeviceDataInsightQueryDAO {
    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceDataDAODynamoDB.class);

    private final Timer aggregationTimer;

    private final AmazonDynamoDB dynamoDBClient;
    private final String tablePrefix;

    public enum DeviceDataAttribute implements Attribute {
        ACCOUNT_ID ("aid", "N"),
        RANGE_KEY ("ts|dev", "S"),  // <utc_timestamp>|<external_device_id>
        AMBIENT_TEMP ("tmp", "N"),
        AMBIENT_LIGHT ("lite", "N"),
        AMBIENT_LIGHT_VARIANCE ("litevar", "N"),
        AMBIENT_HUMIDITY ("hum", "N"),
        AMBIENT_AIR_QUALITY_RAW ("aqr", "N"),
        AUDIO_PEAK_BACKGROUND_DB ("apbg", "N"),
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
         * @param item
         * @return
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
            .put("sound", ImmutableSet.of(DeviceDataAttribute.AUDIO_PEAK_BACKGROUND_DB, DeviceDataAttribute.AUDIO_PEAK_DISTURBANCES_DB))
            .build();

    public final static ImmutableSet<DeviceDataAttribute> ALL_ATTRIBUTES = ImmutableSet.copyOf(DeviceDataAttribute.values());

    private static final int MAX_PUT_ITEMS = 25;
    private static final int MAX_BATCH_WRITE_ATTEMPTS = 5;
    private static final int MAX_QUERY_ATTEMPTS = 5;

    // Store everything to the minute level
    private static final DateTimeFormatter DATE_TIME_READ_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:00Z");
    private static final String DATE_TIME_STRING_TEMPLATE = "yyyy-MM-dd HH:mm";
    private static final DateTimeFormatter DATE_TIME_WRITE_FORMATTER = DateTimeFormat.forPattern(DATE_TIME_STRING_TEMPLATE);

    public DeviceDataDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tablePrefix) {
        this.dynamoDBClient = dynamoDBClient;
        this.tablePrefix = tablePrefix;
        this.aggregationTimer = Metrics.defaultRegistry().newTimer(DeviceDataDAODynamoDB.class, "aggregation");
    }

    public String getTableName(final DateTime dateTime) {
        final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy_MM");
        return tablePrefix + "_" + dateTime.toString(formatter);
    }

    /**
     * Return table names from start to end, in chronological order.
     * @param start
     * @param end
     * @return
     */
    public List<String> getTableNames(final DateTime start, final DateTime end) {
        final LinkedList<String> names = Lists.newLinkedList();
        for (final DateTime dateTime: DateTimeUtil.dateTimesForStartOfMonthBetweenDates(start, end)) {
            final String tableName = getTableName(dateTime);
            names.add(tableName);
        }
        return names;
    }

    public CreateTableResult createTable(final String tableName) {
        final DeviceDataAttribute hashKeyAttribute = DeviceDataAttribute.ACCOUNT_ID;
        final DeviceDataAttribute rangeKeyAttribute = DeviceDataAttribute.RANGE_KEY;

        // attributes
        ArrayList<AttributeDefinition> attributes = Lists.newArrayList();
        attributes.add(new AttributeDefinition().withAttributeName(hashKeyAttribute.name).withAttributeType(hashKeyAttribute.type));
        attributes.add(new AttributeDefinition().withAttributeName(rangeKeyAttribute.name).withAttributeType(rangeKeyAttribute.type));

        // keys
        ArrayList<KeySchemaElement> keySchema = Lists.newArrayList();
        keySchema.add(new KeySchemaElement().withAttributeName(hashKeyAttribute.name).withKeyType(KeyType.HASH));
        keySchema.add(new KeySchemaElement().withAttributeName(rangeKeyAttribute.name).withKeyType(KeyType.RANGE));

        // throughput provision
        // TODO make this configurable
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

    private static HashMap<String, AttributeValue> deviceDataToAttributeMap(final DeviceData data) {
        final HashMap<String, AttributeValue> item = Maps.newHashMap();
        item.put(DeviceDataAttribute.ACCOUNT_ID.name, toAttributeValue(data.accountId));
        item.put(DeviceDataAttribute.RANGE_KEY.name, getRangeKey(data.dateTimeUTC, data.externalDeviceId));
        item.put(DeviceDataAttribute.AMBIENT_TEMP.name, toAttributeValue(data.ambientTemperature));
        item.put(DeviceDataAttribute.AMBIENT_LIGHT.name, toAttributeValue(data.ambientLight));
        item.put(DeviceDataAttribute.AMBIENT_LIGHT_VARIANCE.name, toAttributeValue(data.ambientLightVariance));
        item.put(DeviceDataAttribute.AMBIENT_HUMIDITY.name, toAttributeValue(data.ambientHumidity));
        item.put(DeviceDataAttribute.AMBIENT_AIR_QUALITY_RAW.name, toAttributeValue(data.ambientAirQualityRaw));
        item.put(DeviceDataAttribute.AUDIO_PEAK_BACKGROUND_DB.name, toAttributeValue(data.audioPeakBackgroundDB));
        item.put(DeviceDataAttribute.AUDIO_PEAK_DISTURBANCES_DB.name, toAttributeValue(data.audioPeakDisturbancesDB));
        item.put(DeviceDataAttribute.AUDIO_NUM_DISTURBANCES.name, toAttributeValue(data.audioNumDisturbances));
        item.put(DeviceDataAttribute.WAVE_COUNT.name, toAttributeValue(data.waveCount));
        item.put(DeviceDataAttribute.HOLD_COUNT.name, toAttributeValue(data.holdCount));
        item.put(DeviceDataAttribute.LOCAL_UTC_TIMESTAMP.name, dateTimeToAttributeValue(data.dateTimeUTC.plusMillis(data.offsetMillis)));
        item.put(DeviceDataAttribute.OFFSET_MILLIS.name, toAttributeValue(data.offsetMillis));
        return item;
    }

    private static void backoff(int numberOfAttempts) {
        try {
            long sleepMillis = (long) Math.pow(2, numberOfAttempts) * 50;
            LOGGER.warn("Throttled by DynamoDB, sleeping for {} ms.", sleepMillis);
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while attempting exponential backoff.");
        }
    }

    /**
     * Convert a list of DeviceData to write request items, which can be used for batchInsert.
     * @param deviceDataList
     * @return Map of {tableName => writeRequests}
     */
    public Map<String, List<WriteRequest>> toWriteRequestItems(final List<DeviceData> deviceDataList) {
        // Create a map with hash+range as the key to deduplicate and avoid DynamoDB exceptions
        final Map<String, Map<String, WriteRequest>> writeRequestMap = Maps.newHashMap();
        for (final DeviceData data: deviceDataList) {
            final String tableName = getTableName(data.dateTimeUTC);
            final Map<String, AttributeValue> item = deviceDataToAttributeMap(data);
            final String hashAndRangeKey = item.get(DeviceDataAttribute.ACCOUNT_ID.name).getN() + item.get(DeviceDataAttribute.RANGE_KEY.name).getS();
            final WriteRequest request = new WriteRequest().withPutRequest(new PutRequest().withItem(item));
            if (writeRequestMap.containsKey(tableName)) {
                writeRequestMap.get(tableName).put(hashAndRangeKey, request);
            } else {
                final Map<String, WriteRequest> newMap = Maps.newHashMap();
                newMap.put(hashAndRangeKey, request);
                writeRequestMap.put(tableName, newMap);
            }
        }

        final Map<String, List<WriteRequest>> requestItems = Maps.newHashMapWithExpectedSize(writeRequestMap.size());
        for (final Map.Entry<String, Map<String, WriteRequest>> entry : writeRequestMap.entrySet()) {
            requestItems.put(entry.getKey(), Lists.newArrayList(entry.getValue().values()));
        }

        return requestItems;
    }

    /**
     * Batch insert write requests.
     * Subject to DynamoDB's maximum BatchWriteItem size.
     * Utilizes exponential backoff in case of throttling.
     *
     * @param requestItems - map of {tableName => writeRequests}
     * @return the remaining unprocessed items. The return value of this method can be used to call this method again
     * to process remaining items.
     */
    public Map<String, List<WriteRequest>> batchInsert(final Map<String, List<WriteRequest>> requestItems) {
        int numAttempts = 0;
        Map<String, List<WriteRequest>> remainingItems = requestItems;

        do {
            if (numAttempts > 0) {
                // Being throttled! Back off, buddy.
                backoff(numAttempts);
            }

            numAttempts++;
            final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest().withRequestItems(remainingItems);
            final BatchWriteItemResult result = this.dynamoDBClient.batchWriteItem(batchWriteItemRequest);
            // check for unprocessed items
            remainingItems = result.getUnprocessedItems();
        } while (!remainingItems.isEmpty() && (numAttempts < MAX_BATCH_WRITE_ATTEMPTS));

        return remainingItems;
    }

    private int countWriteRequestItems(final Map<String, List<WriteRequest>> requestItems) {
        int total = 0;
        for (final List<WriteRequest> writeRequests : requestItems.values()) {
            total += writeRequests.size();
        }
        return total;
    }

    /**
     * Batch insert list of DeviceData objects.
     * Subject to DynamoDB's maximum BatchWriteItem size.
     * @param deviceDataList
     * @return The number of successfully inserted elements.
     */
    public int batchInsert(final List<DeviceData> deviceDataList) {

        final Map<String, List<WriteRequest>> requestItems = toWriteRequestItems(deviceDataList);
        final Map<String, List<WriteRequest>> remainingItems = batchInsert(requestItems);

        final int totalItemsToInsert = countWriteRequestItems(requestItems);

        if (!remainingItems.isEmpty()) {
            final int remainingItemsCount = countWriteRequestItems(remainingItems);
            LOGGER.warn("Exceeded {} attempts to batch write to Dynamo. {} items left over.",
                    MAX_BATCH_WRITE_ATTEMPTS, remainingItemsCount);
            return totalItemsToInsert - remainingItemsCount;
        }

        return totalItemsToInsert;
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

    @Override
    public Class name() {
        return  DeviceDataDAODynamoDB.class;
    }

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

    private List<Map<String, AttributeValue>> query(final String tableName,
                                                    final String keyConditionExpression,
                                                    final Collection<DeviceDataAttribute> targetAttributes,
                                                    final Optional<String> filterExpression,
                                                    final Map<String, AttributeValue> filterAttributeValues)
    {
        final List<Map<String, AttributeValue>> results = Lists.newArrayList();

        Map<String, AttributeValue> lastEvaluatedKey = null;
        int numAttempts = 0;
        boolean keepTrying = true;

        final Map<String, String> expressionAttributeNames = AttributeUtils.getExpressionAttributeNames(targetAttributes);
        final String projectionExpression = AttributeUtils.getProjectionExpression(targetAttributes);

        do {
            numAttempts++;
            final QueryRequest queryRequest = new QueryRequest()
                    .withTableName(tableName)
                    .withProjectionExpression(projectionExpression)
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withKeyConditionExpression(keyConditionExpression)
                    .withExpressionAttributeValues(filterAttributeValues)
                    .withExclusiveStartKey(lastEvaluatedKey);

            if (filterExpression.isPresent()) {
                queryRequest.setFilterExpression(filterExpression.get());
            }

            final QueryResult queryResult;
            try {
                queryResult = this.dynamoDBClient.query(queryRequest);
            } catch (ProvisionedThroughputExceededException ptee) {
                backoff(numAttempts);
                continue;
            } catch (ResourceNotFoundException rnfe) {
                // Invalid table name
                LOGGER.error("Got ResourceNotFoundException while attempting to read from table {}; {}", tableName, rnfe);
                break;
            }
            final List<Map<String, AttributeValue>> items = queryResult.getItems();

            if (queryResult.getItems() != null) {
                for (final Map<String, AttributeValue> item : items) {
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
            final Long accountId,
            final String externalDeviceId,
            final DateTime start,
            final DateTime end,
            final Integer slotDuration,
            final Collection<DeviceDataAttribute> targetAttributes)
    {
        final Expression keyConditionExpression = Expressions.and(
                Expressions.compare(DeviceDataAttribute.ACCOUNT_ID, "=", toAttributeValue(accountId)),
                Expressions.between(DeviceDataAttribute.RANGE_KEY, getRangeKey(start, externalDeviceId), getRangeKey(end, externalDeviceId)));

        final List<Map<String, AttributeValue>> results = queryTables(getTableNames(start, end), keyConditionExpression, targetAttributes);
        final List<Map<String, AttributeValue>> filteredResults = Lists.newLinkedList();
        for (final Map<String, AttributeValue> item : results) {
            if (externalDeviceIdFromDDBItem(item).equals(externalDeviceId)) {
                filteredResults.add(item);
            }
        }

        final TimerContext context = aggregationTimer.time();
        try {
            return ImmutableList.copyOf(aggregateDynamoDBItemsToDeviceData(filteredResults, slotDuration));
        } finally {
            context.stop();
        }
    }

    /**
     * Same as the method that accepts targetAttributes, but defaults to getting all attributes.
     * @param accountId
     * @param externalDeviceId
     * @param start
     * @param end
     * @param slotDuration
     * @return
     */
    public ImmutableList<DeviceData> getBetweenByAbsoluteTimeAggregateBySlotDuration(
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
            final Optional<Calibration> calibrationOptional) throws IllegalArgumentException {

        final DateTime queryEndTime = timestampToDateTimeUTC(queryEndTimestampInUTC);
        final DateTime queryStartTime = timestampToDateTimeUTC(queryStartTimestampInUTC);

        LOGGER.trace("Client utcTimeStamp : {} ({})", queryEndTimestampInUTC, queryEndTime);
        LOGGER.trace("QueryEndTime: {} ({})", queryEndTime, queryEndTime.getMillis());
        LOGGER.trace("QueryStartTime: {} ({})", queryStartTime, queryStartTime.getMillis());

        LOGGER.debug("Calling getBetweenByAbsoluteTimeAggregateBySlotDuration with arguments: ({}, {}, {}, {}, {}, {})", accountId, externalDeviceId, queryStartTime, queryEndTime, slotDurationInMinutes, sensorNameToAttributeNames(sensor));
        final List<DeviceData> rows = getBetweenByAbsoluteTimeAggregateBySlotDuration(accountId, externalDeviceId, queryStartTime, queryEndTime, slotDurationInMinutes, sensorNameToAttributeNames(sensor));
        LOGGER.debug("Retrieved {} rows from database", rows.size());

        if(rows.isEmpty()) {
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
            LOGGER.debug("Map not populated, returning empty list of samples.");
            return Collections.EMPTY_LIST;
        }

        // Override map with values from DB
        final Map<Long, Sample> merged = Bucketing.mergeResults(map, optionalPopulatedMap.get());

        LOGGER.trace("New map size = {}", merged.size());

        final List<Sample> sortedList = Bucketing.sortResults(merged, currentOffsetMillis);
        return sortedList;

    }

    public AllSensorSampleList generateTimeSeriesByUTCTimeAllSensors(
            final Long queryStartTimestampInUTC,
            final Long queryEndTimestampInUTC,
            final Long accountId,
            final String externalDeviceId,
            final int slotDurationInMinutes,
            final Integer missingDataDefaultValue,
            final Optional<Device.Color> color,
            final Optional<Calibration> calibrationOptional) {

        // queryEndTime is in UTC. If local now is 8:04pm in PDT, we create a utc timestamp in 8:04pm UTC
        final DateTime queryEndTime = timestampToDateTimeUTC(queryEndTimestampInUTC);
        final DateTime queryStartTime = timestampToDateTimeUTC(queryStartTimestampInUTC);

        LOGGER.trace("Client utcTimeStamp : {} ({})", queryEndTimestampInUTC, queryEndTime);
        LOGGER.trace("QueryEndTime: {} ({})", queryEndTime, queryEndTime.getMillis());
        LOGGER.trace("QueryStartTime: {} ({})", queryStartTime, queryStartTime.getMillis());

        final List<DeviceData> rows = getBetweenByAbsoluteTimeAggregateBySlotDuration(accountId, externalDeviceId, queryStartTime, queryEndTime, slotDurationInMinutes);
        LOGGER.trace("Retrieved {} rows from database", rows.size());

        final AllSensorSampleList sensorDataResults = new AllSensorSampleList();

        if(rows.isEmpty()) {
            return sensorDataResults;
        }

        final AllSensorSampleMap allSensorSampleMap = Bucketing.populateMapAll(rows, color, calibrationOptional);

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

    private Optional<QueryResult> queryWithBackoff(final QueryRequest queryRequest, final int numAttempts) {
        try {
            final QueryResult queryResult = dynamoDBClient.query(queryRequest);
            return Optional.of(queryResult);
        } catch (ProvisionedThroughputExceededException e) {
            backoff(numAttempts);
        }
        return Optional.absent();
    }

    final DeviceData attributeMapToDeviceData(final Map<String, AttributeValue> item) {
        return new DeviceData.Builder()
                .withDateTimeUTC(timestampFromDDBItem(item))
                .withAccountId(Long.valueOf(DeviceDataAttribute.ACCOUNT_ID.get(item).getN()))
                .withExternalDeviceId(externalDeviceIdFromDDBItem(item))
                .withOffsetMillis(DeviceDataAttribute.OFFSET_MILLIS.getInteger(item))
                .withAmbientTemperature(DeviceDataAttribute.AMBIENT_TEMP.getInteger(item))
                .withAmbientLight(DeviceDataAttribute.AMBIENT_LIGHT.getInteger(item))
                .withAmbientLightVariance(DeviceDataAttribute.AMBIENT_LIGHT_VARIANCE.getInteger(item))
                .withAmbientHumidity(DeviceDataAttribute.AMBIENT_HUMIDITY.getInteger(item))
                .withWaveCount(DeviceDataAttribute.WAVE_COUNT.getInteger(item))
                .withHoldCount(DeviceDataAttribute.HOLD_COUNT.getInteger(item))
                .withAudioNumDisturbances(DeviceDataAttribute.AUDIO_NUM_DISTURBANCES.getInteger(item))
                .withAudioPeakBackgroundDB(DeviceDataAttribute.AUDIO_PEAK_BACKGROUND_DB.getInteger(item))
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

    /**
     * Get the most recent DeviceData for the accountId, in the same shard as now.
     * @param accountId
     * @param now
     * @return Optional of the latest DeviceData, or Optional.absent() if data not present or query fails.
     */
    public Optional<DeviceData> getMostRecent(final Long accountId, final DateTime now) {
        final Expression keyConditionExpression = Expressions.compare(DeviceDataAttribute.ACCOUNT_ID, "=", toAttributeValue(accountId));
        final Collection<DeviceDataAttribute> attributes = ALL_ATTRIBUTES;
        final String tableName = getTableName(now);

        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(tableName)
                .withKeyConditionExpression(keyConditionExpression.getExpressionString())
                .withProjectionExpression(AttributeUtils.getProjectionExpression(attributes))
                .withExpressionAttributeNames(AttributeUtils.getExpressionAttributeNames(attributes))
                .withExpressionAttributeValues(keyConditionExpression.getExpressionAttributeValues())
                .withScanIndexForward(false)
                .withLimit(1);

        int numAttempts = 0;
        Optional<QueryResult> queryResultOptional = Optional.absent();
        while ((numAttempts < MAX_QUERY_ATTEMPTS) && !queryResultOptional.isPresent()) {
            numAttempts++;
            queryResultOptional = queryWithBackoff(queryRequest, numAttempts);
        }

        if (queryResultOptional.isPresent()) {
            final List<Map<String, AttributeValue>> items = queryResultOptional.get().getItems();
            if (!items.isEmpty()) {
                final DeviceData deviceData = attributeMapToDeviceData(items.get(0));
                return Optional.of(deviceData);
            }
        }

        return Optional.absent();
    }
    
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
        final Optional<DeviceData> mostRecentDeviceDataByAccountId = getMostRecent(accountId, maxTsLimit);
        if (mostRecentDeviceDataByAccountId.isPresent() && mostRecentDeviceDataByAccountId.get().externalDeviceId == externalDeviceId) {
            return mostRecentDeviceDataByAccountId;
        }

        // Failed to get only the absolute latest value, so do a range query from minTsLimit to maxTsLimit
        // This isn't the most efficient way to do it, but it does make the code simpler.
        final List<DeviceData> deviceDataList = getBetweenByAbsoluteTimeAggregateBySlotDuration(accountId, externalDeviceId, minTsLimit, maxTsLimit, 1);
        if (!deviceDataList.isEmpty()) {
            // They're sorted in chronological order, so get the last one
            return Optional.of(deviceDataList.get(deviceDataList.size() - 1));
        }

        return Optional.absent();
    }

    private List<Map<String, AttributeValue>> queryTables(final Iterable<String> tableNames,
                                                          final Expression keyConditionExpression,
                                                          final Collection<DeviceDataAttribute> attributes)
    {
        final List<Map<String, AttributeValue>> results = Lists.newArrayList();
        final String keyCondition = keyConditionExpression.getExpressionString();
        for (final String table: tableNames) {
            results.addAll(query(table, keyCondition, attributes, Optional.<String>absent(), keyConditionExpression.getExpressionAttributeValues()));
        }
        return results;
    }

    private List<Map<String, AttributeValue>> queryTables(final Iterable<String> tableNames,
                                                          final Expression keyConditionExpression,
                                                          final Expression filterExpression,
                                                          final Collection<DeviceDataAttribute> attributes)
    {
        final List<Map<String, AttributeValue>> results = Lists.newArrayList();
        final String keyCondition = keyConditionExpression.getExpressionString();
        final String filterCondition = filterExpression.getExpressionString();
        final Map<String,AttributeValue> attributeValues = new ImmutableMap.Builder<String, AttributeValue>()
                .putAll(keyConditionExpression.getExpressionAttributeValues())
                .putAll(filterExpression.getExpressionAttributeValues())
                .build();
        for (final String table: tableNames) {
            results.addAll(query(table, keyCondition, attributes, Optional.of(filterCondition), attributeValues));
        }
        return results;
    }

    /**
     *
     * @param accountId
     * @param deviceId
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
    public ImmutableList<DeviceData> getLightByBetweenHourDateByTS(final Long accountId,
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
                Expressions.compare(DeviceDataAttribute.ACCOUNT_ID, "=", toAttributeValue(accountId)),
                Expressions.between(DeviceDataAttribute.RANGE_KEY, getRangeKey(startTime, externalDeviceId), getRangeKey(endTime, externalDeviceId)));

        final List<DeviceData> results = Lists.newArrayList();
        for (final Map<String, AttributeValue> result : queryTables(getTableNames(startTime, endTime), keyConditionExp, filterExp, ALL_ATTRIBUTES)) {
            final DeviceData data = attributeMapToDeviceData(result);
            final int hourOfDay = data.localTime().getHourOfDay();
            if (data.externalDeviceId.equals(externalDeviceId) &&
                    (hourOfDay >= startHour || hourOfDay < endHour)) {
                results.add(data);
            }
        }


        return ImmutableList.copyOf(results);
    }

    private List<Map<String, AttributeValue>> getItemsBetweenLocalTime(
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
                Expressions.compare(DeviceDataAttribute.ACCOUNT_ID, "=", toAttributeValue(accountId)),
                Expressions.between(DeviceDataAttribute.RANGE_KEY, getRangeKey(startUTCTime, externalDeviceId), getRangeKey(endUTCTime, externalDeviceId)));
        final Expression filterExpression = Expressions.between(DeviceDataAttribute.LOCAL_UTC_TIMESTAMP, dateTimeToAttributeValue(startLocalTime), dateTimeToAttributeValue(endLocalTime));

        final List<Map<String, AttributeValue>> results = Lists.newArrayList();
        for (final Map<String, AttributeValue> result : queryTables(getTableNames(startUTCTime, endUTCTime), keyConditionExpression, filterExpression, attributes)) {
            if (externalDeviceIdFromDDBItem(result).equals(externalDeviceId)) {
                results.add(result);
            }
        }

        return results;
    }

    /**
     *
     * @param accountId
     * @param deviceId
     * @param startUTCTime Earliest UTC time to retrieve (inclusive)
     * @param endUTCTime Latest UTC time to retrieve (inclusive)
     * @param startLocalTime Earliest local time to retrieve (inclusive)
     * @param endLocalTime Latest local time to retrieve (inclusive)
     * @param attributes Attributes to be included in the response DeviceDatas.
     * @return DeviceDatas matching the above criteria
     */
    public List<DeviceData> getBetweenLocalTime(final Long accountId,
                                                final DeviceId deviceId,
                                                final DateTime startUTCTime,
                                                final DateTime endUTCTime,
                                                final DateTime startLocalTime,
                                                final DateTime endLocalTime,
                                                final Collection<DeviceDataAttribute> attributes)
    {
        final List<Map<String, AttributeValue>> results = getItemsBetweenLocalTime(accountId, deviceId, startUTCTime, endUTCTime, startLocalTime, endLocalTime, attributes);
        return ImmutableList.copyOf(attributeMapsToDeviceDataList(results));
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
    public ImmutableList<Integer> getAirQualityRawList(final Long accountId,
                                                       final DeviceId deviceId,
                                                       final DateTime startUTCTime,
                                                       final DateTime endUTCTime,
                                                       final DateTime startLocalTime,
                                                       final DateTime endLocalTime)
    {
        final Set<DeviceDataAttribute> attributes = new ImmutableSet.Builder<DeviceDataAttribute>().addAll(BASE_ATTRIBUTES).add(DeviceDataAttribute.AMBIENT_AIR_QUALITY_RAW).build();
        final List<DeviceData> results = getBetweenLocalTime(accountId, deviceId, startUTCTime, endUTCTime, startLocalTime, endLocalTime, attributes);

        final List<Integer> aggregated = averageDailyAirQualityRaw(results);
        return ImmutableList.copyOf(aggregated);
    }

    private List<Map<String, AttributeValue>> getBetweenHourDateByTS(final Long accountId,
                                                             final DeviceId deviceId,
                                                             final DateTime startUTCTime,
                                                             final DateTime endUTCTime,
                                                             final DateTime startLocalTime,
                                                             final DateTime endLocalTime,
                                                             final int startHour,
                                                             final int endHour,
                                                             final boolean sameDay)
    {
        final List<Map<String, AttributeValue>> results = getItemsBetweenLocalTime(accountId, deviceId, startUTCTime, endUTCTime, startLocalTime, endLocalTime, ALL_ATTRIBUTES);
        final List<Map<String, AttributeValue>> filteredResults = Lists.newArrayList();
        for (final Map<String, AttributeValue> result: results) {
            final int hourOfDay = timestampFromDDBItem(result).plusMillis(DeviceDataAttribute.OFFSET_MILLIS.getInteger(result)).getHourOfDay();
            if (sameDay && (hourOfDay >= startHour && hourOfDay < endHour) ||
                    (!sameDay && (hourOfDay >= startHour || hourOfDay < endHour))) {
                filteredResults.add(result);
            }
        }
        return filteredResults;
    }

    /**
     * Get DeviceDatas that fall within [startHour, endHour) where endHour > startHour.
     */
    @Override
    public ImmutableList<DeviceData> getBetweenHourDateByTSSameDay(final Long accountId,
                                                                   final DeviceId deviceId,
                                                                   final DateTime startUTCTime,
                                                                   final DateTime endUTCTime,
                                                                   final DateTime startLocalTime,
                                                                   final DateTime endLocalTime,
                                                                   final int startHour,
                                                                   final int endHour)
    {
        final List<Map<String, AttributeValue>> results = getBetweenHourDateByTS(accountId, deviceId, startUTCTime, endUTCTime, startLocalTime, endLocalTime, startHour, endHour, true);
        return ImmutableList.copyOf(attributeMapsToDeviceDataList(results));
    }

    /**
     *  Get DeviceDatas that are >= startHour or < endHour, where startHour > endHour (because it's from the prev night)
     */
    @Override
    public ImmutableList<DeviceData> getBetweenHourDateByTS(final Long accountId,
                                                            final DeviceId deviceId,
                                                            final DateTime startUTCTime,
                                                            final DateTime endUTCTime,
                                                            final DateTime startLocalTime,
                                                            final DateTime endLocalTime,
                                                            final int startHour,
                                                            final int endHour)
    {
        final List<Map<String, AttributeValue>> results = getBetweenHourDateByTS(accountId, deviceId, startUTCTime, endUTCTime, startLocalTime, endLocalTime, startHour, endHour, false);
        return ImmutableList.copyOf(attributeMapsToDeviceDataList(results));
    }

    /**
     * Same as getBetweenHourDateByTS, but aggregated to slotDuration minutes.
     */
    @Override
    public ImmutableList<DeviceData> getBetweenByLocalHourAggregateBySlotDuration(final Long accountId,
                                                                                  final DeviceId deviceId,
                                                                                  final DateTime start,
                                                                                  final DateTime end,
                                                                                  final DateTime startLocal,
                                                                                  final DateTime endLocal,
                                                                                  int startHour,
                                                                                  int endHour,
                                                                                  final Integer slotDuration) {
        final List<Map<String, AttributeValue>> results = getBetweenHourDateByTS(accountId, deviceId, start, end, startLocal, endLocal, startHour, endHour, false);
        return ImmutableList.copyOf(aggregateDynamoDBItemsToDeviceData(results, slotDuration));
    }

}
