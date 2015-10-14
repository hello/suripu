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
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.DeviceData;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by jakepiccolo on 10/9/15.
 */
public class DeviceDataDAODynamoDB implements DeviceDataIngestDAO {
    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceDataDAODynamoDB.class);

    private final AmazonDynamoDB dynamoDBClient;
    // TODO should have ThreadPoolExecutor to parallelize batchInsert?
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
        ArrayList<AttributeDefinition> attributes = new ArrayList<>();
        attributes.add(new AttributeDefinition().withAttributeName(AttributeNames.DEVICE_ID).withAttributeType("N"));
        attributes.add(new AttributeDefinition().withAttributeName(RANGE_KEY_NAME).withAttributeType("S"));

        // keys
        ArrayList<KeySchemaElement> keySchema = new ArrayList<>();
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
        final HashMap<String, AttributeValue> item = new HashMap<>();
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


    public void insert(final DeviceData deviceData) {
        final HashMap<String, AttributeValue> item = deviceDataToAttributeMap(deviceData);
        dynamoDBClient.putItem(tableName, item);
    }

    public void batchInsert(final List<DeviceData> deviceDataList) {
        final List<WriteRequest> writeRequestList = new LinkedList<>();
        for (final DeviceData data : deviceDataList) {
            final HashMap<String, AttributeValue> item = deviceDataToAttributeMap(data);
            writeRequestList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(item)));
        }

        Map<String, List<WriteRequest>> requestItems = new HashMap<>();
        requestItems.put(this.tableName, writeRequestList);

        do {
            final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest().withRequestItems(requestItems);
            final BatchWriteItemResult result = this.dynamoDBClient.batchWriteItem(batchWriteItemRequest);
            // check for unprocessed items
            requestItems = result.getUnprocessedItems();
            LOGGER.debug("Unprocessed put request count {}", requestItems.size());

        } while (requestItems.size() > 0);
    }

    /**
     * Returns the number of items that were successfully inserted
     * @param deviceDataList
     * @return
     */
    public int batchInsertWithFailureFallback(final List<DeviceData> deviceDataList) {
        final List<List<DeviceData>> deviceDataLists = Lists.partition(deviceDataList, MAX_PUT_ITEMS);
        int successfulInsertions = 0;

        // Insert each chunk
        for (final List<DeviceData> deviceDataListToWrite: deviceDataLists) {
            try {
                batchInsert(deviceDataListToWrite);
                successfulInsertions += deviceDataListToWrite.size();
            } catch (AmazonClientException e) {
                LOGGER.error("Got exception while attempting to batchInsert to DynamoDB: {}", e);

                for (final DeviceData deviceData : deviceDataListToWrite) {
                    try {
                        insert(deviceData);
                        successfulInsertions++;
                    } catch (AmazonClientException ex) {
                        LOGGER.error("Got exception while attempting to insert to DynamoDB: {}", ex);
                    }
                }
            }

        }

        return successfulInsertions;
    }

    public ImmutableList<DeviceData> getBetweenByAbsoluteTimeAggregateBySlotDuration(
            final Long deviceId,
            final Long accountId,
            final DateTime start,
            final DateTime end,
            // TODO this field is currently ignored, no aggregation ATM
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

        final Map<String, Condition> queryConditions = new HashMap<>();
        queryConditions.put(AttributeNames.DEVICE_ID, selectByDeviceId);
        queryConditions.put(RANGE_KEY_NAME, selectByTimestamp);

        final List<DeviceData> results = new ArrayList<>();

        Map<String, AttributeValue> lastEvaluatedKey = null;

        do {
            final QueryRequest queryRequest = new QueryRequest()
                    .withTableName(this.tableName)
                    .withKeyConditions(queryConditions)
                    .withAttributesToGet(targetAttributes)
                    .withExclusiveStartKey(lastEvaluatedKey);

            final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
            final List<Map<String, AttributeValue>> items = queryResult.getItems();

            if (queryResult.getItems() != null) {
                for (final Map<String, AttributeValue> item : items) {
                    if (!item.keySet().containsAll(targetAttributes)) {
                        LOGGER.warn("Missing field in item {}", item);
                        continue;
                    }
                    results.add(this.attributeMapToDeviceData(item));
                }
            }

            lastEvaluatedKey = queryResult.getLastEvaluatedKey();

        } while (lastEvaluatedKey != null);

        return ImmutableList.copyOf(results);
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
}
