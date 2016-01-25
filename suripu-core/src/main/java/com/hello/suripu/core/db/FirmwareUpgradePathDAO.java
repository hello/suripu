package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.UpgradeNodeRequest;
import com.hello.suripu.core.util.FeatureUtils;
import org.apache.commons.math3.util.Pair;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jnorgan on 4/30/15.
 */
public class FirmwareUpgradePathDAO {
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public static final String GROUP_NAME_ATTRIBUTE_NAME = "group_name";
    public static final String FROM_FW_VERSION_ATTRIBUTE_NAME = "from_firmware_version";
    public static final String TO_FW_VERSION_ATTRIBUTE_NAME = "to_firmware_version";
    public static final String ROLLOUT_PERCENT_ATTRIBUTE_NAME = "rollout_percent";
    public static final String TIMESTAMP_ATTRIBUTE_NAME = "created";

    public final static String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ssZ";

    private final static Logger LOGGER = LoggerFactory.getLogger(FirmwareUpgradePathDAO.class);

    public FirmwareUpgradePathDAO(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public void insertFWUpgradeNode(final UpgradeNodeRequest upgradeNode) {

        final Map<String, AttributeValue> item = new HashMap<>();
        item.put(GROUP_NAME_ATTRIBUTE_NAME, new AttributeValue().withS(upgradeNode.groupName));
        item.put(FROM_FW_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(upgradeNode.fromFWVersion.toString()));
        item.put(TO_FW_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(upgradeNode.toFWVersion.toString()));
        item.put(ROLLOUT_PERCENT_ATTRIBUTE_NAME, new AttributeValue().withN(upgradeNode.rolloutPercent.toString()));
        item.put(TIMESTAMP_ATTRIBUTE_NAME, new AttributeValue().withS(dateTimeToString(DateTime.now())));

        final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, item);
        try {
            final PutItemResult result = this.dynamoDBClient.putItem(putItemRequest);
        }catch (AmazonServiceException awsEx){
            LOGGER.error("FW Upgrade Node insert failed. AWS service error: {}", awsEx.getMessage());
        }catch (AmazonClientException awcEx){
            LOGGER.error("FW Upgrade Node insert failed. Client error: {}", awcEx.getMessage());
        }
    }

    public List<UpgradeNodeRequest> getFWUpgradeNodesForGroup(final String groupName) {
        final Condition byGroupName = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(groupName));

        final Map<String, Condition> queryConditions = new HashMap<>();
        queryConditions.put(GROUP_NAME_ATTRIBUTE_NAME, byGroupName);

        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(this.tableName)
                .withKeyConditions(queryConditions)
                .withLimit(50);

        QueryResult queryResult;
        try {
            queryResult = this.dynamoDBClient.query(queryRequest);
        } catch (AmazonServiceException ase){
            LOGGER.error("getFWUpgradeNodesForGroup query failed. {}", ase.getErrorMessage());
            return Collections.EMPTY_LIST;
        } catch (Exception e) {
            LOGGER.error("Exception thrown while querying upgrade nodes. {}", e.getMessage());
            return Collections.EMPTY_LIST;
        }

        final List<Map<String, AttributeValue>> items = queryResult.getItems();

        if (items.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        final List<UpgradeNodeRequest> upgradeNodes = Lists.newArrayList();

        for (final Map<String, AttributeValue> item : items) {
            final Integer fromFW = Integer.parseInt(item.get(FROM_FW_VERSION_ATTRIBUTE_NAME).getN());
            final Integer toFW = Integer.parseInt(item.get(TO_FW_VERSION_ATTRIBUTE_NAME).getN());
            final Float rolloutPercent = item.containsKey(ROLLOUT_PERCENT_ATTRIBUTE_NAME) ? Float.parseFloat(item.get(ROLLOUT_PERCENT_ATTRIBUTE_NAME).getN()) : FeatureUtils.MAX_ROLLOUT_VALUE;
            final UpgradeNodeRequest nodeRequest = new UpgradeNodeRequest(groupName, fromFW, toFW, rolloutPercent);

            upgradeNodes.add(nodeRequest);
        }

        return upgradeNodes;
    }

