package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.models.Calibration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CalibrationDynamoDB implements CalibrationDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(CalibrationDynamoDB.class);

    private final static String SENSE_ATTRIBUTE_NAME = "sense_id";
    private final static String DUST_OFFSET_ATTRIBUTE_NAME = "dust_offset";
    private final static String TESTED_AT_ATTRIBUTE_NAME = "tested_at";

    private final static Integer MAX_GET_SIZE = 100;
    public final static Integer MAX_PUT_SIZE = 50;
    private final static Integer MAX_PUT_FORCE_SIZE_PER_BATCH = 25;
    public final static Integer MAX_PUT_FORCE_SIZE = 500;

    private final AmazonDynamoDB dynamoDBClient;
    private final String calibrationTableName;

    public CalibrationDynamoDB(final AmazonDynamoDB dynamoDBClient, final String calibrationTableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.calibrationTableName = calibrationTableName;
    }


    @Override
    public Calibration get(final String senseId) {
        return getRemotely(senseId, Boolean.FALSE).get();
    }


    @Override
    public Optional<Calibration> getStrict(final String senseId) {
        return getRemotely(senseId, Boolean.TRUE);
    }

    private Optional<Calibration> getRemotely(final String senseId, final Boolean strict) {
        final HashMap<String, AttributeValue> key = Maps.newHashMap();
        key.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(calibrationTableName)
                .withKey(key);

        final GetItemResult getItemResult = dynamoDBClient.getItem(getItemRequest);
        final Map<String, AttributeValue> item = getItemResult.getItem();
        if (item == null) {
            if (strict) {
                LOGGER.warn("Sense {} does not have calibration", senseId);
                return Optional.absent();
            }
            LOGGER.warn("Not in strict mode, returning default calibration for sense {}", senseId);
            return Optional.of(Calibration.createDefault(senseId));
        }
        return Optional.of(Calibration.create(senseId, Integer.valueOf(item.get(DUST_OFFSET_ATTRIBUTE_NAME).getN()), Long.valueOf(item.get(TESTED_AT_ATTRIBUTE_NAME).getN())));
    }


    @Override
    public Optional<Boolean> putForce(final Calibration calibration) {
        final Optional<Boolean> hasPutItem = putWithoutCondition(calibration, false);
        return hasPutItem;
    }


    @Override
    public Optional<Boolean> put(final Calibration calibration) {
        final Optional<Boolean> hasAddedItem = putWithoutCondition(calibration, true);
        if (hasAddedItem.isPresent()) {
            if(!hasAddedItem.get()) {
                final Optional<Boolean> hasUpdatedItem = putWithConditionIfExist(calibration);
                return hasUpdatedItem;
            }
        }
        return hasAddedItem;
    }


    @Override
    public Map<String, Optional<Boolean>> putBatchForce(final List<Calibration> calibrations) {
        if (calibrations.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, Optional<Boolean>> putResults = Maps.newHashMap();

        for (final Calibration calibration : calibrations) {
            putResults.put(calibration.senseId, Optional.of(Boolean.TRUE));
        }

        final List<List<Calibration>> partitionedCalibrationsList = Lists.partition(calibrations, MAX_PUT_FORCE_SIZE_PER_BATCH);

        for (final List<Calibration> partitionedCalibrations : partitionedCalibrationsList) {
            final List<WriteRequest> calibrationPutRequests = new ArrayList<>();
            for (final Calibration calibration : partitionedCalibrations) {
                calibrationPutRequests.add(new WriteRequest().withPutRequest(new PutRequest().withItem(getAttributeMapFromCalibration(calibration))));
            }
            Map<String, List<WriteRequest>> requestItems = Maps.newHashMap();
            requestItems.put(calibrationTableName, calibrationPutRequests);
            final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest().withRequestItems(requestItems);
            try {
                final BatchWriteItemResult batchWriteItemResult = dynamoDBClient.batchWriteItem(batchWriteItemRequest);
                final Map<String, List<WriteRequest>> unprocessedItems = batchWriteItemResult.getUnprocessedItems();
                if (unprocessedItems.containsKey(calibrationTableName)) {
                    for (final WriteRequest writeRequest : unprocessedItems.get(calibrationTableName)) {
                        putResults.put(writeRequest.getPutRequest().getItem().get(SENSE_ATTRIBUTE_NAME).getS(), Optional.<Boolean>absent());
                    }
                }
            }
            catch (AmazonServiceException ase) {
                LOGGER.error(ase.getMessage());
            }
        }
        return putResults;
    }


    @Override
    public Map<String, Optional<Boolean>> putBatch(final List<Calibration> calibrations) {
        if (calibrations.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, Optional<Boolean>> putResults = Maps.newHashMap();

        for (final Calibration calibration : calibrations) {
            final Optional<Boolean> hasPutSuccessfully = put(calibration);
            putResults.put(calibration.senseId, hasPutSuccessfully);
        }
        return putResults;
    }


    private Optional<Boolean> putWithoutCondition(final Calibration calibration, final Boolean checkExist) {
        final Map<String, AttributeValue> attributes = getAttributeMapFromCalibration(calibration);

        PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(calibrationTableName)
                .withItem(attributes);

        if (checkExist) {
            final Map<String, ExpectedAttributeValue> putConditions = Maps.newHashMap();
            putConditions.put(SENSE_ATTRIBUTE_NAME, new ExpectedAttributeValue(false));

            putItemRequest = new PutItemRequest()
                    .withTableName(calibrationTableName)
                    .withItem(attributes)
                    .withExpected(putConditions);
        }

        try {
            final PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);
            if (putItemResult != null) {
                return Optional.of(Boolean.TRUE);
            };
        }
        catch (ConditionalCheckFailedException cce) {
            LOGGER.info("Put condition failed for sense_id {} - tested_at {}", calibration.senseId, calibration.testedAt);
            return Optional.of(Boolean.FALSE);
        }
        catch (AmazonServiceException ase) {
            LOGGER.error(ase.getMessage());
        }

        return Optional.absent();
    }

    private Optional<Boolean> putWithConditionIfExist(final Calibration calibration) {
        final HashMap<String, AttributeValue> key = Maps.newHashMap();
        key.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(calibration.senseId));

        final Map<String, AttributeValueUpdate> attributeUpdates = Maps.newHashMap();
        attributeUpdates.put(DUST_OFFSET_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(calibration.dustOffset))));
        attributeUpdates.put(TESTED_AT_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(calibration.testedAt))));

        final Map<String, ExpectedAttributeValue> putConditions = Maps.newHashMap();
        putConditions.put(TESTED_AT_ATTRIBUTE_NAME, new ExpectedAttributeValue()
                .withComparisonOperator(ComparisonOperator.LT)
                .withValue(new AttributeValue().withN(String.valueOf(calibration.testedAt))));

        final UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(calibrationTableName)
                .withKey(key)
                .withAttributeUpdates(attributeUpdates)
                .withExpected(putConditions);

        try {
            final UpdateItemResult updateItemResult = dynamoDBClient.updateItem(updateItemRequest);
            return Optional.of(updateItemResult != null);
        }
        catch (ConditionalCheckFailedException cce) {
            LOGGER.info("Update condition failed for sense_id {} - tested_at {}", calibration.senseId, calibration.testedAt);
            return Optional.of(Boolean.FALSE);
        }
        catch (AmazonServiceException ase) {
            LOGGER.error(ase.getMessage());
        }
        return Optional.absent();
    }


    @Override
    public Map<String, Calibration> getBatch(final Set<String> senseIds) {
        return getBatchRemotely(senseIds, Boolean.FALSE);
    }

    @Override
    public Map<String, Calibration> getBatchStrict(final Set<String> senseIds) {
        return getBatchRemotely(senseIds, Boolean.TRUE);
    }

    private Map<String, Calibration> getBatchRemotely(final Set<String> senseIds, final Boolean strict) {
        final Map<String, Calibration> calibrationMap = Maps.newHashMap();
        final Set<String> calibratedSenseIds = Sets.newHashSet();

        if (!strict) {
            for (final String senseId : senseIds) {
                calibrationMap.put(senseId, Calibration.createDefault(senseId));
            }
        }

        final List<String> senseIdsList = Lists.newArrayList(senseIds);
        final List<List<String>> partitionedSenseIdsList = Lists.partition(senseIdsList, MAX_GET_SIZE);


        for (final List<String> partitionedSenseIds : partitionedSenseIdsList ) {
            final BatchGetItemRequest batchGetItemRequest = new BatchGetItemRequest();
            final List<Map<String, AttributeValue>> itemKeys = Lists.newArrayList();


            for (final String senseId : partitionedSenseIds) {
                final Map<String, AttributeValue> attributeValueMap = Maps.newHashMap();
                attributeValueMap.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
                itemKeys.add(attributeValueMap);
            }

            final KeysAndAttributes key = new KeysAndAttributes().withKeys(itemKeys).withAttributesToGet(SENSE_ATTRIBUTE_NAME, DUST_OFFSET_ATTRIBUTE_NAME, TESTED_AT_ATTRIBUTE_NAME);
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
                        final Long testedAt = Long.valueOf(response.get(TESTED_AT_ATTRIBUTE_NAME).getN());

                        calibrationMap.put(senseId, Calibration.create(senseId, dustOffset, testedAt));
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

    @Override
    public Boolean delete(String senseId) {
        final HashMap<String, AttributeValue> key = Maps.newHashMap();
        key.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        final DeleteItemRequest deleteItemRequest = new DeleteItemRequest()
                .withTableName(calibrationTableName)
                .withKey(key);
        try {
            final DeleteItemResult deleteItemResult = dynamoDBClient.deleteItem(deleteItemRequest);
            return deleteItemResult != null;
        }
        catch (AmazonServiceException ase) {
            LOGGER.error(ase.getMessage());
        }
        return Boolean.FALSE;
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

    private Map<String, AttributeValue> getAttributeMapFromCalibration(final Calibration calibration) {
        final Map<String, AttributeValue> attributes = Maps.newHashMap();
        attributes.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(calibration.senseId));
        attributes.put(DUST_OFFSET_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(calibration.dustOffset)));
        attributes.put(TESTED_AT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(calibration.testedAt)));
        return attributes;
    }
}
