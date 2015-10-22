package com.hello.suripu.core.pill.heartbeat;

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
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PillHeartBeatDAODynamoDB implements PillHeartBeatDAO {


    private final static Logger LOGGER = LoggerFactory.getLogger(PillHeartBeatDAODynamoDB.class);
    private final Integer DYNAMO_BATCH_WRITE_LIMIT = 25;

    public static final String PILL_ID_ATTRIBUTE_NAME = "pill_id";

    public static final String UTC_DATETIME_ATTRIBUTE_NAME = "utc_dt";
    protected static final String BATTERY_LEVEL_ATTRIBUTE_NAME= "battery_level";
    protected static final String UPTIME_ATTRIBUTE_NAME= "uptime";
    protected static final String FIRMWARE_VERSION_ATTRIBUTE_NAME = "fw_version";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";  // Due to Joda Time's behavior, it is not a good idea to store timezone as offset in the string

    private static final Set<String> TARGET_ATTRIBUTES = ImmutableSet.of(
            PILL_ID_ATTRIBUTE_NAME,
            UTC_DATETIME_ATTRIBUTE_NAME,
            BATTERY_LEVEL_ATTRIBUTE_NAME,
            UPTIME_ATTRIBUTE_NAME,
            FIRMWARE_VERSION_ATTRIBUTE_NAME
    );

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


    private Optional<PillHeartBeat> fromDynamoDBItem(final Map<String, AttributeValue> item) {

        if(item == null || item.isEmpty()) {
            return Optional.absent();
        }

        try {
            final String pillId = item.get(PILL_ID_ATTRIBUTE_NAME).getS();
            final DateTime utcDateTime = new DateTime(DateTime.parse(item.get(UTC_DATETIME_ATTRIBUTE_NAME).getS(), DateTimeFormat.forPattern(DATETIME_FORMAT)), DateTimeZone.UTC);
            final Integer batteryLevel = Integer.parseInt(item.get(BATTERY_LEVEL_ATTRIBUTE_NAME).getN());
            final Integer uptimeInSeconds = Integer.parseInt(item.get(UPTIME_ATTRIBUTE_NAME).getN());
            final Integer firmwareVersion = Integer.parseInt(item.get(FIRMWARE_VERSION_ATTRIBUTE_NAME).getN());
            return Optional.of(PillHeartBeat.create(pillId, batteryLevel, firmwareVersion, uptimeInSeconds, utcDateTime));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return Optional.absent();
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

    public List<PillHeartBeat> putBatch(final Set<PillHeartBeat> pillHeartBeats) {
        final List<PillHeartBeat> unprocessedHeartbeats = Lists.newArrayList();
        final List<WriteRequest> writeRequests = Lists.newArrayList();

        for(final PillHeartBeat heartBeat : pillHeartBeats) {
            writeRequests.add(transform(heartBeat));
        }

        final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest();
        int i = 0;
        for (final List<WriteRequest> partition : Lists.partition(writeRequests, DYNAMO_BATCH_WRITE_LIMIT)) {
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

                final Collection<List<WriteRequest>> unprocessedRequests = unprocessed.values();
                for (final List<WriteRequest> requestList : unprocessedRequests) {
                    for (final WriteRequest request : requestList) {
                        final Optional<PillHeartBeat> optionalHeartbeat = fromDynamoDBItem(request.getPutRequest().getItem());
                        if (optionalHeartbeat.isPresent()) {
                            unprocessedHeartbeats.add(optionalHeartbeat.get());
                        }
                    }
                }

//                final float ratio = unprocessed.size() / (float) partition.size() * 100.0f;
//                LOGGER.info("Table {} : {}%  ({} attempted, {} unprocessed)", tableName, Math.round(ratio), partition.size(), unprocessed.size());

            }catch (AmazonClientException e) {
                LOGGER.error("Error persisting last seen device data: {}", e.getMessage());
                unprocessedHeartbeats.addAll(pillHeartBeats);
            }
        }

//        LOGGER.error("Iterations: {}", i);
        return unprocessedHeartbeats;
    }

    @Override
    public void put(final Set<PillHeartBeat> pillHeartBeats) {
        final List<WriteRequest> writeRequests = Lists.newArrayList();

        for(final PillHeartBeat heartBeat : pillHeartBeats) {
            writeRequests.add(transform(heartBeat));
        }
        saveHeartBeat(writeRequests);
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
    public Optional<PillHeartBeat> get(final String pillId) {

        final Map<String, Condition> queryConditions = defaultQueryConditions(pillId);

        final QueryRequest queryRequest = new QueryRequest(this.tableName)
                .withKeyConditions(queryConditions)
                .withAttributesToGet(TARGET_ATTRIBUTES)
                .withLimit(1)
                .withScanIndexForward(false);

        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        final List<PillHeartBeat> pillHeartBeats = heartBeats(queryResult);

        if(pillHeartBeats.isEmpty()){
            return Optional.absent();
        }

        return Optional.of(pillHeartBeats.get(0));
    }


    @Override
    public List<PillHeartBeat> get(final String pillId, final DateTime latest) {
        final Map<String, Condition> queryConditions = defaultQueryConditions(pillId);

        final Condition rangeQuery = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN)
                .withAttributeValueList(
                        new AttributeValue().withS(latest.minusDays(7).toString(DATETIME_FORMAT)),
                        new AttributeValue().withS(latest.toString(DATETIME_FORMAT))
                );
        queryConditions.put(UTC_DATETIME_ATTRIBUTE_NAME, rangeQuery);

        final QueryRequest queryRequest = new QueryRequest(this.tableName)
                .withKeyConditions(queryConditions)
                .withAttributesToGet(TARGET_ATTRIBUTES)
                .withLimit(500)
                .withScanIndexForward(false);

        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        return heartBeats(queryResult);
    }

    /**
     * Convert query result to list of heartbeats
      * @param queryResult
     * @return
     */
    private List<PillHeartBeat> heartBeats(final QueryResult queryResult) {

        final List<Map<String, AttributeValue>> items = queryResult.getItems();
        if(items == null || items.isEmpty()){
            return Collections.EMPTY_LIST;
        }

        final List<PillHeartBeat> heartBeats = Lists.newArrayListWithCapacity(items.size());

        for (final Map<String, AttributeValue> item : items) {
            final Optional<PillHeartBeat> pillHeartBeatOptional = fromDynamoDBItem(item);
            if(pillHeartBeatOptional.isPresent()){
                heartBeats.add(pillHeartBeatOptional.get());
            }
        }

        return heartBeats;
    }


    /**
     * Returns query condition for given pill
     * @param pillId
     * @return
     */
    private Map<String, Condition> defaultQueryConditions(final String pillId) {
        final Map<String, Condition> queryConditions = Maps.newHashMap();
        final Condition selectByPillId  = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(pillId));
        queryConditions.put(PILL_ID_ATTRIBUTE_NAME, selectByPillId);
        return queryConditions;
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
