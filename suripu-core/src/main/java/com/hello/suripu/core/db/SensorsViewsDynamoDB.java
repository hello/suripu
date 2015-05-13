package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
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
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.models.Sample;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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

    protected static final String UTC_TIMESTAMP_ATTRIBUTE_NAME = "utc_ts";
    protected static final String TEMP_ATTRIBUTE_NAME = "temp";
    protected static final String HUMIDITY_ATTRIBUTE_NAME = "humidity";
    protected static final String LIGHT_ATTRIBUTE_NAME = "light";
    protected static final String SOUND_ATTRIBUTE_NAME = "sound";
    protected static final String DUST_ATTRIBUTE_NAME = "dust";
    protected static final String FIRMWARE_VERSION_ATTRIBUTE_NAME = "fw_version";
    protected static final String OFFSET_MILLIS_ATTRIBUTE_NAME = "offset";
    protected static final String UPDATED_AT_UTC_ATTRIBUTE_NAME = "updated_at_utc"; // local time at time of event?

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

    public WriteRequest transform(final String senseId, final Integer temp, final Integer humidity,
                     final Integer light, final Integer sound, final Integer dust, final DateTime utcDateTime, final Integer fwVersion){
        final Map<String, AttributeValue> items = Maps.newHashMap();
        items.put(SENSE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        items.put(UTC_TIMESTAMP_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(utcDateTime.getMillis())));

        items.put(TEMP_ATTRIBUTE_NAME, new AttributeValue().withN(temp.toString()));
        items.put(HUMIDITY_ATTRIBUTE_NAME, new AttributeValue().withN(humidity.toString()));
        items.put(LIGHT_ATTRIBUTE_NAME, new AttributeValue().withN(light.toString()));
        items.put(SOUND_ATTRIBUTE_NAME, new AttributeValue().withN(sound.toString()));
        items.put(DUST_ATTRIBUTE_NAME, new AttributeValue().withN(dust.toString()));

        items.put(FIRMWARE_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(fwVersion.toString()));
        items.put(UPDATED_AT_UTC_ATTRIBUTE_NAME, new AttributeValue().withS(utcDateTime.toString(DATETIME_FORMAT)));

        final PutRequest putRequest = new PutRequest(items);
        return new WriteRequest(putRequest);
    }



    public WriteRequest transform(final String deviceName, final DataInputProtos.periodic_data periodic_data) {
        final Map<String, AttributeValue> items = Maps.newHashMap();
        items.put(SENSE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceName));

        items.put(TEMP_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(periodic_data.getTemperature())));
        items.put(HUMIDITY_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(periodic_data.getHumidity())));
        items.put(LIGHT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(periodic_data.getLight())));
        items.put(SOUND_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(periodic_data.getAudioPeakBackgroundEnergyDb())));
        items.put(DUST_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(periodic_data.getDustMax())));

        items.put(FIRMWARE_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(periodic_data.getFirmwareVersion())));
        items.put(UPDATED_AT_UTC_ATTRIBUTE_NAME, new AttributeValue().withS(new DateTime(periodic_data.getUnixTime() * 1000L, DateTimeZone.UTC).toString(DATETIME_FORMAT)));

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


    public void saveLastSeenDeviceData(final Map<String, DataInputProtos.periodic_data> lastSeenDeviceData) {
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
