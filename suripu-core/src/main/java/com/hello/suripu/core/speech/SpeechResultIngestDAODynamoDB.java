package com.hello.suripu.core.speech;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.collect.Lists;
import com.hello.suripu.core.speech.interfaces.SpeechResultIngestDAO;
import com.hello.suripu.core.speech.models.SpeechResult;
import com.hello.suripu.core.speech.models.SpeechToTextAttribute;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by ksg on 7/19/16
 */
public class SpeechResultIngestDAODynamoDB implements SpeechResultIngestDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeechResultIngestDAODynamoDB.class);

    private final Table table;

    private SpeechResultIngestDAODynamoDB(Table table) {
        this.table = table;
    }

    public static SpeechResultIngestDAODynamoDB create (final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.getTable(tableName);
        return new SpeechResultIngestDAODynamoDB(table);
    }

    @Override
    public Boolean putItem(final SpeechResult speechResult) {
        final Item item = speechResultToDDBItem(speechResult);

        try {
            table.putItem(item);
        } catch (Exception e) {
            LOGGER.error("error=put-item-fail error_msg={}", e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public Boolean updateItem(final SpeechResult speechResult) {
        // check that fields to update is present
        final ValueMap valueMap = new ValueMap();
        valueMap.withString(SpeechToTextAttribute.RESULT.query(), speechResult.result.toString());
        String updateExpression = String.format("SET %s=%s", SpeechToTextAttribute.RESULT.shortName(), SpeechToTextAttribute.RESULT.query());

        valueMap.withString(SpeechToTextAttribute.UPDATED_UTC.query(), getDateString(speechResult.updatedUTC));
        updateExpression += String.format(", %s=%s", SpeechToTextAttribute.UPDATED_UTC.shortName(), SpeechToTextAttribute.UPDATED_UTC.query());

        if (speechResult.handlerType.isPresent()) {
            valueMap.withString(SpeechToTextAttribute.HANDLER_TYPE.query(), speechResult.handlerType.get());
            updateExpression += String.format(", %s=%s", SpeechToTextAttribute.HANDLER_TYPE.shortName(), SpeechToTextAttribute.HANDLER_TYPE.query());
        }

        if (speechResult.s3ResponseKeyname.isPresent()) {
            valueMap.withString(SpeechToTextAttribute.S3_KEYNAME.query(), speechResult.s3ResponseKeyname.get());
            updateExpression += String.format(", %s=%s", SpeechToTextAttribute.S3_KEYNAME.shortName(), SpeechToTextAttribute.S3_KEYNAME.query());
        }

        if (speechResult.command.isPresent()) {
            valueMap.withString(SpeechToTextAttribute.COMMAND.query(), speechResult.command.get());
            updateExpression += String.format(", %s=%s", SpeechToTextAttribute.COMMAND.shortName(), SpeechToTextAttribute.COMMAND.query());
        }

        if (speechResult.responseText.isPresent()) {
            valueMap.withString(SpeechToTextAttribute.RESPONSE_TEXT.query(), speechResult.responseText.get());
            updateExpression += String.format(", %s=%s", SpeechToTextAttribute.RESPONSE_TEXT.shortName(), SpeechToTextAttribute.RESPONSE_TEXT.query());
        }

        return updateQuery(speechResult.audioIdentifier, updateExpression, valueMap, SpeechToTextAttribute.COMMAND.shortName());
    }

    private Boolean updateQuery(final String uuid, final String updateExpression, final ValueMap valueMap, final String updatedAttribute) {
        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(SpeechToTextAttribute.UUID.shortName(), uuid)
                .withUpdateExpression(updateExpression)
                .withValueMap(valueMap)
                .withReturnValues(ReturnValue.ALL_NEW);

        try {
            final UpdateItemOutcome outcome = table.updateItem(updateItemSpec);
            final String updatedValue = outcome.getItem().getString(updatedAttribute);
            LOGGER.debug("action=update-success uuid={} attribute={} new_value={}", uuid, updatedAttribute, updatedValue);
            return true;
        } catch (Exception e) {
            LOGGER.error("error=update-fail error_msg={} uuid={} attribute={}", e.getMessage(), uuid, updatedAttribute);
        }
        return false;
    }
    //endregion


    // All the helper functions
    private String getDateString(final DateTime dateTime) {
        return dateTime.toString(DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT));
    }

    private Item speechResultToDDBItem(final SpeechResult speechResult) {

        final List<String> confidences = SpeechUtils.wakewordsMapToDDBAttribute(speechResult.wakeWordsConfidence);

        final Item item = new Item()
                .withString(SpeechToTextAttribute.UUID.shortName(), speechResult.audioIdentifier)
                .withString(SpeechToTextAttribute.CREATED_UTC.shortName(), getDateString(speechResult.dateTimeUTC))
                .withString(SpeechToTextAttribute.SERVICE.shortName(), speechResult.service.value)
                .withInt(SpeechToTextAttribute.WAKE_ID.shortName(), speechResult.wakeWord.getId())
                .withList(SpeechToTextAttribute.WAKE_CONFIDENCE.shortName(), confidences)
                .withString(SpeechToTextAttribute.RESULT.shortName(), speechResult.result.toString())
                .withString(SpeechToTextAttribute.UPDATED_UTC.shortName(), getDateString(speechResult.updatedUTC));

        // optional attributes

        // transcribed text
        if (speechResult.text.isPresent() && !speechResult.text.get().isEmpty()) {
            item.withString(SpeechToTextAttribute.TEXT.shortName(), speechResult.text.get());
        }

        if (speechResult.confidence.isPresent()) {
            item.withFloat(SpeechToTextAttribute.CONFIDENCE.shortName(), speechResult.confidence.get());
        }

        // command found
        if (speechResult.command.isPresent() && !speechResult.command.get().isEmpty()) {
            item.withString(SpeechToTextAttribute.COMMAND.shortName(), speechResult.command.get());
        }

        if (speechResult.handlerType.isPresent() && !speechResult.handlerType.get().isEmpty()) {
            item.withString(SpeechToTextAttribute.HANDLER_TYPE.shortName(), speechResult.handlerType.get());
        }

        if (speechResult.responseText.isPresent() && !speechResult.responseText.get().isEmpty()) {
            item.withString(SpeechToTextAttribute.RESPONSE_TEXT.shortName(), speechResult.responseText.get());
        }

        if (speechResult.s3ResponseKeyname.isPresent() && !speechResult.s3ResponseKeyname.get().isEmpty()) {
            item.withString(SpeechToTextAttribute.S3_KEYNAME.shortName(), speechResult.s3ResponseKeyname.get());
        }

        return item;
    }
    // endregion

    public static void createTable(final AmazonDynamoDB amazonDynamoDB, final String tableName) throws InterruptedException {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.createTable(
                tableName,
                Lists.newArrayList(
                        new KeySchemaElement().withAttributeName(SpeechToTextAttribute.UUID.shortName()).withKeyType(KeyType.HASH)
                ),
                Lists.newArrayList(
                        new AttributeDefinition().withAttributeName(SpeechToTextAttribute.UUID.shortName()).withAttributeType(ScalarAttributeType.S)
                ),
                new ProvisionedThroughput()
                        .withReadCapacityUnits(1L)
                        .withWriteCapacityUnits(1L)
        );

        table.waitForActive();
    }
}
