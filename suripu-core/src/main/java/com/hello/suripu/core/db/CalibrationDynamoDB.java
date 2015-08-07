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
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
    private final static String METADATA_ATTRIBUTE_NAME = "metadata";

    private final static Integer MAX_BATCH_QUERY_SIZE = 100;
    public final static String DEFAULT_FACTORY_DEVICE_ID = "0000000000000000";

    private final AmazonDynamoDB dynamoDBClient;
    private final String calibrationTableName;

    public CalibrationDynamoDB(final AmazonDynamoDB dynamoDBClient, final String calibrationTableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.calibrationTableName = calibrationTableName;
    }

    @Override
    public Optional<Calibration> get(final String senseId) {
        return getRemotely(senseId, Boolean.FALSE);
    }

    @Override
    public Optional<Calibration> getStrict(final String senseId) {
        return getRemotely(senseId, Boolean.TRUE);
    }

    private Optional<Calibration> getRemotely(final String senseId, final Boolean strict) {
        if(DEFAULT_FACTORY_DEVICE_ID.equals(senseId) ) {
            if (strict.equals(Boolean.TRUE)) {
                LOGGER.warn("Factory sense {} needs no calibration", senseId);
                return Optional.absent();
            }
            LOGGER.warn("Not in strict mode, returning default calibration for factory sense");
            return Optional.of(Calibration.createDefault(senseId));

        }
        final HashMap<String, AttributeValue> key = Maps.newHashMap();
        key.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(calibrationTableName)
                .withKey(key);

        final GetItemResult getItemResult = dynamoDBClient.getItem(getItemRequest);
        final Map<String, AttributeValue> item = getItemResult.getItem();
        if (item == null) {
            if (strict.equals(Boolean.TRUE)) {
                LOGGER.warn("Sense {} does not have calibration", senseId);
                return Optional.absent();
            }
            LOGGER.warn("Not in strict mode, returning default calibration for sense {}", senseId);
            return Optional.of(Calibration.createDefault(senseId));
        }
        return Optional.of(Calibration.create(senseId, Integer.valueOf(item.get(DUST_OFFSET_ATTRIBUTE_NAME).getN()), item.get(METADATA_ATTRIBUTE_NAME).getS()));
    }

    @Override
    public void put(final String senseId, final Integer dustOffset, final String metadata) {
        final Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        attributes.put(DUST_OFFSET_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(dustOffset)));
        attributes.put(METADATA_ATTRIBUTE_NAME, new AttributeValue().withS(metadata));

        final PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(calibrationTableName)
                .withItem(attributes);

        final PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);
        LOGGER.debug("{}", putItemResult);
        // TODO: Log consumed capacity
    }

    @Override
    public Map<String, Calibration> getBatch(final Set<String> senseIds) {
        final Map<String, Calibration> calibrationMap = Maps.newHashMap();
        final Set<String> calibratedSenseIds = Sets.newHashSet();

        for (final String senseId : senseIds) {
            calibrationMap.put(senseId, Calibration.createDefault(senseId));
        }

        final List<String> senseIdsList = Lists.newArrayList(senseIds);
        final List<List<String>> partitionedSenseIdsList = Lists.partition(senseIdsList, MAX_BATCH_QUERY_SIZE);


        for (final List<String> partitionedSenseIds : partitionedSenseIdsList ) {
            final BatchGetItemRequest batchGetItemRequest = new BatchGetItemRequest();
            final List<Map<String, AttributeValue>> itemKeys = Lists.newArrayList();


            for (final String senseId : partitionedSenseIds) {
                final Map<String, AttributeValue> attributeValueMap = Maps.newHashMap();
                attributeValueMap.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
                itemKeys.add(attributeValueMap);
            }

            final KeysAndAttributes key = new KeysAndAttributes().withKeys(itemKeys).withAttributesToGet(SENSE_ATTRIBUTE_NAME, DUST_OFFSET_ATTRIBUTE_NAME, METADATA_ATTRIBUTE_NAME);
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
                        final String metadata = response.containsKey(METADATA_ATTRIBUTE_NAME) ? response.get(METADATA_ATTRIBUTE_NAME).getS() : "";

                        calibrationMap.put(senseId, Calibration.create(senseId, dustOffset, metadata));
                        calibratedSenseIds.add(senseId);
                    }
                }
            } catch (AmazonServiceException ase) {
                LOGGER.error("Failed to get calibration for {}", partitionedSenseIds);
            }

            final Set<String> uncalibratedSenseIds = Sets.difference(senseIds, calibratedSenseIds);

            if (!uncalibratedSenseIds.isEmpty()) {
                LOGGER.warn("There are {} senses without calibration data", uncalibratedSenseIds.size());
                LOGGER.trace("Senses without calibration data {}", uncalibratedSenseIds);
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
