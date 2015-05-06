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
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
