package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.WifiInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class WifiInfoDynamoDB implements WifiInfoDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(WifiInfoDynamoDB.class);

    public final static String SENSE_ATTRIBUTE_NAME = "sense_id";
    public final static String SSID_ATTRIBUTE_NAME = "ssid";
    public final static String RSSI_ATTRIBUTE_NAME = "rssi";
    public final static String LAST_UPDATED_ATTRIBUTE_NAME = "last_updated";


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
            if (item == null) {
                LOGGER.warn("Sense {} does not have wifi info", senseId);
                return Optional.absent();
            }
            return createWifiInfofromDynamoDBItem(item);
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

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
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

    public static Optional<WifiInfo> createWifiInfofromDynamoDBItem(final Map<String, AttributeValue> item) {
        return Optional.of(WifiInfo.create(
                        item.get(WifiInfoDynamoDB.SENSE_ATTRIBUTE_NAME).getS(),
                        item.get(WifiInfoDynamoDB.SSID_ATTRIBUTE_NAME).getS(),
                        Integer.valueOf(item.get(WifiInfoDynamoDB.RSSI_ATTRIBUTE_NAME).getN()),
                        Long.valueOf(item.get(WifiInfoDynamoDB.LAST_UPDATED_ATTRIBUTE_NAME).getN()))
        );
    }

    public Map<String, AttributeValue> createDynamoDBItemFromWifiInfo(final WifiInfo wifiInfo) {
        final Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(wifiInfo.senseId));
        attributes.put(SSID_ATTRIBUTE_NAME, new AttributeValue().withS(wifiInfo.ssid));
        attributes.put(RSSI_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(wifiInfo.rssi)));
        attributes.put(LAST_UPDATED_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(wifiInfo.lastUpdated)));
        return attributes;
    }
}
