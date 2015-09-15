package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TeamStore implements TeamStoreDAO, BaseDynamoDB {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeamStore.class);

    private final static String NAME_ATTRIBUTE_NAME = "name";
    private final static String TYPE_ATTRIBUTE_NAME = "type";
    private final static String MEMBERS_ATTRIBUTE_NAME = "members";

    public enum Type {
        DEVICES("devices"),
        USERS("users");

        private final String value;
        private Type(final String value) {
            this.value = value;
        }
    }

    private final AmazonDynamoDB dynamoDB;
    private final String tableName;


    public TeamStore(final AmazonDynamoDB dynamoDB, final String tableName) {
        this.dynamoDB = dynamoDB;
        this.tableName = tableName;
    }

    @Override
    public void createTeam(final Team team, final Type type) {

        final Map<String, AttributeValue> item = new HashMap<>();
        item.put(TYPE_ATTRIBUTE_NAME, new AttributeValue().withS(type.toString()));
        item.put(NAME_ATTRIBUTE_NAME, new AttributeValue().withS(team.name));
        item.put(MEMBERS_ATTRIBUTE_NAME, new AttributeValue().withSS(team.ids));

        final PutItemRequest putItemRequest = new PutItemRequest();
        putItemRequest.withTableName(tableName)
                .withItem(item);

        dynamoDB.putItem(putItemRequest);
    }


    @Override
    public Optional<Team> getTeam(final String teamName, final Type type) {

        final Map<String, AttributeValue> item = new HashMap<>();
        item.put(TYPE_ATTRIBUTE_NAME, new AttributeValue().withS(type.toString()));
        item.put(NAME_ATTRIBUTE_NAME, new AttributeValue().withS(teamName));


        final GetItemRequest getItemRequest = new GetItemRequest();
        getItemRequest.withTableName(tableName)
                .withKey(item);

        final GetItemResult result;

        try {
            result = dynamoDB.getItem(getItemRequest);
        }catch (AmazonServiceException awsEx){
            LOGGER.error("getTeam request failed. AWS service error: {}", awsEx.getMessage());
            return Optional.absent();
        }catch (AmazonClientException awcEx){
            LOGGER.error("getTeam request failed. Client error: {}", awcEx.getMessage());
            return Optional.absent();
        }catch (Exception e) {
            LOGGER.error("getTeam request failed. {}", e.getMessage());
            return Optional.absent();
        }

        if(result.getItem() == null){
            return Optional.absent();
        }

        return makeTeam(result.getItem(), type);
    }


    @Override
    public List<Team> getTeams(final Type type) {

        final Map<String, Condition> conditions = new HashMap<>();
        conditions.put(TYPE_ATTRIBUTE_NAME, new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(type.toString())));

        conditions.put(NAME_ATTRIBUTE_NAME, new Condition()
                .withComparisonOperator(ComparisonOperator.GT)
                .withAttributeValueList(new AttributeValue().withS(" ")));

        final QueryRequest queryRequest = new QueryRequest();
        queryRequest.withTableName(tableName)
                .withKeyConditions(conditions)
                .withLimit(100);

        final QueryResult result = dynamoDB.query(queryRequest);

        final List<Team> teams = new ArrayList<>();
        for(Map<String, AttributeValue> attributeValueMap : result.getItems()) {
            final Optional<Team> team = makeTeam(attributeValueMap, type);
            if(team.isPresent()) {
                teams.add(team.get());
            }
        }

        return teams;
    }


    @Override
    public void add(final String teamName, final Type type, final List<String> ids) {
        update(teamName, type, ids, AttributeAction.ADD);
    }

    @Override
    public void remove(final String teamName, final Type type, final List<String> ids) {
        update(teamName, type, ids, AttributeAction.DELETE);
    }

    @Override
    public void delete(final Team team, final Type type) {
        final DeleteItemRequest deleteItemRequest = new DeleteItemRequest();
        deleteItemRequest.withTableName(tableName);
        final Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(TYPE_ATTRIBUTE_NAME, new AttributeValue().withS(type.toString()));
        attributes.put(NAME_ATTRIBUTE_NAME, new AttributeValue().withS(team.name));
        deleteItemRequest.withKey(attributes);

        dynamoDB.deleteItem(deleteItemRequest);
    }


    /**
     * Converts DynamoDB map to Team object
     * @param attributes
     * @param type
     * @return Optional of Team
     */
    private Optional<Team> makeTeam(Map<String, AttributeValue> attributes, Type type) {
        if(!attributes.containsKey(NAME_ATTRIBUTE_NAME) || !attributes.containsKey(MEMBERS_ATTRIBUTE_NAME)) {
            return Optional.absent();
        }

        final Set<String> members = new HashSet<>();
        if(attributes.get(MEMBERS_ATTRIBUTE_NAME).getSS() == null) {
            return Optional.absent();
        }

        members.addAll(attributes.get(MEMBERS_ATTRIBUTE_NAME).getSS());
        final Team team = Team.create(attributes.get(NAME_ATTRIBUTE_NAME).getS(), members);
        return Optional.of(team);
    }

    /**
     * Adds or removes members from set
     * @param teamName
     * @param type
     * @param deviceId
     * @param attributeAction
     */
    private void update(final String teamName, final Type type, final List<String> deviceId, final AttributeAction attributeAction) {
        final UpdateItemRequest updateItemRequest = new UpdateItemRequest();
        final Map<String, AttributeValue> keys = new HashMap<>();
        keys.put(TYPE_ATTRIBUTE_NAME, new AttributeValue().withS(type.toString()));
        keys.put(NAME_ATTRIBUTE_NAME, new AttributeValue().withS(teamName));
        final AttributeValueUpdate update = new AttributeValueUpdate()
                .withAction(attributeAction)
                .withValue(new AttributeValue().withSS(deviceId));

        updateItemRequest
                .withTableName(tableName)
                .withKey(keys)
                .addAttributeUpdatesEntry(MEMBERS_ATTRIBUTE_NAME, update);

        dynamoDB.updateItem(updateItemRequest);
    }


    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(TYPE_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(NAME_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(TYPE_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(NAME_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }
}
