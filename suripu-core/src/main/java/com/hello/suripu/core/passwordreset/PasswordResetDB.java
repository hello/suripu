package com.hello.suripu.core.passwordreset;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.BaseDynamoDB;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

public class PasswordResetDB implements BaseDynamoDB {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetDB.class);

    private final static String UUID_ATTRIBUTE_NAME = "uuid";
    private final static String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";
    private final static String EXPIRES_AT_ATTRIBUTE_NAME = "expires_at";
    private final static String STATE_ATTRIBUTE_NAME = "state";
    private final static String CREATED_AT_ATTRIBUTE_NAME = "created_at";

    private final static Integer EXPIRES_IN_HOURS_DEFAULT = 24;

    private final AmazonDynamoDB amazonDynamoDB;
    private final String tableName;
    private final Integer expiresInHours;

    private PasswordResetDB(final AmazonDynamoDB amazonDynamoDB, final String tableName, final Integer expiresInHours) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.tableName = tableName;
        this.expiresInHours = expiresInHours;
    }

    public static PasswordResetDB create(final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        return new PasswordResetDB(amazonDynamoDB, tableName, EXPIRES_IN_HOURS_DEFAULT);
    }

    public static PasswordResetDB create(final AmazonDynamoDB amazonDynamoDB, final String tableName, final Integer expiresInHours) {
        return new PasswordResetDB(amazonDynamoDB, tableName, expiresInHours);
    }


    /**
     *
     * @param passwordReset
     */
    public void save(final PasswordReset passwordReset) {
        final PutItemRequest putItemRequest = new PutItemRequest();
        final Map<String, AttributeValue> item = Maps.newHashMap();
        item.put(UUID_ATTRIBUTE_NAME, new AttributeValue().withS(passwordReset.uuid.toString()));
        item.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(passwordReset.accountId.toString()));
        item.put(STATE_ATTRIBUTE_NAME, new AttributeValue().withS(passwordReset.state));

        final Long createdAt = passwordReset.createdAt.getMillis();
        item.put(CREATED_AT_ATTRIBUTE_NAME, new AttributeValue().withN(createdAt.toString()));

        final Long expiresAt = DateTime.now(DateTimeZone.UTC).plusHours(expiresInHours).getMillis();
        item.put(EXPIRES_AT_ATTRIBUTE_NAME, new AttributeValue().withN(expiresAt.toString()));

        putItemRequest.withTableName(tableName).withItem(item);
        final PutItemResult result = amazonDynamoDB.putItem(putItemRequest);
    }

    /**
     *
     * @param uuid
     * @return
     */
    public Optional<PasswordReset> get(UUID uuid) {
        final GetItemRequest getItemRequest = new GetItemRequest();
        getItemRequest.withTableName(tableName);
        final Map<String, AttributeValue> attributeValueMap = Maps.newHashMap();
        attributeValueMap.put(UUID_ATTRIBUTE_NAME, new AttributeValue().withS(uuid.toString()));
        getItemRequest.withKey(attributeValueMap);

        final GetItemResult result = amazonDynamoDB.getItem(getItemRequest);
        return fromItem(result.getItem());
    }

    /**
     * Convert Item map to PasswordReset and validates it hasn't expired
     * @param item
     * @return
     */
    private Optional<PasswordReset> fromItem(Map<String, AttributeValue> item) {
        if(item == null || item.isEmpty()) {
            return Optional.absent();
        }

        for (final String attributeName : Lists.newArrayList(UUID_ATTRIBUTE_NAME, ACCOUNT_ID_ATTRIBUTE_NAME,
                EXPIRES_AT_ATTRIBUTE_NAME, STATE_ATTRIBUTE_NAME, CREATED_AT_ATTRIBUTE_NAME)) {
            if(!item.containsKey(attributeName)) {
                return Optional.absent();
            }
        }

        if(!item.containsKey(EXPIRES_AT_ATTRIBUTE_NAME)) {
            return Optional.absent();
        }

        final Long createdAtMillis = Long.valueOf(item.get(CREATED_AT_ATTRIBUTE_NAME).getN());
        if (hasExpired(createdAtMillis, DateTime.now(), expiresInHours)) {
            return Optional.absent();
        }

        final PasswordReset passwordReset = PasswordReset.recreate(
                Long.valueOf(item.get(ACCOUNT_ID_ATTRIBUTE_NAME).getN()),
                "",
                item.get(UUID_ATTRIBUTE_NAME).getS(),
                item.get(STATE_ATTRIBUTE_NAME).getS(),
                new DateTime(Long.valueOf(item.get(CREATED_AT_ATTRIBUTE_NAME).getN()), DateTimeZone.UTC)
        );

        return Optional.of(passwordReset);
    }

    /**
     * Checks if it has been more than n hours ago since object was created
     * @param createdAtMillis
     * @param now
     * @param expiresInHours
     * @return
     */
    public static Boolean hasExpired(final Long createdAtMillis, final DateTime now, final Integer expiresInHours) {
        final DateTime createdAt = new DateTime(createdAtMillis, DateTimeZone.UTC);
        return now.getMillis() > createdAt.plusHours(expiresInHours).getMillis();
    }


    /**
     * Remove the PasswordReset request
     * @param uuid
     * @param accountId
     * @return
     */
    public Boolean delete(final UUID uuid, final Long accountId) {
        final DeleteItemRequest deleteItemRequest = new DeleteItemRequest();
        final Map key = Maps.newHashMap();
        key.put(UUID_ATTRIBUTE_NAME, new AttributeValue().withS(uuid.toString()));
        deleteItemRequest.withTableName(tableName).withKey(key);
        try {
            amazonDynamoDB.deleteItem(deleteItemRequest);
            return Boolean.TRUE;
        } catch (Exception e) {
            LOGGER.error("Error deleting passwordReset {} for account {}. Reason: {}", uuid, accountId, e.getMessage());
        }

        return Boolean.FALSE;
    }

    /**
     * Create DynamoDB Table
     * @param tableName
     * @param amazonDynamoDB
     * @return
     */
    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB amazonDynamoDB) {
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(UUID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(UUID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = amazonDynamoDB.createTable(request);
        return result;
    }

}
