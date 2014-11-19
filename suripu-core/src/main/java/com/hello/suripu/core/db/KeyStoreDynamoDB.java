package com.hello.suripu.core.db;

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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class KeyStoreDynamoDB implements KeyStore {

    private final static Logger LOGGER = LoggerFactory.getLogger(KeyStoreDynamoDB.class);

    private final AmazonDynamoDBClient dynamoDBClient;
    private final String keyStoreTableName;

    private final static String DEVICE_ID_ATTRIBUTE_NAME = "device_id";
    private final static String AES_KEY_ATTRIBUTE_NAME = "aes_key";

    //TODO : when transition is over, remove this
    private final static byte[] DEFAULT_AES_KEY = "1234567891234567".getBytes();


    final CacheLoader loader = new CacheLoader<String, Optional<byte[]>>() {
        public Optional<byte[]> load(String key) {
            return getRemotely(key);
        }
    };

    final LoadingCache<String, Optional<byte[]>> cache;

    public KeyStoreDynamoDB(
            final AmazonDynamoDBClient dynamoDBClient,
            final String keyStoreTableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.keyStoreTableName = keyStoreTableName;
        this.cache = CacheBuilder.newBuilder().build(loader);
    }

    @Override
    public Optional<byte[]> get(final String deviceId) {
        LOGGER.warn("Calling get with {}", deviceId);
        try {
            return cache.get(deviceId);
        } catch (ExecutionException e) {
            LOGGER.error("Exception from cache {}", e.getMessage());
        }

        return Optional.absent();
    }

    @Override
    public void put(final String deviceId, final String publicKey) {
        final Map<String, AttributeValue> attributes = new HashMap<String, AttributeValue>();
        attributes.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
        attributes.put(AES_KEY_ATTRIBUTE_NAME, new AttributeValue().withS(publicKey));

        final PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(keyStoreTableName)
                .withItem(attributes);

        final PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);
        // TODO: Log consumed capacity
    }

    private Optional<byte[]> getRemotely(final String deviceId) {
        final HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
        key.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(keyStoreTableName)
                .withKey(key);

        final GetItemResult getItemResult = dynamoDBClient.getItem(getItemRequest);

        LOGGER.warn(getItemResult.toString());
        if(getItemResult.getItem() == null || !getItemResult.getItem().containsKey(AES_KEY_ATTRIBUTE_NAME)) {
            LOGGER.warn("Did not find anything for device_id = {}", deviceId);

            return Optional.of(DEFAULT_AES_KEY);
        }

        final String hexEncodedKey = getItemResult.getItem().get(AES_KEY_ATTRIBUTE_NAME).getS();
        try {
            return Optional.of(Hex.decodeHex(hexEncodedKey.toCharArray()));
        } catch (DecoderException e) {
            LOGGER.error("Failed to decode key from store");
        }

        return Optional.absent();

    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(DEVICE_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(DEVICE_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
