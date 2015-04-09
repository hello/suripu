package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.google.common.base.Optional;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.models.Event;
import org.joda.time.DateTime;
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
    private final static String TYPE = "algorithm_type";
    private final static String TARGET_DATE = "date";
    private final static String INBED_TIME = "in_bed";
    private final static String FALL_ASLEEP_TIME = "fall_asleep";
    private final static String WAKEUP_TIME = "wake_up";
    private final static String OUTBED_TIME = "out_bed";
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

    public boolean setResult(final Long accountId, final AlgorithmName name, final SleepEvents<Optional<Event>> result){
        final HashMap<String, AttributeValueUpdate> items = new HashMap<>();
        final DateTime now = DateTime.now();
        items.put(UPDATED_AT, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(now.getMillis()))));

        final HashMap<String, AttributeValue> keys = new HashMap<>();
        keys.put(ACCOUNT_ID, new AttributeValue().withN(accountId.toString()));
        keys.put(TYPE, new AttributeValue().withS(name.toString()));

        final UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(this.tableName)
                .withKey(keys)
                .withAttributeUpdates(items)
                .withReturnValues(ReturnValue.ALL_NEW);

        return true;
    }
}
