package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.Calibration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CalibrationDynamoDB implements CalibrationDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(CalibrationDynamoDB.class);
    private final static String SENSE_ATTRIBUTE_NAME = "sense_id";
    private final static String DUST_OFFSET_ATTRIBUTE_NAME = "dust_offset";
    private final static Integer MAX_BATCH_QUERY_SIZE = 100;

    private final AmazonDynamoDB dynamoDBClient;
    private final String calibrationTableName;

    public CalibrationDynamoDB(final AmazonDynamoDB dynamoDBClient, final String calibrationTableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.calibrationTableName = calibrationTableName;
    }

    @Override
    public Calibration get(final String senseId) {
        final HashMap<String, AttributeValue> key = Maps.newHashMap();
        key.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(calibrationTableName)
                .withKey(key);

        final GetItemResult getItemResult = dynamoDBClient.getItem(getItemRequest);
        final Map<String, AttributeValue> item = getItemResult.getItem();
        if (item == null) {
            return Calibration.createWithDefaultDustOffset();
        }
        return new Calibration(Integer.valueOf(item.get(DUST_OFFSET_ATTRIBUTE_NAME).getN()));
    }

    @Override
    public void put(final String senseId, final Integer offset) {

        final Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        attributes.put(DUST_OFFSET_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(offset)));

        final PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(calibrationTableName)
                .withItem(attributes);

        final PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);
        LOGGER.debug("{}", putItemResult);
        // TODO: Log consumed capacity
    }

    @Override
    public Map<String, Calibration> getBatch(final Set<String> senseIds) {
        final List<String> senseIdsList = Lists.newArrayList(senseIds);
        final List<List<String>> partitionedSenseIdsList = Lists.partition(senseIdsList, MAX_BATCH_QUERY_SIZE);
        final Map<String, Calibration> calibrationMap = Maps.newHashMap();
        for (final List<String> partitionedSenseIds : partitionedSenseIdsList ) {
            final BatchGetItemRequest batchGetItemRequest = new BatchGetItemRequest();
            final List<Map<String, AttributeValue>> itemKeys = Lists.newArrayList();

            for (final String senseId : partitionedSenseIds) {
                final Map<String, AttributeValue> attributeValueMap = Maps.newHashMap();
                attributeValueMap.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
                itemKeys.add(attributeValueMap);
            }

            final KeysAndAttributes key = new KeysAndAttributes().withKeys(itemKeys).withAttributesToGet(SENSE_ATTRIBUTE_NAME, DUST_OFFSET_ATTRIBUTE_NAME);
            final Map<String, KeysAndAttributes> requestItems = Maps.newHashMap();
            requestItems.put(calibrationTableName, key);
            batchGetItemRequest.withRequestItems(requestItems);

            try {
                final BatchGetItemResult batchGetItemResult = dynamoDBClient.batchGetItem(batchGetItemRequest);
                for (final String item : batchGetItemResult.getResponses().keySet()) {
                    final List<Map<String, AttributeValue>> responses = batchGetItemResult.getResponses().get(item);
                    for (final Map<String, AttributeValue> response : responses) {
                        final String senseId = response.get(SENSE_ATTRIBUTE_NAME).getS();
                        final Integer dustOffset = Integer.valueOf(response.get(DUST_OFFSET_ATTRIBUTE_NAME).getN());
                        calibrationMap.put(senseId, new Calibration(dustOffset));
                    }
                }
            } catch (AmazonServiceException ase) {
                LOGGER.error("Failed getting keys.");
            }
        }
        return calibrationMap;
    }
    
    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(SENSE_ATTRIBUTE_NAME).withKeyType(KeyType.HASH)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(SENSE_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
