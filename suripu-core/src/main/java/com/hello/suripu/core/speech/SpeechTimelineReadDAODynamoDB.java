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
import com.google.common.collect.Maps;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by ksg on 8/22/16
 */
public class SpeechTimelineReadDAODynamoDB implements SpeechTimelineReadDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeechTimelineReadDAODynamoDB.class);

    private final Table table;
    private final Vault kmsVault;

     private SpeechTimelineReadDAODynamoDB(final Table table, final Vault kmsVault) {
         this.table = table;
         this.kmsVault = kmsVault;
    }

    public static SpeechTimelineReadDAODynamoDB create(final AmazonDynamoDB amazonDynamoDB,
                                                       final String tableName,
                                                       final Vault kmsVault) {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.getTable(tableName);
        return new SpeechTimelineReadDAODynamoDB(table, kmsVault);
    }

    @Override
    public Optional<SpeechTimeline> getItem(final Long accountId, final DateTime dateTime) {

        final Item item = table.getItem(
                SpeechTimelineAttribute.ACCOUNT_ID.shortName(), accountId,
                SpeechTimelineAttribute.TS.shortName(), dateToString(dateTime));

        if (item != null) {
            return DDBItemToSpeechTimeline(item);
        }
        return Optional.absent();
    }

    @Override
    public Optional<SpeechTimeline> getLatest(final Long accountId, final int lookBackMinutes) {
        final DateTime queryTime = DateTime.now(DateTimeZone.UTC).minusMinutes(lookBackMinutes);

        // query = account_id = :aid AND ts >= :ts
        final String keyCondition = getExpression(SpeechTimelineAttribute.ACCOUNT_ID, "=") + " AND " +
                getExpression(SpeechTimelineAttribute.TS, ">=");

        final ValueMap valueMap = new ValueMap()
                .withNumber(SpeechTimelineAttribute.ACCOUNT_ID.queryHolder(), accountId)
                .withString(SpeechTimelineAttribute.TS.queryHolder(), dateToString(queryTime));

        final List<SpeechTimeline> results = query(keyCondition, valueMap, false, 1); // "false" for reverse chronological query
        if (!results.isEmpty()) {
            return Optional.of(results.get(0)); // latest only
        }
        return Optional.absent();

    }

    @Override
    public List<SpeechTimeline> getItemsByDate(final Long accountId, final DateTime startDate, final DateTime endDate, final int limit) {

        // query = account_id = :aid AND ts BETWEEN :ts1 AND :ts2
        final String keyCondition = getExpression(SpeechTimelineAttribute.ACCOUNT_ID, "=") + " AND " +
                getBetweenExpression(SpeechTimelineAttribute.TS);

        final ValueMap valueMap = new ValueMap()
                .withNumber(SpeechTimelineAttribute.ACCOUNT_ID.queryHolder(), accountId)
                .withString(String.format("%s1", SpeechTimelineAttribute.TS.queryHolder()), dateToString(startDate))
                .withString(String.format("%s2", SpeechTimelineAttribute.TS.queryHolder()), dateToString(endDate));

        return query(keyCondition, valueMap, false, limit); // "false" for reverse chronological query
    }


    private List<SpeechTimeline> query(final String keyCondition, final ValueMap valueMap, final Boolean scanForward, final int limit) {

        final QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression(keyCondition)
                .withValueMap(valueMap)
                .withScanIndexForward(scanForward)
                .withMaxResultSize(limit);

        final ItemCollection<QueryOutcome> items = table.query(querySpec);
        LOGGER.debug("action=get-speech-timeline-by-date query_result_size={}", items.getTotalCount());

        final Iterator<Item> iterator = items.iterator();
        final List<SpeechTimeline> results = Lists.newArrayList();
        while (iterator.hasNext()) {
            final Optional<SpeechTimeline> speechTimelineOptional = DDBItemToSpeechTimeline(iterator.next());
            if (speechTimelineOptional.isPresent()) {
                results.add(speechTimelineOptional.get());
            }
        }

        return results;
    }

    private Optional<SpeechTimeline> DDBItemToSpeechTimeline(Item item) {
        final Long accountId = item.getLong(SpeechTimelineAttribute.ACCOUNT_ID.shortName());
        final Map<String, String> encryptionContext = Maps.newHashMap();
        encryptionContext.put("account_id", accountId.toString());

        final String encryptedUUID = item.getString(SpeechTimelineAttribute.ENCRYPTED_UUID.shortName());

        final Optional<String> optionalUUID = kmsVault.decrypt(encryptedUUID, encryptionContext);
        if (!optionalUUID.isPresent()) {
            return Optional.absent();
        }

        final SpeechTimeline speechTimeline = new SpeechTimeline(
                accountId,
                DateTimeUtil.datetimeStringToDateTime(item.getString(SpeechTimelineAttribute.TS.shortName())),
                item.getString(SpeechTimelineAttribute.SENSE_ID.shortName()),
                optionalUUID.get());

        return Optional.of(speechTimeline);
    }


    //region helper functions
    private String getExpression(final SpeechTimelineAttribute attribute, final String comparator) {
        return String.format("%s %s %s", attribute.shortName(), comparator, attribute.queryHolder());
    }

    private String getBetweenExpression(final SpeechTimelineAttribute attribute) {
        return String.format("%s BETWEEN %s AND %s", attribute.shortName(),
                String.format("%s1", attribute.queryHolder()), String.format("%s2", attribute.queryHolder()));
    }

    private String dateToString(final DateTime dateTime) {
        return dateTime.toString(DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT));
    }
    //endregion

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