    public Optional<UpgradeNodeRequest> deleteFWUpgradeNode(final String groupName, final Integer fromFWVersion){
        try {
            final Map<String, ExpectedAttributeValue> deleteConditions = new HashMap<String, ExpectedAttributeValue>();

            deleteConditions.put(GROUP_NAME_ATTRIBUTE_NAME, new ExpectedAttributeValue(
                    new AttributeValue().withS(groupName)
            ));
            deleteConditions.put(FROM_FW_VERSION_ATTRIBUTE_NAME, new ExpectedAttributeValue(
                    new AttributeValue().withN(fromFWVersion.toString())
            ));

            HashMap<String, AttributeValue> keys = new HashMap<String, AttributeValue>();
            keys.put(GROUP_NAME_ATTRIBUTE_NAME, new AttributeValue().withS(groupName));
            keys.put(FROM_FW_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(fromFWVersion.toString()));

            final DeleteItemRequest deleteItemRequest = new DeleteItemRequest()
                    .withTableName(tableName)
                    .withKey(keys)
                    .withExpected(deleteConditions)
                    .withReturnValues(ReturnValue.ALL_OLD);

            final DeleteItemResult result = this.dynamoDBClient.deleteItem(deleteItemRequest);

            return attributeValuesToUpgradeNode(result.getAttributes());

        } catch (AmazonServiceException ase) {
            LOGGER.error("Failed to delete Upgrade Node for Group: {}. Service error {}", groupName, ase.getMessage());
        } catch (AmazonClientException awcEx){
            LOGGER.error("Failed to delete Upgrade Node for Group: {}. Client error: {}", awcEx.getMessage());
        } catch (Exception e) {
            LOGGER.error("Exception thrown while deleting upgrade node. {}", e.getMessage());
            return Optional.absent();
        }

        return Optional.absent();
    }
    
    public Optional<Pair<String, Float>> getNextFWVersionForGroup(final String GroupName, final String fromFWVersion) {

        final Condition byGroupName = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(GroupName));

        final Condition byRange = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(new AttributeValue().withS(fromFWVersion));

        final Map<String, Condition> queryConditions = new HashMap<>();
        queryConditions.put(GROUP_NAME_ATTRIBUTE_NAME, byGroupName);
        queryConditions.put(FROM_FW_VERSION_ATTRIBUTE_NAME, byRange);

        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(this.tableName)
                .withKeyConditions(queryConditions)
                .withLimit(2);

        QueryResult queryResult;
        try {
            queryResult = this.dynamoDBClient.query(queryRequest);
        } catch (AmazonServiceException ase){
            LOGGER.error("getNextFirmwareVersion query failed. {}", ase.getErrorMessage());
            return Optional.absent();
        } catch (Exception e) {
            LOGGER.error("Exception thrown while querying next firmware version. {}", e.getMessage());
            return Optional.absent();
        }

        final List<Map<String, AttributeValue>> items = queryResult.getItems();

        if (items.isEmpty()) {
            return Optional.absent();
        }

        final Map<String, AttributeValue> item = items.get(0);
        final String itemNextFW = item.get(TO_FW_VERSION_ATTRIBUTE_NAME).getS();

        final Float rolloutPercent = item.containsKey(ROLLOUT_PERCENT_ATTRIBUTE_NAME) ? Float.parseFloat(item.get(ROLLOUT_PERCENT_ATTRIBUTE_NAME).getN()) : FeatureUtils.MAX_ROLLOUT_VALUE;

        final Pair<String, Float> nextFW = new Pair<>(itemNextFW, rolloutPercent);
        return Optional.of(nextFW);
    }

    private Optional<UpgradeNodeRequest> attributeValuesToUpgradeNode(final Map<String, AttributeValue> item){

        try {

            final String groupName = item.get(GROUP_NAME_ATTRIBUTE_NAME).getS();
            final Integer fromFWVersion = Integer.valueOf(item.get(FROM_FW_VERSION_ATTRIBUTE_NAME).getN());
            final Integer newFWVersion = Integer.valueOf(item.get(TO_FW_VERSION_ATTRIBUTE_NAME).getN());
            final Float rolloutPercent = Float.valueOf(item.get(ROLLOUT_PERCENT_ATTRIBUTE_NAME).getN());

            return Optional.of(new UpgradeNodeRequest(groupName, fromFWVersion, newFWVersion, rolloutPercent));
        }catch (Exception ex){
            LOGGER.error("attributeValuesToUpgradeNode error: {}", ex.getMessage());
        }

        return Optional.absent();
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(GROUP_NAME_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(FROM_FW_VERSION_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(GROUP_NAME_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(FROM_FW_VERSION_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N)

        );

        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        return dynamoDBClient.createTable(request);
    }
    public static String dateTimeToString(final DateTime dateTime) {
        return dateTime.toString(DATETIME_FORMAT);
    }
}
