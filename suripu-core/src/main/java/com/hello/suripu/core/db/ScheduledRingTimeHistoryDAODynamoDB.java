package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.models.RingTime;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by pangwu on 9/23/14.
 */
public class ScheduledRingTimeHistoryDAODynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(AlarmDAODynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public static final String MORPHEUS_ID_ATTRIBUTE_NAME = "device_id";

    public static final String RING_TIME_ATTRIBUTE_NAME = "ring_time";
    public static final String CREATED_AT_ATTRIBUTE_NAME = "created_at_utc";



    public ScheduledRingTimeHistoryDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }


    @Timed
    public RingTime getNextRingTime(final String deviceId){
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();
        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.LE.toString())
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(DateTime.now().getMillis())));


        queryConditions.put(CREATED_AT_ATTRIBUTE_NAME, selectDateCondition);


        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(deviceId));
        queryConditions.put(MORPHEUS_ID_ATTRIBUTE_NAME, selectAccountIdCondition);

        final Collection<String> targetAttributeSet = new HashSet<String>();
        Collections.addAll(targetAttributeSet,
                MORPHEUS_ID_ATTRIBUTE_NAME,
                RING_TIME_ATTRIBUTE_NAME,
                CREATED_AT_ATTRIBUTE_NAME);

        final QueryRequest queryRequest = new QueryRequest(this.tableName)
                .withKeyConditions(queryConditions)
                .withAttributesToGet(targetAttributeSet)
                .withLimit(1)
                .withScanIndexForward(false);

        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        if(queryResult.getItems() == null){
            return RingTime.createEmpty();
        }

        final List<Map<String, AttributeValue>> items = queryResult.getItems();

        for(final Map<String, AttributeValue> item:items){
            final RingTime ringTime = ringTimeFromItemSet(deviceId, targetAttributeSet, item);
            if(ringTime != null){
                return ringTime;
            }
        }

        return RingTime.createEmpty();
    }

    @Timed
    public static RingTime ringTimeFromItemSet(final String deviceId, final Collection<String> targetAttributeSet, final Map<String, AttributeValue> item){

        if(!item.keySet().containsAll(targetAttributeSet)){
            LOGGER.warn("Missing field in item {}", item);
            return null;
        }

        try {
            final String ringTimeJSON = item.get(RING_TIME_ATTRIBUTE_NAME).getS();
            final ObjectMapper mapper = new ObjectMapper();
            final RingTime ringTime = mapper.readValue(ringTimeJSON, RingTime.class);

            return ringTime;
        }catch (Exception ex){
            LOGGER.error("Get ring time failed for device {}.", deviceId);
        }

        return null;

    }

    @Timed
    public void setNextRingTime(final String deviceId, final RingTime ringTime){
        this.setNextRingTime(deviceId, ringTime, DateTime.now());
    }

    @Timed
    public void setNextRingTime(final String deviceId, final RingTime ringTime, final DateTime currentTime){


        final HashMap<String, AttributeValue> items = new HashMap<String, AttributeValue>();
        items.put(MORPHEUS_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final String ringTimeJSON = mapper.writeValueAsString(ringTime);
            items.put(CREATED_AT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(currentTime.getMillis())));
            items.put(RING_TIME_ATTRIBUTE_NAME, new AttributeValue().withS(ringTimeJSON));

            final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, items);
            final PutItemResult result = this.dynamoDBClient.putItem(putItemRequest);

        } catch (JsonProcessingException ex) {
            LOGGER.error("Set ring time for device {} failed, error: {}", deviceId, ex.getMessage());
        } catch (AmazonServiceException awsServiceExp){
            LOGGER.error("Set ring time for device {} failed due to service error: {}", deviceId, awsServiceExp.getMessage());
        } catch (AmazonClientException awsClientExp){
            LOGGER.error("Set ring time for device {} failed due to client error: {}", deviceId, awsClientExp.getMessage());
        } catch (Exception ex){
            LOGGER.error("Set ring time for device {} failed, general error: {}", deviceId, ex.getMessage());
        }



    }


    /**
     * TODO: this is awfully similar to getNextRingTimeForSense. Probably needs refactoring.
     * @param senseId
     * @param upToOneWeekAgo
     * @return
     */
    public List<RingTime> getRingTimesBetween(final String senseId, final DateTime upToOneWeekAgo) {
        final Map<String, Condition> queryConditions = Maps.newHashMap();
        final List<AttributeValue> values = Lists.newArrayList();
        final String eveningMillis = String.valueOf(upToOneWeekAgo.getMillis());

        LOGGER.debug("Evening: {} ({})", upToOneWeekAgo.toString(), upToOneWeekAgo.getMillis());

        values.add(new AttributeValue().withN(eveningMillis));
//        values.add(new AttributeValue().withN(morningMillis));

        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.GT.toString())
                .withAttributeValueList(values);
        queryConditions.put(CREATED_AT_ATTRIBUTE_NAME, selectDateCondition);


        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(senseId));
        queryConditions.put(MORPHEUS_ID_ATTRIBUTE_NAME, selectAccountIdCondition);

        final Set<String> targetAttributeSet = Sets.newHashSet(MORPHEUS_ID_ATTRIBUTE_NAME,RING_TIME_ATTRIBUTE_NAME, CREATED_AT_ATTRIBUTE_NAME);

        final QueryRequest queryRequest = new QueryRequest(tableName).withKeyConditions(queryConditions)
                .withAttributesToGet(targetAttributeSet)
                .withLimit(50)
                .withScanIndexForward(false);
        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);

        if(queryResult.getItems() == null){
            return Collections.EMPTY_LIST;
        }

        final List<Map<String, AttributeValue>> items = queryResult.getItems();

        final List<RingTime> ringTimes = Lists.newArrayList();
        for(final Map<String, AttributeValue> item: items){
            final Optional<RingTime> ringTime = Optional.fromNullable(ringTimeFromItemSet(senseId, targetAttributeSet, item));
            if(ringTime.isPresent()) {
                ringTimes.add(ringTime.get());
            }
        }

        return ringTimes;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(MORPHEUS_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(CREATED_AT_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(MORPHEUS_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(CREATED_AT_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }


}
