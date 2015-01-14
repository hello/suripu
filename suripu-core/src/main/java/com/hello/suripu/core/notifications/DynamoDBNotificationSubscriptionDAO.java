package com.hello.suripu.core.notifications;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.models.MobilePushRegistration;
import com.hello.suripu.core.oauth.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class DynamoDBNotificationSubscriptionDAO implements NotificationSubscriptionsDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBNotificationSubscriptionDAO.class);
    private final AmazonDynamoDB dynamoDB;
    private final String tableName;

    private final AmazonSNSClient amazonSNSClient;
    private final Map<String, String> arns;

    public DynamoDBNotificationSubscriptionDAO(final AmazonDynamoDB dynamoDB, final String tableName, final AmazonSNSClient amazonSNSClient, final Map<String, String> arns) {
        checkNotNull(arns, "arns can't be null");

        this.dynamoDB = dynamoDB;
        this.tableName = tableName;
        this.amazonSNSClient = amazonSNSClient;
        this.arns = ImmutableMap.copyOf(arns);
    }

    @Override
    public Optional<MobilePushRegistration> getSubscription(final Long accountId, final String deviceToken) {

        final QueryResult result = query(accountId, deviceToken);

        final List<MobilePushRegistration> registrations = fromDynamoDBItems(result.getItems());
        if(registrations.size() == 1) {
            return Optional.of(registrations.get(0));
        }

        return Optional.absent();
    }

    @Override
    public List<MobilePushRegistration> getSubscriptions(final Long accountId) {
        final QueryResult result = query(accountId);
        final List<MobilePushRegistration> registrations = fromDynamoDBItems(result.getItems());
        return ImmutableList.copyOf(registrations);
    }

    @Override
    public void subscribe(final Long accountId, final MobilePushRegistration mobilePushRegistration) {

        final MobilePushRegistration updated = createSNSEndpoint(accountId, mobilePushRegistration);
        LOGGER.debug("Account {} is subscribed to {}", updated.accountId, updated.endpoint.get());

        final PutItemRequest putItemRequest = makePutItemRequest(updated);
        final PutItemResult result = dynamoDB.putItem(putItemRequest);
        LOGGER.debug("results {}", result.toString());
    }


    @Override
    public boolean unsubscribe(final Long accountId, final String deviceToken) {

        final Optional<MobilePushRegistration> optional = this.getSubscription(accountId, deviceToken);
        if(!optional.isPresent()) {
            LOGGER.debug("Did not find subscription with token = {}", deviceToken);
            throw new WebApplicationException(404);
        }

        final DeleteEndpointRequest deleteEndpointRequest = new DeleteEndpointRequest();
        deleteEndpointRequest.withEndpointArn(optional.get().endpoint.get());
        amazonSNSClient.deleteEndpoint(deleteEndpointRequest);

        final DeleteItemRequest deleteItemRequest = makeDeleteItemRequest(optional.get());
        final DeleteItemResult result = dynamoDB.deleteItem(deleteItemRequest);
        return true;
    }


    @Override
    public boolean unsubscribe(final AccessToken accessToken) {

        final List<MobilePushRegistration> registrations = this.getSubscriptions(accessToken.accountId);

        for(final MobilePushRegistration registration : registrations) {
            if(registration.oauthToken.equals(accessToken.serializeAccessToken())) {
                unsubscribe(accessToken.accountId, registration.deviceToken);
                return true;
            }
        }

        return false;
    }


    /**
     * Converts a dynamoDB Items list to a list of MobilePushRegistration
     * @param items
     * @return
     */
    private List<MobilePushRegistration> fromDynamoDBItems(final List<Map<String, AttributeValue>> items) {
        final List<MobilePushRegistration> registrations = new ArrayList<>();

        for (Map<String, AttributeValue> item : items) {
            final String mAccountId = item.get("account_id").getN();
            final String endpoint = item.get("endpoint").getS();
            final String token = item.get("token").getS();

            final String os = (item.containsKey("os")) ? item.get("os").getS() : "";
            final String version = (item.containsKey("version")) ? item.get("version").getS() : "";
            final String appVersion = (item.containsKey("app_version")) ? item.get("app_version").getS() : "";
            final String oauthToken = (item.containsKey("oauth_token")) ? item.get("oauth_token").getS() : "";

            final MobilePushRegistration m = new MobilePushRegistration(
                    Long.parseLong(mAccountId),
                    os,
                    version,
                    appVersion,
                    token,
                    oauthToken,
                    endpoint
            );
            registrations.add(m);
        }

        return registrations;
    }

    /**
     * Creates a specific endpoint for this user + MobilePushRegistration
     *
     * @param accountId
     * @param mobilePushRegistration
     */
    private MobilePushRegistration createSNSEndpoint(final Long accountId, final MobilePushRegistration mobilePushRegistration) {
        final CreatePlatformEndpointRequest request = new CreatePlatformEndpointRequest();

        request.setCustomUserData(accountId.toString());
        request.withToken(mobilePushRegistration.deviceToken); //custom per user
        request.setPlatformApplicationArn(arns.get(mobilePushRegistration.os));

        // TODO: catch exceptions when creating endpoint fails
        final CreatePlatformEndpointResult result = amazonSNSClient.createPlatformEndpoint(request);

        final MobilePushRegistration m = MobilePushRegistration.withEndpointForAccount(
                mobilePushRegistration,
                result.getEndpointArn(),
                accountId
        );

        return m;
    }

    private PutItemRequest makePutItemRequest(final MobilePushRegistration mobilePushRegistration) {
        final PutItemRequest putItemRequest = new PutItemRequest();
        putItemRequest.addItemEntry("account_id", new AttributeValue().withN(mobilePushRegistration.accountId.get().toString()));
        putItemRequest.addItemEntry("token", new AttributeValue().withS(mobilePushRegistration.deviceToken));
        putItemRequest.addItemEntry("endpoint", new AttributeValue().withS(mobilePushRegistration.endpoint.get()));
        putItemRequest.addItemEntry("os", new AttributeValue().withS(mobilePushRegistration.os));
        putItemRequest.addItemEntry("version", new AttributeValue().withS(mobilePushRegistration.version));
        putItemRequest.addItemEntry("app_version", new AttributeValue().withS(mobilePushRegistration.appVersion));

        putItemRequest.withTableName(tableName);

        return putItemRequest;
    }

    private DeleteItemRequest makeDeleteItemRequest(final MobilePushRegistration mobilePushRegistration) {
        final HashMap<String, AttributeValue> key = new HashMap<>();
        key.put("account_id", new AttributeValue().withN(mobilePushRegistration.accountId.get().toString()));
        key.put("token", new AttributeValue().withS(mobilePushRegistration.deviceToken));

        final DeleteItemRequest deleteItemRequest = new DeleteItemRequest();
        deleteItemRequest.withTableName(tableName)
                .withKey(key);

        return deleteItemRequest;
    }

    private QueryResult query(final Long accountId) {
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

        return dynamoDB.query(queryRequest);
    }

    /**
     * Query DynamoDB for the MobilePushRegistration associated with given account and deviceToken
     * @param accountId
     * @param deviceToken
     * @return
     */
    private QueryResult query(final Long accountId, final String deviceToken) {
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

        return dynamoDB.query(queryRequest);
    }
}
