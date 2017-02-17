package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
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
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.hello.suripu.core.metrics.DeviceEvents;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SenseEventsDynamoDB implements SenseEventsDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseEventsDynamoDB.class);

    public final static String DEVICE_ID_ATTRIBUTE_NAME = "device_id";
    public final static String CREATED_AT_ATTRIBUTE_NAME = "created_at";
    public final static String EVENTS_ATTRIBUTE_NAME = "events";

    public final static String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ssZ";
    private final static Integer BATCH_SIZE = 20;
    private final static Integer MAX_RETRY = 5;
    private final static Integer DEFAULT_LIMIT_SIZE = 200;

    private final AmazonDynamoDB amazonDynamoDB;
    private final String tableName;

    public SenseEventsDynamoDB(final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.tableName = tableName;
    }


    public static String dateTimeToString(final DateTime dateTime) {
        return dateTime.toString(DATETIME_FORMAT);
    }

    public static DateTime stringToDateTime(final String createdAt) {
        LOGGER.info("created at {}", createdAt);
        final String adjustedCreatedAt = (createdAt.contains("Z") || createdAt.contains("+")) ? createdAt : createdAt + "Z"; // we need to self correct for old values in db
        LOGGER.info("adjusted created at{}", adjustedCreatedAt);
        return DateTime.parse(adjustedCreatedAt, DateTimeFormat.forPattern(DATETIME_FORMAT));
    }

    public static String deviceEventsKey(final DeviceEvents deviceEvents) {
        final String createdAt = dateTimeToString(deviceEvents.createdAt);
        return String.format("%s|%s", deviceEvents.deviceId, createdAt);
    }


    /**
     * Group events per device_id and seconds
     * @param deviceEventsList
     * @return
     */
    public static Multimap<String, String> transform(List<DeviceEvents> deviceEventsList) {
        // This is ugly
        //
        // DEVICE_ID | 2015-04-09 10:00:01 -> { ble_dismissed, led : color }
        // DEVICE_ID | 2015-04-09 10:00:02 -> { ble_dismissed, led : color }

        final Multimap<String, String> eventsPerSecond = ArrayListMultimap.create();
        for (final DeviceEvents deviceEvents : deviceEventsList) {

            final String uniqueKey = deviceEventsKey(deviceEvents);
            eventsPerSecond.putAll(uniqueKey, deviceEvents.events);
        }

        return eventsPerSecond;
    }



    @Override
    public List<DeviceEvents> get(final String deviceId, final DateTime start, final Integer limit) {
        final Map<String, Condition> queryConditions = new HashMap<>();

        final Condition byDeviceId = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(deviceId));
        queryConditions.put(DEVICE_ID_ATTRIBUTE_NAME, byDeviceId);

        final Condition byTime = new Condition()
                .withComparisonOperator(ComparisonOperator.LE)
                .withAttributeValueList(new AttributeValue().withS(dateTimeToString(start)));

        queryConditions.put(CREATED_AT_ATTRIBUTE_NAME, byTime);


        final QueryRequest queryRequest = new QueryRequest();
        queryRequest.withTableName(tableName)
                .withKeyConditions(queryConditions)
                .withLimit(Math.min(limit, DEFAULT_LIMIT_SIZE * 2))
                .setScanIndexForward(false);
        final QueryResult queryResult = amazonDynamoDB.query(queryRequest);

        return fromDynamoDBItems(queryResult.getItems());
    }

    /**
     * Retrieve list of events for device id starting at time start
     * @param deviceId
     * @param start
     * @return
     */
    @Override
    public List<DeviceEvents> get(final String deviceId, final DateTime start) {
        return get(deviceId, start, DEFAULT_LIMIT_SIZE);
    }

    @Override
    public List<DeviceEvents> getAlarms(final String deviceId, final DateTime oldest, final DateTime newest) {
        final Map<String, Condition> queryConditions = new HashMap<>();
        final Map<String, Condition> filterConditions = new HashMap<>();

        final Condition byDeviceId = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(deviceId));


        final Condition byTime = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN)
                .withAttributeValueList(new AttributeValue().withS(dateTimeToString(oldest)), new AttributeValue().withS(dateTimeToString(newest)));

        final Condition alarms = new Condition()
                .withComparisonOperator(ComparisonOperator.CONTAINS)
                .withAttributeValueList(new AttributeValue().withS("alarm:ring"));

        queryConditions.put(DEVICE_ID_ATTRIBUTE_NAME, byDeviceId);
        queryConditions.put(CREATED_AT_ATTRIBUTE_NAME, byTime);


        filterConditions.put(EVENTS_ATTRIBUTE_NAME, alarms);


        final QueryRequest queryRequest = new QueryRequest();
        queryRequest.withTableName(tableName)
                .withKeyConditions(queryConditions)
                .withQueryFilter(filterConditions);
        final QueryResult queryResult = amazonDynamoDB.query(queryRequest);

        return fromDynamoDBItems(queryResult.getItems());
    }


    @Override
    public Integer write(final List<DeviceEvents> deviceEventsList) {
        if (deviceEventsList == null || deviceEventsList.isEmpty()) {
            return 0;
        }

        try {
            final List<DeviceEvents> reversed = Lists.reverse(deviceEventsList);
            for (final List<DeviceEvents> deviceEventsSublist : Lists.partition(reversed, BATCH_SIZE)) {

                int retries = 0;
                final Map<String, List<WriteRequest>> requests = Maps.newHashMap();
                final List<WriteRequest> writeRequests = Lists.newArrayList();

                final Multimap<String, String> eventsPerSecond = transform(deviceEventsSublist);
                for (final String key : eventsPerSecond.asMap().keySet()) {
                    final String[] parts = key.split("\\|");
                    final String deviceId = parts[0];
                    final String createdAt = parts[1];

                    if (eventsPerSecond.get(key).isEmpty()) {
                        LOGGER.debug("Skipping empty events list for device_id = {}", deviceId);
                        continue;
                    }

                    final Map<String, AttributeValue> req = Maps.newHashMap();
                    req.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
                    req.put(CREATED_AT_ATTRIBUTE_NAME, new AttributeValue().withS(createdAt));

                    final Set<String> events = ImmutableSet.copyOf(eventsPerSecond.get(key));
                    req.put(EVENTS_ATTRIBUTE_NAME, new AttributeValue().withSS(events));

                    final PutRequest pr = new PutRequest().withItem(req);

                    writeRequests.add(new WriteRequest().withPutRequest(pr));

                    if (writeRequests.isEmpty()) {
                        LOGGER.warn("No requests to send to dynamoDB");
                        continue;
                    }
                }

                requests.put(tableName, writeRequests);
                BatchWriteItemResult results = amazonDynamoDB.batchWriteItem(requests);

                while (!results.getUnprocessedItems().isEmpty() && retries < MAX_RETRY) {

                    retries += 1;
                    try {
                        LOGGER.debug("retrying for the {} time", retries);
                        Thread.sleep(retries * retries * 500L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    results = amazonDynamoDB.batchWriteItem(results.getUnprocessedItems());
                }

            }

            return deviceEventsList.size();
        } catch (Exception e) {
            LOGGER.error("Failed saving sense events: {}", e.getMessage());
        }

        return 0;
    }

    public static Optional<DeviceEvents> fromDynamoDBItem(final Map<String, AttributeValue> item) {
        if(item == null) {
            return Optional.absent();
        }

        if(item.containsKey(DEVICE_ID_ATTRIBUTE_NAME) && item.containsKey(CREATED_AT_ATTRIBUTE_NAME) && item.containsKey(EVENTS_ATTRIBUTE_NAME)) {
            final DeviceEvents deviceEvents = new DeviceEvents(
                    item.get(DEVICE_ID_ATTRIBUTE_NAME).getS(),
                    stringToDateTime(item.get(CREATED_AT_ATTRIBUTE_NAME).getS()),
                    Sets.newHashSet(item.get(EVENTS_ATTRIBUTE_NAME).getSS())
            );
            return Optional.of(deviceEvents);
        }

        return Optional.absent();
    }

    public static List<DeviceEvents> fromDynamoDBItems(final List<Map<String, AttributeValue>> items) {
        final List<DeviceEvents> deviceEventsList = Lists.newArrayList();
        if(items == null || items.isEmpty()) {
            return deviceEventsList;
        }

        for(final Map<String, AttributeValue> item : items) {
            final Optional<DeviceEvents> deviceEvents = fromDynamoDBItem(item);
            if(deviceEvents.isPresent()) {
                deviceEventsList.add(deviceEvents.get());
            }
        }

        return deviceEventsList;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(SenseEventsDynamoDB.DEVICE_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(SenseEventsDynamoDB.CREATED_AT_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(SenseEventsDynamoDB.DEVICE_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(SenseEventsDynamoDB.CREATED_AT_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
