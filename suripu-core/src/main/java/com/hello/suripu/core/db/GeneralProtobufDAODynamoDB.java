package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.models.TimelineLog;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
    private final String rangeKeyColumnName;
    private final String payloadColumnName;
    private final Logger logger;

    public GeneralProtobufDAODynamoDB(Logger logger, final AmazonDynamoDB dynamoDBClient, final String tableName, final String hashKeyColumnName, final String rangeKeyColumnName, final String payloadColumnName) {
        this.logger = logger;
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
        this.hashKeyColumnName = hashKeyColumnName;
        this.rangeKeyColumnName = rangeKeyColumnName;
        this.payloadColumnName = payloadColumnName;
    }

    public boolean put(final String key,final String rangeKey,final byte [] payload) {
        final ByteBuffer data = ByteBuffer.wrap(payload);

        final Map<String, AttributeValueUpdate> items = new HashMap<>();
        final DateTime now = DateTime.now();


        final HashMap<String, AttributeValue> keyValueMap = new HashMap<>();
        keyValueMap.put(hashKeyColumnName, new AttributeValue().withS(key));
        keyValueMap.put(rangeKeyColumnName, new AttributeValue().withS(rangeKey));
        keyValueMap.put(payloadColumnName, new AttributeValue().withB(data));


        final PutItemRequest request = new PutItemRequest()
                .withTableName(this.tableName)
                .withItem(keyValueMap);

        try {
            final PutItemResult result = this.dynamoDBClient.putItem(request);
        }catch (AmazonServiceException awsException){
            logger.error("Server exception {} result for key {}, rangekey {}",
                    awsException.getMessage(),
                    key,
                    rangeKey);
            return false;
        }catch (AmazonClientException acExp){

            logger.error("AmazonClientException exception {} result for key {}, rangekey {}",
                    acExp.getMessage(),
                    key,
                    rangeKey);
            return false;

        }

        return true;

    }


    Map<String, byte []> getAllAfterAndIncluding(final String key,final String rangeKey, final int limit) {

        final Map<String, byte []> finalResult = new HashMap<>();
        final Map<String, Condition> queryConditions = Maps.newHashMap();

        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.LE.toString())
                .withAttributeValueList(new AttributeValue().withS(rangeKey));

        queryConditions.put(rangeKeyColumnName, selectDateCondition);

        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(String.valueOf(key)));
        queryConditions.put(hashKeyColumnName, selectAccountIdCondition);

        //put all attributes that you want back from the server in this thing
        final Collection<String> targetAttributeSet = Sets.newHashSet();

        Collections.addAll(targetAttributeSet,
                hashKeyColumnName,
                rangeKeyColumnName,
                payloadColumnName
        );



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
            logger.error("DynamoDB query did not return anything for account_id {} on table {}",key,this.tableName);
            return finalResult;
        }


        //iterate through items
        for(final Map<String, AttributeValue> item : items) {
            if (!item.keySet().containsAll(targetAttributeSet)) {
                logger.warn("Missing field in item {}", item);
                continue;
            }

            final String thisItemsKey = item.get(hashKeyColumnName).getS();

            final ByteBuffer byteBuffer = item.get(payloadColumnName).getB();
            final byte[] protoData = byteBuffer.array();

            finalResult.put(thisItemsKey,protoData);
        }

        return finalResult;

    }



}
