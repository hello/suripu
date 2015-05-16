package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirmwareVersionMappingDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(FirmwareVersionMappingDAO.class);

    public final static String FW_HASH = "fw_hash";
    public final static String HUMAN_VERSION = "human_version";

    private final AmazonDynamoDB amazonDynamoDB;
    private final String tableName;

    public FirmwareVersionMappingDAO(final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.tableName = tableName;
    }

    public void put(final String fwHash, final String humanVersion) {
        final Map<String, AttributeValue> attributes = Maps.newHashMap();
        attributes.put(FW_HASH, new AttributeValue().withS(fwHash));
        attributes.put(HUMAN_VERSION, new AttributeValue().withS(humanVersion));

        final PutItemResult result = amazonDynamoDB.putItem(tableName, attributes);
    }

    public List<String> get(final String fwHash) {
        final Map<String, AttributeValue> attributes = Maps.newHashMap();
        attributes.put(FW_HASH, new AttributeValue().withS(fwHash));


        final Condition byFirmwareHash = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(String.valueOf(fwHash)));

        final Condition byRange = new Condition()
                    .withComparisonOperator(ComparisonOperator.GT.toString())
                    .withAttributeValueList(new AttributeValue().withS(" "));

        final Map<String, Condition> queryConditions = new HashMap<>();
        queryConditions.put(FW_HASH, byFirmwareHash);
        queryConditions.put(HUMAN_VERSION, byRange);

        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(this.tableName)
                .withKeyConditions(queryConditions)
                .withLimit(10);

        final QueryResult queryResult;

        try {
            queryResult = amazonDynamoDB.query(queryRequest);
        } catch (AmazonServiceException ase){
            LOGGER.error("Firmware Version map query failed.");
            return Collections.EMPTY_LIST;
        }

        final List<Map<String, AttributeValue>> items = queryResult.getItems();

        final List<String> humanVersions = Lists.newArrayList();

        if (queryResult.getItems() != null) {
            for (final Map<String, AttributeValue> item : items) {
                humanVersions.add(item.get(HUMAN_VERSION).getS());
            }
        }

        return humanVersions;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB amazonDynamoDB) {
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(FW_HASH).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(HUMAN_VERSION).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(FW_HASH).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(HUMAN_VERSION).withAttributeType(ScalarAttributeType.S)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = amazonDynamoDB.createTable(request);
        return result;
    }
}
