package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ReturnConsumedCapacity;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.google.common.util.concurrent.RateLimiter;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
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

    public static final String CREATED_AT_ATTRIBUTE_NAME = "created_at_utc";

    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss Z";



    public SmartAlarmLoggerDynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public List<Map.Entry<Long, RingTime>> scanSmartRingTimesTooCloseToExpected(){
        final List<Map.Entry<Long, RingTime>> nonSmartRings = new ArrayList<>();

        final RateLimiter rateLimiter = RateLimiter.create(10.0);

        // Track how much throughput we consume on each page
        int permitsToConsume = 1;
        Map<String, AttributeValue> exclusiveStartKey = null;

        int total = 0;
        LOGGER.info(",account,smart,expected,last_cycle,diff,fixable");
        do {
            rateLimiter.acquire(permitsToConsume);
            final ScanRequest scan = new ScanRequest()
                    .withTableName(this.tableName)
                    .withLimit(100)
                    .withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                    .withExclusiveStartKey(exclusiveStartKey);
            final ScanResult result = this.dynamoDBClient.scan(scan);
            exclusiveStartKey = result.getLastEvaluatedKey();

            // Account for the rest of the throughput we consumed,
            // now that we know how much that scan request cost
            double consumedCapacity = result.getConsumedCapacity().getCapacityUnits();
            permitsToConsume = (int)(consumedCapacity - 1.0);
            if(permitsToConsume <= 0) {
                permitsToConsume = 1;
            }

            total += result.getItems().size();

            // Process results here
            nonSmartRings.addAll(getNonSmartAlarmResult(result));

        } while (exclusiveStartKey  != null);

        LOGGER.info("Scanned {} items, {} are within 5 minutes of expected ring time.", total, nonSmartRings.size());
        return nonSmartRings;
    }

    private List<Map.Entry<Long, RingTime>> getNonSmartAlarmResult(final ScanResult scanResult){
        final List<Map.Entry<Long, RingTime>> result = new ArrayList<>();

        final List<Map<String, AttributeValue>> items = scanResult.getItems();

        for(final Map<String, AttributeValue> row:items){
            final String smartRingTimeString = row.get(SMART_RING_TIME_ATTRIBUTE_NAME).getS();
            final String expectedRingTimeString = row.get(EXPECTED_RING_TIME_ATTRIBUTE_NAME).getS();
            if(smartRingTimeString == null || expectedRingTimeString == null){
//                LOGGER.error("invalid row, missing smart ring time or expected ring time");
                continue;
            }
            final DateTime expectedRingTime = expectedRingTimeString.split(" ").length == 3 ?
                    DateTime.parse(expectedRingTimeString, DateTimeFormat.forPattern(DATETIME_FORMAT)) :
                    DateTime.parse(expectedRingTimeString, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT));

            final DateTime smartRingTime = smartRingTimeString.split(" ").length == 3 ?
                    DateTime.parse(smartRingTimeString, DateTimeFormat.forPattern(DATETIME_FORMAT)) :
                    DateTime.parse(smartRingTimeString, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT));

            final Long accountId = Long.valueOf(row.get(ACCOUNT_ID_ATTRIBUTE_NAME).getN());
            if(expectedRingTime.isBefore(smartRingTime)){
//                LOGGER.error("Smart ring time after expected ring time, smart: {}, expect: {}, for account {}",
//                        smartRingTime, expectedRingTime,
//                        accountId);
                continue;
            }

            if(expectedRingTime.minusMinutes(5).isBefore(smartRingTime) || expectedRingTime.minusMinutes(25).isAfter(smartRingTime)){
                // TODO: We still need to save the timezone id, parsing the string will give us
                // correct offset but Joda Time will not preserve the time zone.
                final RingTime ringTime  = new RingTime(smartRingTime.getMillis(), expectedRingTime.getMillis(), new long[0], true);

                final AbstractMap.SimpleEntry entry = new AbstractMap.SimpleEntry(accountId, ringTime);
                if(result.size() > 0 && result.get(result.size() - 1).getValue().expectedRingTimeUTC == expectedRingTime.getMillis()) {
                    result.set(result.size() - 1, entry);
                }else{
                    result.add(entry);
                }

            }

            final double diffInMinute = (expectedRingTime.getMillis() - smartRingTime.getMillis()) / 1000 / 60d;
            LOGGER.info("scanned,{},{},{},{},{},{}", accountId,
                    smartRingTimeString, expectedRingTimeString, row.get(LAST_SLEEP_CYCLE_ATTRIBUTE_NAME).getS(),
                    diffInMinute,
                    (diffInMinute < 5 || diffInMinute > 25) ? true : false);
        }

        return result;
    }

    public void log(final Long accountId, final DateTime lastSleepCycleEnd, final DateTime now,
                    final DateTime nextRingTimeWithLocalTimeZone,
                    final DateTime nextRegularRingTimeWithLocalTimeZone){
        final Map<String, AttributeValue> items = new HashMap<>();
        items.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(accountId.toString()));
        items.put(CURRENT_TIME_ATTRIBUTE_NAME, new AttributeValue().withS(now.toString(DATETIME_FORMAT)));
        items.put(EXPECTED_RING_TIME_ATTRIBUTE_NAME, new AttributeValue().withS(nextRegularRingTimeWithLocalTimeZone.toString(DATETIME_FORMAT)));
        items.put(SMART_RING_TIME_ATTRIBUTE_NAME, new AttributeValue().withS(nextRingTimeWithLocalTimeZone.toString(DATETIME_FORMAT)));
        items.put(LAST_SLEEP_CYCLE_ATTRIBUTE_NAME, new AttributeValue().withS(lastSleepCycleEnd.toString(DATETIME_FORMAT)));

        final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, items);
        try {
            final PutItemResult result = this.dynamoDBClient.putItem(putItemRequest);
        }catch (AmazonServiceException awsEx){
            LOGGER.error("Log smart alarm for account {} failed, aws service error {}", accountId, awsEx.getMessage());
        }catch (AmazonClientException awcEx){
            LOGGER.error("Log smart alarm for account {} failed, client error.", accountId, awcEx.getMessage());
        }
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
