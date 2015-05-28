package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
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
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.TimeZoneHistory;
import com.yammer.metrics.annotation.Timed;
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
 * Created by pangwu on 6/13/14.
 */
public class TimeZoneHistoryDAODynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(TimeZoneHistoryDAODynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";

    public static final String UPDATED_AT_ATTRIBUTE_NAME = "updated_at_server_time_millis";
    //public static final String OFFSET_MILLIS_ATTRIBUTE_NAME = "offset_millis";
    public static final String TIMEZONE_NAME_ATTRIBUTE_NAME = "time_zone_name";

    private static int MAX_CALL_COUNT = 3;


    public TimeZoneHistoryDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }


    @Timed
    public Optional<TimeZoneHistory> updateTimeZone(long accountId, final String clientTimeZoneId, int clientTimeZoneOffsetMillis){

        final long updatedAt = DateTime.now().getMillis();
        DateTimeZone clientTimeZone = DateTimeZone.UTC;

        try{
            clientTimeZone = DateTimeZone.forID(clientTimeZoneId);
        }catch (IllegalArgumentException ex){
            clientTimeZone = DateTimeZone.forOffsetMillis(clientTimeZoneOffsetMillis);
        }

        String timeZoneId = clientTimeZone.getID();


        final Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));
        item.put(UPDATED_AT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(updatedAt)));
        item.put(TIMEZONE_NAME_ATTRIBUTE_NAME, new AttributeValue().withS(timeZoneId));

        final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, item);
        final PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);

        if(putItemResult != null){
            LOGGER.debug("Update time zone for account: {}, offset: {}, id: {}", accountId, clientTimeZone.getOffset(DateTime.now().getMillis()), timeZoneId);
            return Optional.of(new TimeZoneHistory(updatedAt, clientTimeZone.getOffset(DateTime.now().getMillis()), timeZoneId));
        }

        LOGGER.warn("Account {} update timezone failed, AWS error.", accountId);
        return Optional.absent();

    }


    @Deprecated
    @Timed
    public Optional<TimeZoneHistory> getTimeZoneAt(long accountId, long instant){
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();

        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.LE.toString())
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(DateTime.now().getMillis())));


        queryConditions.put(UPDATED_AT_ATTRIBUTE_NAME, selectDateCondition);

        // AND accound_id = :accound_id
        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectAccountIdCondition);

        Map<String, AttributeValue> lastEvaluatedKey = null;
        final Collection<String> targetAttributeSet = new HashSet<String>();
        Collections.addAll(targetAttributeSet,
                UPDATED_AT_ATTRIBUTE_NAME,
                TIMEZONE_NAME_ATTRIBUTE_NAME);


        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(this.tableName)
                .withKeyConditions(queryConditions)
                .withAttributesToGet(targetAttributeSet)
                .withLimit(1)
                .withScanIndexForward(false);

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


            final DateTimeZone dateTimeZoneFromId = DateTimeZone.forID(item.get(TIMEZONE_NAME_ATTRIBUTE_NAME).getS());

            final int offsetMillis = dateTimeZoneFromId.getOffset(instant);
            final long updatedAt = Long.valueOf(item.get(UPDATED_AT_ATTRIBUTE_NAME).getN());
            LOGGER.debug("Get time zone for account: {}, offset: {}, id: {}", accountId, offsetMillis, dateTimeZoneFromId.getID());
            return Optional.of(new TimeZoneHistory(updatedAt, offsetMillis, dateTimeZoneFromId.getID()));
        }

        LOGGER.warn("Account {} get timezone failed, no data or aws error.", accountId);
        return Optional.absent();
    }

    @Timed
    public Optional<TimeZoneHistory> getCurrentTimeZone(final long accountId){

        final Optional<TimeZoneHistory> lastTimeZone = getTimeZoneAt(accountId, DateTime.now().getMillis());
        if(!lastTimeZone.isPresent()){
            return Optional.absent();
        }

        return lastTimeZone;

    }

    @Timed
    public Map<DateTime, TimeZoneHistory> getAllTimeZones(final long accountId){
        final Map<String, Condition> queryConditions = Maps.newHashMap();
        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectAccountIdCondition);

        final Collection<String> targetAttributeSet = new HashSet<String>();
        Collections.addAll(targetAttributeSet,
                UPDATED_AT_ATTRIBUTE_NAME,
                TIMEZONE_NAME_ATTRIBUTE_NAME);

        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(tableName)
                .withKeyConditions(queryConditions)
                .withAttributesToGet(targetAttributeSet)
                .withLimit(100)
                .withScanIndexForward(false);

        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        final List<Map<String, AttributeValue>> items = queryResult.getItems();

        if(items == null){
            return Collections.EMPTY_MAP;
        }

        final Map<DateTime, TimeZoneHistory> timeZoneHistoryMap = Maps.newHashMap();

        for(final Map<String, AttributeValue> item:items){
            if(!item.keySet().containsAll(targetAttributeSet)){
                LOGGER.warn("Missing field in item {} for account = {}", item, accountId);
                continue;
            }


            final DateTimeZone dateTimeZoneFromId = DateTimeZone.forID(item.get(TIMEZONE_NAME_ATTRIBUTE_NAME).getS());

            final long updatedAt = Long.valueOf(item.get(UPDATED_AT_ATTRIBUTE_NAME).getN());
            final int offsetMillis = dateTimeZoneFromId.getOffset(updatedAt);

            final TimeZoneHistory timeZoneHistory = new TimeZoneHistory(updatedAt, offsetMillis, dateTimeZoneFromId.getID());
            timeZoneHistoryMap.put(new DateTime(updatedAt, DateTimeZone.UTC), timeZoneHistory);
        }

        return timeZoneHistoryMap;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(UPDATED_AT_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(UPDATED_AT_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }


}
