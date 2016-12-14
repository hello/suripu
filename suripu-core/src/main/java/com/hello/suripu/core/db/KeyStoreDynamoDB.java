package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.firmware.HardwareVersion;
import com.hello.suripu.core.models.DeviceKeyStoreRecord;
import com.hello.suripu.core.util.DateTimeUtil;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KeyStoreDynamoDB implements KeyStore {

    private final static Logger LOGGER = LoggerFactory.getLogger(KeyStoreDynamoDB.class);

    private final AmazonDynamoDB dynamoDBClient;
    private final String keyStoreTableName;

    private final static String DEVICE_ID_ATTRIBUTE_NAME = "device_id";
    private final static String AES_KEY_ATTRIBUTE_NAME = "aes_key";
    private final static String CREATED_AT_ATTRIBUTE_NAME = "created_at";
    private final static String HARDWARE_VERSION_ATTRIBUTE_NAME = "hw_version";

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


    private String dateToString(final DateTime createdAt) {
        return createdAt.toString(DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT));
    }

    @Override
    public Optional<byte[]> get(final String deviceId) {
        return getRemotely(deviceId, false);
    }

    @Override
    public Optional<byte[]> getStrict(final String deviceId) {
        return getRemotely(deviceId, true);
    }

    @Override
    public Optional<DeviceKeyStoreRecord> getKeyStoreRecord(final String deviceId) {
        return getRecordRemotely(deviceId);
    }

    @Override
    public void put(final String deviceId, final String aesKey) {
        final Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
        attributes.put(AES_KEY_ATTRIBUTE_NAME, new AttributeValue().withS(aesKey));

        final PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(keyStoreTableName)
                .withItem(attributes);

        final PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);
        // TODO: Log consumed capacity
    }

    @Override
    public void put(final String deviceId, final String aesKey, final String metadata) {
        put(deviceId,aesKey,metadata, DateTime.now(DateTimeZone.UTC), HardwareVersion.SENSE_ONE);
    }

    @Override
    public void put(final String deviceId, final String aesKey, final String serialNumber, final DateTime createdAtUtc, final HardwareVersion hardwareVersion) {
        final Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
        attributes.put(AES_KEY_ATTRIBUTE_NAME, new AttributeValue().withS(aesKey.toUpperCase()));
        attributes.put(METADATA, new AttributeValue().withS(serialNumber));
        attributes.put(CREATED_AT_ATTRIBUTE_NAME, new AttributeValue().withS(createdAtUtc.toString(DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT))));
        attributes.put(HARDWARE_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(hardwareVersion.value)));

        final PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(keyStoreTableName)
                .withItem(attributes);

        final PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);
    }

    @Override
    public boolean putOnlyIfAbsent(String deviceId, String aesKey, String serialNumber, DateTime createdAt) {
        final ImmutableMap<String, AttributeValue> attributes = new ImmutableMap.Builder<String, AttributeValue>()
                .put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId))
                .put(AES_KEY_ATTRIBUTE_NAME, new AttributeValue().withS(aesKey.toUpperCase()))
                .put(METADATA, new AttributeValue().withS(serialNumber))
                .put(CREATED_AT_ATTRIBUTE_NAME, new AttributeValue().withS(dateToString(createdAt)))
                .build();

        final PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(keyStoreTableName)
                .withItem(attributes)
                .withExpected(new ImmutableMap.Builder<String, ExpectedAttributeValue>()
                        // When exists is false and the id already exists a ConditionalCheckFailedException will be thrown
                        .put(DEVICE_ID_ATTRIBUTE_NAME, new ExpectedAttributeValue(false))
                        .build());
        try {
            final PutItemResult putItemResult = dynamoDBClient.putItem(putItemRequest);
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        } catch (Exception e) {
            LOGGER.error("Failed insert keys in table because of: {}", e.getMessage());
            return false;
        }
    }

    /**
     *
     * @param deviceIds set of external sense/pill IDs
     * @return a map of id to decoded key in bytes or absent if the id isn't found
     * this method should only be called by internal workers and never be exposed to any APIs
     */
    @Override
    public Map<String, Optional<byte[]>> getBatch(final Set<String> deviceIds) {
        final Map<String, DeviceKeyStoreRecord> deviceKeyStoreRecordMap = getKeyStoreRecordBatch(deviceIds);

        final Map<String, Optional<byte[]>> keyMap = Maps.newHashMap();
        for (final String deviceId : deviceIds) {
            keyMap.put(deviceId, Optional.<byte[]>absent());
        }

        for (final DeviceKeyStoreRecord deviceKeyStoreRecord : deviceKeyStoreRecordMap.values()) {
            final Optional<byte[]> decodedKey = decodeKey(deviceKeyStoreRecord.uncensoredKey());
            keyMap.put(deviceKeyStoreRecord.deviceId, decodedKey);
        }
        return keyMap;
    }


    /**
     *
     * @param deviceIds set of external sense/pill IDs
     * @return a map of id to device keystore record which contains censored keys which can be safely exposed
     * this map doesn't include devices which we don't have entry on keystore table
     */
    @Override
    public Map<String, DeviceKeyStoreRecord> getKeyStoreRecordBatch(final Set<String> deviceIds) {

        final BatchGetItemRequest batchGetItemRequest = new BatchGetItemRequest();
        final List<Map<String, AttributeValue>> itemKeys = Lists.newArrayList();

        for (final String deviceId : deviceIds) {
            final Map<String, AttributeValue> attributeValueMap = Maps.newHashMap();
            attributeValueMap.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
            itemKeys.add(attributeValueMap);
        }

        final KeysAndAttributes key = new KeysAndAttributes().withKeys(itemKeys).withAttributesToGet(DEVICE_ID_ATTRIBUTE_NAME, AES_KEY_ATTRIBUTE_NAME, METADATA, CREATED_AT_ATTRIBUTE_NAME, HARDWARE_VERSION_ATTRIBUTE_NAME);
        final Map<String, KeysAndAttributes> requestItems = Maps.newHashMap();
        requestItems.put(keyStoreTableName, key);

        batchGetItemRequest.withRequestItems(requestItems);

        try {
            final BatchGetItemResult batchGetItemResult = dynamoDBClient.batchGetItem(batchGetItemRequest);
            final Map<String, DeviceKeyStoreRecord> results = Maps.newHashMap();

            for (final String item : batchGetItemResult.getResponses().keySet()) {
                final List<Map<String, AttributeValue>> responses = batchGetItemResult.getResponses().get(item);
                for (final Map<String, AttributeValue> response : responses) {
                    final String deviceId = response.get(DEVICE_ID_ATTRIBUTE_NAME).getS();

                    final String aesKey = response.containsKey(AES_KEY_ATTRIBUTE_NAME) ? response.get(AES_KEY_ATTRIBUTE_NAME).getS() : "";
                    final String metadata = response.containsKey(METADATA) ? response.get(METADATA).getS() : "";
                    final String createdAt = response.containsKey(CREATED_AT_ATTRIBUTE_NAME) ? response.get(CREATED_AT_ATTRIBUTE_NAME).getS() : "";
                    final HardwareVersion hw = response.containsKey(HARDWARE_VERSION_ATTRIBUTE_NAME) ?
                            HardwareVersion.fromInt(Integer.parseInt(response.get(HARDWARE_VERSION_ATTRIBUTE_NAME).getN()))
                            : HardwareVersion.SENSE_ONE;
                    results.put(deviceId, DeviceKeyStoreRecord.forSense(deviceId, aesKey, metadata, createdAt, hw));
                }
            }
            return results;
        } catch (AmazonServiceException ase){
            LOGGER.error("Failed getting keys. {}", ase.getMessage());

        }
        return Collections.EMPTY_MAP;
    }

    private Optional<byte[]> fromItem(final Map<String, AttributeValue> item, final String deviceId, final Boolean strict) {
        if(item == null || !item.containsKey(AES_KEY_ATTRIBUTE_NAME)) {
            LOGGER.warn("Did not find AES key for device_id = {}.", deviceId);
            if(strict) {
                return Optional.absent();
            }
            LOGGER.warn("Not in strict mode, returning default AES key instead for device = {}.", deviceId);
            return Optional.of(DEFAULT_AES_KEY);
        }

        final String hexEncodedKey = item.get(AES_KEY_ATTRIBUTE_NAME).getS();
        return decodeKey(hexEncodedKey);
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

        return fromItem(getItemResult.getItem(), deviceId, strict);

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
        final String metadata = getItemResult.getItem().containsKey(METADATA) ? getItemResult.getItem().get(METADATA).getS() : "n/a";
        final String createdAt = getItemResult.getItem().containsKey(CREATED_AT_ATTRIBUTE_NAME) ? getItemResult.getItem().get(CREATED_AT_ATTRIBUTE_NAME).getS() : "";
        final HardwareVersion hardwareVersion = HardwareVersion.fromInt(getItemResult.getItem().containsKey(HARDWARE_VERSION_ATTRIBUTE_NAME) ? Integer.parseInt(getItemResult.getItem().get(HARDWARE_VERSION_ATTRIBUTE_NAME).getN()) : 1);
        return Optional.of(DeviceKeyStoreRecord.forSense(deviceId, aesKey, metadata, createdAt, hardwareVersion));
    }


    private Optional<byte[]> decodeKey(final String encodedKey) {
        try {
            return Optional.of(Hex.decodeHex(encodedKey.toCharArray()));
        } catch (DecoderException e) {
            LOGGER.error("Failed to decode key {}", encodedKey);
        }
        return Optional.absent();
    }
}
