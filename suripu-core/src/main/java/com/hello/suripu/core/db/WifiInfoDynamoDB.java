package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WifiInfoDynamoDB implements WifiInfoDAO, BaseDynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(WifiInfoDynamoDB.class);

    public final static String SENSE_ATTRIBUTE_NAME = "sense_id";
    public final static String SSID_ATTRIBUTE_NAME = "ssid";
    public final static String RSSI_ATTRIBUTE_NAME = "rssi";
    public final static String LAST_UPDATED_ATTRIBUTE_NAME = "last_updated";
    public final static Integer MAXIMUM_BATCH_WRITE_SIZE = 25;


    private final AmazonDynamoDB dynamoDBClient;
    private final String wifiTableName;

    public WifiInfoDynamoDB(final AmazonDynamoDB dynamoDBClient, final String wifiTableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.wifiTableName = wifiTableName;
    }

    @Override
    public Optional<WifiInfo> get(final String senseId) {
        final HashMap<String, AttributeValue> key = Maps.newHashMap();
        key.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(wifiTableName)
                .withKey(key);

        try {
            final GetItemResult getItemResult = dynamoDBClient.getItem(getItemRequest);
            final Map<String, AttributeValue> item = getItemResult.getItem();
            return createWifiInfofromDynamoDBItem(item, senseId);
        } catch (AmazonServiceException ase) {
            LOGGER.error("Failed to get wifi info for {}", senseId);
            return Optional.absent();
        }

    }

    @Override
    public Boolean put(final WifiInfo wifiInfo) {
        final PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(wifiTableName)
                .withItem(createDynamoDBItemFromWifiInfo(wifiInfo));

        try {
            final PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);
            return putItemResult != null;
        }
        catch (AmazonServiceException ase) {
            LOGGER.error(ase.getMessage());
        }

        return Boolean.FALSE;
    }

    @Override
    public Boolean putBatch(final List<WifiInfo> wifiInfoList) {
        final ImmutableList<WifiInfo> selectedWifiInfoList =  wifiInfoList.size() <= MAXIMUM_BATCH_WRITE_SIZE
            ? ImmutableList.copyOf(wifiInfoList)
            : ImmutableList.copyOf(wifiInfoList.subList(0, MAXIMUM_BATCH_WRITE_SIZE));

        final List<WriteRequest> wifiInfoWriteRequestList = new ArrayList<>();
        for (final WifiInfo wifiInfo : selectedWifiInfoList) {
            wifiInfoWriteRequestList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(createDynamoDBItemFromWifiInfo(wifiInfo))));
        }

        Map<String, List<WriteRequest>> requestItems = new HashMap<>();
        requestItems.put(wifiTableName, wifiInfoWriteRequestList);

        final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest().withRequestItems(requestItems);

        try {
            final BatchWriteItemResult batchWriteItemResult = dynamoDBClient.batchWriteItem(batchWriteItemRequest);
            return batchWriteItemResult != null;
        }
        catch (AmazonServiceException ase) {
            LOGGER.error(ase.getMessage());
        }
        return Boolean.FALSE;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(SENSE_ATTRIBUTE_NAME).withKeyType(KeyType.HASH)
        );

        request.withAttributeDefinitions(
            new AttributeDefinition().withAttributeName(SENSE_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }

    public static Optional<WifiInfo> createWifiInfofromDynamoDBItem(final Map<String, AttributeValue> item, final String senseId) {
        if (item == null) {
            LOGGER.warn("Sense {} does not have wifi info", senseId);
            return Optional.absent();
        }

        final Set<String> requiredAttributes = Sets.newHashSet(SSID_ATTRIBUTE_NAME, RSSI_ATTRIBUTE_NAME);
        final Set<String> itemAttributes = item.keySet();

        if (!itemAttributes.containsAll(requiredAttributes)) {
            return Optional.absent();
        }

        final DateTime lastUpdated = item.containsKey(LAST_UPDATED_ATTRIBUTE_NAME)
            ? DateTime.parse(item.get(LAST_UPDATED_ATTRIBUTE_NAME).getS(), DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT))
            : DateTime.now(DateTimeZone.UTC);
        return Optional.of(WifiInfo.create(
            item.get(SENSE_ATTRIBUTE_NAME).getS(),
            item.get(SSID_ATTRIBUTE_NAME).getS(),
            Integer.valueOf(item.get(RSSI_ATTRIBUTE_NAME).getN()),
            lastUpdated)
        );
    }

    public static Map<String, AttributeValue> createDynamoDBItemFromWifiInfo(final WifiInfo wifiInfo) {
        final Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(wifiInfo.senseId));
        attributes.put(SSID_ATTRIBUTE_NAME, new AttributeValue().withS(wifiInfo.ssid));
        attributes.put(RSSI_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(wifiInfo.rssi)));
        attributes.put(LAST_UPDATED_ATTRIBUTE_NAME, new AttributeValue().withS(wifiInfo.lastUpdated.toString(DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT))));
        return attributes;
    }
}
