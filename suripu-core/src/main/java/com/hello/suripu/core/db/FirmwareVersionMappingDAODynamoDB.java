package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FirmwareVersionMappingDAODynamoDB implements FirmwareVersionMappingDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(FirmwareVersionMappingDAODynamoDB.class);

    public final static String FW_HASH = "fw_hash";
    public final static String HUMAN_VERSION = "human_versions";

    private final AmazonDynamoDB amazonDynamoDB;
    private final String tableName;

    public FirmwareVersionMappingDAODynamoDB(final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.tableName = tableName;
    }

    @Override
    public void put(final String fwHash, final String humanVersion) {
        final Map<String, AttributeValue> attributes = Maps.newHashMap();
        attributes.put(FW_HASH, new AttributeValue().withS(fwHash));


        final Map<String, AttributeValueUpdate> attributeValueUpdateMap = Maps.newHashMap();

        attributeValueUpdateMap.put(HUMAN_VERSION, new AttributeValueUpdate()
                .withAction(AttributeAction.ADD)
                .withValue(new AttributeValue().withSS(Sets.newHashSet(humanVersion))));

        final UpdateItemResult updateItemResult = amazonDynamoDB.updateItem(tableName, attributes, attributeValueUpdateMap);
    }

    @Override
    public List<String> get(final String fwHash) {
        final Map<String, AttributeValue> key = Maps.newHashMap();
        key.put(FW_HASH, new AttributeValue().withS(fwHash));

        final GetItemRequest getItemRequest = new GetItemRequest();
        getItemRequest.withTableName(tableName).withKey(key).withAttributesToGet(HUMAN_VERSION);

        GetItemResult getItemResult;
        try {
            getItemResult = amazonDynamoDB.getItem(getItemRequest);
        } catch (AmazonServiceException ase){
            LOGGER.error("error=fw_ver_map_failed hash={}", fwHash);
            return Collections.EMPTY_LIST;
        }

        final Map<String, AttributeValue> item = getItemResult.getItem();

        if (item != null && item.containsKey(HUMAN_VERSION)) {
            return item.get(HUMAN_VERSION).getSS();
        }

        return Lists.newArrayList();
    }

    @Override
    public Map<String, List<String>> getBatch(final ImmutableSet<String> fwHashSet) {
        final BatchGetItemRequest batchGetItemRequest = new BatchGetItemRequest();
        final List<Map<String, AttributeValue>> itemKeys = Lists.newArrayList();

        for (final String fwHash : fwHashSet) {
            final Map<String, AttributeValue> attributeValueMap = Maps.newHashMap();
            attributeValueMap.put(FW_HASH, new AttributeValue().withS(fwHash));
            itemKeys.add(attributeValueMap);
        }

        final KeysAndAttributes key = new KeysAndAttributes().withKeys(itemKeys).withAttributesToGet(FW_HASH, HUMAN_VERSION);
        final Map<String, KeysAndAttributes> requestItems = Maps.newHashMap();
        requestItems.put(tableName, key);

        batchGetItemRequest.withRequestItems(requestItems);

        try {
            final BatchGetItemResult batchGetItemResult = amazonDynamoDB.batchGetItem(batchGetItemRequest);
            final Map<String, List<String>> results = Maps.newHashMap();

            for (final String item : batchGetItemResult.getResponses().keySet()) {
                final List<Map<String, AttributeValue>> responses = batchGetItemResult.getResponses().get(item);
                for (final Map<String, AttributeValue> response : responses) {
                    if (response.containsKey(HUMAN_VERSION)) {
                        final List<String> fwVersions = response.get(HUMAN_VERSION).getSS();
                        results.put(response.get(FW_HASH).getS(), fwVersions);
                    }
                }
            }
            return results;
        } catch (AmazonServiceException ase){
            LOGGER.error("error=fw_ver_map_failed");

        }
        return Collections.EMPTY_MAP;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB amazonDynamoDB) {
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(FW_HASH).withKeyType(KeyType.HASH)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(FW_HASH).withAttributeType(ScalarAttributeType.S)
        );

        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = amazonDynamoDB.createTable(request);
        return result;
    }
}
