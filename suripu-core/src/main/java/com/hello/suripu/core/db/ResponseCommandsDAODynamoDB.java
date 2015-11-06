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
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.google.common.base.Joiner;
import com.yammer.metrics.annotation.Timed;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jnorgan on 4/23/15.
 */
public class ResponseCommandsDAODynamoDB {
    private final AmazonDynamoDB dynamoDBClient;
    private final String tableName;

    public static final String DEVICE_ID_ATTRIBUTE_NAME = "device_id";
    public static final String COMMAND_ATTRIBUTE_NAME = "commands";
    public static final String FW_VERSION_ATTRIBUTE_NAME = "firmware_version";
    public static final String TIMESTAMP_ATTRIBUTE_NAME = "request_time";
    public final static String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ssZ";

    private final static Logger LOGGER = LoggerFactory.getLogger(ResponseCommandsDAODynamoDB.class);

    public enum ResponseCommand {
        RESET_TO_FACTORY_FW("reset_to_factory_fw"),
        RESET_MCU("reset_mcu"),
        SET_LOG_LEVEL("set_log_level");

        private String value;
        private ResponseCommand(final String value) {
            this.value = value;
        }

        public static ResponseCommand create(final String val) {
            for (final ResponseCommand command : ResponseCommand.values()) {
                if (command.value.equals(val.toLowerCase())) {
                    return command;
                }
            }
            throw new IllegalArgumentException(String.format("%s is not a valid Response Command name.", val));
        }
    }


    public ResponseCommandsDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tableName){
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    @Timed
    public void insertResponseCommands(final String deviceId, final Integer fwVersion, final Map<ResponseCommand, String> respCommands) {

        Map<String, AttributeValue> commands = new HashMap<>();
        for (Map.Entry<ResponseCommand, String> cmd : respCommands.entrySet()) {
            final ResponseCommand resp = cmd.getKey();
            commands.put(resp.value, new AttributeValue().withS(cmd.getValue()));
        }

        final Map<String, AttributeValue> item = new HashMap<>();
        item.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
        item.put(FW_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(fwVersion.toString()));
        item.put(COMMAND_ATTRIBUTE_NAME, new AttributeValue().withM(commands));
        item.put(TIMESTAMP_ATTRIBUTE_NAME, new AttributeValue().withS(dateTimeToString(DateTime.now())));


        final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, item);
        try {
            final PutItemResult result = this.dynamoDBClient.putItem(putItemRequest);
        }catch (AmazonServiceException awsEx){
            LOGGER.error("Response command failed. AWS service error: {}", awsEx.getMessage());
        }catch (AmazonClientException awcEx){
            LOGGER.error("Response command failed. Client error: {}", awcEx.getMessage());
        }

    }

    public Map<ResponseCommand, String> getResponseCommands(final String deviceId, final Integer fwVersion, final List<ResponseCommand> commandsList) {
        if (commandsList.isEmpty()) {
            return Collections.EMPTY_MAP;
        }

        try {

            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#commands", COMMAND_ATTRIBUTE_NAME);
            final List<String> expressionCommands = new ArrayList<>();
            Integer cmdCount = 0;
            for (ResponseCommand command : commandsList) {
                expressionAttributeNames.put("#cmd" + cmdCount.toString(), command.value);
                expressionCommands.add("#commands.#cmd" + cmdCount.toString());
                cmdCount++;
            }
            final String exprCmds = Joiner.on(",").join(expressionCommands);

            final HashMap<String, AttributeValue> keys = new HashMap<>();
            keys.put(DEVICE_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
            keys.put(FW_VERSION_ATTRIBUTE_NAME, new AttributeValue().withN(fwVersion.toString()));

            final UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                    .withTableName(tableName)
                    .withKey(keys)
                    .withUpdateExpression("remove " + exprCmds)
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withReturnValues(ReturnValue.ALL_OLD);

            final UpdateItemResult updateResult = this.dynamoDBClient.updateItem(updateItemRequest);

            if (updateResult.getAttributes() == null) {
                LOGGER.debug("Update results null. Nothing removed.");
                return Collections.EMPTY_MAP;
            }

            final Map<ResponseCommand, String> respCommandMap = new HashMap<>();
            final Map<String, AttributeValue> updatedEntry = updateResult.getAttributes();

            if(updatedEntry == null || updatedEntry.isEmpty()) {
                return Collections.EMPTY_MAP;
            }

            for(final ResponseCommand remCmd: commandsList){
                //Build response Command map
                if(!updatedEntry.get(COMMAND_ATTRIBUTE_NAME).getM().containsKey(remCmd.value)) {
                    continue;
                }

                if (updatedEntry.containsKey(DEVICE_ID_ATTRIBUTE_NAME)
                        && updatedEntry.containsKey(FW_VERSION_ATTRIBUTE_NAME)
                        && updatedEntry.containsKey(COMMAND_ATTRIBUTE_NAME)
                        && updatedEntry.containsKey(TIMESTAMP_ATTRIBUTE_NAME)) {

                    final Integer itemFWVersion = Integer.parseInt(updatedEntry.get(FW_VERSION_ATTRIBUTE_NAME).getN());
                    final Map<String, AttributeValue> itemCommand = updatedEntry.get(COMMAND_ATTRIBUTE_NAME).getM();
                    final String itemCmdValue = itemCommand.get(remCmd.value).getS();

                    if (itemFWVersion.equals(fwVersion)) {
                        respCommandMap.put(remCmd, itemCmdValue);
                    }
                }
            }

            return respCommandMap;

        }   catch (Exception e) {
            LOGGER.debug("Failed to retrieve Response Command for Device: {}", deviceId);
        }
        return Collections.EMPTY_MAP;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        //Ensuring unique Device&Command
        request.withKeySchema(
                new KeySchemaElement().withAttributeName(DEVICE_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(FW_VERSION_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(DEVICE_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S),
                new AttributeDefinition().withAttributeName(FW_VERSION_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N)

        );

        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        return dynamoDBClient.createTable(request);
    }

    public static String dateTimeToString(final DateTime dateTime) {
        return dateTime.toString(DATETIME_FORMAT);
    }

    public static DateTime stringToDateTime(final String dateString) {
        return DateTime.parse(dateString, DateTimeFormat.forPattern(DATETIME_FORMAT));
    }

}

