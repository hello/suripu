package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;

import java.util.HashMap;
import java.util.Map;

public class PublicKeyStoreDynamoDB implements PublicKeyStore {

    private final AmazonDynamoDBClient dynamoDBClient;
    private final String keyStoreTableName;

    public PublicKeyStoreDynamoDB(
            final AmazonDynamoDBClient dynamoDBClient,
            final String keyStoreTableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.keyStoreTableName = keyStoreTableName;
    }

    @Override
    public byte[] get(final String deviceId) {
        final Map<String, AttributeValue> attributes = new HashMap<String, AttributeValue>();
        attributes.put("public_key", new AttributeValue().withS(""));

        final HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
        key.put("device_id", new AttributeValue().withS(deviceId));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(keyStoreTableName)
                .withKey(key);

        final GetItemResult getItemResult = dynamoDBClient.getItem(getItemRequest);
        final String base64EncodedPublicKey = getItemResult.getItem().get("public_key").getS();
        return base64EncodedPublicKey.getBytes();
    }

    @Override
    public void put(final String deviceId, final String publicKey) {
        final Map<String, AttributeValue> attributes = new HashMap<String, AttributeValue>();
        attributes.put("device_id", new AttributeValue().withS(deviceId));
        attributes.put("public_key", new AttributeValue().withS(publicKey));

//        final HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
//        key.put("device_id", new AttributeValue().withS(deviceId));
        final PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(keyStoreTableName)
                .withItem(attributes);

        final PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);
        // TODO: Log consumed capacity
    }
}
