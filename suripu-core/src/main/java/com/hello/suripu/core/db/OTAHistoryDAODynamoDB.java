package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.OTAHistory;
import com.yammer.metrics.annotation.Timed;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jnorgan on 4/21/15.
 */
public class OTAHistoryDAODynamoDB {
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;
    private ObjectMapper mapper = new ObjectMapper();

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
        item.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(historyEntry.deviceId));
        item.put(EVENT_TIME_ATTRIBUTE_NAME, new AttributeValue().withN(historyEntry.eventTime.toString()));
        item.put(CURRENT_FW_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(historyEntry.currentFW.toString()));
        item.put(NEW_FW_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(historyEntry.newFW.toString()));
        item.put(FILE_LIST_ATTRIBUTE_NAME, new AttributeValue().withSS(historyEntry.fileList));

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

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(DEVICE_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(EVENT_TIME_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(DEVICE_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(EVENT_TIME_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N)

        );


        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }

}
