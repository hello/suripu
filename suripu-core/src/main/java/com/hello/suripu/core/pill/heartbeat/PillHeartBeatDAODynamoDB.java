package com.hello.suripu.core.pill.heartbeat;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
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
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.DeviceStatus;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PillHeartBeatDAODynamoDB implements PillHeartBeatDAO {


    private final static Logger LOGGER = LoggerFactory.getLogger(PillHeartBeatDAODynamoDB.class);
    private final Integer DYNAMO_BATCH_WRITE_LIMIT = 25;

    public static final String PILL_ID_ATTRIBUTE_NAME = "pill_id";

    protected static final String UTC_DATETIME_ATTRIBUTE_NAME = "utc_dt";
    protected static final String BATTERY_LEVEL_ATTRIBUTE_NAME= "battery_level";
    protected static final String UPTIME_ATTRIBUTE_NAME= "uptime";
    protected static final String FIRMWARE_VERSION_ATTRIBUTE_NAME = "fw_version";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";  // Due to Joda Time's behavior, it is not a good idea to store timezone as offset in the string

    private final String tableName;
    private final AmazonDynamoDB dynamoDBClient;


    private PillHeartBeatDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    /**
     * Creates an instance of PillStatusDynamoDB
     * @param dynamoDBClient
     * @param tableName
     * @return
     */
    public static PillHeartBeatDAODynamoDB create(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        return new PillHeartBeatDAODynamoDB(dynamoDBClient, tableName);
    }



    private Map<String, AttributeValue> toDynamoDBItem(final PillHeartBeat heartBeat) {
        final Map<String, AttributeValue> item = Maps.newHashMap();
        item.put(PILL_ID_ATTRIBUTE_NAME, new AttributeValue().withS(heartBeat.pillId));
        item.put(UTC_DATETIME_ATTRIBUTE_NAME, new AttributeValue().withS(heartBeat.createdAtUTC.toString(DATETIME_FORMAT)));
        item.put(BATTERY_LEVEL_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(heartBeat.batteryLevel)));
        item.put(UPTIME_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(heartBeat.uptimeInSeconds)));
        item.put(FIRMWARE_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(heartBeat.firmwareVersion)));
        return item;
    }

    private WriteRequest transform(final PillHeartBeat heartBeat) {
        final Map<String, AttributeValue> item = toDynamoDBItem(heartBeat);
        return new WriteRequest(new PutRequest().withItem(item));
    }

    @Override
    public void put(final PillHeartBeat heartBeat) {
        final Map<String, AttributeValue> item = toDynamoDBItem(heartBeat);
        final PutItemRequest request = new PutItemRequest();
        request.withTableName(tableName)
                .withItem(item);

        try {
            final PutItemResult result = dynamoDBClient.putItem(request);
        } catch (AmazonServiceException e) {
            LOGGER.error("Failed updating heartbeat info for pill {}, Reason: {}", heartBeat.pillId, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unknown error. Failed updating heartbeat info for pill {}, Reason: {}", heartBeat.pillId, e.getMessage());
        }
    }

    @Override
    public void put(final List<PillHeartBeat> pillHeartBeats) {
        final Map<String, WriteRequest> uniqueWriteRequests = Maps.newHashMap();
        for(final PillHeartBeat heartBeat : pillHeartBeats) {
            uniqueWriteRequests.put(heartBeat.pillId, transform(heartBeat));
        }
        saveHeartBeat(Lists.newArrayList(uniqueWriteRequests.values()));
    }


    /**
     * Saves batch of heartbeat data to dynamoDB
     * @param lastSeenWriteRequests
     */
    private void saveHeartBeat(final List<WriteRequest> lastSeenWriteRequests) {
        final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest();
        int i = 0;
        for (final List<WriteRequest> partition : Lists.partition(lastSeenWriteRequests, DYNAMO_BATCH_WRITE_LIMIT)) {
            i++;
            final Map<String, List<WriteRequest>> map = Maps.newHashMap();
            map.put(tableName, partition);
            batchWriteItemRequest.withRequestItems(map);
            try {
                final BatchWriteItemResult result = dynamoDBClient.batchWriteItem(batchWriteItemRequest);
                final Map<String, List<WriteRequest>> unprocessed = result.getUnprocessedItems();
                if (unprocessed == null || unprocessed.isEmpty()) {
                    continue;
                }

                final float ratio = unprocessed.size() / (float) partition.size() * 100.0f;
                LOGGER.info("Table {} : {}%  ({} attempted, {} unprocessed)", tableName, Math.round(ratio), partition.size(), unprocessed.size());
            }catch (AmazonClientException e) {
                LOGGER.error("Error persisting last seen device data: {}", e.getMessage());
            }
        }

        LOGGER.error("Iterations: {}", i);
    }


    @Override
    public List<DeviceStatus> get(final String pillId) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<DeviceStatus> get(final String pillId, final DateTime end) {
        return Collections.EMPTY_LIST;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        // TODO make this work for creating the daily/monthly shards
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(PILL_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(UTC_DATETIME_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(PILL_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(UTC_DATETIME_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
