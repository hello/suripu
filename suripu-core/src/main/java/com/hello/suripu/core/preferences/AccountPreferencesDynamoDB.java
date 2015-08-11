package com.hello.suripu.core.preferences;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
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
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

public class AccountPreferencesDynamoDB implements AccountPreferencesDAO {

    private final static String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";

    private final AmazonDynamoDB dynamoDB;
    private final String tableName;
    private final Set<AccountPreference.EnabledPreference> optOuts;

    private AccountPreferencesDynamoDB(final AmazonDynamoDB amazonDynamoDB, final String tableName, final Set<AccountPreference.EnabledPreference> optOuts) {
        this.dynamoDB = amazonDynamoDB;
        this.tableName = tableName;
        this.optOuts = ImmutableSet.copyOf(optOuts);
    }


    public static AccountPreferencesDynamoDB create(final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        final Set<AccountPreference.EnabledPreference> optOuts = Sets.newHashSet(AccountPreference.EnabledPreference.PUSH_SCORE, AccountPreference.EnabledPreference.PUSH_ALERT_CONDITIONS);
        return new AccountPreferencesDynamoDB(amazonDynamoDB, tableName, optOuts);
    }

    @Override
    public AccountPreference put(final Long accountId, final AccountPreference preference) {
        final UpdateItemRequest updateItemRequest = new UpdateItemRequest();
//        final PutItemRequest putItemRequest = new PutItemRequest();
//        putItemRequest.addItemEntry(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(accountId.toString()));
//
//        putItemRequest.addItemEntry(preference.key.toString(), new AttributeValue().withBOOL(preference.enabled));
//        putItemRequest.setTableName(tableName);
//        dynamoDB.putItem(putItemRequest);
        updateItemRequest.addKeyEntry(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(accountId.toString()));
        final AttributeValueUpdate update = new AttributeValueUpdate();
        update.setValue(new AttributeValue().withBOOL(preference.enabled));
        updateItemRequest.addAttributeUpdatesEntry(preference.key.toString(), update);
        updateItemRequest.setTableName(tableName);
        dynamoDB.updateItem(updateItemRequest);
        return preference;
    }

    @Override
    public Map<AccountPreference.EnabledPreference, Boolean> putAll(Long accountId, Map<AccountPreference.EnabledPreference, Boolean> changes) {
        final UpdateItemRequest updateItemRequest = new UpdateItemRequest();
        updateItemRequest.addKeyEntry(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(accountId.toString()));
        for (Map.Entry<AccountPreference.EnabledPreference, Boolean> entry : changes.entrySet()) {
            final AttributeValueUpdate update = new AttributeValueUpdate();
            update.setValue(new AttributeValue().withBOOL(entry.getValue()));
            updateItemRequest.addAttributeUpdatesEntry(entry.getKey().toString(), update);
        }
        updateItemRequest.setTableName(tableName);
        dynamoDB.updateItem(updateItemRequest);
        return changes;
    }

    @Override
    public Map<AccountPreference.EnabledPreference, Boolean> get(final Long accountId) {
        final GetItemRequest getItemRequest = new GetItemRequest();
        getItemRequest.addKeyEntry(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(accountId.toString()));
        getItemRequest.setTableName(tableName);
        final GetItemResult result = dynamoDB.getItem(getItemRequest);

        return itemToPreferences(result.getItem(), optOuts);
    }

    public static Map<AccountPreference.EnabledPreference, Boolean> itemToPreferences(final Map<String, AttributeValue> item, final Set<AccountPreference.EnabledPreference> optOuts) {
        final Map<AccountPreference.EnabledPreference, Boolean> temp = Maps.newHashMap();
        for(final AccountPreference.EnabledPreference pref : AccountPreference.EnabledPreference.values()) {
            temp.put(pref, optOuts.contains(pref)); // TRUE if in opt outs otherwise it's opt-in so, FALSE by default
        }

        if(item == null || item.isEmpty()) {
            return temp;
        }

        for(final AccountPreference.EnabledPreference pref : AccountPreference.EnabledPreference.values()) {
            if(item.containsKey(pref.toString())) {
                temp.put(pref, item.get(pref.toString()).getBOOL());
            }
        }
        return temp;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient) {
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }


    public Boolean isEnabled(final Long accountId, final AccountPreference.EnabledPreference preference) {
        final Map<AccountPreference.EnabledPreference, Boolean> prefs = this.get(accountId);
        return prefs.containsKey(preference) && prefs.get(preference);
    }
}
