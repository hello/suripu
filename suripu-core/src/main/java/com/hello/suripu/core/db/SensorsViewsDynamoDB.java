package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.FirmwareInfo;
import com.hello.suripu.core.models.Sample;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SensorsViewsDynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(SensorsViewsDynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableNamePrefix;
    private final String lastSeenTableName;

    public static final String SENSE_ID_ATTRIBUTE_NAME = "sense_id";
    public static final Integer MAX_LAST_SEEN_DEVICES = 100;

    protected static final String UTC_TIMESTAMP_ATTRIBUTE_NAME = "utc_ts";
    protected static final String TEMP_ATTRIBUTE_NAME = "temp";
    protected static final String HUMIDITY_ATTRIBUTE_NAME = "humidity";
    protected static final String LIGHT_ATTRIBUTE_NAME = "light";
    protected static final String SOUND_ATTRIBUTE_NAME = "sound";
    protected static final String DUST_ATTRIBUTE_NAME = "dust";
    protected static final String FIRMWARE_VERSION_ATTRIBUTE_NAME = "fw_version";
    protected static final String OFFSET_MILLIS_ATTRIBUTE_NAME = "offset";
    protected static final String UPDATED_AT_UTC_ATTRIBUTE_NAME = "updated_at_utc";
    protected static final String TZ_OFFSET_ATTRIBUTE_NAME = "offset";

    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";  // Due to Joda Time's behavior, it is not a good idea to store timezone as offset in the string
    private final String DYNAMO_DB_TABLE_FORMAT = "yyyy_MM_dd";
    private final Integer DYNAMO_BATCH_WRITE_LIMIT = 25;

    public String tableNameForDateTimeUpload(final DateTime dateTime) {
        return String.format("%s_%s", tableNamePrefix, dateTime.toString(DYNAMO_DB_TABLE_FORMAT));
    }

    public SensorsViewsDynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableNamePrefix, final String lastSeenTableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableNamePrefix = tableNamePrefix;
        this.lastSeenTableName = lastSeenTableName;
    }

    public WriteRequest transform(final String deviceName, final DeviceData deviceData) {
        final Map<String, AttributeValue> items = Maps.newHashMap();
        items.put(SENSE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceName));

        items.put(TEMP_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(deviceData.ambientTemperature)));
        items.put(HUMIDITY_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(deviceData.ambientHumidity)));
        items.put(LIGHT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(deviceData.ambientLight)));
        items.put(SOUND_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(deviceData.audioPeakDisturbancesDB)));
        items.put(DUST_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(deviceData.ambientAirQualityRaw)));

        items.put(FIRMWARE_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(deviceData.firmwareVersion)));
        items.put(UPDATED_AT_UTC_ATTRIBUTE_NAME, new AttributeValue().withS(deviceData.dateTimeUTC.toString(DATETIME_FORMAT)));

        final PutRequest putRequest = new PutRequest(items);
        return new WriteRequest(putRequest);
    }


    public ArrayListMultimap<String, WriteRequest> batch(final ArrayListMultimap<String, WriteRequest> writeRequests) {
        final ArrayListMultimap<String, WriteRequest> remaining = ArrayListMultimap.create();

        // we partition writes per table.
        // this is sub optimal for when inserts span two tables
        // but this happens only a couple minutes per day, so eh.
        final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest();
        try {
            for (final String table : writeRequests.keySet()) {
                for (final List<WriteRequest> partition : Lists.partition(writeRequests.get(table), DYNAMO_BATCH_WRITE_LIMIT)) {

                    final Map<String, List<WriteRequest>> map = Maps.newHashMap();
                    map.put(table, partition);
                    batchWriteItemRequest.withRequestItems(map);

                    final BatchWriteItemResult result = dynamoDBClient.batchWriteItem(batchWriteItemRequest);
                    final Map<String, List<WriteRequest>> unprocessed = result.getUnprocessedItems();
                    if (unprocessed == null || unprocessed.isEmpty()) {
                        continue;
                    }

                    final float ratio = unprocessed.size() / (float) partition.size() * 100.0f;
                    LOGGER.info("Table {} : {}%  ({} attempted, {} unprocessed)", table, Math.round(ratio), partition.size(), unprocessed.size());
                    remaining.putAll(table, unprocessed.get(table));
                }
            }

        } catch (final AmazonServiceException exception) {
            LOGGER.error("Failed when trying to save {} items: {}", writeRequests.values().size(), exception.getMessage());
            return writeRequests;

        } catch (final AmazonClientException exception) {
            LOGGER.error("Failed when trying to save {} items: {}", writeRequests.values().size(), exception.getMessage());
            return writeRequests;
        }

        // we want to blow up on any other exception

        // return what needs to be re-inserted
        return remaining;
    }



    private void saveLastSeen(final List<WriteRequest> lastSeenWriteRequests) {
        final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest();
        for (final List<WriteRequest> partition : Lists.partition(lastSeenWriteRequests, DYNAMO_BATCH_WRITE_LIMIT)) {

            final Map<String, List<WriteRequest>> map = Maps.newHashMap();
            map.put(lastSeenTableName, partition);
            batchWriteItemRequest.withRequestItems(map);
            try {
                final BatchWriteItemResult result = dynamoDBClient.batchWriteItem(batchWriteItemRequest);
                final Map<String, List<WriteRequest>> unprocessed = result.getUnprocessedItems();
                if (unprocessed == null || unprocessed.isEmpty()) {
                    continue;
                }

                final float ratio = unprocessed.size() / (float) partition.size() * 100.0f;
                LOGGER.info("Table {} : {}%  ({} attempted, {} unprocessed)", lastSeenTableName, Math.round(ratio), partition.size(), unprocessed.size());
            }catch (AmazonClientException e) {
                LOGGER.error("Error persisting last seen device data: {}", e.getMessage());
            }
        }
    }

    public void saveLastSeenDeviceData(final Map<String, DeviceData> lastSeenDeviceData) {
        final List<WriteRequest> writeRequests = Lists.newArrayList();
        for(final String deviceName: lastSeenDeviceData.keySet()) {
            writeRequests.add(transform(deviceName, lastSeenDeviceData.get(deviceName)));
        }
        saveLastSeen(writeRequests);
    }


    public List<Sample> last(final String senseId, final DateTime dateTime, final Set<String> attributesToGet) {

        final Map<String, Condition> queryConditions = Maps.newHashMap();
        final Condition selectBySenseId  = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(String.valueOf(senseId)));

        final int mod = DateTime.now().minuteOfDay().get();
