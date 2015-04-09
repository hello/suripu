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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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


    public Integer write(final List<DeviceEvents> deviceEventsList) {

        if(deviceEventsList.isEmpty()) {
            LOGGER.warn("Can't persist empty list of events");
            return 0;
        }


        final List<DeviceEvents> reversed = Lists.reverse(deviceEventsList);
        for(final List<DeviceEvents> deviceEventsSublist : Lists.partition(reversed, 20)) {

            int retries = 0;
            final Map<String, List<WriteRequest>> requests = Maps.newHashMap();
            final List<WriteRequest> writeRequests = Lists.newArrayList();

            final Set<String> uniquesHashRange = Sets.newHashSet();
            for (final DeviceEvents deviceEvents : deviceEventsSublist) {


                final String createdAt = deviceEvents.createdAt.toString("yyyy-MM-dd HH:mm:ss");
                final String uniqueKey = String.format("%s-%s", deviceEvents.deviceId, createdAt);
                if(!uniquesHashRange.add(uniqueKey)) {
                    LOGGER.warn("Key {} already exists in this batch write requests, keeping first one seen.", uniqueKey);
                    continue;
                }

                if (deviceEvents.events.isEmpty()) {
                    LOGGER.debug("Skipping empty events list for device_id = {}", deviceEvents.deviceId);
                    continue;
                }

                final Map<String, AttributeValue> req = Maps.newHashMap();
                req.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceEvents.deviceId));

                req.put(CREATED_AT_ATTRIBUTE_NAME, new AttributeValue().withS(createdAt));
                final Set<String> events = Sets.newHashSet();
                for (final String key : deviceEvents.events.keySet()) {
                    final String event = String.format("%s_%s", key, deviceEvents.events.get(key));
                    events.add(event);
                }
                req.put(EVENTS_ATTRIBUTE_NAME, new AttributeValue().withSS(events));
                final PutRequest pr = new PutRequest().withItem(req);

                writeRequests.add(new WriteRequest().withPutRequest(pr));
            }

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
                amazonDynamoDB.batchWriteItem(results.getUnprocessedItems());
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
