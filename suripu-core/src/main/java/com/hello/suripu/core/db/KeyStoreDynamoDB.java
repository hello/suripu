package com.hello.suripu.core.db;

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
import com.hello.suripu.core.models.DeviceKeyStoreRecord;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class KeyStoreDynamoDB implements KeyStore {

    private final static Logger LOGGER = LoggerFactory.getLogger(KeyStoreDynamoDB.class);

    private final AmazonDynamoDB dynamoDBClient;
    private final String keyStoreTableName;

    private final static String DEVICE_ID_ATTRIBUTE_NAME = "device_id";
    private final static String AES_KEY_ATTRIBUTE_NAME = "aes_key";
    public final static String DEFAULT_FACTORY_DEVICE_ID = "0000000000000000";
    private final static String METADATA = "metadata";

    public final static byte[] DEFAULT_AES_KEY = "1234567891234567".getBytes(); // change this and you die

    public KeyStoreDynamoDB(
            final AmazonDynamoDB dynamoDBClient,
            final String keyStoreTableName,
            final byte[] defaultAESKey,
            final Integer cacheExpireAfterInSeconds) {
        this.dynamoDBClient = dynamoDBClient;
        this.keyStoreTableName = keyStoreTableName;
    }

    @Override
    public Optional<byte[]> get(final String deviceId) {
//        LOGGER.info("Calling get with {}", deviceId);
//        try {
//            return cache.get(deviceId);
//        } catch (ExecutionException e) {
//            LOGGER.error("Exception from cache {}", e.getMessage());
//        } catch(UncheckedExecutionException e) {
//            LOGGER.error("Unchecked Exception from cache {}", e.getMessage());
//        }
//
//        return Optional.absent();
        return getRemotely(deviceId, false);
    }

    @Override
    public Optional<byte[]> getStrict(final String deviceId) {
        return getRemotely(deviceId, true);
    }

    @Override
    public Optional<DeviceKeyStoreRecord> getKeyStoreRecord(String deviceId) {
        return getRecordRemotely(deviceId);
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

    @Override
    public void put(final String deviceId, final String publicKey, final String metadata) {
        final Map<String, AttributeValue> attributes = new HashMap<String, AttributeValue>();
        attributes.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
        attributes.put(AES_KEY_ATTRIBUTE_NAME, new AttributeValue().withS(publicKey));
        attributes.put("metadata", new AttributeValue().withS(metadata));

        final PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(keyStoreTableName)
                .withItem(attributes);

        final PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);
        // TODO: Log consumed capacity
    }

    private Optional<byte[]> getRemotely(final String deviceId, final Boolean strict) {
        if(DEFAULT_FACTORY_DEVICE_ID.equals(deviceId) && strict) {
            LOGGER.warn("Device not properly provisioned, got {} as a deviceId", deviceId);
                return Optional.absent();
        }

        final HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
        key.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(keyStoreTableName)
                .withKey(key);

        final GetItemResult getItemResult = dynamoDBClient.getItem(getItemRequest);

        if(getItemResult.getItem() == null || !getItemResult.getItem().containsKey(AES_KEY_ATTRIBUTE_NAME)) {
            LOGGER.warn("Did not find AES key for device_id = {}.", deviceId);
            if(strict) {
                return Optional.absent();
            }
            LOGGER.warn("Not in strict mode, returning default AES key instead for device = {}.", deviceId);
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

    private Optional<DeviceKeyStoreRecord> getRecordRemotely(final String deviceId) {
        if(DEFAULT_FACTORY_DEVICE_ID.equals(deviceId)) {
            LOGGER.warn("Device not properly provisioned, got {} as a deviceId", deviceId);
            return Optional.absent();
        }

        final HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
        key.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
        final GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(keyStoreTableName)
                .withKey(key);

        final GetItemResult getItemResult = dynamoDBClient.getItem(getItemRequest);

        if(getItemResult.getItem() == null || !getItemResult.getItem().containsKey(AES_KEY_ATTRIBUTE_NAME)) {
            LOGGER.warn("Did not find anything for device_id = {}", deviceId);
            return Optional.absent();
        }

        final String aesKey = getItemResult.getItem().get(AES_KEY_ATTRIBUTE_NAME).getS();

        if (!getItemResult.getItem().containsKey(METADATA)) {
            return Optional.of(new DeviceKeyStoreRecord(censorKey(aesKey), "n/a"));
        }
        return Optional.of(new DeviceKeyStoreRecord(censorKey(aesKey), getItemResult.getItem().get(METADATA).getS()));
    }

    private String censorKey(final String key) {
        char[] censoredParts = new char[key.length() - 8];
        Arrays.fill(censoredParts, 'x');
        return new StringBuilder(key).replace(4, key.length() - 4, new String(censoredParts)).toString();
    }
}
