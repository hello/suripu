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
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.UpgradeNodeRequest;
import com.yammer.metrics.annotation.Timed;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jnorgan on 4/30/15.
 */
public class FirmwareUpgradePathDAO {
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public static final String GROUP_NAME_ATTRIBUTE_NAME = "group_name";
    public static final String FROM_FW_VERSION_ATTRIBUTE_NAME = "from_firmware_version";
    public static final String TO_FW_VERSION_ATTRIBUTE_NAME = "to_firmware_version";
    public static final String TIMESTAMP_ATTRIBUTE_NAME = "created";

    public final static String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ssZ";

    private final static Logger LOGGER = LoggerFactory.getLogger(FirmwareUpgradePathDAO.class);

    public FirmwareUpgradePathDAO(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    @Timed
    public void insertFWUpgradeNode(final UpgradeNodeRequest upgradeNode) {

        final Map<String, AttributeValue> item = new HashMap<>();
        item.put(GROUP_NAME_ATTRIBUTE_NAME, new AttributeValue().withS(upgradeNode.groupName));
        item.put(FROM_FW_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(upgradeNode.fromFWVersion.toString()));
        item.put(TO_FW_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(upgradeNode.toFWVersion.toString()));
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

    @Timed
    public Optional<Integer> getNextFWVersionForGroup(final String GroupName, final Integer fromFWVersion) {

        final Condition byGroupName = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(GroupName));

        final Condition byRange = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(new AttributeValue().withN(fromFWVersion.toString()));

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
        final Integer itemNextFW = Integer.parseInt(item.get(TO_FW_VERSION_ATTRIBUTE_NAME).getN());

        return Optional.of(itemNextFW);
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
