package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 9/23/14.
 */
public class RingTimeDAODynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(AlarmDAODynamoDB.class);
    private final AmazonDynamoDBClient dynamoDBClient;
    private final String tableName;

    public static final String MORPHEUS_ID_ATTRIBUTE_NAME = "device_id";

    public static final String RING_AT_ATTRIBUTE_NAME = "ring_time";
    public static final String ALARM_DATE_LOCAL_UTC_ATTRIBUTE_NAME = "alarm_local_date";


    public RingTimeDAODynamoDB(final AmazonDynamoDBClient dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }


    public Optional<DateTime> getRingTime(final String deviceId, final DateTime dateLocalUTC){
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();
        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(dateLocalUTC.getMillis())));


        queryConditions.put(ALARM_DATE_LOCAL_UTC_ATTRIBUTE_NAME, selectDateCondition);


        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(deviceId));
        queryConditions.put(MORPHEUS_ID_ATTRIBUTE_NAME, selectAccountIdCondition);

        final Collection<String> targetAttributeSet = new HashSet<String>();
        Collections.addAll(targetAttributeSet,
                MORPHEUS_ID_ATTRIBUTE_NAME,
                RING_AT_ATTRIBUTE_NAME,
                ALARM_DATE_LOCAL_UTC_ATTRIBUTE_NAME);

        final QueryRequest queryRequest = new QueryRequest(this.tableName)
                .withKeyConditions(queryConditions)
                .withAttributesToGet(targetAttributeSet)
                .withLimit(1)
                .withScanIndexForward(true);

        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        if(queryResult.getItems() == null){
            return Optional.absent();
        }

        final List<Map<String, AttributeValue>> items = queryResult.getItems();

        for(final Map<String, AttributeValue> item:items){
            if(!item.keySet().containsAll(targetAttributeSet)){
                LOGGER.warn("Missing field in item {}", item);
                continue;
            }

            try {
                return Optional.of(new DateTime(Long.valueOf(item.get(RING_AT_ATTRIBUTE_NAME).getN()), DateTimeZone.UTC));
            }catch (Exception ex){
                LOGGER.error("Get ring time failed for device {} on date {}", deviceId, new DateTime(dateLocalUTC, DateTimeZone.UTC));
            }
        }

        return Optional.absent();
    }

    public void setRingTime(final String deviceId, final DateTime dateLocalUTC, final DateTime ringTime, final DateTimeZone localTimeZone){
        final DateTime localRingTime = new DateTime(ringTime.getMillis(), localTimeZone);
        if(dateLocalUTC.getYear() != localRingTime.getYear() ||
                dateLocalUTC.getMonthOfYear() != localRingTime.getMonthOfYear() ||
                dateLocalUTC.getDayOfMonth() != localRingTime.getDayOfMonth()){
            throw new IllegalArgumentException("The alarm is not for the target date!");
        }

        final HashMap<String, AttributeValue> items = new HashMap<String, AttributeValue>();
        items.put(MORPHEUS_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
        items.put(ALARM_DATE_LOCAL_UTC_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(dateLocalUTC.getMillis())));
        items.put(RING_AT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(ringTime.getMillis())));

        final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, items);
        final PutItemResult result = this.dynamoDBClient.putItem(putItemRequest);


    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(MORPHEUS_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(ALARM_DATE_LOCAL_UTC_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(MORPHEUS_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(ALARM_DATE_LOCAL_UTC_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }


}
