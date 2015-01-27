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
import com.google.common.collect.Maps;

import java.util.Map;

public class AccountPreferencesDynamoDB implements AccountPreferencesDAO {

    private final static String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";
    private final static String ENHANCED_AUDIO_ATTRIBUTE_NAME = "enhanced_audio";


    private final AmazonDynamoDB dynamoDB;
    private final String tableName;

    public AccountPreferencesDynamoDB(final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        this.dynamoDB = amazonDynamoDB;
        this.tableName = tableName;
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
    public Map<AccountPreference.EnabledPreference, Boolean> get(final Long accountId) {
        final GetItemRequest getItemRequest = new GetItemRequest();
        getItemRequest.addKeyEntry(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(accountId.toString()));
        getItemRequest.setTableName(tableName);
        final GetItemResult result = dynamoDB.getItem(getItemRequest);

        return itemToPreferences(result.getItem());
    }

    private Map<AccountPreference.EnabledPreference, Boolean> itemToPreferences(final Map<String, AttributeValue> item) {
        final Map<AccountPreference.EnabledPreference, Boolean> temp = Maps.newHashMap();
        for(final AccountPreference.EnabledPreference pref : AccountPreference.EnabledPreference.values()) {
            temp.put(pref, Boolean.FALSE); // OPT IN
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
}
