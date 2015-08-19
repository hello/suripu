package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 8/18/15.
 */
public class GeneralProtobufDAODynamoDB {
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;
    private final String hashKeyColumnName;
    private final Optional<String> rangeKeyColumnName;
    private final String payloadColumnName;
    private final Logger logger;

    public GeneralProtobufDAODynamoDB(Logger logger, final AmazonDynamoDB dynamoDBClient, final String tableName, final String hashKeyColumnName, final Optional<String> rangeKeyColumnName, final String payloadColumnName) {
        this.logger = logger;
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
        this.hashKeyColumnName = hashKeyColumnName;
        this.rangeKeyColumnName = rangeKeyColumnName;
        this.payloadColumnName = payloadColumnName;
    }

    public boolean put(final String key, final String rangeKey, final byte[] payload) {
        final ByteBuffer data = ByteBuffer.wrap(payload);

        final Map<String, AttributeValueUpdate> items = new HashMap<>();
        final DateTime now = DateTime.now();


        final HashMap<String, AttributeValue> keyValueMap = new HashMap<>();
        keyValueMap.put(hashKeyColumnName, new AttributeValue().withS(key));
        if (rangeKeyColumnName.isPresent()) {
            keyValueMap.put(rangeKeyColumnName.get(), new AttributeValue().withS(rangeKey));
        }
        keyValueMap.put(payloadColumnName, new AttributeValue().withB(data));


        final PutItemRequest request = new PutItemRequest()
                .withTableName(this.tableName)
                .withItem(keyValueMap);

        try {
            final PutItemResult result = this.dynamoDBClient.putItem(request);
        } catch (AmazonServiceException awsException) {
            logger.error("Server exception {} result for key {}, rangekey {}",
                    awsException.getMessage(),
                    key,
                    rangeKey);
            return false;
        } catch (AmazonClientException acExp) {

            logger.error("AmazonClientException exception {} result for key {}, rangekey {}",
                    acExp.getMessage(),
                    key,
                    rangeKey);
            return false;

        }

        return true;

    }

    public Map<String, byte[]> getBySingleKeyNoRangeKey(final String key) {
        final Map<String, Condition> queryConditions = Maps.newHashMap();

        return getBySingleKey(key, queryConditions, 1);

    }

    public Map<String, byte[]> getBySingleKeyLessThanRangeKey(final String key, final String rangeKey, final int limit) {

        final Map<String, Condition> queryConditions = Maps.newHashMap();

        if (rangeKeyColumnName.isPresent()) {

            final Condition selectRangeKeyCondition = new Condition()
                    .withComparisonOperator(ComparisonOperator.LE.toString())
                    .withAttributeValueList(new AttributeValue().withS(rangeKey));

            queryConditions.put(rangeKeyColumnName.get(), selectRangeKeyCondition);
        }

        return getBySingleKey(key, queryConditions, limit);
    }

    public Map<String, byte[]> getByMultipleKeysLessThanRangeKey(final List<String> keys, final String rangeKey, final int limit) {

        final Map<String, Condition> queryConditions = Maps.newHashMap();

        if (rangeKeyColumnName.isPresent()) {

            final Condition selectRangeKeyCondition = new Condition()
                    .withComparisonOperator(ComparisonOperator.LE.toString())
                    .withAttributeValueList(new AttributeValue().withS(rangeKey));

            queryConditions.put(rangeKeyColumnName.get(), selectRangeKeyCondition);
        }

        return getByMultipleKeys(keys, queryConditions, limit);
    }

    public Map<String, byte[]> getBySingleKeyBetweenRangeKeys(final String key, final String rangeKey1, final String rangeKey2, final int limit) {

        final Map<String, Condition> queryConditions = Maps.newHashMap();

        if (rangeKeyColumnName.isPresent()) {

            final List<AttributeValue> values = Lists.newArrayList();

            values.add(new AttributeValue().withS(rangeKey1));
            values.add(new AttributeValue().withS(rangeKey2));

            final Condition selectRangeKeyCondition = new Condition()
                    .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                    .withAttributeValueList(values);

            queryConditions.put(rangeKeyColumnName.get(), selectRangeKeyCondition);
        }

        return getBySingleKey(key, queryConditions, limit);
    }


