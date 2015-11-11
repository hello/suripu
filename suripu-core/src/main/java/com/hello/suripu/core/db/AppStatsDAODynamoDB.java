package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
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
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;

public class AppStatsDAODynamoDB implements AppStatsDAO {
    private final static String ACCOUNT_ID_ATTRIBUTE_NAME = "account_id";
    private final static String INSIGHTS_LAST_VIEWED_ATTRIBUTE_NAME = "insights_last_viewed";
    private final static String QUESTIONS_LAST_VIEWED_ATTRIBUTE_NAME = "questions_last_viewed";

    private final AmazonDynamoDB dynamoDB;
    private final String tableName;

    public AppStatsDAODynamoDB(final AmazonDynamoDB dynamoDB,
                               final String tableName) {
        this.dynamoDB = dynamoDB;
        this.tableName = tableName;
    }


    @Override
    public void putInsightsLastViewed(Long accountId, final DateTime lastViewed) {
        final Long lastViewedMillis = lastViewed.getMillis();
        final UpdateItemRequest updateRequest = new UpdateItemRequest();
        updateRequest.addKeyEntry(ACCOUNT_ID_ATTRIBUTE_NAME,
                new AttributeValue().withN(accountId.toString()));
        final AttributeValueUpdate update = new AttributeValueUpdate();
        update.setValue(new AttributeValue().withN(lastViewedMillis.toString()));
        updateRequest.addAttributeUpdatesEntry(INSIGHTS_LAST_VIEWED_ATTRIBUTE_NAME, update);
        updateRequest.setTableName(tableName);
        dynamoDB.updateItem(updateRequest);
    }

    @Override
    public Optional<DateTime> getInsightsLastViewed(Long accountId) {
        final GetItemRequest getRequest = new GetItemRequest();
        getRequest.addKeyEntry(ACCOUNT_ID_ATTRIBUTE_NAME,
                new AttributeValue().withN(accountId.toString()));
        getRequest.setAttributesToGet(Lists.newArrayList(INSIGHTS_LAST_VIEWED_ATTRIBUTE_NAME));
        getRequest.setTableName(tableName);
        final GetItemResult result = dynamoDB.getItem(getRequest);
        final Map<String, AttributeValue> attributes = result.getItem();
        if (attributes != null && attributes.containsKey(INSIGHTS_LAST_VIEWED_ATTRIBUTE_NAME)) {
            final AttributeValue attribute = attributes.get(INSIGHTS_LAST_VIEWED_ATTRIBUTE_NAME);
            final String value = attribute.getN();
            final long lastViewedMillis = Long.parseLong(value, 10);
            return Optional.of(new DateTime(lastViewedMillis, DateTimeZone.UTC));
        } else {
            return Optional.absent();
        }
    }

    @Override
    public void putQuestionsLastViewed(Long accountId, final DateTime lastViewed) {
        final Long lastViewedMillis = lastViewed.getMillis();
        final UpdateItemRequest updateRequest = new UpdateItemRequest();
        updateRequest.addKeyEntry(ACCOUNT_ID_ATTRIBUTE_NAME,
                                  new AttributeValue().withN(accountId.toString()));
        final AttributeValueUpdate update = new AttributeValueUpdate();
        update.setValue(new AttributeValue().withN(lastViewedMillis.toString()));
        updateRequest.addAttributeUpdatesEntry(QUESTIONS_LAST_VIEWED_ATTRIBUTE_NAME, update);
        updateRequest.setTableName(tableName);
        dynamoDB.updateItem(updateRequest);
    }

    @Override
    public Optional<DateTime> getQuestionsLastViewed(Long accountId) {
        final GetItemRequest getRequest = new GetItemRequest();
        getRequest.addKeyEntry(ACCOUNT_ID_ATTRIBUTE_NAME,
                               new AttributeValue().withN(accountId.toString()));
        getRequest.setAttributesToGet(Lists.newArrayList(QUESTIONS_LAST_VIEWED_ATTRIBUTE_NAME));
        getRequest.setTableName(tableName);
        final GetItemResult result = dynamoDB.getItem(getRequest);
        final Map<String, AttributeValue> attributes = result.getItem();
        if (attributes != null && attributes.containsKey(QUESTIONS_LAST_VIEWED_ATTRIBUTE_NAME)) {
            final AttributeValue attribute = attributes.get(QUESTIONS_LAST_VIEWED_ATTRIBUTE_NAME);
            final String value = attribute.getN();
            final long lastViewedMillis = Long.parseLong(value, 10);
            return Optional.of(new DateTime(lastViewedMillis, DateTimeZone.UTC));
        } else {
            return Optional.absent();
        }
    }


    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient) {
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

        return dynamoDBClient.createTable(request);
    }
}
