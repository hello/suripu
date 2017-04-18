package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
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
import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmExpansion;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.util.PillColorUtil;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 9/25/14.
 */
public class MergedUserInfoDynamoDB implements MergedUserInfoDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(MergedUserInfoDynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final String MORPHEUS_ID_ATTRIBUTE_NAME = "device_id";
    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";

    // Alarm template
    public static final String ALARM_TEMPLATES_ATTRIBUTE_NAME = "alarm_templates";

    // Ring time
    public static final String EXPECTED_RING_TIME_ATTRIBUTE_NAME = "expected_ring_at_utc";
    public static final String ACTUAL_RING_TIME_ATTRIBUTE_NAME = "actual_ring_at_utc";
    public static final String SOUND_IDS_ATTRIBUTE_NAME = "sound_ids";
    public static final String IS_SMART_ALARM_ATTRIBUTE_NAME = "is_smart";

    // Timezone history
    public static final String TIMEZONE_ID_ATTRIBUTE_NAME = "timezone_id";

    // Pill color
    public static final String PILL_COLOR_ATTRIBUTE_NAME = "pill_color";

    public static final String UPDATED_AT_ATTRIBUTE_NAME = "updated_at";


    private static int MAX_CALL_COUNT = 3;
    public static final int MAX_ALARM_COUNT = 7;

    public MergedUserInfoDynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    @Override
    public String tableName() {
        return tableName;
    }

    private Map<String, AttributeValueUpdate> generateTimeZoneUpdateItem(final DateTimeZone timeZone){
        final Map<String, AttributeValueUpdate> items = new HashMap<>();
        items.put(TIMEZONE_ID_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withS(timeZone.getID())));
        items.put(ACTUAL_RING_TIME_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(RingTime.createEmpty().expectedRingTimeUTC))));
        items.put(EXPECTED_RING_TIME_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(RingTime.createEmpty().expectedRingTimeUTC))));
        items.put(IS_SMART_ALARM_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withBOOL(RingTime.createEmpty().isEmpty())));
        return items;
    }

    private Map<String, AttributeValueUpdate> generatePillColorUpdateItem(final OutputProtos.SyncResponse.PillSettings pillSettings){
        final Map<String, AttributeValueUpdate> items = new HashMap<>();

        final byte[] bytes = pillSettings.toByteArray();
        final ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.put(bytes, 0, bytes.length);
        byteBuffer.position(0);  // http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/JavaDocumentAPIBinaryTypeExample.html

        items.put(PILL_COLOR_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withB(byteBuffer)));

        return items;
    }

    private Map<String, AttributeValueUpdate> generateRingTimeUpdateItem(final RingTime ringTime){
        final Map<String, AttributeValueUpdate> items = new HashMap<>();
        items.put(EXPECTED_RING_TIME_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(ringTime.expectedRingTimeUTC))));
        items.put(ACTUAL_RING_TIME_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(ringTime.actualRingTimeUTC))));
        items.put(IS_SMART_ALARM_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withBOOL(ringTime.fromSmartAlarm)));

        try {
            final String soundJSON = this.objectMapper.writeValueAsString(ringTime.soundIds);
            items.put(SOUND_IDS_ATTRIBUTE_NAME, new AttributeValueUpdate()
                    .withAction(AttributeAction.PUT)
                    .withValue(new AttributeValue().withS(soundJSON)));
        } catch (JsonProcessingException e) {
            LOGGER.error("Deserialize sound ids error: {}", e.getMessage());
            return Collections.EMPTY_MAP;
        }

        return items;
    }

    protected static boolean shouldUpdateRingTime(final List<Alarm> oldAlarms, final List<Alarm> newAlarms,
                                                  final DateTimeZone userTimeZone,
                                                  final DateTime now){
        final RingTime updateRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(newAlarms,
                Alarm.Utils.alignToMinuteGranularity(now).getMillis(), userTimeZone);
        final RingTime oldRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(oldAlarms,
                Alarm.Utils.alignToMinuteGranularity(now).getMillis(), userTimeZone);
        return oldRingTime.expectedRingTimeUTC != updateRingTime.expectedRingTimeUTC;
    }

    protected Map<String, AttributeValueUpdate> appendRingTimeUpdateItem(final List<Alarm> newAlarms, final DateTimeZone userTimeZone, final DateTime now){
        final RingTime updateRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(newAlarms,
                Alarm.Utils.alignToMinuteGranularity(now).getMillis(), userTimeZone);
        return generateRingTimeUpdateItem(updateRingTime);
    }

    private Map<String, AttributeValueUpdate> generateAlarmUpdateItem(final List<Alarm> newAlarmList,
                                                                      final List<Alarm> oldAlarmList,
                                                                      final DateTimeZone userTimeZone){
        final Map<String, AttributeValueUpdate> items = new HashMap<>();
        final DateTime now = DateTime.now().withZone(userTimeZone);
        if(shouldUpdateRingTime(oldAlarmList, newAlarmList, userTimeZone, now)) {
            final Map<String, AttributeValueUpdate> ringTimeUpdateItems = appendRingTimeUpdateItem(newAlarmList, userTimeZone, now);
            items.putAll(ringTimeUpdateItems);
        }

        final String alarmListJSON;
        try {
            alarmListJSON = this.objectMapper.writeValueAsString(newAlarmList);
        } catch (JsonProcessingException e) {
            LOGGER.error("Deserialize alarmList error: {}", e.getMessage());
            return Collections.EMPTY_MAP;
        }
        items.put(ALARM_TEMPLATES_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withS(alarmListJSON)));
        return items;
    }

    private Map<String, AttributeValueUpdate> generatePillColorDeleteItem(){
        final Map<String, AttributeValueUpdate> items = new HashMap<>();

        items.put(PILL_COLOR_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.DELETE));
        return items;
    }


    private UpdateItemRequest generateUpdateRequest(final String deviceId, final long accountId, final Map<String, AttributeValueUpdate> items){
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        items.put(UPDATED_AT_ATTRIBUTE_NAME, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT)
                .withValue(new AttributeValue().withN(String.valueOf(now.getMillis()))));

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


    @Override
    public boolean setTimeZone(final String deviceId, final long accountId, final DateTimeZone timeZone){
        final Map<String, AttributeValueUpdate> items = generateTimeZoneUpdateItem(timeZone);
        if(items.isEmpty()){
            return false;
        }

        final UpdateItemRequest request = generateUpdateRequest(deviceId, accountId, items);
        final UpdateItemResult result = this.dynamoDBClient.updateItem(request);
        return true;
    }

    @Override
    public boolean deletePillColor(final String senseId, final long accountId, final String pillId){
        final Map<String, AttributeValueUpdate> items = generatePillColorDeleteItem();
        final UpdateItemRequest request = generateUpdateRequest(senseId, accountId, items);
        final UpdateItemResult result = this.dynamoDBClient.updateItem(request);
        return true;
    }

    @Override
    public Optional<Color> setNextPillColor(final String senseId, final long accountId, final String pillId){
        final List<UserInfo> userInfoList = this.getInfo(senseId);
        final List<Color> availableColor = PillColorUtil.getPillColors();

        for(final UserInfo userInfo:userInfoList){
            if(!userInfo.pillColor.isPresent()){
                continue;
            }

            final OutputProtos.SyncResponse.PillSettings colorSetting = userInfo.pillColor.get();
            final Color usedColor = PillColorUtil.pillColor(colorSetting.getPillColor());
            availableColor.remove(usedColor);
        }

        if(availableColor.isEmpty()){
            LOGGER.error("Too many pills registered, failed to assign color for pill {}, sense {}", pillId, senseId);
            return Optional.absent();
        }

        try {
            // WARNING: potential race condition here.
            final Color pillColor = availableColor.get(0);
            this.setPillColor(senseId, accountId, pillId, pillColor);
            return Optional.of(pillColor);
        }catch (AmazonServiceException ase){
            LOGGER.error("Set pill {} color for sense {} failed: {}", pillId, senseId, ase.getErrorMessage());
        }

        return Optional.absent();
    }

    @Override
    public boolean setPillColor(final String deviceId, final long accountId, final String pillId, final Color pillColor){
        final byte[] argb = PillColorUtil.colorToARGB(pillColor);
        final int intARGB = PillColorUtil.argbToIntBasedOnSystemEndianess(argb);
        final OutputProtos.SyncResponse.PillSettings pillSettings = OutputProtos.SyncResponse.PillSettings.newBuilder()
                .setPillColor(intARGB)
                .setPillId(pillId)
                .build();
        final Map<String, AttributeValueUpdate> items = generatePillColorUpdateItem(pillSettings);
        if(items.isEmpty()){
            return false;
        }

        final UpdateItemRequest request = generateUpdateRequest(deviceId, accountId, items);
        final UpdateItemResult result = this.dynamoDBClient.updateItem(request);
        return true;
    }

    @Override
    public boolean setAlarms(final String deviceId, final long accountId,
                             final long lastUpdatedAt,
                             final List<Alarm> oldAlarms,
                             final List<Alarm> newAlarms,
                             final DateTimeZone userTimeZone){
        final Map<String, AttributeValueUpdate> items = generateAlarmUpdateItem(newAlarms, oldAlarms, userTimeZone);

        if(items.isEmpty()){
            return false;
        }

        final UpdateItemRequest request = generateUpdateRequest(deviceId, accountId, items);
        final HashMap<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(UPDATED_AT_ATTRIBUTE_NAME, new ExpectedAttributeValue()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withValue(new AttributeValue().withN(String.valueOf(lastUpdatedAt))));

        request.withExpected(expected);
        try {
            final UpdateItemResult result = this.dynamoDBClient.updateItem(request);
        }catch (ConditionalCheckFailedException conditionalCheckFailedException){
            LOGGER.warn("Cannot update alarm for device {}, account {}, last updated at {}",
                    deviceId,
                    accountId,
                    lastUpdatedAt);
            return false;
        }
        return true;
    }

    @Override
    @Deprecated
    public boolean createUserInfoWithEmptyAlarmList(final String deviceId, final long accountId, final DateTimeZone userTimeZone){
        final Map<String, AttributeValueUpdate> items = generateAlarmUpdateItem(Collections.EMPTY_LIST, Collections.EMPTY_LIST, userTimeZone);

        if(items.isEmpty()){
            return false;
        }

        final UpdateItemRequest request = generateUpdateRequest(deviceId, accountId, items);
        final UpdateItemResult result = this.dynamoDBClient.updateItem(request);
        return true;
    }

    @Override
    public boolean setRingTime(final String deviceId, final long accountId, final RingTime ringTime){
        final Map<String, AttributeValueUpdate> items = generateRingTimeUpdateItem(ringTime);
        if(items.isEmpty()){
            return false;
        }

        final UpdateItemRequest request = generateUpdateRequest(deviceId, accountId, items);
        final UpdateItemResult result = this.dynamoDBClient.updateItem(request);

        return true;
    }

    @Override
    public Optional<UserInfo> getInfo(final String deviceId, final long accountId){
        final List<UserInfo> userInfos = getInfo(deviceId);
        for(final UserInfo userInfo : userInfos){
            if(userInfo.accountId == accountId){
                return Optional.of(userInfo);
            }
        }

        return Optional.absent();
    }

    @Override
    public Optional<UserInfo> unlinkAccountToDevice(final long accountId, final String deviceId){
        try {
            final Map<String, ExpectedAttributeValue> deleteConditions = new HashMap<String, ExpectedAttributeValue>();

            deleteConditions.put(MORPHEUS_ID_ATTRIBUTE_NAME, new ExpectedAttributeValue(
                    new AttributeValue().withS(deviceId)
            ));
            deleteConditions.put(ACCOUNT_ID_ATTRIBUTE_NAME, new ExpectedAttributeValue(
                    new AttributeValue().withN(String.valueOf(accountId))
            ));

            HashMap<String, AttributeValue> keys = new HashMap<String, AttributeValue>();
            keys.put(MORPHEUS_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
            keys.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(accountId)));

            final DeleteItemRequest deleteItemRequest = new DeleteItemRequest()
                    .withTableName(tableName)
                    .withKey(keys)
                    .withExpected(deleteConditions)
                    .withReturnValues(ReturnValue.ALL_OLD);

            final DeleteItemResult result = this.dynamoDBClient.deleteItem(deleteItemRequest);

            return attributeValuesToUserInfo(result.getAttributes());
        }  catch (AmazonServiceException ase) {
            LOGGER.error("Failed to get item after for device {} and account {}, error {}", deviceId, accountId, ase.getMessage());
        }

        return Optional.absent();
    }

    @Override
    public List<UserInfo> getInfo(final String deviceId){
        final Map<String, Condition> queryConditions = new HashMap<String, Condition>();
        final Condition selectByDeviceId  = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(deviceId));
        queryConditions.put(MORPHEUS_ID_ATTRIBUTE_NAME, selectByDeviceId);

        final HashSet<String> targetAttributes = new HashSet<String>();
        Collections.addAll(targetAttributes,
                MORPHEUS_ID_ATTRIBUTE_NAME, ACCOUNT_ID_ATTRIBUTE_NAME,
                ALARM_TEMPLATES_ATTRIBUTE_NAME,
                EXPECTED_RING_TIME_ATTRIBUTE_NAME, ACTUAL_RING_TIME_ATTRIBUTE_NAME, SOUND_IDS_ATTRIBUTE_NAME, IS_SMART_ALARM_ATTRIBUTE_NAME,
                TIMEZONE_ID_ATTRIBUTE_NAME,
                PILL_COLOR_ATTRIBUTE_NAME,
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
        final List<UserInfo> userInfos = new ArrayList<UserInfo>();

        for (final Map<String, AttributeValue> item:items) {
            final HashSet<String> requiredAttributes = new HashSet<String>();
            Collections.addAll(requiredAttributes,
                    MORPHEUS_ID_ATTRIBUTE_NAME, ACCOUNT_ID_ATTRIBUTE_NAME, TIMEZONE_ID_ATTRIBUTE_NAME, UPDATED_AT_ATTRIBUTE_NAME);
            if(!item.keySet().containsAll(requiredAttributes)){
                LOGGER.warn("Corrupted row retrieved for device {}", deviceId);
                continue;
            }

            final Optional<UserInfo> alarmInfoOptional = attributeValuesToUserInfo(item);
            if(!alarmInfoOptional.isPresent()){
                LOGGER.error("Get alarm info for device id {} failed.", deviceId);
                continue;
            }
            userInfos.add(alarmInfoOptional.get());
        }

        return ImmutableList.copyOf(userInfos);
    }


    private Optional<UserInfo> attributeValuesToUserInfo(final Map<String, AttributeValue> item){

        try {
            final long accountId = Long.valueOf(item.get(ACCOUNT_ID_ATTRIBUTE_NAME).getN());
            final String deviceId = item.get(MORPHEUS_ID_ATTRIBUTE_NAME).getS();
            final List<Alarm> alarmListOptional = getAlarmListFromAttributes(deviceId, accountId, item);
            final Optional<RingTime> ringTimeOptional = getRingTimeFromAttributes(deviceId, accountId, item);
            final Optional<DateTimeZone> dateTimeZoneOptional = getTimeZoneFromAttributes(deviceId, accountId, item);
            final Optional<OutputProtos.SyncResponse.PillSettings> pillColorOptional = getPillColorFromAttributes(deviceId, accountId, item);
            return Optional.of(new UserInfo(deviceId, accountId,
                    alarmListOptional, ringTimeOptional,
                    dateTimeZoneOptional,
                    pillColorOptional,
                    Long.valueOf(item.get(UPDATED_AT_ATTRIBUTE_NAME).getN())));
        }catch (Exception ex){
            LOGGER.error("attributeValuesToUserInfo error: {}", ex.getMessage());
        }

        return Optional.absent();
    }

    private List<Alarm> getAlarmListFromAttributes(final String deviceId, final long accountId, final Map<String, AttributeValue> item){
        final HashSet<String> alarmAttributes = new HashSet<String>();
        Collections.addAll(alarmAttributes, ALARM_TEMPLATES_ATTRIBUTE_NAME);

        if(!item.keySet().containsAll(alarmAttributes)){
            return Collections.EMPTY_LIST;
        }

        final String alarmListJSON = item.get(ALARM_TEMPLATES_ATTRIBUTE_NAME).getS();
        try {
            final List<Alarm> alarmList = this.objectMapper.readValue(alarmListJSON, new TypeReference<List<Alarm>>(){});
            return alarmList;
        } catch (IOException e) {
            LOGGER.error("Deserialize JSON for alarm list failed, device {}, account id {}.", deviceId, accountId);
        }

        return Collections.EMPTY_LIST;
    }


    private Optional<RingTime> getRingTimeFromAttributes(final String deviceId, final long accountId, final Map<String, AttributeValue> item){
        final HashSet<String> requiredAttributes = new HashSet<String>();
        Collections.addAll(requiredAttributes, ACTUAL_RING_TIME_ATTRIBUTE_NAME, EXPECTED_RING_TIME_ATTRIBUTE_NAME, SOUND_IDS_ATTRIBUTE_NAME);

        if(!item.keySet().containsAll(requiredAttributes)){
            return Optional.absent();
        }

        final long expected = Long.valueOf(item.get(EXPECTED_RING_TIME_ATTRIBUTE_NAME).getN());
        final long actual = Long.valueOf(item.get(ACTUAL_RING_TIME_ATTRIBUTE_NAME).getN());
        final String soundArrayJSON = item.get(SOUND_IDS_ATTRIBUTE_NAME).getS();

        try {
            final long[] soundIds = this.objectMapper.readValue(soundArrayJSON, long[].class);
            boolean isSmart = false;
            if(item.containsKey(IS_SMART_ALARM_ATTRIBUTE_NAME)){
               isSmart = item.get(IS_SMART_ALARM_ATTRIBUTE_NAME).getBOOL();
            }

            final Optional<DateTimeZone> dateTimeZoneOptional = getTimeZoneFromAttributes(deviceId, accountId, item);
            if(!dateTimeZoneOptional.isPresent()) {
                return Optional.of(new RingTime(actual, expected, soundIds, isSmart));
            }

            final DateTime expectedRingTime = new DateTime(expected, dateTimeZoneOptional.get());
            final String alarmListJSON = item.get(ALARM_TEMPLATES_ATTRIBUTE_NAME).getS();
            final List<Alarm> alarmList = this.objectMapper.readValue(alarmListJSON, new TypeReference<List<Alarm>>(){});

            final List<AlarmExpansion> expansions = Alarm.Utils.getExpansionsAtExpectedTime(expectedRingTime, alarmList);

            return Optional.of(new RingTime(actual, expected, soundIds, isSmart, expansions));

        } catch (IOException e) {
            LOGGER.error("Deserialize JSON for ring time failed {}, device {}, account id {}.", e.getMessage(), deviceId, accountId);
        }

        return Optional.absent();
    }


    @Override
    public Optional<DateTimeZone> getTimezone(final String senseId, final Long accountId) {
        final GetItemRequest getItemRequest = new GetItemRequest();
        final Map<String, AttributeValue> keys = new HashMap<>();
        keys.put(MORPHEUS_ID_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        keys.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(accountId.toString()));

        getItemRequest.withTableName(tableName)
                .withKey(keys);

        final GetItemResult result = dynamoDBClient.getItem(getItemRequest);
        if(result.getItem() == null) {
            LOGGER.warn("Timezone item was null for sense {} and accountId {}", senseId, accountId);
            return Optional.absent();
        }
        return getTimeZoneFromAttributes(senseId, accountId, result.getItem());
    }

    static Optional<DateTimeZone> getTimeZoneFromAttributes(String deviceId, long accountId, Map<String, AttributeValue> item){
        final HashSet<String> timezoneAttributes = new HashSet<>();
        Collections.addAll(timezoneAttributes, TIMEZONE_ID_ATTRIBUTE_NAME);
        if(!item.keySet().containsAll(timezoneAttributes)){
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

    static Optional<OutputProtos.SyncResponse.PillSettings> getPillColorFromAttributes(String deviceId, long accountId, Map<String, AttributeValue> item){
        final HashSet<String> pillColorAttributes = new HashSet<String>();
        Collections.addAll(pillColorAttributes, PILL_COLOR_ATTRIBUTE_NAME);

        if(!item.keySet().containsAll(pillColorAttributes)){
            return Optional.absent();
        }

        try {
            final byte[] bytes = item.get(PILL_COLOR_ATTRIBUTE_NAME).getB().array();
            final OutputProtos.SyncResponse.PillSettings pillSettings = OutputProtos.SyncResponse.PillSettings.parseFrom(bytes);
            return Optional.of(pillSettings);
        } catch (InvalidProtocolBufferException ipbExc){
            LOGGER.error("Invalid protobuf. Get pill color failed {}, device {}, account id {}.", ipbExc.getMessage(), deviceId, accountId);
        } catch (Exception e) {
            LOGGER.error("Get pill color failed {}, device {}, account id {}.", e.getMessage(), deviceId, accountId);
        }

        return Optional.absent();
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