    public Map<String, byte[]> getByMultipleKeys(final List<String> keys, final Map<String, Condition> queryConditions, final int limit) {
        final List<AttributeValue> values = Lists.newArrayList();

        for (final String key : keys) {
            values.add(new AttributeValue().withS(key));
        }

        final Condition selectHashKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.IN)
                .withAttributeValueList(values);

        queryConditions.put(hashKeyColumnName, selectHashKeyCondition);

        return get(queryConditions, limit);
    }

    public Map<String, byte[]> getBySingleKey(final String key, final Map<String, Condition> queryConditions, final int limit) {


        final Condition selectHashKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(String.valueOf(key)));
        queryConditions.put(hashKeyColumnName, selectHashKeyCondition);

        return get(queryConditions, limit);
    }


    public Map<String, byte[]> get(final Map<String, Condition> queryConditions, final int limit) {

        final Map<String, byte[]> finalResult = new HashMap<>();

        final Collection<String> targetAttributeSet = Sets.newHashSet();

        Collections.addAll(targetAttributeSet,
                hashKeyColumnName,
                payloadColumnName
        );

        if (rangeKeyColumnName.isPresent()) {
            targetAttributeSet.add(rangeKeyColumnName.get());
        }


        // Perform query
        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(this.tableName)
                .withKeyConditions(queryConditions)
                .withAttributesToGet(targetAttributeSet)
                .withScanIndexForward(false)
                .withLimit(limit);

        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        final List<Map<String, AttributeValue>> items = queryResult.getItems();


        if (items == null) {
            logger.error("DynamoDB query did not return anything for query {} on table {}", queryRequest, this.tableName);
            return finalResult;
        }


        //iterate through items
        for (final Map<String, AttributeValue> item : items) {
            if (!item.keySet().containsAll(targetAttributeSet)) {
                logger.warn("Missing field in item {}", item);
                continue;
            }

            final String thisItemsKey = item.get(hashKeyColumnName).getS();

            final ByteBuffer byteBuffer = item.get(payloadColumnName).getB();
            final byte[] protoData = byteBuffer.array();

            finalResult.put(thisItemsKey, protoData);
        }

        return finalResult;

    }

    public static CreateTableResult createTable(final String tableName,final String hashKeyColumnName, final Optional<String> rangeKeyColumnName, final AmazonDynamoDBClient dynamoDBClient, final int readThroughput, final int writeThroughput){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        if (rangeKeyColumnName.isPresent()) {
            request.withKeySchema(
                    new KeySchemaElement().withAttributeName(hashKeyColumnName).withKeyType(KeyType.HASH),
                    new KeySchemaElement().withAttributeName(rangeKeyColumnName.get()).withKeyType(KeyType.RANGE)
            );

            request.withAttributeDefinitions(
                    new AttributeDefinition().withAttributeName(hashKeyColumnName).withAttributeType(ScalarAttributeType.S),
                    new AttributeDefinition().withAttributeName(rangeKeyColumnName.get()).withAttributeType(ScalarAttributeType.S)

            );

        }
        else {
            request.withKeySchema(
                    new KeySchemaElement().withAttributeName(hashKeyColumnName).withKeyType(KeyType.HASH)
            );

            request.withAttributeDefinitions(
                    new AttributeDefinition().withAttributeName(hashKeyColumnName).withAttributeType(ScalarAttributeType.S)

            );
        }


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits((long)readThroughput)
                .withWriteCapacityUnits((long) writeThroughput));

        return dynamoDBClient.createTable(request);
    }
}
