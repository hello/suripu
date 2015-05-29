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
import com.google.common.base.Optional;
import com.google.common.util.concurrent.RateLimiter;
import com.hello.suripu.core.models.SmartAlarmHistory;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 4/10/15.
 */
public class SmartAlarmLoggerDynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(SmartAlarmLoggerDynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";

    public static final String EXPECTED_RING_TIME_ATTRIBUTE_NAME = "expected_ring_time";
    public static final String SMART_RING_TIME_ATTRIBUTE_NAME = "smart_ring_time";
    public static final String LAST_SLEEP_CYCLE_ATTRIBUTE_NAME = "last_sleep_cycle_time";
    public static final String CURRENT_TIME_ATTRIBUTE_NAME = "current_time";
    public static final String TIMEZONE_ID_ATTRIBUTE_NAME = "timezone_id";

    public static final String CREATED_AT_ATTRIBUTE_NAME = "created_at_utc";

    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";  // Due to Joda Time's behavior, it is not a good idea to store timezone as offset in the string



    public SmartAlarmLoggerDynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public void log(final Long accountId, final DateTime lastSleepCycleEnd, final DateTime now,
                    final DateTime nextRingTimeWithLocalTimeZone,
                    final DateTime nextRegularRingTimeWithLocalTimeZone,
                    final DateTimeZone userTimeZone){
        final Map<String, AttributeValue> items = new HashMap<>();
        items.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(accountId.toString()));
        items.put(CURRENT_TIME_ATTRIBUTE_NAME, new AttributeValue().withS(now.toString(DATETIME_FORMAT)));
        items.put(EXPECTED_RING_TIME_ATTRIBUTE_NAME, new AttributeValue().withS(nextRegularRingTimeWithLocalTimeZone.toString(DATETIME_FORMAT)));
        items.put(SMART_RING_TIME_ATTRIBUTE_NAME, new AttributeValue().withS(nextRingTimeWithLocalTimeZone.toString(DATETIME_FORMAT)));
        items.put(LAST_SLEEP_CYCLE_ATTRIBUTE_NAME, new AttributeValue().withS(lastSleepCycleEnd.toString(DATETIME_FORMAT)));
        items.put(TIMEZONE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(userTimeZone.getID()));

        final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, items);
        try {
            final PutItemResult result = this.dynamoDBClient.putItem(putItemRequest);
        }catch (AmazonServiceException awsEx){
            LOGGER.error("Log smart alarm for account {} failed, aws service error {}", accountId, awsEx.getMessage());
        }catch (AmazonClientException awcEx){
            LOGGER.error("Log smart alarm for account {} failed, client error.", accountId, awcEx.getMessage());
        }
    }

    @Timed
    public List<SmartAlarmHistory> getSmartAlarmHistoryByScheduleTime(final long accountId,
                                                                      final DateTime start, final DateTime end){
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();
        final double maxRateLimitSec = 100d;
        final Condition selectDateCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                .withAttributeValueList(new AttributeValue().withS(start.toString(DATETIME_FORMAT)),
                        new AttributeValue().withS(end.toString(DATETIME_FORMAT)));


        queryConditions.put(CURRENT_TIME_ATTRIBUTE_NAME, selectDateCondition);

        // AND accound_id = :accound_id
        final Condition selectAccountIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectAccountIdCondition);

        Map<String, AttributeValue> lastEvaluatedKey = null;
        final List<SmartAlarmHistory> history = new ArrayList<>();
        final Collection<String> targetAttributeSet = new HashSet<String>();
        Collections.addAll(targetAttributeSet,
                ACCOUNT_ID_ATTRIBUTE_NAME,
                CURRENT_TIME_ATTRIBUTE_NAME,
                EXPECTED_RING_TIME_ATTRIBUTE_NAME,
                SMART_RING_TIME_ATTRIBUTE_NAME,
                LAST_SLEEP_CYCLE_ATTRIBUTE_NAME,
                TIMEZONE_ID_ATTRIBUTE_NAME
                );

        final RateLimiter rateLimiter = RateLimiter.create(maxRateLimitSec);
        do {
            rateLimiter.acquire();
            final QueryRequest queryRequest = new QueryRequest()
                    .withTableName(this.tableName)
                    .withKeyConditions(queryConditions)
                    .withAttributesToGet(targetAttributeSet)
                    .withLimit(100)
                    .withExclusiveStartKey(lastEvaluatedKey);

            final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
            lastEvaluatedKey = queryResult.getLastEvaluatedKey();

            final List<Map<String, AttributeValue>> items = queryResult.getItems();

            for (final Map<String, AttributeValue> item : items) {
                final Optional<SmartAlarmHistory> smartAlarmHistoryOptional = smartAlarmHistoryFromAttributeValues(item);
                if(smartAlarmHistoryOptional.isPresent()){
                    history.add(smartAlarmHistoryOptional.get());
                }
            }

            LOGGER.warn("Account {} get timezone failed, no data or aws error.", accountId);
        }while (lastEvaluatedKey != null);

        return history;
    }

    private Optional<SmartAlarmHistory> smartAlarmHistoryFromAttributeValues(final Map<String, AttributeValue> item){
        final Collection<String> targetAttributeSet = new HashSet<String>();
        Collections.addAll(targetAttributeSet,
                ACCOUNT_ID_ATTRIBUTE_NAME,
                CURRENT_TIME_ATTRIBUTE_NAME,
                EXPECTED_RING_TIME_ATTRIBUTE_NAME,
                SMART_RING_TIME_ATTRIBUTE_NAME,
                LAST_SLEEP_CYCLE_ATTRIBUTE_NAME
                // TIMEZONE_ID_ATTRIBUTE_NAME  // backward compatibility
        );

        if (!item.keySet().containsAll(targetAttributeSet)) {
            LOGGER.warn("Missing field in item {}", item);
            return Optional.absent();
        }

        final long accountId = Long.valueOf(item.get(ACCOUNT_ID_ATTRIBUTE_NAME).getN());
        final String scheduledAtLocal = item.get(CURRENT_TIME_ATTRIBUTE_NAME).getS();
        final String expectedRingLocal = item.get(EXPECTED_RING_TIME_ATTRIBUTE_NAME).getS();
        final String actualRingLocal = item.get(SMART_RING_TIME_ATTRIBUTE_NAME).getS();
        final String lastSleepCycleLocal = item.get(LAST_SLEEP_CYCLE_ATTRIBUTE_NAME).getS();
        final String timeZoneId = item.containsKey(TIMEZONE_ID_ATTRIBUTE_NAME) ? item.get(TIMEZONE_ID_ATTRIBUTE_NAME).getS() : null;
        final SmartAlarmHistory smartAlarmHistory = SmartAlarmHistory.create(accountId,
                scheduledAtLocal,
                expectedRingLocal,
                actualRingLocal,
                lastSleepCycleLocal,
                timeZoneId);
        return Optional.of(smartAlarmHistory);
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(CURRENT_TIME_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(CURRENT_TIME_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }

}
