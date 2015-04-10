package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.hello.suripu.core.metrics.DeviceEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SenseEventsDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseEventsDAO.class);

    public final static String DEVICE_ID_ATTRIBUTE_NAME = "device_id";
    public final static String CREATED_AT_ATTRIBUTE_NAME = "created_at";
    public final static String EVENTS_ATTRIBUTE_NAME = "events";

    private final AmazonDynamoDB amazonDynamoDB;
    private final String tableName;

    public SenseEventsDAO(AmazonDynamoDB amazonDynamoDB, String tableName) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.tableName = tableName;
    }


    public static String deviceEventsKey(final DeviceEvents deviceEvents) {
        final String createdAt = deviceEvents.createdAt.toString("yyyy-MM-dd HH:mm:ss");
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


    public Integer write(final List<DeviceEvents> deviceEventsList) {

        if(deviceEventsList.isEmpty()) {
            return 0;
        }

        final List<DeviceEvents> reversed = Lists.reverse(deviceEventsList);
        for(final List<DeviceEvents> deviceEventsSublist : Lists.partition(reversed, 20)) {

            int retries = 0;
            final Map<String, List<WriteRequest>> requests = Maps.newHashMap();
            final List<WriteRequest> writeRequests = Lists.newArrayList();


            final Multimap<String, String> eventsPerSecond = transform(deviceEventsSublist);
            for(final String key : eventsPerSecond.asMap().keySet()) {
                final String[] parts = key.split("\\|");
                final String deviceId = parts[0];
                final String createdAt = parts[1];

                if(eventsPerSecond.get(key).isEmpty()) {
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

                if(writeRequests.isEmpty()) {
                    LOGGER.warn("No requests to send to dynamoDB");
                    continue;
                }


                requests.put(tableName, writeRequests);
                BatchWriteItemResult results = amazonDynamoDB.batchWriteItem(requests);

                while(!results.getUnprocessedItems().isEmpty() && retries < 5) {

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

        }

        return deviceEventsList.size();
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(SenseEventsDAO.DEVICE_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(SenseEventsDAO.CREATED_AT_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(SenseEventsDAO.DEVICE_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(SenseEventsDAO.CREATED_AT_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
