package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnConsumedCapacity;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureStore.class);
    private static final String NAME_ATTRIBUTE_NAME = "name";
    private static final String NAMESPACE_ATTRIBUTE_NAME = "ns";
    private static final String VALUE_ATTRIBUTE_NAME = "value";

    private final AmazonDynamoDB amazonDynamoDB;
    private final String tableName;
    private final String namespace;


    public FeatureStore(final AmazonDynamoDB amazonDynamoDB, final String tableName, final String namespace) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.tableName = tableName;
        this.namespace = namespace;
    }

    public Map<String, Feature> getData() {
        LOGGER.trace("Calling getData");

        final Map<String, Condition> conditions = new HashMap<>();
        conditions.put(NAMESPACE_ATTRIBUTE_NAME, new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(namespace)));
        conditions.put(NAME_ATTRIBUTE_NAME, new Condition()
                .withComparisonOperator(ComparisonOperator.GT)
                .withAttributeValueList(new AttributeValue().withS(" ")));

        final int queryLimit = 100; //TODO: scan table instead?

        final QueryRequest query = new QueryRequest(this.tableName)
                .withKeyConditions(conditions)
                .withLimit(queryLimit)
                .withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);


        final QueryResult results = this.amazonDynamoDB.query(query);


        final Map<String, Feature> finalMap = new HashMap<>();

        for(final Map<String, AttributeValue> map : results.getItems()) {
            final String name = map.get(NAME_ATTRIBUTE_NAME).getS();
            final String value = map.get(VALUE_ATTRIBUTE_NAME).getS();
            try {
                finalMap.put(name, Feature.convertToFeature(name, value));
            } catch (Feature.FeatureException e) {
                LOGGER.error("Feature {} is not properly formatted: {}", name, e.getMessage());
            }
        }

        return finalMap;
    }

    public void put(final Feature feature) {
        final PutItemRequest putItemRequest = new PutItemRequest();
        putItemRequest.withTableName(tableName)
                .addItemEntry(NAMESPACE_ATTRIBUTE_NAME,new AttributeValue().withS(namespace))
                .addItemEntry(NAME_ATTRIBUTE_NAME, new AttributeValue().withS(feature.name))
                .addItemEntry(VALUE_ATTRIBUTE_NAME, new AttributeValue().withS(feature.serialize()));

        amazonDynamoDB.putItem(putItemRequest);
    }

    public List<Feature> getAllFeatures() {
        final Map<String, Feature> features = getData();
        return Lists.newArrayList(features.values());
    }


    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(NAMESPACE_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(NAME_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(NAMESPACE_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(NAME_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
