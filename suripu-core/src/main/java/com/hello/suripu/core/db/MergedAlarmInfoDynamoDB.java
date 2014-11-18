package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmInfo;
import com.hello.suripu.core.models.RingTime;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 9/25/14.
 */
public class MergedAlarmInfoDynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(MergedAlarmInfoDynamoDB.class);
    private final AmazonDynamoDBClient dynamoDBClient;
    private final String tableName;

    public static final String MORPHEUS_ID_ATTRIBUTE_NAME = "device_id";
    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";

    // Alarm template
    public static final String ALARM_TEMPLATES_ATTRIBUTE_NAME = "alarm_templates";

    // Ring time
    public static final String EXPECTED_RING_TIME_ATTRIBUTE_NAME = "expected_ring_at_utc";
    public static final String ACTUAL_RING_TIME_ATTRIBUTE_NAME = "actual_ring_at_utc";
    public static final String SOUND_IDS_ATTRIBUTE_NAME = "sound_ids";

    // Timezone history
    public static final String TIMEZONE_ID_ATTRIBUTE_NAME = "timezone_id";

    public static final String UPDATED_AT_ATTRIBUTE_NAME = "updated_at";


    private static int MAX_CALL_COUNT = 3;
    public static final int MAX_ALARM_COUNT = 7;


    private Map<String, AttributeValueUpdate> generateTimeZoneUpdateItem(final DateTimeZone timeZone){
        final Map<String, AttributeValueUpdate> items = new HashMap<>();
        items.put(TIMEZONE_ID_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withS(timeZone.getID())));
        return items;
    }


    private Map<String, AttributeValueUpdate> generateRingTimeUpdateItem(final RingTime ringTime){
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, AttributeValueUpdate> items = new HashMap<>();
        items.put(EXPECTED_RING_TIME_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(ringTime.expectedRingTimeUTC))));
        items.put(ACTUAL_RING_TIME_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(ringTime.actualRingTimeUTC))));

        try {
            final String soundJSON = mapper.writeValueAsString(ringTime.soundIds);
            items.put(SOUND_IDS_ATTRIBUTE_NAME, new AttributeValueUpdate()
                    .withAction(AttributeAction.PUT)
                    .withValue(new AttributeValue().withS(soundJSON)));
        } catch (JsonProcessingException e) {
            LOGGER.error("Deserialize sound ids error: {}", e.getMessage());
            return Collections.EMPTY_MAP;
        }

        return items;
    }

    private Map<String, AttributeValueUpdate> generateAlarmUpdateItem(final List<Alarm> alarmList){
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, AttributeValueUpdate> items = new HashMap<>();
        final String alarmListJSON;
        try {
            alarmListJSON = mapper.writeValueAsString(alarmList);
        } catch (JsonProcessingException e) {
            LOGGER.error("Deserialize alarmList error: {}", e.getMessage());
            return Collections.EMPTY_MAP;
        }
        items.put(ALARM_TEMPLATES_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withS(alarmListJSON)));
        return items;
    }


    private UpdateItemRequest generateUpdateRequest(final String deviceId, final long accountId, final Map<String, AttributeValueUpdate> items){
        items.put(UPDATED_AT_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(DateTime.now().getMillis()))));

        final HashMap<String, AttributeValue> keys = new HashMap<>();
        keys.put(MORPHEUS_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
        keys.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));

        final UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(this.tableName)
                .withKey(keys)
                .withAttributeUpdates(items)
                .withReturnValues(ReturnValue.ALL_NEW);

        return updateItemRequest;
    }


    public boolean setTimeZone(final String deviceId, final long accountId, final DateTimeZone timeZone){
        final Map<String, AttributeValueUpdate> items = generateTimeZoneUpdateItem(timeZone);
        if(items.isEmpty()){
            return false;
        }

        final UpdateItemRequest request = generateUpdateRequest(deviceId, accountId, items);
        final UpdateItemResult result = this.dynamoDBClient.updateItem(request);
        return true;
    }

    public boolean setAlarms(final String deviceId, final long accountId, final List<Alarm> alarms){
        final Map<String, AttributeValueUpdate> items = generateAlarmUpdateItem(alarms);
        if(items.isEmpty()){
            return false;
        }

        final UpdateItemRequest request = generateUpdateRequest(deviceId, accountId, items);
        final UpdateItemResult result = this.dynamoDBClient.updateItem(request);
        return true;
    }

    public boolean setRingTime(final String deviceId, final long accountId, final RingTime ringTime){
        final Map<String, AttributeValueUpdate> items = generateRingTimeUpdateItem(ringTime);
        if(items.isEmpty()){
            return false;
        }

        final UpdateItemRequest request = generateUpdateRequest(deviceId, accountId, items);
        final UpdateItemResult result = this.dynamoDBClient.updateItem(request);
        return true;
    }

    public Optional<AlarmInfo> getInfo(final String deviceId, final long accountId){
        final List<AlarmInfo> alarmInfos = getInfo(deviceId);
        for(final AlarmInfo alarmInfo:alarmInfos){
            if(alarmInfo.accountId == accountId){
                return Optional.of(alarmInfo);
            }
        }

        return Optional.absent();
    }

    public List<AlarmInfo> getInfo(final String deviceId){
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();
        final Condition selectByDeviceId  = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(deviceId));
        queryConditions.put(MORPHEUS_ID_ATTRIBUTE_NAME, selectByDeviceId);

        final HashSet<String> targetAttributes = new HashSet<String>();
        Collections.addAll(targetAttributes,
                MORPHEUS_ID_ATTRIBUTE_NAME, ACCOUNT_ID_ATTRIBUTE_NAME,
                ALARM_TEMPLATES_ATTRIBUTE_NAME,
                EXPECTED_RING_TIME_ATTRIBUTE_NAME, ACTUAL_RING_TIME_ATTRIBUTE_NAME, SOUND_IDS_ATTRIBUTE_NAME,
                TIMEZONE_ID_ATTRIBUTE_NAME,
                UPDATED_AT_ATTRIBUTE_NAME);

        final QueryRequest queryRequest = new QueryRequest(this.tableName)
                .withKeyConditions(queryConditions)
                .withAttributesToGet(targetAttributes);

        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);
        if(queryResult.getItems() == null){
            return Collections.EMPTY_LIST;
        }

        if(queryResult.getItems().size() == 0){
            return Collections.EMPTY_LIST;
        }

        final List<Map<String, AttributeValue>> items = queryResult.getItems();
        final List<AlarmInfo> alarmInfos = new ArrayList<AlarmInfo>();

        for (final Map<String, AttributeValue> item:items) {
            final HashSet<String> accountDeviceIdAttributes = new HashSet<String>();
            Collections.addAll(targetAttributes,
                    MORPHEUS_ID_ATTRIBUTE_NAME, ACCOUNT_ID_ATTRIBUTE_NAME);
            if(!item.keySet().containsAll(accountDeviceIdAttributes)){
                LOGGER.warn("Corrupted row retrieved for device {}", deviceId);
                continue;
            }

            final long accountId = Long.valueOf(item.get(ACCOUNT_ID_ATTRIBUTE_NAME).getN());
            final List<Alarm> alarmListOptional = getAlarmListFromAttributes(deviceId, accountId, item);
            final Optional<RingTime> ringTimeOptional = getRingTimeFromAttributes(deviceId, accountId, item);
            final Optional<DateTimeZone> dateTimeZoneOptional = getTimeZoneFromAttributes(deviceId, accountId, item);
            alarmInfos.add(new AlarmInfo(deviceId, accountId, alarmListOptional, ringTimeOptional, dateTimeZoneOptional));
        }

        return ImmutableList.copyOf(alarmInfos);
    }

    public static List<Alarm> getAlarmListFromAttributes(final String deviceId, final long accountId, final Map<String, AttributeValue> item){
        final HashSet<String> alarmAttributes = new HashSet<String>();
        Collections.addAll(alarmAttributes, ALARM_TEMPLATES_ATTRIBUTE_NAME);

        if(!item.keySet().containsAll(alarmAttributes)){
            return Collections.EMPTY_LIST;
        }

        final String alarmListJSON = item.get(ALARM_TEMPLATES_ATTRIBUTE_NAME).getS();
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final List<Alarm> alarmList = mapper.readValue(alarmListJSON, new TypeReference<List<Alarm>>(){});
            return alarmList;
        } catch (IOException e) {
            LOGGER.error("Deserialize JSON for alarm list failed, device {}, account id {}.", deviceId, accountId);
        }

        return Collections.EMPTY_LIST;
    }


    public static Optional<RingTime> getRingTimeFromAttributes(final String deviceId, final long accountId, final Map<String, AttributeValue> item){
        final HashSet<String> alarmAttributes = new HashSet<String>();
        Collections.addAll(alarmAttributes, ACTUAL_RING_TIME_ATTRIBUTE_NAME, EXPECTED_RING_TIME_ATTRIBUTE_NAME, SOUND_IDS_ATTRIBUTE_NAME);

        if(!item.keySet().containsAll(alarmAttributes)){
            return Optional.absent();
        }

        final long expected = Long.valueOf(item.get(EXPECTED_RING_TIME_ATTRIBUTE_NAME).getN());
        final long actual = Long.valueOf(item.get(ACTUAL_RING_TIME_ATTRIBUTE_NAME).getN());

        final String soundArrayJSON = item.get(SOUND_IDS_ATTRIBUTE_NAME).getS();
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final long[] soundIds = mapper.readValue(soundArrayJSON, long[].class);
            return Optional.of(new RingTime(actual, expected, soundIds));
        } catch (IOException e) {
            LOGGER.error("Deserialize JSON for ring time failed {}, device {}, account id {}.", e.getMessage(), deviceId, accountId);
        }

        return Optional.absent();
    }


    public static Optional<DateTimeZone> getTimeZoneFromAttributes(final String deviceId, final long accountId, final Map<String, AttributeValue> item){
        final HashSet<String> alarmAttributes = new HashSet<String>();
        Collections.addAll(alarmAttributes, TIMEZONE_ID_ATTRIBUTE_NAME);

        if(!item.keySet().containsAll(alarmAttributes)){
            return Optional.absent();
        }

        final String timeZoneId = item.get(TIMEZONE_ID_ATTRIBUTE_NAME).getS();
        try{
            return Optional.of(DateTimeZone.forID(timeZoneId));
        }catch (Exception ex){
            LOGGER.error("Create timezone failed {}, device {}, account id {}.", ex.getMessage(), deviceId, accountId);
        }

        return Optional.absent();
    }


    public MergedAlarmInfoDynamoDB(final AmazonDynamoDBClient dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(MORPHEUS_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(MORPHEUS_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }

}
