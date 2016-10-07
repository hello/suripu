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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.alarm.TooManyAlarmsException;
import com.hello.suripu.core.models.Alarm;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by pangwu on 9/16/14.
 */
public class AlarmDAODynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(AlarmDAODynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";

    public static final String ALARM_TEMPLATES_ATTRIBUTE_NAME = "alarm_templates";
    public static final String UPDATED_AT_ATTRIBUTE_NAME = "updated_at";

    private static int MAX_CALL_COUNT = 3;
    public static final int MAX_ALARM_COUNT = 30;


    public AlarmDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }


    public void setAlarms(long accountId, final List<Alarm> alarms){
        if(alarms.size() > MAX_ALARM_COUNT){
            LOGGER.error("Account {} tries to set {} alarms to db, data too large.", accountId, alarms.size());
            throw new TooManyAlarmsException("Data too large.");
        }

        if(alarms.size() == 0){
            LOGGER.warn("Account {} set empty data for alarms.");
        }

        final Set<Integer> alarmDays = new HashSet<Integer>();
        final Alarm.Utils.AlarmStatus alarmStatus = Alarm.Utils.isValidAlarms(alarms, DateTime.now(), DateTimeZone.UTC);
        if(!alarmStatus.equals(Alarm.Utils.AlarmStatus.OK)){
            throw new RuntimeException("Invalid alarms");
        }

        final ObjectMapper mapper = new ObjectMapper();
        try {
            final String alarmJSONString = mapper.writeValueAsString(alarms);
            final HashMap<String, AttributeValue> items = new HashMap<String, AttributeValue>();
            items.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));
            items.put(ALARM_TEMPLATES_ATTRIBUTE_NAME, new AttributeValue().withS(alarmJSONString));
            items.put(UPDATED_AT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(DateTime.now().getMillis())));

            final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, items);
            final PutItemResult result = this.dynamoDBClient.putItem(putItemRequest);


        } catch (JsonProcessingException e) {
            //e.printStackTrace();
            LOGGER.error("Serialize alarms failed for account id: {}, error {}", accountId, e.getMessage());
        }

    }

    @Deprecated
    public ImmutableList<Alarm> getAlarms(long accountId){
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();
        final Condition selectByAccountId  = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));
        queryConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, selectByAccountId);
        final HashSet<String> targetAttributes = new HashSet<String>();
        Collections.addAll(targetAttributes, ACCOUNT_ID_ATTRIBUTE_NAME, ALARM_TEMPLATES_ATTRIBUTE_NAME, UPDATED_AT_ATTRIBUTE_NAME);

        final QueryRequest queryRequest = new QueryRequest(this.tableName)
                .withKeyConditions(queryConditions)
                .withAttributesToGet(targetAttributes)
                .withLimit(1);

        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        if(queryResult.getItems() == null){
            return ImmutableList.copyOf(Collections.EMPTY_LIST);
        }

        final List<Map<String, AttributeValue>> items = queryResult.getItems();

        for(final Map<String, AttributeValue> item:items){
            if(!item.keySet().containsAll(targetAttributes)){
                LOGGER.warn("Missing field in item {}", item);
                continue;
            }

            final String alarmJSONString = item.get(ALARM_TEMPLATES_ATTRIBUTE_NAME).getS();
            if(alarmJSONString == null){
                LOGGER.warn("Corrupted data.");
                continue;
            }

            try {
                final List<Alarm> alarms = new ObjectMapper().readValue(alarmJSONString, new TypeReference<List<Alarm>>(){});
                return ImmutableList.copyOf(alarms);

            } catch (IOException e) {
                //e.printStackTrace();
                LOGGER.error("Parse alarm for account id {}, failed: {}", accountId, e.getMessage());
            }
        }

        return ImmutableList.copyOf(Collections.EMPTY_LIST);

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
