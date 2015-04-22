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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.models.OTAHistory;
import com.yammer.metrics.annotation.Timed;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jnorgan on 4/21/15.
 */
public class OTAHistoryDAODynamoDB {
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public static final String DEVICE_ID_ATTRIBUTE_NAME = "device_id";
    public static final String EVENT_TIME_ATTRIBUTE_NAME = "event_time";
    public static final String CURRENT_FW_VERSION_ATTRIBUTE_NAME = "current_firmware_version";
    public static final String NEW_FW_VERSION_ATTRIBUTE_NAME = "new_firmware_version";
    public static final String FILE_LIST_ATTRIBUTE_NAME = "file_list";

    private final static Logger LOGGER = LoggerFactory.getLogger(OTAHistoryDAODynamoDB.class);

    public OTAHistoryDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    @Timed
    public Optional<OTAHistory> insertOTAEvent(final OTAHistory historyEntry) {
        final Map<String, AttributeValue> item = new HashMap<>();
        item.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(historyEntry.device_id));
        item.put(EVENT_TIME_ATTRIBUTE_NAME, new AttributeValue().withS(historyEntry.event_time));
        item.put(CURRENT_FW_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(historyEntry.current_fw_version.toString()));
        item.put(NEW_FW_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(historyEntry.new_fw_version.toString()));
        item.put(FILE_LIST_ATTRIBUTE_NAME, new AttributeValue().withSS(historyEntry.file_list));

        final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, item);
        try {
            final PutItemResult result = this.dynamoDBClient.putItem(putItemRequest);
            return Optional.of(historyEntry);
        }catch (AmazonServiceException awsEx){
            LOGGER.error("OTA Event insert failed. AWS service error {}", awsEx.getMessage());
        }catch (AmazonClientException awcEx){
            LOGGER.error("OTA Event insert failed. Client error.", awcEx.getMessage());
        }

        return Optional.absent();
    }

    public List<OTAHistory> getOTAEvents(final String deviceId, final DateTime startTime, final DateTime endTime) {
        final Map<String, Condition> queryConditions = Maps.newHashMap();
        final List<AttributeValue> values = Lists.newArrayList();

        values.add(new AttributeValue().withS(startTime.toString()));
        values.add(new AttributeValue().withS(endTime.toString()));

        final Condition betweenDatesCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                .withAttributeValueList(values);
        queryConditions.put(EVENT_TIME_ATTRIBUTE_NAME, betweenDatesCondition);

        final Condition selectDeviceIdCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(deviceId));
        queryConditions.put(DEVICE_ID_ATTRIBUTE_NAME, selectDeviceIdCondition);

        final Set<String> targetAttributeSet = Sets.newHashSet(DEVICE_ID_ATTRIBUTE_NAME,
                EVENT_TIME_ATTRIBUTE_NAME,
                CURRENT_FW_VERSION_ATTRIBUTE_NAME,
                NEW_FW_VERSION_ATTRIBUTE_NAME,
                FILE_LIST_ATTRIBUTE_NAME);

        final QueryRequest queryRequest = new QueryRequest(tableName).withKeyConditions(queryConditions)
                .withAttributesToGet(targetAttributeSet)
                .withLimit(50)
                .withScanIndexForward(false);
        QueryResult queryResult;
        try {
            queryResult = this.dynamoDBClient.query(queryRequest);
        } catch (AmazonServiceException ase){
            LOGGER.error("OTA history query failed. Check parameters used.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if(queryResult.getItems() == null){
            return Collections.EMPTY_LIST;
        }

        final List<OTAHistory> otaHistoryList = Lists.newArrayList();

        final List<Map<String, AttributeValue>> items = queryResult.getItems();
        for(final Map<String, AttributeValue> item: items){
            final OTAHistory otaEntry = new OTAHistory(item.get(DEVICE_ID_ATTRIBUTE_NAME).getS(),
                    item.get(EVENT_TIME_ATTRIBUTE_NAME).getS(),
                    Integer.parseInt(item.get(CURRENT_FW_VERSION_ATTRIBUTE_NAME).getN()),
                    Integer.parseInt(item.get(NEW_FW_VERSION_ATTRIBUTE_NAME).getN()),
                    item.get(FILE_LIST_ATTRIBUTE_NAME).getSS());
            otaHistoryList.add(otaEntry);
        }
        return otaHistoryList;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(DEVICE_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(EVENT_TIME_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(DEVICE_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(EVENT_TIME_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)

        );

        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        return dynamoDBClient.createTable(request);
    }

}
