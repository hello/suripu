package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.google.common.base.Optional;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Created by pangwu on 3/30/15.
 */
public class AlgorithmTestDAODynamoDB {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlgorithmTestDAODynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    private final static String ACCOUNT_ID = "account_id";
    private final static String TARGET_DATE = "date";
    private final static String INBED_TIME = "_in_bed";
    private final static String FALL_ASLEEP_TIME = "_fall_asleep";
    private final static String WAKEUP_TIME = "_wake_up";
    private final static String OUTBED_TIME = "_out_bed";
    private final static String UPDATED_AT = "updated_at";

    public static enum AlgorithmName{
        GLOBAL_SCORING,
        HMM,
        VOTING
    }

    public AlgorithmTestDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    private String getTimeStringFromOptionalEvent(final Optional<Event> eventOptional){
        if(!eventOptional.isPresent()){
            return "N/A";
        }

        return new DateTime(eventOptional.get().getStartTimestamp(),
                DateTimeZone.forOffsetMillis(eventOptional.get().getTimezoneOffset()))
                .toString(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT);
    }

    private HashMap<String, AttributeValueUpdate> addTimeStringToUpdateItems(final AlgorithmName name, final SleepEvents<Optional<Event>> result){
        final HashMap<String, AttributeValueUpdate> items = new HashMap<>();
        final String inBedTime = getTimeStringFromOptionalEvent(result.goToBed);
        final String sleepTime = getTimeStringFromOptionalEvent(result.fallAsleep);
        final String wakeUpTime = getTimeStringFromOptionalEvent(result.wakeUp);
        final String outBedTime = getTimeStringFromOptionalEvent(result.outOfBed);
        items.put(name.toString() + INBED_TIME, new AttributeValueUpdate()
            .withAction(AttributeAction.PUT)
            .withValue(new AttributeValue().withS(inBedTime)));
        items.put(name.toString() + FALL_ASLEEP_TIME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withS(sleepTime)));
        items.put(name.toString() + WAKEUP_TIME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withS(wakeUpTime)));
        items.put(name.toString() + OUTBED_TIME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withS(outBedTime)));
        return items;
    }

    public boolean setResult(final Long accountId, final DateTime targetDateLocalUTC, final AlgorithmName name, final SleepEvents<Optional<Event>> events){
        final HashMap<String, AttributeValueUpdate> items = new HashMap<>();
        final DateTime now = DateTime.now();
        items.put(UPDATED_AT, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(now.getMillis()))));

        items.putAll(addTimeStringToUpdateItems(name, events));


        final HashMap<String, AttributeValue> keys = new HashMap<>();
        keys.put(ACCOUNT_ID, new AttributeValue().withN(accountId.toString()));
        keys.put(TARGET_DATE, new AttributeValue().withS(targetDateLocalUTC.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT)));

        final UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(this.tableName)
                .withKey(keys)
                .withAttributeUpdates(items)
                .withReturnValues(ReturnValue.ALL_NEW);
        final UpdateItemResult result = this.dynamoDBClient.updateItem(updateItemRequest);
        return true;
    }


    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(ACCOUNT_ID).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(TARGET_DATE).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(ACCOUNT_ID).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(TARGET_DATE).withAttributeType(ScalarAttributeType.S)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
