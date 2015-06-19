package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.RateLimiter;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.UserInfo;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by pangwu on 2/26/15.
 */
public class RingTimeHistoryDAODynamoDB {
    private final static Logger LOGGER = LoggerFactory.getLogger(AlarmDAODynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    private ObjectMapper mapper = new ObjectMapper();

    public static final String MORPHEUS_ID_ATTRIBUTE_NAME = "device_id";
    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";
    public static final String ACTUAL_RING_TIME_ATTRIBUTE_NAME = "actual_ring_time";
    public static final String EXPECTED_RING_TIME_ATTRIBUTE_NAME = "expected_ring_time";
    public static final String RINGTIME_OBJECT_ATTRIBUTE_NAME = "ring_time_object";
    public static final String CREATED_AT_ATTRIBUTE_NAME = "created_at_utc";

    public RingTimeHistoryDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    private static List<UserInfo> retrieveRingTimeOwner(final List<UserInfo> userInfo, final RingTime ringTime){
        final List<UserInfo> owners = new ArrayList<>();
        for(final UserInfo info:userInfo){
            if(info.ringTime.isPresent() && !ringTime.isEmpty()){
                if(!info.ringTime.get().equals(ringTime)){
                    continue;
                }

                owners.add(info);
            }
        }

        return owners;
    }

    @Timed
    public void setNextRingTime(final String deviceId, final List<UserInfo> userInfo, final RingTime ringTime){
        final List<UserInfo> owners = retrieveRingTimeOwner(userInfo, ringTime);
        final DateTime now = DateTime.now();

        if(owners.size() == 0){
            LOGGER.error("Cannot retrieve ring time owner for device {}, actual ring {}, expected ring {}",
                    deviceId,
                    ringTime.actualRingTimeUTC,
                    ringTime.expectedRingTimeUTC);
        }


        for(final UserInfo owner:owners) {
            this.setNextRingTime(deviceId, owner.accountId, ringTime, now);
        }
    }

    @Timed
    protected void setNextRingTime(final String deviceId, final Long accountId, final RingTime ringTime){
        setNextRingTime(deviceId, accountId, ringTime, DateTime.now());
    }

    @Timed
    protected void setNextRingTime(final String deviceId, final Long accountId, final RingTime ringTime, final DateTime currentTime){


        final HashMap<String, AttributeValue> items = new HashMap<String, AttributeValue>();
        items.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(accountId.toString()));
        items.put(MORPHEUS_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));

        items.put(CREATED_AT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(currentTime.getMillis())));
        items.put(ACTUAL_RING_TIME_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(ringTime.actualRingTimeUTC)));
        items.put(EXPECTED_RING_TIME_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(ringTime.expectedRingTimeUTC)));


        try {
            final String ringTimeJSON = this.mapper.writeValueAsString(ringTime);
            items.put(RINGTIME_OBJECT_ATTRIBUTE_NAME, new AttributeValue().withS(ringTimeJSON));
            final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, items);
            final PutItemResult result = this.dynamoDBClient.putItem(putItemRequest);
        } catch (JsonProcessingException e) {
            LOGGER.error("set next ringtime for device {} failed: {}", deviceId, e.getMessage());
        }catch (AmazonServiceException awsServiceExp){
            LOGGER.error("set next ringtime for device {} failed due to service exception: {}",
                    deviceId, awsServiceExp.getMessage());
        } catch (AmazonClientException awsClientExp){
            LOGGER.error("set next ringtime for device {} failed due to client exception: {}",
                    deviceId, awsClientExp.getMessage());
        } catch (Exception ex){
            LOGGER.error("set next ringtime for device {} failed due to general exception: {}",
                    deviceId, ex.getMessage());
        }


    }

    public List<RingTime> getRingTimesBetween(final String senseId, final Long accountId,
                                              final DateTime startTime,
                                              final DateTime endTime) {
        final Map<String, Condition> queryConditions = Maps.newHashMap();
        final List<AttributeValue> values = Lists.newArrayList();
        final double maxRateLimitSec = 100d;
        values.add(new AttributeValue().withN(String.valueOf(startTime.getMillis())));
        values.add(new AttributeValue().withN(String.valueOf(endTime.getMillis())));

        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                .withAttributeValueList(values);
        queryConditions.put(EXPECTED_RING_TIME_ATTRIBUTE_NAME, selectDateCondition);

        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(accountId.toString()));

        final Condition selectSenseIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(senseId));

        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectAccountIdCondition);
        //queryConditions.put(MORPHEUS_ID_ATTRIBUTE_NAME, selectSenseIdCondition);
        final Map<String, Condition> filterConditions = Maps.newHashMap();
        filterConditions.put(MORPHEUS_ID_ATTRIBUTE_NAME, selectSenseIdCondition);

        final Set<String> targetAttributeSet = Sets.newHashSet(ACCOUNT_ID_ATTRIBUTE_NAME,
                MORPHEUS_ID_ATTRIBUTE_NAME,
                EXPECTED_RING_TIME_ATTRIBUTE_NAME,
                ACTUAL_RING_TIME_ATTRIBUTE_NAME,
                RINGTIME_OBJECT_ATTRIBUTE_NAME,
                CREATED_AT_ATTRIBUTE_NAME);
        final List<RingTime> ringTimes = Lists.newArrayList();
        Map<String, AttributeValue> lastEvaluatedKey = null;
        final RateLimiter rateLimiter = RateLimiter.create(maxRateLimitSec);
        do {
            rateLimiter.acquire();
            final QueryRequest queryRequest = new QueryRequest(tableName).withKeyConditions(queryConditions)
                    .withQueryFilter(filterConditions)
                    .withAttributesToGet(targetAttributeSet)
                    .withLimit(50)
                    .withExclusiveStartKey(lastEvaluatedKey)
                    .withScanIndexForward(false);
            final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
            lastEvaluatedKey = queryResult.getLastEvaluatedKey();

            if (queryResult.getItems() == null) {
                return Collections.EMPTY_LIST;
            }

            final List<Map<String, AttributeValue>> items = queryResult.getItems();


            for (final Map<String, AttributeValue> item : items) {
                final Optional<RingTime> ringTime = Optional.fromNullable(ringTimeFromItemSet(senseId, targetAttributeSet, item));
                if (!ringTime.isPresent()) {
                    continue;
                }

                ringTimes.add(ringTime.get());
            }
        }while (lastEvaluatedKey != null);

        Collections.sort(ringTimes, new Comparator<RingTime>() {
            @Override
            public int compare(final RingTime o1, final RingTime o2) {
                return Long.compare(o1.actualRingTimeUTC, o2.actualRingTimeUTC);
            }
        });

        return ringTimes;
    }

    @Timed
    public RingTime ringTimeFromItemSet(final String deviceId, final Collection<String> targetAttributeSet, final Map<String, AttributeValue> item){

        if(!item.keySet().containsAll(targetAttributeSet)){
            LOGGER.warn("Missing field in item {}", item);
            return null;
        }

        try {
            final String ringTimeJSONString = item.get(RINGTIME_OBJECT_ATTRIBUTE_NAME).getS();
            final RingTime ringTime = this.mapper.readValue(ringTimeJSONString, RingTime.class);

            return ringTime;
        }catch (Exception ex){
            LOGGER.error("Get ring time failed for device {}.", deviceId);
        }

        return null;

    }


    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(EXPECTED_RING_TIME_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(EXPECTED_RING_TIME_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
