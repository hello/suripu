package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PillViewsDynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(PillViewsDynamoDB.class);
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableNamePrefix;
    private final String lastSeenTableName;

    public static final String PILL_ID_ATTRIBUTE_NAME = "pill_id";

    protected static final String UTC_TIMESTAMP_ATTRIBUTE_NAME = "utc_ts";
    protected static final String MOTION_RANGE_ATTRIBUTE_NAME = "motion_range";
    protected static final String KICKOFF_COUNTS_ATTRIBUTE_NAME = "kickoff_counts";
    protected static final String ON_DURATION_SECONDS_ATTRIBUTE_NAME = "on_duration_seconds";
    protected static final String SVM_NO_GRAVITY_ATTRIBUTE_NAME = "svm_no_gravity";
    protected static final String BATTERY_LEVEL_ATTRIBUTE_NAME= "battery_level";
    protected static final String UPTIME_ATTRIBUTE_NAME= "uptime";

    protected static final String FIRMWARE_VERSION_ATTRIBUTE_NAME = "fw_version";
    protected static final String UPDATED_AT_UTC_ATTRIBUTE_NAME = "updated_at_utc";
    protected static final String OFFSET_ATTRIBUTE_NAME = "offset";


    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";  // Due to Joda Time's behavior, it is not a good idea to store timezone as offset in the string
    private final String DYNAMO_DB_TABLE_FORMAT = "yyyy_MM_dd";
    private final Integer DYNAMO_BATCH_WRITE_LIMIT = 25;

    public String tableNameForDateTimeUpload(final DateTime dateTime) {
        return String.format("%s_%s", tableNamePrefix, dateTime.toString(DYNAMO_DB_TABLE_FORMAT));
    }

    public PillViewsDynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableNamePrefix, final String lastSeenTableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableNamePrefix = tableNamePrefix;
        this.lastSeenTableName = lastSeenTableName;
    }


    public WriteRequest transform(final String pillId, final TrackerMotion trackerMotion) {
        final Map<String, AttributeValue> items = Maps.newHashMap();
        items.put(PILL_ID_ATTRIBUTE_NAME, new AttributeValue().withS(pillId));

        items.put(SVM_NO_GRAVITY_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(trackerMotion.value)));
        items.put(MOTION_RANGE_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(trackerMotion.motionRange)));
        items.put(MOTION_RANGE_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(trackerMotion.motionRange)));
        items.put(KICKOFF_COUNTS_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(trackerMotion.kickOffCounts)));
        items.put(ON_DURATION_SECONDS_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(trackerMotion.onDurationInSeconds)));
        items.put(ON_DURATION_SECONDS_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(trackerMotion.onDurationInSeconds)));



        items.put(UPDATED_AT_UTC_ATTRIBUTE_NAME, new AttributeValue().withS(new DateTime(trackerMotion.timestamp, DateTimeZone.UTC).toString(DATETIME_FORMAT)));


        final PutRequest putRequest = new PutRequest(items);
        return new WriteRequest(putRequest);
    }


    public void update(final String pillId, final Integer uptime, final Integer firmwareVersion, final Integer batteryLevel, final DateTime dateTimeUTC) {

        final Map<String, AttributeValue> key = Maps.newHashMap();
        key.put(PILL_ID_ATTRIBUTE_NAME, new AttributeValue().withS(pillId));


        final Map<String, AttributeValueUpdate> updates = Maps.newHashMap();
        updates.put(BATTERY_LEVEL_ATTRIBUTE_NAME, new AttributeValueUpdate(new AttributeValue().withN(String.valueOf(batteryLevel)), AttributeAction.PUT));
        updates.put(UPTIME_ATTRIBUTE_NAME, new AttributeValueUpdate(new AttributeValue().withN(String.valueOf(uptime)),AttributeAction.PUT));
        updates.put(FIRMWARE_VERSION_ATTRIBUTE_NAME, new AttributeValueUpdate(new AttributeValue().withN(String.valueOf(uptime)),AttributeAction.PUT));
        updates.put(UPDATED_AT_UTC_ATTRIBUTE_NAME, new AttributeValueUpdate(new AttributeValue().withS(dateTimeUTC.toString(DATETIME_FORMAT)), AttributeAction.PUT));

        final UpdateItemRequest req = new UpdateItemRequest();
        req.withTableName(lastSeenTableName)
            .withAttributeUpdates(updates)
            .withKey(key);

        try {
            final UpdateItemResult result = dynamoDBClient.updateItem(req);
        } catch (AmazonServiceException e) {
            LOGGER.error("Failed updating heartbeat info for pill {}, Reason: {}", pillId, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unknown error. Failed updating heartbeat info for pill {}, Reason: {}", pillId, e.getMessage());
        }
    }

    /**
     * Retrieves last data upload from pill and associates it to account id and internal sense id
     */

    public Optional<TrackerMotion> lastSeen(final String pillId, final Long accountId, final Long internalPillId) {

        final Map<String, AttributeValue> key = Maps.newHashMap();
        key.put(PILL_ID_ATTRIBUTE_NAME, new AttributeValue().withS(pillId));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withKey(key)
                .withTableName(lastSeenTableName);
        final GetItemResult result = dynamoDBClient.getItem(getItemRequest);
        return fromDynamoDB(result.getItem(), pillId, accountId, internalPillId);
    }


    private Optional<TrackerMotion> fromDynamoDB(final Map<String, AttributeValue> item, final String senseId, final Long accountId, final Long internalPillId) {
        if(item == null || item.isEmpty()) {
            return Optional.absent();
        }

        final String dateTimeUTC = (item.containsKey(UPDATED_AT_UTC_ATTRIBUTE_NAME) ? item.get(UPDATED_AT_UTC_ATTRIBUTE_NAME).getS() : "");
        if(dateTimeUTC.isEmpty()) {
            LOGGER.error("Malformed data stored in last seen for device_id={}.", senseId);
            return Optional.absent();
        }

        final DateTime dateTime = DateTime.parse(dateTimeUTC, DateTimeFormat.forPattern(DATETIME_FORMAT));
        final Integer offsetMillis = (item.containsKey(OFFSET_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(OFFSET_ATTRIBUTE_NAME).getN()) : 0);

        final Long motionRange = (item.containsKey(MOTION_RANGE_ATTRIBUTE_NAME) ? Long.valueOf(item.get(MOTION_RANGE_ATTRIBUTE_NAME).getN()) : 0L);
        final Long kickOffCounts = (item.containsKey(KICKOFF_COUNTS_ATTRIBUTE_NAME) ? Long.valueOf(item.get(KICKOFF_COUNTS_ATTRIBUTE_NAME).getN()) : 0L);
        final Long onDuration = (item.containsKey(ON_DURATION_SECONDS_ATTRIBUTE_NAME) ? Long.valueOf(item.get(ON_DURATION_SECONDS_ATTRIBUTE_NAME).getN()) : 0L);

        final Integer svmNoGravity = (item.containsKey(SVM_NO_GRAVITY_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(SVM_NO_GRAVITY_ATTRIBUTE_NAME).getN()) : 0);

        final TrackerMotion.Builder builder = new TrackerMotion.Builder()
                .withAccountId(accountId)
                .withTrackerId(internalPillId)
                .withId(0L) // ?
                .withKickOffCounts(kickOffCounts)
                .withMotionRange(motionRange)
                .withOnDurationInSeconds(onDuration)
                .withValue(svmNoGravity)
                .withOffsetMillis(offsetMillis)
                .withTimestampMillis(dateTime.getMillis());

        return Optional.of(builder.build());
    }



    public Optional<DeviceStatus> lastHeartBeat(final String pillId, final Long internalPillId) {

        final Map<String, AttributeValue> key = Maps.newHashMap();
        key.put(PILL_ID_ATTRIBUTE_NAME, new AttributeValue().withS(pillId));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withKey(key)
                .withTableName(lastSeenTableName);
        final GetItemResult result = dynamoDBClient.getItem(getItemRequest);
        return deviceStatusFromDynamoDB(result.getItem(), pillId, internalPillId);
    }

    private Optional<DeviceStatus> deviceStatusFromDynamoDB(final Map<String, AttributeValue> item, final String senseId, final Long internalPillId) {
        if(item == null || item.isEmpty()) {
            return Optional.absent();
        }

        final String dateTimeUTC = (item.containsKey(UPDATED_AT_UTC_ATTRIBUTE_NAME) ? item.get(UPDATED_AT_UTC_ATTRIBUTE_NAME).getS() : "");
        if(dateTimeUTC.isEmpty()) {
            LOGGER.error("Malformed data stored in last seen for device_id={}.", senseId);
            return Optional.absent();
        }

        final DateTime dateTime = DateTime.parse(dateTimeUTC, DateTimeFormat.forPattern(DATETIME_FORMAT));
        final Integer offsetMillis = (item.containsKey(OFFSET_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(OFFSET_ATTRIBUTE_NAME).getN()) : 0);

        final Integer firmwareVersion = (item.containsKey(FIRMWARE_VERSION_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(FIRMWARE_VERSION_ATTRIBUTE_NAME).getN()) : 0);
        final Integer batteryLevel = (item.containsKey(BATTERY_LEVEL_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(BATTERY_LEVEL_ATTRIBUTE_NAME).getN()) : 0);
        final Integer uptime = (item.containsKey(BATTERY_LEVEL_ATTRIBUTE_NAME) ? Integer.valueOf(item.get(BATTERY_LEVEL_ATTRIBUTE_NAME).getN()) : 0);

       final DeviceStatus deviceStatus = new DeviceStatus(0L, internalPillId, firmwareVersion.toString(), batteryLevel, dateTime, uptime);

        return Optional.of(deviceStatus);
    }


    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        // TODO make this work for creating the daily/monthly shards
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(PILL_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(UTC_TIMESTAMP_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(PILL_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(UTC_TIMESTAMP_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }

    public static CreateTableResult createLastSeenTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        // TODO make this work for creating the daily/monthly shards
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(PILL_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(PILL_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