//        final Condition lessThanMod  = new Condition()
//                .withComparisonOperator(ComparisonOperator.LE)
//                .withAttributeValueList(new AttributeValue().withN(String.valueOf(mod)));

        queryConditions.put(SENSE_ID_ATTRIBUTE_NAME, selectBySenseId);
//        queryConditions.put(MINUTE_OF_DAY_ATTRIBUTE_NAME, lessThanMod);

        final HashSet<String> targetAttributes = Sets.newHashSet();
        if(attributesToGet.isEmpty()) {
            Collections.addAll(targetAttributes, SENSE_ID_ATTRIBUTE_NAME, UTC_TIMESTAMP_ATTRIBUTE_NAME, UPDATED_AT_UTC_ATTRIBUTE_NAME, TEMP_ATTRIBUTE_NAME);
        }

        final QueryRequest queryRequest = new QueryRequest(tableNameForDateTimeUpload(dateTime))
                .withKeyConditions(queryConditions)
                .withAttributesToGet(targetAttributes)
                .withReturnConsumedCapacity("TOTAL");

        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        if(queryResult.getItems() == null){
            return ImmutableList.copyOf(Collections.EMPTY_LIST);
        }

        final List<Sample> samples = Lists.newArrayList();
        for(final Map<String, AttributeValue> valueMap : queryResult.getItems()) {

            final DateTime dt = new DateTime(Long.parseLong(valueMap.get(UTC_TIMESTAMP_ATTRIBUTE_NAME).getN()), DateTimeZone.UTC);
            final Float temp = Float.parseFloat(valueMap.get(TEMP_ATTRIBUTE_NAME).getN());
            final Sample sample = new Sample(dt.getMillis(), temp, Integer.parseInt(valueMap.get(OFFSET_MILLIS_ATTRIBUTE_NAME).getN()));
            samples.add(sample);
        }
        LOGGER.info("{}", queryResult.getConsumedCapacity().toString());
        return samples;
    }

    /**
     * Retrieves last data upload from Sense and associates it to account id and internal sense id
     * @param senseId
     * @param accountId
     * @param internalSenseId
     * @return
     */
    public Optional<DeviceData> lastSeen(final String senseId, final Long accountId, final Long internalSenseId) {

        final Map<String, AttributeValue> key = Maps.newHashMap();
        key.put(SENSE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withKey(key)
                .withTableName(lastSeenTableName);
        try{
            final GetItemResult getItemResult  = dynamoDBClient.getItem(getItemRequest);
            return fromDynamoDB(getItemResult.getItem(), senseId, accountId, internalSenseId);
        }
        catch (AmazonServiceException ase) {
            LOGGER.error("Failed to get last seen for sense {} because {}", senseId, ase.getMessage());
        }
        return Optional.absent();
    }

    public Optional<List<FirmwareInfo>> lastSeenFirmwareBatch(final Set<String> deviceIds) {

        if (deviceIds.size() > MAX_LAST_SEEN_DEVICES) {
            LOGGER.error("Device limit ({}) exceeded while querying last seen firmware.", MAX_LAST_SEEN_DEVICES);
            return Optional.absent();
        }

        final List<Map<String, AttributeValue>> conditions = Lists.newArrayList();

        final BatchGetItemRequest request = new BatchGetItemRequest();

        for (final String devId : deviceIds) {
            final Map<String, AttributeValue> cond = new HashMap<String, AttributeValue>();
            cond.put(SENSE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(devId));
            conditions.add(cond);
        }

        final KeysAndAttributes keys = new KeysAndAttributes().withKeys(conditions);
        request.addRequestItemsEntry(lastSeenTableName, keys);

        BatchGetItemResult batchResult;

        try {
            batchResult = dynamoDBClient.batchGetItem(request);
        }catch (AmazonServiceException awsEx){
            LOGGER.error("Batch sense data request failed. AWS service error: {}", awsEx.getMessage());
            return Optional.absent();
        }catch (AmazonClientException awcEx){
            LOGGER.error("Batch sense data request failed. Client error: {}", awcEx.getMessage());
            return Optional.absent();
        }catch (Exception e) {
            LOGGER.error("Batch sense data request failed. {}", e.getMessage());
            return Optional.absent();
        }

        final Map<String,List<Map<String,AttributeValue>>> resultsMap = batchResult.getResponses();
        final List<FirmwareInfo> fwVersions = Lists.newArrayList();

        for (final Map<String, AttributeValue> item : resultsMap.get(lastSeenTableName)) {
            if (!item.containsKey(SENSE_ID_ATTRIBUTE_NAME)) {
                continue;
            }
            final String deviceId = item.get(SENSE_ID_ATTRIBUTE_NAME).getS();
            final Integer firmwareVersion = (item.containsKey(FIRMWARE_VERSION_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(FIRMWARE_VERSION_ATTRIBUTE_NAME).getN()) : 0);
            final String updatedAt = (item.containsKey(UPDATED_AT_UTC_ATTRIBUTE_NAME) ? item.get(UPDATED_AT_UTC_ATTRIBUTE_NAME).getS(): "");
            final DateTime dateTime = DateTime.parse(updatedAt, DateTimeFormat.forPattern(DATETIME_FORMAT));
            final FirmwareInfo latestFWInfo = new FirmwareInfo(firmwareVersion.toString(), deviceId, dateTime.getMillis());
            fwVersions.add(latestFWInfo);
        }

        if (fwVersions.isEmpty()) {
            return Optional.absent();
        }

        return Optional.of(fwVersions);
    }


    public Optional<DeviceStatus> senseStatus(final String senseId, final Long accountId, final Long internalSenseId) {
        final Map<String, AttributeValue> key = Maps.newHashMap();
        key.put(SENSE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withKey(key)
                .withTableName(lastSeenTableName);
        final GetItemResult result = dynamoDBClient.getItem(getItemRequest);
        return deviceStatusfromDynamoDB(result.getItem(), senseId, accountId, internalSenseId);
    }


    private Optional<DeviceStatus> deviceStatusfromDynamoDB(final Map<String, AttributeValue> item, final String senseId, final Long accountId, final Long internalSenseId) {
        if(item == null || item.isEmpty()) {
            return Optional.absent();
        }

        final String dateTimeUTC = (item.containsKey(UPDATED_AT_UTC_ATTRIBUTE_NAME) ? item.get(UPDATED_AT_UTC_ATTRIBUTE_NAME).getS() : "");
        if(dateTimeUTC.isEmpty()) {
            LOGGER.error("Malformed data stored in last seen for device_id={}.", senseId);
            return Optional.absent();
        }

        final String firmwareVersion = item.containsKey(FIRMWARE_VERSION_ATTRIBUTE_NAME)
                ? Integer.toHexString(Integer.valueOf(item.get(FIRMWARE_VERSION_ATTRIBUTE_NAME).getN()))
                : "-";

        final DateTime dateTime = DateTime.parse(dateTimeUTC, DateTimeFormat.forPattern(DATETIME_FORMAT));

        final DeviceStatus deviceStatus = DeviceStatus.sense(internalSenseId, firmwareVersion, dateTime);
        return Optional.of(deviceStatus);
    }


    private Optional<DeviceData> fromDynamoDB(final Map<String, AttributeValue> item, final String senseId, final Long accountId, final Long internalSenseId) {
        if(item == null || item.isEmpty()) {
            return Optional.absent();
        }

        final String dateTimeUTC = (item.containsKey(UPDATED_AT_UTC_ATTRIBUTE_NAME) ? item.get(UPDATED_AT_UTC_ATTRIBUTE_NAME).getS() : "");
        if(dateTimeUTC.isEmpty()) {
            LOGGER.error("Malformed data stored in last seen for device_id={}.", senseId);
            return Optional.absent();
        }

        final Integer ambientTemp = (item.containsKey(TEMP_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(TEMP_ATTRIBUTE_NAME).getN()) : 0);
        final Integer ambientHumidity = (item.containsKey(HUMIDITY_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(HUMIDITY_ATTRIBUTE_NAME).getN()) : 0);
        final Integer ambientLight = (item.containsKey(LIGHT_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(LIGHT_ATTRIBUTE_NAME).getN()) : 0);
        final Integer ambientDustRaw = (item.containsKey(DUST_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(DUST_ATTRIBUTE_NAME).getN()) : 0);


        final Integer firmwareVersion = (item.containsKey(FIRMWARE_VERSION_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(FIRMWARE_VERSION_ATTRIBUTE_NAME).getN()) : 0);
        final Integer sound = (item.containsKey(SOUND_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(SOUND_ATTRIBUTE_NAME).getN()) : 0);
        final Integer offsetMillis = (item.containsKey(OFFSET_MILLIS_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(OFFSET_MILLIS_ATTRIBUTE_NAME).getN()) : 0);
        final DateTime dateTime = DateTime.parse(dateTimeUTC, DateTimeFormat.forPattern(DATETIME_FORMAT));

        final DeviceData deviceData = new DeviceData.Builder()
                .withDeviceId(internalSenseId)
                .withAccountId(accountId)
                .withAmbientTemperature(ambientTemp)
                .withAmbientHumidity(ambientHumidity)
                .calibrateAmbientLight(ambientLight)
                .withAmbientAirQualityRaw(ambientDustRaw)
                .withAlreadyCalibratedAudioPeakBackgroundDB(sound)
                .withDateTimeUTC(dateTime)
                .withOffsetMillis(offsetMillis)
                .withFirmwareVersion(firmwareVersion)
                .build();

        return Optional.of(deviceData);
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        // TODO make this work for creating the daily/monthly shards
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(SENSE_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(UTC_TIMESTAMP_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(SENSE_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(UTC_TIMESTAMP_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }

    public static CreateTableResult createLastSeenTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        // TODO make this work for creating the daily/monthly shards
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(SENSE_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(SENSE_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );

        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
