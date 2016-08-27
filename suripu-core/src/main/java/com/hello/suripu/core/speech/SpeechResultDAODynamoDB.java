package com.hello.suripu.core.speech;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.BatchGetItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableKeysAndAttributes;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ksg on 7/19/16
 */
public class SpeechResultDAODynamoDB {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeechResultDAODynamoDB.class);

    private static final int MAX_BATCH_GET_SIZE = 100;
    private static final DateTimeFormatter DATE_TIME_READ_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ");

    private enum SpeechToTextAttribute implements Attribute {
        UUID("uuid", "S", ":uuid"),                  // uuid of saved audio in S3
        CREATED_UTC("created_utc", "S", ":ts"),    // timestamp  y-m-d h:m:s
        TEXT("text", "S", ":t"),                  // transcribed text
        SERVICE("service", "S", ":s"),            // service used -- google
        CONFIDENCE("conf", "N", ":c"),            // transcription confidence
        INTENT("intent", "S", ":i"),              // the next 4 attributes describes the parsed command
        ACTION("cmd_action", "S", ":a"),
        INTENT_CATEGORY("cat", "S", ":ic"),
        COMMAND("cmd", "S", ":cmd"),
        WAKE_ID("wake_id", "N", ":wid"),            // wake-word ID
        WAKE_CONFIDENCE("wake_conf", "SS", ":wc"), // confidence of all wake-words
        RESULT("cmd_result", "S", ":res"),              // result of speech command (OK, REJECT, TRY_AGAIN, FAILURE)
        RESPONSE_TEXT("resp_text", "S", ":rt"),
        UPDATED_UTC("updated", "S", ":up");

        private final String name;
        private final String type;
        private final String query;

        SpeechToTextAttribute(String name, String type, String query) {
            this.name = name;
            this.type = type;
            this.query = query;
        }

        public String sanitizedName() {
            return toString();
        }
        public String shortName() {
            return name;
        }

        public String query() { return query; }
        @Override
        public String type() {
            return type;
        }

    }


    private final Table table;
    private final DynamoDB dynamoDB;

    private SpeechResultDAODynamoDB(Table table, DynamoDB dynamoDB) {
        this.table = table;
        this.dynamoDB = dynamoDB;
    }

    public static SpeechResultDAODynamoDB create (final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.getTable(tableName);
        return new SpeechResultDAODynamoDB(table, dynamoDB);
    }

    //region crud
    public Optional<SpeechResult> getItem(final String uuid) {
        try {
            final Item item = table.getItem(SpeechToTextAttribute.UUID.shortName(), uuid);
            final SpeechResult result = DDBItemToSpeechResult(item);
            return Optional.of(result);
        } catch (Exception e) {
            LOGGER.error("error=unable-to-get-item query={}", uuid);
        }
        return Optional.absent();
    }


    public List<SpeechResult> getItems(final List<String> uuids) {

        final List<List<String>> uuidBatches = Lists.partition(uuids, MAX_BATCH_GET_SIZE);
        final List<SpeechResult> speechResults = Lists.newArrayList();

        for (List<String> uuidBatch : uuidBatches) {
            final TableKeysAndAttributes tableKeysAndAttributes = new TableKeysAndAttributes(table.getTableName());
            for (final String uuid : uuidBatch) {
                tableKeysAndAttributes.addHashOnlyPrimaryKey(SpeechToTextAttribute.UUID.shortName(), uuid);
            }

            BatchGetItemOutcome outcome = dynamoDB.batchGetItem(tableKeysAndAttributes);
            Map<String, KeysAndAttributes> unprocessed = null;

            do {
                for (final String tableName : outcome.getTableItems().keySet()) {
                    final List<Item> items = outcome.getTableItems().get(tableName);
                    for (final Item item : items) {
                        speechResults.add(DDBItemToSpeechResult(item));
                    }
                    unprocessed = outcome.getUnprocessedKeys();
                    if (!unprocessed.isEmpty()) {
                        outcome = dynamoDB.batchGetItemUnprocessed(unprocessed);
                    }
                }
            } while (!(unprocessed != null && unprocessed.isEmpty()));
        }

        return speechResults;
    }


    public boolean putItem(final SpeechResult speechResult) {
        final Item item = speechResultToDDBItem(speechResult);
        try {
            table.putItem(item);
        } catch (Exception e) {
            LOGGER.error("error=put-item-fail msg={}", e.getMessage());
            return false;
        }
        return true;
    }

    public boolean updateItemCommand(final SpeechResult speechResult) {
        final String updateExpression = String.format("SET %s = %s, %s = %s, %s = %s, %s = %s, %s = %s",
                SpeechToTextAttribute.ACTION.shortName(), SpeechToTextAttribute.ACTION.query(),
                SpeechToTextAttribute.INTENT.shortName(), SpeechToTextAttribute.INTENT.query(),
                SpeechToTextAttribute.INTENT_CATEGORY.shortName(), SpeechToTextAttribute.INTENT_CATEGORY.query(),
                SpeechToTextAttribute.COMMAND.shortName(), SpeechToTextAttribute.COMMAND.query(),
                SpeechToTextAttribute.UPDATED_UTC.shortName(), SpeechToTextAttribute.UPDATED_UTC.query()
        );

        final ValueMap valueMap = new ValueMap()
                .withString(SpeechToTextAttribute.INTENT.query(), speechResult.intent.toString())
                .withString(SpeechToTextAttribute.ACTION.query(), speechResult.action.toString())
                .withString(SpeechToTextAttribute.INTENT_CATEGORY.query(), speechResult.intentCategory.toString())
                .withString(SpeechToTextAttribute.COMMAND.query(), speechResult.command)
                .withString(SpeechToTextAttribute.UPDATED_UTC.query(), getDateString(speechResult.updatedUTC));

        return updateQuery(speechResult.audioIdentifier, updateExpression, valueMap, SpeechToTextAttribute.COMMAND.shortName());
    }

    public boolean updateItemResult(final SpeechResult speechResult) {
        final String updateExpression = String.format("SET %s = %s, %s = %s, %s = %s",
                SpeechToTextAttribute.RESULT.shortName(), SpeechToTextAttribute.RESULT.query(),
                SpeechToTextAttribute.RESPONSE_TEXT.shortName(), SpeechToTextAttribute.RESPONSE_TEXT.query(),
                SpeechToTextAttribute.UPDATED_UTC.shortName(), SpeechToTextAttribute.UPDATED_UTC.query()
        );

        final ValueMap valueMap = new ValueMap()
                .withString(SpeechToTextAttribute.RESULT.query(), speechResult.result.toString())
                .withString(SpeechToTextAttribute.RESPONSE_TEXT.query(), speechResult.responseText)
                .withString(SpeechToTextAttribute.UPDATED_UTC.query(), getDateString(speechResult.updatedUTC));

        return updateQuery(speechResult.audioIdentifier, updateExpression, valueMap, SpeechToTextAttribute.RESULT.shortName());
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

    private DateTime getDateTime(final String dateString) {
        return DateTime.parse(dateString + "Z", DATE_TIME_READ_FORMATTER).withZone(DateTimeZone.UTC);
    }
// DateTime.parse(dateString + ":00Z", DATE_TIME_READ_FORMATTER).withZone(DateTimeZone.UTC);
    // region DDBItemToObject
    private Map<String, Float> wakeWordsConfidenceFromDDBItem(final Item item) {
        final Set<String> values = item.getStringSet(SpeechToTextAttribute.WAKE_CONFIDENCE.shortName());
        final Map<String, Float> confidences = Maps.newHashMap();

        for (final String value : values) {
            final String[] parts = value.split("_");
            final String wakeWord = WakeWord.fromString(parts[0]).getWakeWordText();
            confidences.put(wakeWord, Float.valueOf(parts[1]));
        }
        return confidences;
    }

    private SpeechResult DDBItemToSpeechResult(final Item item) {
        final SpeechToTextService service = SpeechToTextService.fromString(item.getString(SpeechToTextAttribute.SERVICE.shortName()));
        final Intention.IntentType intent = Intention.IntentType.fromString(item.getString(SpeechToTextAttribute.INTENT.shortName()));
        final Intention.ActionType action = Intention.ActionType.fromString(item.getString(SpeechToTextAttribute.ACTION.shortName()));
        final Intention.IntentCategory category = Intention.IntentCategory.fromString(item.getString(SpeechToTextAttribute.INTENT_CATEGORY.shortName()));
        final WakeWord wakeWord = WakeWord.fromInteger(item.getInt(SpeechToTextAttribute.WAKE_ID.shortName()));
        final Result result = Result.fromString(item.getString(SpeechToTextAttribute.RESULT.shortName()));

        return new SpeechResult.Builder()
                .withAudioIndentifier(item.getString(SpeechToTextAttribute.UUID.shortName()))
                .withDateTimeUTC(getDateTime(item.getString(SpeechToTextAttribute.CREATED_UTC.shortName())))
                .withUpdatedUTC(getDateTime(item.getString(SpeechToTextAttribute.UPDATED_UTC.shortName())))
                .withText(item.getString(SpeechToTextAttribute.TEXT.shortName()))
                .withResponseText(item.getString(SpeechToTextAttribute.RESPONSE_TEXT.shortName()))
                .withService(service)
                .withConfidence(item.getFloat(SpeechToTextAttribute.CONFIDENCE.shortName()))
                .withIntent(intent)
                .withAction(action)
                .withIntentCategory(category)
                .withCommand(item.getString(SpeechToTextAttribute.COMMAND.shortName()))
                .withWakeWord(wakeWord)
                .withWakeWordsConfidence(wakeWordsConfidenceFromDDBItem(item))
                .withResult(result)
                .build();
    }

    private Item speechResultToDDBItem(final SpeechResult speechResult) {
        final List<String> confidences = SpeechUtils.wakewordsMapToDDBAttribute(speechResult.wakeWordsConfidence);
        return new Item()
                .withString(SpeechToTextAttribute.UUID.shortName(), speechResult.audioIdentifier)
                .withString(SpeechToTextAttribute.CREATED_UTC.shortName(), getDateString(speechResult.dateTimeUTC))
                .withString(SpeechToTextAttribute.TEXT.shortName(), speechResult.text)
                .withString(SpeechToTextAttribute.RESPONSE_TEXT.shortName(), speechResult.responseText)
                .withString(SpeechToTextAttribute.SERVICE.shortName(), speechResult.service.value)
                .withFloat(SpeechToTextAttribute.CONFIDENCE.shortName(), speechResult.confidence)
                .withString(SpeechToTextAttribute.INTENT.shortName(), speechResult.intent.toString())
                .withString(SpeechToTextAttribute.ACTION.shortName(), speechResult.action.toString())
                .withString(SpeechToTextAttribute.INTENT_CATEGORY.shortName(), speechResult.intentCategory.toString())
                .withString(SpeechToTextAttribute.COMMAND.shortName(), speechResult.command)
                .withInt(SpeechToTextAttribute.WAKE_ID.shortName(), speechResult.wakeWord.getId())
                .withList(SpeechToTextAttribute.WAKE_CONFIDENCE.shortName(), confidences)
                .withString(SpeechToTextAttribute.RESULT.shortName(), speechResult.result.toString())
                .withString(SpeechToTextAttribute.UPDATED_UTC.shortName(), getDateString(speechResult.updatedUTC));
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
