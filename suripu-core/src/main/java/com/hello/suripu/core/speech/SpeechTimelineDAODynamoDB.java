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
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Created by ksg on 8/22/16
 */
public class SpeechTimelineDAODynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(SpeechTimelineDAODynamoDB.class);

    private enum SpeechTimelineAttribute implements Attribute {
        ACCOUNT_ID("account_id", "N", ":aid"),  // Hash Key (account-id)
        TS("ts", "S", ":ts"),                    // Range Key (timestamp in UTC)
        SENSE_ID("sense_id", "S", ":sid"),      // sense-id
        ENCRYPTED_UUID("e_uuid", "S", ":euuid");        // encrypted UUID to audio identifier

        private final String name;
        private final String type;
        private final String queryHolder;

        SpeechTimelineAttribute(String name, String type, String queryHolder) {
            this.name = name;
            this.type = type;
            this.queryHolder = queryHolder;
        }

        public String sanitizedName() {
            return toString();
        }
        public String shortName() {
            return name;
        }
        public String type() {
            return type;
        }

        public String queryHolder() { return queryHolder; }
    }

    private Table table;
    private SpeechTimelineDAODynamoDB(final Table table) { this.table = table; }

    public static SpeechTimelineDAODynamoDB create(final AmazonDynamoDB amazonDynamoDB, final String tableName) {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.getTable(tableName);
        return new SpeechTimelineDAODynamoDB(table);
    }

    private static final DateTimeFormatter DATE_TIME_WRITE_FORMATTER = DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT);
    private static final int DATE_TIME_STRING_LENGTH = DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT.length();

    /**
     * Insert one item to DynamoDB
     * @param speechTimeline item to insert
     * @return insert success or failure
     */
    public boolean putItem(final SpeechTimeline speechTimeline) {
        final Item item = speechTimelineToItem(speechTimeline);
        try {
            table.putItem(item);
        } catch (Exception e) {
            LOGGER.error("error=put-speech-timeline-item-fail error_msg={}", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * get single item
     * @param accountId account
     * @param dateTime timestamp
     * @return optional SpeechTimeline
     */
    public Optional<SpeechTimeline> getItem(final Long accountId, final DateTime dateTime) {
        final Item item = table.getItem(SpeechTimelineAttribute.ACCOUNT_ID.shortName(), accountId,
                SpeechTimelineAttribute.TS.shortName(), dateTime.toString(DATE_TIME_WRITE_FORMATTER));

        if (item == null) {
            return Optional.absent();
        }

        return Optional.of(DDBItemToSpeechTimeline(item));
    }

    /**
     * get last spoken command
     * @param accountId account
     * @param lookBackMinutes look back this much from now
     * @return optional SpeechTimeline
     */
    public Optional<SpeechTimeline> getLatest(final Long accountId, final int lookBackMinutes) {
        final DateTime queryTime = DateTime.now(DateTimeZone.UTC).minusMinutes(lookBackMinutes);

        final String keyCondition = getExpression(SpeechTimelineAttribute.ACCOUNT_ID, "=") + " AND " +
                getExpression(SpeechTimelineAttribute.TS, ">=");

        final ValueMap valueMap = new ValueMap()
                .withNumber(SpeechTimelineAttribute.ACCOUNT_ID.queryHolder(), accountId)
                .withString(SpeechTimelineAttribute.TS.queryHolder(), queryTime.toString(DATE_TIME_WRITE_FORMATTER));

        final List<SpeechTimeline> results = query(keyCondition, valueMap, false, 1);
        if (!results.isEmpty()) {
            return Optional.of(results.get(0)); // latest only
        }
        return Optional.absent();

    }

    /**
     * get spoken commands between dates (inclusive for both ends), with limit
     * @param accountId account
     * @param startDate query start date
     * @param endDate query end date
     * @param limit number of results to return
     * @return List of SpeechTimeline
     */
    public List<SpeechTimeline> getItemsByDate(final Long accountId, final DateTime startDate, final DateTime endDate, final int limit) {
        final String keyCondition = getExpression(SpeechTimelineAttribute.ACCOUNT_ID, "=") + " AND " +
                getBetweenExpression(SpeechTimelineAttribute.TS);

        final ValueMap valueMap = new ValueMap()
                .withNumber(SpeechTimelineAttribute.ACCOUNT_ID.queryHolder(), accountId)
                .withString(String.format("%s1", SpeechTimelineAttribute.TS.queryHolder()), startDate.toString(DATE_TIME_WRITE_FORMATTER))
                .withString(String.format("%s2", SpeechTimelineAttribute.TS.queryHolder()), endDate.toString(DATE_TIME_WRITE_FORMATTER));

        return query(keyCondition, valueMap, false, limit);
    }

    private List<SpeechTimeline> query(final String keyCondition, final ValueMap valueMap, final Boolean scanForward, final int limit) {

        final QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression(keyCondition)
                .withValueMap(valueMap)
                .withScanIndexForward(false)
                .withMaxResultSize(limit);

        ItemCollection<QueryOutcome> items = table.query(querySpec);
        LOGGER.debug("action=get-speech-timeline-by-date query_result_size={}", items.getTotalCount());

        Iterator<Item> iterator = items.iterator();
        final List<SpeechTimeline> results = Lists.newArrayList();
        while (iterator.hasNext()) {
            results.add(DDBItemToSpeechTimeline(iterator.next()));
        }

        return results;
    }

    private SpeechTimeline DDBItemToSpeechTimeline(Item item) {
        return SpeechTimeline.create(
                item.getLong(SpeechTimelineAttribute.ACCOUNT_ID.shortName()),
                DateTimeUtil.datetimeStringToDateTime(item.getString(SpeechTimelineAttribute.TS.shortName())),
                item.getString(SpeechTimelineAttribute.SENSE_ID.shortName()),
                item.getString(SpeechTimelineAttribute.ENCRYPTED_UUID.shortName())
        );
    }

    private Item speechTimelineToItem(SpeechTimeline speechTimeline) {
        return new Item().withPrimaryKey(SpeechTimelineAttribute.ACCOUNT_ID.shortName(), speechTimeline.accountId)
                .withString(SpeechTimelineAttribute.TS.shortName(), speechTimeline.dateTimeUTC.toString(DATE_TIME_WRITE_FORMATTER))
                .withString(SpeechTimelineAttribute.SENSE_ID.shortName(), speechTimeline.senseId)
                .withString(SpeechTimelineAttribute.ENCRYPTED_UUID.shortName(), speechTimeline.encryptedUUID);

    }

    private String getExpression(final SpeechTimelineAttribute attribute, final String comparator) {
        return String.format("%s %s %s", attribute.shortName(), comparator, attribute.queryHolder());
    }

    private String getBetweenExpression(final SpeechTimelineAttribute attribute) {
        return String.format("%s BETWEEN %s AND %s", attribute.shortName(),
                String.format("%s1", attribute.queryHolder()), String.format("%s2", attribute.queryHolder()));
    }

    public static void createTable(final AmazonDynamoDB amazonDynamoDB, final String tableName) throws InterruptedException {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.createTable(
                tableName,
                Lists.newArrayList(
                        new KeySchemaElement().withAttributeName(SpeechTimelineAttribute.ACCOUNT_ID.shortName()).withKeyType(KeyType.HASH),
                        new KeySchemaElement().withAttributeName(SpeechTimelineAttribute.TS.shortName()).withKeyType(KeyType.RANGE)
                ),
                Lists.newArrayList(
                        new AttributeDefinition().withAttributeName(SpeechTimelineAttribute.ACCOUNT_ID.shortName()).withAttributeType(ScalarAttributeType.N),
                        new AttributeDefinition().withAttributeName(SpeechTimelineAttribute.TS.shortName()).withAttributeType(ScalarAttributeType.S)
                ),
                new ProvisionedThroughput()
                        .withReadCapacityUnits(1L)
                        .withWriteCapacityUnits(1L)
        );

        table.waitForActive();
    }

}
