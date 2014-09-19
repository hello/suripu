package com.hello.suripu.core.db.notifications;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.MobilePushRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamoDBNotificationSubscriptionDAO implements NotificationSubscriptionsDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBNotificationSubscriptionDAO.class);
    private final AmazonDynamoDB dynamoDB;
    private final String tableName;

    public DynamoDBNotificationSubscriptionDAO(final AmazonDynamoDB dynamoDB, final String tableName) {
        this.dynamoDB = dynamoDB;
        this.tableName = tableName;
    }

    @Override
    public Optional<MobilePushRegistration> getSubscription(Long accountId, String deviceToken) {

        final String key = accountId.toString();

        final Condition hashKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(key));

        final Condition rangeKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(deviceToken));

        final Map<String, Condition> keyConditions = new HashMap<String, Condition>();
        keyConditions.put("account_id", hashKeyCondition);
        keyConditions.put("token", rangeKeyCondition);

        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(tableName)
                .withKeyConditions(keyConditions)
                .withLimit(1);

        final QueryResult result = dynamoDB.query(queryRequest);

        final List<MobilePushRegistration> registrations = fromDynamoDBItems(result.getItems());
        if(registrations.size() == 1) {
            return Optional.of(registrations.get(0));
        }

        return Optional.absent();
    }

    @Override
    public List<MobilePushRegistration> getSubscriptions(Long accountId) {
        final String key = accountId.toString();

        final Condition hashKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(key));

        final Condition rangeKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.GT)
                .withAttributeValueList(new AttributeValue().withS(" "));

        final Map<String, Condition> keyConditions = new HashMap<String, Condition>();
        keyConditions.put("account_id", hashKeyCondition);
        keyConditions.put("token", rangeKeyCondition);

        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(tableName)
                .withKeyConditions(keyConditions)
                .withLimit(5);

        final QueryResult result = dynamoDB.query(queryRequest);
        final List<MobilePushRegistration> registrations = fromDynamoDBItems(result.getItems());
        return ImmutableList.copyOf(registrations);
    }

    @Override
    public void subscribe(Long accountId, MobilePushRegistration mobilePushRegistration) {
        final PutItemRequest putItemRequest = new PutItemRequest();
        putItemRequest.addItemEntry("account_id", new AttributeValue().withN(accountId.toString()));
        putItemRequest.addItemEntry("token", new AttributeValue().withS(mobilePushRegistration.deviceToken));
        putItemRequest.addItemEntry("endpoint", new AttributeValue().withS(mobilePushRegistration.endpoint.get()));
        putItemRequest.withTableName(tableName);
        PutItemResult result = dynamoDB.putItem(putItemRequest);
        LOGGER.debug("results {}", result);
    }

    @Override
    public boolean unsubscribe(final Long accountId, final String deviceToken) {

        final Map<String, ExpectedAttributeValue> expectedValues = new HashMap<String, ExpectedAttributeValue>();


        final HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
        key.put("account_id", new AttributeValue().withN(accountId.toString()));
        key.put("token", new AttributeValue().withS(deviceToken));
        final DeleteItemRequest deleteItemRequest = new DeleteItemRequest();
        deleteItemRequest.withTableName(tableName)
                .withKey(key);

//        .addExpectedEntry("token", new ExpectedAttributeValue()
//                .withComparisonOperator("EQ")
//                .withAttributeValueList(new AttributeValue().withS(deviceToken))
//        );

        final DeleteItemResult result = dynamoDB.deleteItem(deleteItemRequest);
        return true;
    }

    private List<MobilePushRegistration> fromDynamoDBItems(List<Map<String, AttributeValue>> items) {
        final List<MobilePushRegistration> registrations = new ArrayList<>();

        for (Map<String, AttributeValue> item : items) {
            final String mAccountId = item.get("account_id").getN();
            final String endpoint = item.get("endpoint").getS();
            final String token = item.get("token").getS();

            // TODO: retrieve these from the object
            final String os = "ios";
            final String version = "7.1.2";
            final String appVersion = "1.0";
            final MobilePushRegistration m = new MobilePushRegistration(
                    Long.parseLong(mAccountId),
                    os,
                    version,
                    appVersion,
                    token,
                    endpoint
            );
            registrations.add(m);
        }

        return registrations;
    }
}
