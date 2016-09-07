package com.hello.suripu.core.speech;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.BatchGetItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableKeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.speech.interfaces.SpeechResultReadDAO;
import com.hello.suripu.core.speech.models.Result;
import com.hello.suripu.core.speech.models.SpeechResult;
import com.hello.suripu.core.speech.models.SpeechToTextAttribute;
import com.hello.suripu.core.speech.models.SpeechToTextService;
import com.hello.suripu.core.speech.models.WakeWord;
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
public class SpeechResultReadDAODynamoDB implements SpeechResultReadDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeechResultReadDAODynamoDB.class);

    private static final int MAX_BATCH_GET_SIZE = 100;
    private static final int MAX_GET_ITEMS_ATTEMPTS = 3;
    private static final DateTimeFormatter DATE_TIME_READ_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ");

    private final Table table;
    private final DynamoDB dynamoDB;

    private SpeechResultReadDAODynamoDB(Table table, DynamoDB dynamoDB) {
        this.table = table;
        this.dynamoDB = dynamoDB;
    }

    public static SpeechResultReadDAODynamoDB create (final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.getTable(tableName);
        return new SpeechResultReadDAODynamoDB(table, dynamoDB);
    }

    @Override
    public Optional<SpeechResult> getItem(final String uuid) {
        try {
            final Item item = table.getItem(SpeechToTextAttribute.UUID.shortName(), uuid);
            if (item != null) {
                final SpeechResult result = DDBItemToSpeechResult(item);
                return Optional.of(result);
            }
        } catch (Exception e) {
            LOGGER.error("error=unable-to-get-item error_msg={}", e.getMessage());
        }
        return Optional.absent();
    }

    @Override
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
            int attempts = 0;

            do {
                attempts++;
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
            } while (!(unprocessed != null && unprocessed.isEmpty()) && attempts < MAX_GET_ITEMS_ATTEMPTS);
        }

        return speechResults;
    }

    // All the helper functions
    private DateTime getDateTime(final String dateString) {
        return DateTime.parse(dateString + "Z", DATE_TIME_READ_FORMATTER).withZone(DateTimeZone.UTC);
    }

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
        final WakeWord wakeWord = WakeWord.fromInteger(item.getInt(SpeechToTextAttribute.WAKE_ID.shortName()));
        final Result result = Result.fromString(item.getString(SpeechToTextAttribute.RESULT.shortName()));

        final SpeechResult.Builder builder = new SpeechResult.Builder();
        builder.withAudioIndentifier(item.getString(SpeechToTextAttribute.UUID.shortName()))
                .withDateTimeUTC(getDateTime(item.getString(SpeechToTextAttribute.CREATED_UTC.shortName())))
                .withService(service)
                .withWakeWord(wakeWord)
                .withWakeWordsConfidence(wakeWordsConfidenceFromDDBItem(item))
                .withResult(result)
                .withUpdatedUTC(getDateTime(item.getString(SpeechToTextAttribute.UPDATED_UTC.shortName())));

        if (item.hasAttribute(SpeechToTextAttribute.TEXT.shortName())) {
            builder.withText(item.getString(SpeechToTextAttribute.TEXT.shortName()));
        }

        if (item.hasAttribute(SpeechToTextAttribute.RESPONSE_TEXT.shortName())) {
            builder.withResponseText(item.getString(SpeechToTextAttribute.RESPONSE_TEXT.shortName()));
        }

        if (item.hasAttribute(SpeechToTextAttribute.CONFIDENCE.shortName())) {
            builder.withConfidence(item.getFloat(SpeechToTextAttribute.CONFIDENCE.shortName()));
        }

        if (item.hasAttribute(SpeechToTextAttribute.HANDLER_TYPE.shortName())) {
            builder.withHandlerType(item.getString(SpeechToTextAttribute.HANDLER_TYPE.shortName()));
        }

        if (item.hasAttribute(SpeechToTextAttribute.S3_KEYNAME.shortName())) {
            builder.withS3Keyname(item.getString(SpeechToTextAttribute.S3_KEYNAME.shortName()));
        }

        if (item.hasAttribute(SpeechToTextAttribute.COMMAND.shortName())) {
            builder.withCommand(item.getString(SpeechToTextAttribute.COMMAND.shortName()));
        }

        return builder.build();
    }
}
