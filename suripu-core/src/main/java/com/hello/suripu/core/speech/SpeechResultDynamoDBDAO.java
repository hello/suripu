package com.hello.suripu.core.speech;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ksg on 7/19/16
 */
public class SpeechResultDynamoDBDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(SpeechResultDynamoDBDAO.class);

    private enum SpeechToTextAttribute implements Attribute {
        ACCOUNT_ID("aid", "N", ":aid"),
        RANGE_KEY("tsdev", "S", ":rk"),         // note, KeyConditionExpression doesn't like "|"
        AUDIO_FILE_ID("file_id", "S", ":aud"),  // uuid of saved audio in S3
        TEXT("text", "S", ":t"),                // transcribed text
        SERVICE("service", "S", ":s"),          // service used -- google
        CONFIDENCE("conf", "N", ":c"),          // transcription confidence
        INTENT("intent", "S", ":i"),            // the next 4 attributes describes the parsed command
        ACTION("action", "S", ":act"),
        INTENT_CATEGORY("cat", "S", ":cat"),
        COMMAND("cmd", "S", ":cmd"),
        WAKE_ID("wake_id", "N", "wid"),             // wake-word ID
        WAKE_CONFIDENCE("wake_conf", "NS", "wc"),   // confidence of all wake-words
        RESULT("result", "S", "res"),
        RESPONSE_TEXT("resp_text", "S", ":rt"),     // result of speech command (OK, REJECT, TRY_AGAIN, FAILURE)
        UPDATED("updated", "S", ":up");

        private final String name;
        private final String type;
        private final String placeholder;

        SpeechToTextAttribute(String name, String type, String placeholder) {
            this.name = name;
            this.type = type;
            this.placeholder = placeholder;
        }

        public String sanitizedName() {
            return toString();
        }
        public String shortName() {
            return name;
        }
        public String getPlaceholder() { return placeholder; }

        @Override
        public String type() {
            return type;
        }

    }


    private final Table table;

    private SpeechResultDynamoDBDAO(Table table) {
        this.table = table;
    }

    public static SpeechResultDynamoDBDAO create (final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.getTable(tableName);
        return new SpeechResultDynamoDBDAO(table);
    }

    private static final Set<SpeechToTextAttribute> TARGET_ATTRIBUTES = new ImmutableSet.Builder<SpeechToTextAttribute>()
            .add(SpeechToTextAttribute.ACCOUNT_ID)
            .add(SpeechToTextAttribute.RANGE_KEY)
            .add(SpeechToTextAttribute.AUDIO_FILE_ID)
            .add(SpeechToTextAttribute.TEXT)
            .add(SpeechToTextAttribute.SERVICE)
            .add(SpeechToTextAttribute.CONFIDENCE)
            .add(SpeechToTextAttribute.INTENT)
            .add(SpeechToTextAttribute.ACTION)
            .add(SpeechToTextAttribute.INTENT_CATEGORY)
            .add(SpeechToTextAttribute.COMMAND)
            .add(SpeechToTextAttribute.WAKE_ID)
            .add(SpeechToTextAttribute.WAKE_CONFIDENCE)
            .add(SpeechToTextAttribute.RESULT)
            .build();

    private static final DateTimeFormatter DATE_TIME_WRITE_FORMATTER = DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT);
    private static final int DATE_TIME_STRING_LENGTH = DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT.length();

    /**
     * Get last speech results for account-id
     * @param accountId user
     * @param senseId sense external id
     * @param lookBackMinutes how far back to query
     * @return Optional SpeechToTextResult
     */
    public Optional<SpeechResult> getLatest(final Long accountId, final String senseId, final int lookBackMinutes) {
        final DateTime queryTime = DateTime.now(DateTimeZone.UTC).minusMinutes(lookBackMinutes);

        //query condition: "aid = :val1 and ts|dev >= :val2"
        final String keyCondition = getExpression(SpeechToTextAttribute.ACCOUNT_ID, "=") + " AND " +
                getExpression(SpeechToTextAttribute.RANGE_KEY, ">=");

        final AttributeValue rangeKey = getRangeKey(queryTime, senseId);
        final ValueMap valueMap = new ValueMap()
                .withLong(SpeechToTextAttribute.ACCOUNT_ID.getPlaceholder(), accountId)
                .withString(SpeechToTextAttribute.RANGE_KEY.getPlaceholder(), rangeKey.getS());

        final QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression(keyCondition)
                .withValueMap(valueMap)
                .withScanIndexForward(false)    // reverse chronological
                .withMaxResultSize(1);          // only get one command

        ItemCollection<QueryOutcome> items = table.query(querySpec);
        LOGGER.debug("action=get-latest-speech-result query_result_size={}", items.getTotalCount());

        Iterator<Item> iterator = items.iterator();

        final List<SpeechResult> results = Lists.newArrayList();
        while(iterator.hasNext()) {
            results.add(DDBItemToSpeechResult(iterator.next()));
        }

        if (!results.isEmpty()) {
            return Optional.of(results.get(0)); // latest only
        }
        return Optional.absent();
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

    public Optional<SpeechResult> getItem(final Long accountId, final DateTime dateTime, final String senseId) {
        final AttributeValue rangeKey = getRangeKey(dateTime, senseId);
        final Item item = table.getItem(
                SpeechToTextAttribute.ACCOUNT_ID.shortName(), accountId,
                SpeechToTextAttribute.RANGE_KEY.shortName(), rangeKey.getS());

        if (item == null) {
            return Optional.absent();
        }

        final SpeechResult result = DDBItemToSpeechResult(item);
        return Optional.of(result);
    }

    //TODO
    public boolean updateItem(final SpeechResult speechResult) {
        return true;
    }


    // All the helper functions
    private static AttributeValue getRangeKey(final DateTime dateTime, final String senseId) {
        return new AttributeValue(dateTime.toString(DATE_TIME_WRITE_FORMATTER) + "|" + senseId);
    }

    private String getExpression(final SpeechToTextAttribute attribute, final String comparator) {
        return String.format("%s %s %s", attribute.shortName(), comparator, attribute.getPlaceholder());
    }


    // region DDBItemToObject
    private  String senseIdFromDDBItem(final Item item) {
        final String rangeKey = item.getString(SpeechToTextAttribute.RANGE_KEY.shortName());
        return rangeKey.substring(DATE_TIME_STRING_LENGTH + 1);
    }

    private DateTime dateTimeFromDDBItem(final Item item) {
        final String rangeKey = item.getString(SpeechToTextAttribute.RANGE_KEY.shortName());
        final String dateString = rangeKey.substring(0, DATE_TIME_STRING_LENGTH);
        return DateTimeUtil.datetimeStringToDateTime(dateString);
    }

    private Map<String, Float> wakeWordsConfidenceFromDDBItem(final Item item) {
        final Set<BigDecimal> values = item.getNumberSet(SpeechToTextAttribute.WAKE_CONFIDENCE.shortName());
        final Map<String, Float> confidences = Maps.newHashMap();

        int setId = 1;
        for (final BigDecimal value : values) {
            final String wakeWord = WakeWord.fromInteger(setId).getWakeWordText();
            confidences.put(wakeWord, Float.valueOf(value.toString()));
        }
        return confidences;
    }

    private Set<Number> wakewordsMapToDDBAttribute(final Map<String, Float> wakeWordsMaps) {
        // get wake word confidence vector
        final Set<Number> confidences = Sets.newHashSet();
        for (final WakeWord word : WakeWord.values()) {
            if (!word.equals(WakeWord.ERROR)) {
                final String wakeWord = word.getWakeWordText();
                if (wakeWordsMaps.containsKey(wakeWord)) {
                    confidences.add(wakeWordsMaps.get(wakeWord));
                }
            }
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
                .withAccountId(item.getLong(SpeechToTextAttribute.ACCOUNT_ID.shortName()))
                .withDateTimeUTC(dateTimeFromDDBItem(item))
                .withUpdatedUTC(DateTimeUtil.datetimeStringToDateTime(item.getString(SpeechToTextAttribute.UPDATED.shortName())))
                .withSenseId(senseIdFromDDBItem(item))
                .withAudioIndentifier(item.getString(SpeechToTextAttribute.AUDIO_FILE_ID.shortName()))
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
        final Set<Number> confidences = wakewordsMapToDDBAttribute(speechResult.wakeWordsConfidence);
        final AttributeValue rangeKey = getRangeKey(speechResult.dateTimeUTC, speechResult.senseId);

        return new Item()
                .withPrimaryKey(SpeechToTextAttribute.ACCOUNT_ID.shortName(), speechResult.accountId)
                .withString(SpeechToTextAttribute.RANGE_KEY.shortName(), rangeKey.getS())
                .withString(SpeechToTextAttribute.AUDIO_FILE_ID.shortName(), speechResult.audioIdentifier)
                .withString(SpeechToTextAttribute.TEXT.shortName(), speechResult.text)
                .withString(SpeechToTextAttribute.RESPONSE_TEXT.shortName(), speechResult.responseText)
                .withString(SpeechToTextAttribute.SERVICE.shortName(), speechResult.service.value)
                .withFloat(SpeechToTextAttribute.CONFIDENCE.shortName(), speechResult.confidence)
                .withString(SpeechToTextAttribute.INTENT.shortName(), speechResult.intent.toString())
                .withString(SpeechToTextAttribute.ACTION.shortName(), speechResult.action.toString())
                .withString(SpeechToTextAttribute.INTENT_CATEGORY.shortName(), speechResult.intentCategory.toString())
                .withString(SpeechToTextAttribute.COMMAND.shortName(), speechResult.command)
                .withInt(SpeechToTextAttribute.WAKE_ID.shortName(), speechResult.wakeWord.getId())
                .withNumberSet(SpeechToTextAttribute.WAKE_CONFIDENCE.shortName(), confidences)
                .withString(SpeechToTextAttribute.RESULT.shortName(), speechResult.result.toString())
                .withString(SpeechToTextAttribute.UPDATED.shortName(), speechResult.updatedUTC.toString(DATE_TIME_WRITE_FORMATTER));
    }
    // endregion


    public static void createTable(final AmazonDynamoDB amazonDynamoDB, final String tableName) throws InterruptedException {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.createTable(
                tableName,
                Lists.newArrayList(
                        new KeySchemaElement().withAttributeName(SpeechToTextAttribute.ACCOUNT_ID.shortName()).withKeyType(KeyType.HASH),
                        new KeySchemaElement().withAttributeName(SpeechToTextAttribute.RANGE_KEY.shortName()).withKeyType(KeyType.RANGE)
                ),
                Lists.newArrayList(
                        new AttributeDefinition().withAttributeName(SpeechToTextAttribute.ACCOUNT_ID.shortName()).withAttributeType(ScalarAttributeType.N),
                        new AttributeDefinition().withAttributeName(SpeechToTextAttribute.RANGE_KEY.shortName()).withAttributeType(ScalarAttributeType.S)
                ),
                new ProvisionedThroughput()
                        .withReadCapacityUnits(1L)
                        .withWriteCapacityUnits(1L)
        );

        table.waitForActive();
    }
}
