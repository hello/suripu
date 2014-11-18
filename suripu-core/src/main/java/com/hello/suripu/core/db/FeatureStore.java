package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnConsumedCapacity;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureStore.class);

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
        conditions.put("ns", new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(namespace)));
        conditions.put("name", new Condition()
                .withComparisonOperator(ComparisonOperator.GT)
                .withAttributeValueList(new AttributeValue().withS(" ")));

        final int queryLimit = 100; //TODO: scan table instead?

        final QueryRequest query = new QueryRequest(tableName)
                .withKeyConditions(conditions)
                .withLimit(queryLimit)
                .withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);


        final QueryResult results = amazonDynamoDB.query(query);


        final Map<String, Feature> finalMap = new HashMap<>();

        for(final Map<String, AttributeValue> map : results.getItems()) {
            final String name = map.get("name").getS();
            final String value = map.get("value").getS();
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
                .addItemEntry("ns",new AttributeValue().withS(namespace))
                .addItemEntry("name", new AttributeValue().withS(feature.name))
                .addItemEntry("value", new AttributeValue().withS(feature.serialize()));

        amazonDynamoDB.putItem(putItemRequest);
    }

    public List<Feature> getAllFeatures() {
        final Map<String, Feature> features = getData();
        return Lists.newArrayList(features.values());
    }


}
