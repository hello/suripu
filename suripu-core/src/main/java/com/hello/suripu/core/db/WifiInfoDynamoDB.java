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

    private final static String SENSE_ATTRIBUTE_NAME = "sense_id";
    private final static String SSID_ATTRIBUTE_NAME = "ssid";
    private final static String RSSI_ATTRIBUTE_NAME = "rssi";
    private final static String LAST_UPDATED_ATTRIBUTE_NAME = "last_updated";


    private final AmazonDynamoDB dynamoDBClient;
    private final String wifiTableName;

    public WifiInfoDynamoDB(final AmazonDynamoDB dynamoDBClient, final String wifiTableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.wifiTableName = wifiTableName;
    }

    @Override
    public WifiInfo get(final String senseId) {
        return getRemotely(senseId, Boolean.FALSE).get();
    }

    @Override
    public Optional<WifiInfo> getStrict(final String senseId) {
        return getRemotely(senseId, Boolean.TRUE);
    }

    private Optional<WifiInfo> getRemotely(final String senseId, final Boolean strict) {
        final HashMap<String, AttributeValue> key = Maps.newHashMap();
        key.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(senseId));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(wifiTableName)
                .withKey(key);

        final GetItemResult getItemResult = dynamoDBClient.getItem(getItemRequest);
        final Map<String, AttributeValue> item = getItemResult.getItem();
        if (item == null) {
            if (strict) {
                LOGGER.warn("Sense {} does not have wifi info", senseId);
                return Optional.absent();
            }
            return Optional.of(WifiInfo.createEmpty(senseId));
        }
        return Optional.of(WifiInfo.create(senseId, item.get(SSID_ATTRIBUTE_NAME).getS(), Integer.valueOf(item.get(RSSI_ATTRIBUTE_NAME).getN()), Long.valueOf(item.get(LAST_UPDATED_ATTRIBUTE_NAME).getN())));
    }

    @Override
    public Boolean put(final WifiInfo wifiInfo) {
        final Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(SENSE_ATTRIBUTE_NAME, new AttributeValue().withS(wifiInfo.senseId));
        attributes.put(SSID_ATTRIBUTE_NAME, new AttributeValue().withS(wifiInfo.ssid));
        attributes.put(RSSI_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(wifiInfo.rssi)));
        attributes.put(LAST_UPDATED_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(wifiInfo.lastUpdated)));

        final PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(wifiTableName)
                .withItem(attributes);

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
}
