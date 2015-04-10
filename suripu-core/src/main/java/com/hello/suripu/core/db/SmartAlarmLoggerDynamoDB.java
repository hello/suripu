package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pangwu on 4/10/15.
 */
public class SmartAlarmLoggerDynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(SmartAlarmLoggerDynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public static final String MORPHEUS_ID_ATTRIBUTE_NAME = "device_id";
    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";

    public static final String RING_TIME_ATTRIBUTE_NAME = "ring_time";
    public static final String LAST_SLEEP_CYCLE_ATTRIBUTE_NAME = "last_sleep_cycle_time";
    public static final String CREATED_AT_ATTRIBUTE_NAME = "created_at_utc";



    public SmartAlarmLoggerDynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(MORPHEUS_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(MORPHEUS_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }

}
