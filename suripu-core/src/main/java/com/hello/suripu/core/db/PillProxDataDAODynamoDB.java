package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Expressions;
import com.hello.suripu.core.db.dynamo.expressions.Expression;
import com.hello.suripu.core.db.responses.DynamoDBResponse;
import com.hello.suripu.core.models.PillProxData;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by kingshy on 11/10/15.
 *
 * Similar to prod_sense_data_*, store pill data in monthly DDB tables.
 * Schema:
 * aid      (HK, account_id, N)
 * ts|pill  (RK, ts|pill_external_id, S)
 * prox      (prox, N)
 * om       (offset_millis, N)
 * lutcts   (local_utc_ts, S)
 * see https://hello.hackpad.com/Pill-Data-Explained-wgXcyalTFcq
 */

public class PillProxDataDAODynamoDB extends TimeSeriesDAODynamoDB<PillProxData> {
    private final static Logger LOGGER = LoggerFactory.getLogger(PillProxDataDAODynamoDB.class);

    public PillProxDataDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tablePrefix) {
        super(dynamoDBClient, tablePrefix);
    }

    public enum PillDataAttribute implements Attribute {
        ACCOUNT_ID ("aid", "N"),
        TS_PILL_ID ("ts|pil", "S"),
        PROX ("prox", "N"),
        OFFSET_MILLIS ("om", "N"),
        LOCAL_UTC_TS ("lutcts", "S");

        private final String name;
        private final String type;

        PillDataAttribute(final String name, final String type) {
            this.name = name;
            this.type = type;
        }

        private Long getLong(final Map<String, AttributeValue> item) {
            if (item.containsKey(this.name)) {
                return Long.parseLong(item.get(this.name).getN());
            }
            return 0L;
        }

        private Integer getInteger(final Map<String, AttributeValue> item) {
            if (item.containsKey(this.name)) {
                return Integer.parseInt(item.get(this.name).getN());
            }
            return 0;
        }

        public String sanitizedName() {
            return toString();
        }
        public String shortName() {
            return name;
        }
    }

    private static final Set<PillDataAttribute> TARGET_ATTRIBUTES = new ImmutableSet.Builder<PillDataAttribute>()
            .add(PillDataAttribute.ACCOUNT_ID)
            .add(PillDataAttribute.TS_PILL_ID)
            .add(PillDataAttribute.PROX)
            .add(PillDataAttribute.OFFSET_MILLIS)
            .add(PillDataAttribute.LOCAL_UTC_TS)
            .build();


    // Store one datapoint per minute, ts can contain seconds value
    private static final DateTimeFormatter DATE_TIME_READ_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ");
    private static final String DATE_TIME_STRING_TEMPLATE = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DATE_TIME_WRITE_FORMATTER = DateTimeFormat.forPattern(DATE_TIME_STRING_TEMPLATE);

    //region Override abstract methods
    @Override
    protected Logger logger() {
        return LOGGER;
    }

    @Override
    protected Integer maxQueryAttempts() {
        return 5;
    }

    @Override
    protected Integer maxBatchWriteAttempts() {
        return 5;
    }

    @Override
    protected String hashKeyName() {
        return PillDataAttribute.ACCOUNT_ID.name;
    }

    @Override
    protected String rangeKeyName() {
        return PillDataAttribute.TS_PILL_ID.name;
    }

    @Override
    protected String hashKeyType() {
        return PillDataAttribute.ACCOUNT_ID.type;
    }

    @Override
    protected String rangeKeyType() {
        return PillDataAttribute.TS_PILL_ID.type;
    }

    @Override
    protected String getHashKey(AttributeValue attributeValue) {
        return attributeValue.getN();
    }

    @Override
    protected String getRangeKey(AttributeValue attributeValue) {
        return attributeValue.getS();
    }

    @Override
    protected DateTime getTimestamp(PillProxData pillProxData) {
        return new DateTime(pillProxData.ts, DateTimeZone.UTC).withMillisOfSecond(0);
    }

    @Override
    protected Map<String, AttributeValue> toAttributeMap(PillProxData pillProxData) {
        final Map<String, AttributeValue> item = Maps.newHashMap();
        item.put(PillDataAttribute.ACCOUNT_ID.name, new AttributeValue().withN(String.valueOf(pillProxData.accountId)));
        item.put(PillDataAttribute.TS_PILL_ID.name, getRangeKey(pillProxData.ts, pillProxData.pillId));
        item.put(PillDataAttribute.PROX.name, new AttributeValue().withN(String.valueOf(pillProxData.proxValue)));
        item.put(PillDataAttribute.OFFSET_MILLIS.name, new AttributeValue().withN(String.valueOf(pillProxData.offsetMillis)));

        final DateTime localUTCDateTIme = new DateTime(pillProxData.ts, DateTimeZone.UTC).plusMillis(pillProxData.offsetMillis);
        item.put(PillDataAttribute.LOCAL_UTC_TS.name, new AttributeValue().withS(localUTCDateTIme.toString(DATE_TIME_WRITE_FORMATTER)));

        return item;
    }

    @Override
    public String getTableName(final DateTime dateTime) {
        final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy_MM");
        return tablePrefix + "_" + dateTime.toString(formatter);
    }

    @Override
    public List<String> getTableNames(DateTime start, DateTime end) {
        final LinkedList<String> names = Lists.newLinkedList();
        for (final DateTime dateTime: DateTimeUtil.dateTimesForStartOfMonthBetweenDates(start, end)) {
            final String tableName = getTableName(dateTime);
            names.add(tableName);
        }
        return names;
    }
    //endregion

    private static AttributeValue getRangeKey(final DateTime dateTime, final String pillId) {
        return new AttributeValue(dateTime.toString(DATE_TIME_WRITE_FORMATTER) + "|" + pillId);
    }

    private static AttributeValue getRangeKey(final Long timestamp, final String pillId) {
        final DateTime dateTime = new DateTime(timestamp, DateTimeZone.UTC).withMillisOfSecond(0);
        return getRangeKey(dateTime, pillId);
    }

    private static AttributeValue toAttributeValue(final Long value) {
        return new AttributeValue().withN(String.valueOf(value));
    }

    private static AttributeValue dateTimeToAttributeValue(final DateTime dateTime) {
        return new AttributeValue(dateTime.toString(DATE_TIME_WRITE_FORMATTER));
    }

    private String externalTrackerIdFromDDBItem(final Map<String, AttributeValue> item) {
        return item.get(PillDataAttribute.TS_PILL_ID.name).getS().substring(DATE_TIME_STRING_TEMPLATE.length() + 1);
    }

    private DateTime timestampFromDDBItem(final Map<String, AttributeValue> item) {
        final String dateString = item.get(PillDataAttribute.TS_PILL_ID.name).getS()
                .substring(0, DATE_TIME_STRING_TEMPLATE.length());
        return DateTime.parse(dateString + "Z", DATE_TIME_READ_FORMATTER).withZone(DateTimeZone.UTC);
    }


    private PillProxData fromDynamoDBItem(final Map<String, AttributeValue> item) {
        return PillProxData.create(88L, "test", 99, DateTime.now());
    }



    /**
     * Insert a list of any size to DDB, if size > 25, will be partitioned
     * @param pillProxDataList list of pill data to insert
     * @return number of successful inserts
     */
    public int batchInsert(final List<PillProxData> pillProxDataList, int batchSize) {
        final List<List<PillProxData>> dataList = Lists.partition(pillProxDataList, MAX_PUT_ITEMS);
        int numberInserted = 0;

        for (final List<PillProxData> trackerMotions : dataList) {
            try {
                numberInserted += batchInsert(trackerMotions);
            } catch (AmazonClientException e) {
                LOGGER.error("Got exception while attempting to batchInsert to DynamoDB: {}", e);
            }
        }
        return numberInserted;
    }

    /**
     * Get a single datapoint (used for tests for now)
     * @param accountId hash key
     * @param externalPillId range key pill id
     * @param queryDateTimeUTC range key ts
     * @return list of tracker motion
     */
    public Optional<PillProxData> getSinglePillData(final Long accountId,
                                                 final String externalPillId,
                                                 final DateTime queryDateTimeUTC) {
        // add two minutes for query time upper bound.
        final DynamoDBResponse response = getItemsBetweenTS(accountId, queryDateTimeUTC, queryDateTimeUTC.plusMinutes(2), Optional.of(externalPillId));
        final ImmutableList<PillProxData> items = ImmutableList.copyOf(attributeMapsToPillDataList(response.data));

        if (items.isEmpty()) {
            return Optional.absent();
        }

        return Optional.of(items.get(0));
    }

    final List<PillProxData> attributeMapsToPillDataList(final List<Map<String, AttributeValue>> items) {
        final List<PillProxData> dataList = Lists.newArrayListWithCapacity(items.size());
        for (final Map<String, AttributeValue> item : items) {
            dataList.add(fromDynamoDBItem(item));
        }
        return dataList;
    }

    private DynamoDBResponse getItemsBetweenTS(final long accountId,
                                               final DateTime startTimestampUTC,
                                               final DateTime endTimestampUTC,
                                               final Optional<String> optionalExternalPillId) {

        // aid = accountId, ts >= start, ts <= end
        // with pill_id == "", there is no need to minus 1 minute from the endTimestamp
        String externalPillId = "";
        DateTime queryEndTimestampUTC = endTimestampUTC;
        if (optionalExternalPillId.isPresent()) {
            externalPillId = optionalExternalPillId.get();
            queryEndTimestampUTC = endTimestampUTC.minusMinutes(1);
        }

        final Expression keyConditionExpression = Expressions.and(
                Expressions.equals(PillDataAttribute.ACCOUNT_ID, toAttributeValue(accountId)),
                Expressions.between(PillDataAttribute.TS_PILL_ID,
                        getRangeKey(startTimestampUTC, externalPillId),
                        getRangeKey(queryEndTimestampUTC, externalPillId)));

        final List<Map<String, AttributeValue>> results = Lists.newArrayList();
        final DynamoDBResponse response = queryTables(
                getTableNames(startTimestampUTC, queryEndTimestampUTC),
                keyConditionExpression,
                TARGET_ATTRIBUTES);

        for (final Map<String, AttributeValue> result : response.data) {
            results.add(result);
        }
        return new DynamoDBResponse(results, response.status, response.exception);
    }

    private DynamoDBResponse getItemsBetweenLocalTS(final long accountId,
                                               final DateTime startLocalTime,
                                               final DateTime endLocalTime,
                                               final Optional<String> optionalExternalPillId) {
        // aid = accountId, lutcts >= startLocal, lutcts <= endLocal (note, inclusive)
        final DateTime startTimestampUTC = startLocalTime.minusDays(1).minusMinutes(1);
        final DateTime endTimestampUTC = endLocalTime.plusDays(1).plusMinutes(1);

        String externalPillId = "";
        if (optionalExternalPillId.isPresent()) {
            externalPillId = optionalExternalPillId.get();
        }

        final Expression keyConditionExpression = Expressions.and(
                Expressions.equals(PillDataAttribute.ACCOUNT_ID, toAttributeValue(accountId)),
                Expressions.between(PillDataAttribute.TS_PILL_ID,
                        getRangeKey(startTimestampUTC, externalPillId),
                        getRangeKey(endTimestampUTC, externalPillId)));

        final Expression filterExpression = Expressions.between(
                PillDataAttribute.LOCAL_UTC_TS,
                dateTimeToAttributeValue(startLocalTime),
                dateTimeToAttributeValue(endLocalTime));

        final DynamoDBResponse response = queryTables(getTableNames(startTimestampUTC, endTimestampUTC),
                keyConditionExpression,
                filterExpression,
                TARGET_ATTRIBUTES);

        final List<Map<String, AttributeValue>> results = Lists.newArrayList();
        for (final Map<String, AttributeValue> result : response.data) {
            results.add(result);
        }
        return new DynamoDBResponse(results, response.status, response.exception);
    }

    //region query methods mirroring TrackerMotionDAO
    public ImmutableList<PillProxData> getBetween(final long accountId,
                                                   final DateTime startTimestampUTC,
                                                   final DateTime endTimestampUTC) {
        final DynamoDBResponse response = getItemsBetweenTS(accountId, startTimestampUTC, endTimestampUTC, Optional.<String>absent());
        return ImmutableList.copyOf(attributeMapsToPillDataList(response.data));
    }

    public ImmutableList<PillProxData> getBetween(final long accountId,
                                                   final DateTime startTimestampUTC,
                                                   final DateTime endTimestampUTC,
                                                   final String ExternalPillId) {
        final DynamoDBResponse response = getItemsBetweenTS(accountId, startTimestampUTC, endTimestampUTC, Optional.of(ExternalPillId));
        return ImmutableList.copyOf(attributeMapsToPillDataList(response.data));
    }

    public ImmutableList<PillProxData>  getBetweenLocalUTC(final long accountId,
                                                            final DateTime startLocalTime,
                                                            final DateTime endLocalTime) {
        final DynamoDBResponse response = getItemsBetweenLocalTS(accountId, startLocalTime, endLocalTime, Optional.<String>absent());
        return ImmutableList.copyOf(attributeMapsToPillDataList(response.data));
    }

    public Integer getDataCountBetweenLocalUTC(final long accountId,
                                               final DateTime startLocalTime,
                                               final DateTime endLocalTime) {
        final DynamoDBResponse response = getItemsBetweenLocalTS(accountId, startLocalTime, endLocalTime, Optional.<String>absent());
        return response.data.size();
    }

    //region TODO
    public ImmutableList<PillProxData> getBetweenGrouped(final long accountId,
                                                          final DateTime startLocalTime,
                                                          final DateTime endLocalTime,
                                                          final Integer slotDuration) {
        return ImmutableList.of();
    }

    // used by TrackerMotionDAOIT. not implementing.
    public Integer deleteDataTrackerID(final Long trackerId) {
        return 0;
    }

    public ImmutableList<PillProxData> getTrackerOffsetMillis(final long accountId,
                                                               final DateTime startDate,
                                                               final DateTime endDate) {
        return ImmutableList.of();
    }
    //endregion TODO

    //endregion

    public Optional<PillProxData> getMostRecent(final String externalPillId, final Long accountId, final DateTime now) {
        final DateTime startTime = now.minusDays(15);
        final Expression keyConditionExpression = Expressions.and(
                Expressions.equals(PillDataAttribute.ACCOUNT_ID, toAttributeValue(accountId)),
                Expressions.between(PillDataAttribute.TS_PILL_ID, getRangeKey(startTime, externalPillId), getRangeKey(now, externalPillId)));

        final Optional<Map<String, AttributeValue>> latest = getLatest(getTableName(now), keyConditionExpression, TARGET_ATTRIBUTES);

        if (latest.isPresent()) {
            final PillProxData pillProxData = fromDynamoDBItem(latest.get());
            if (pillProxData.pillId.equals(externalPillId)) {
                return Optional.of(pillProxData);
            }
        }

        // Getting the absolute most recent didn't work, so try querying relevant tables.
        final DynamoDBResponse response = queryTables(getTableNames(startTime, now), keyConditionExpression, TARGET_ATTRIBUTES);

        // Iterate through results in reverse order (most recent first)
        for (final Map<String, AttributeValue> item: Lists.reverse(response.data)) {
            final PillProxData pillProxData = fromDynamoDBItem(item);
            if (pillProxData.pillId.equals(externalPillId)) {
                return Optional.of(pillProxData);
            }
        }

        return Optional.absent();
    }


    public Class name() {
        return  PillProxDataDAODynamoDB.class;
    }

    public CreateTableResult createTable(final String tableName) {
        if (!tableName.startsWith(this.tablePrefix)) {
            final String status = String.format("Fail to create %s, wrong prefix!", tableName);
            return new CreateTableResult().withTableDescription(
                    new TableDescription().withTableStatus(status));
        }

        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(PillDataAttribute.ACCOUNT_ID.name).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(PillDataAttribute.TS_PILL_ID.name).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition()
                        .withAttributeName(PillDataAttribute.ACCOUNT_ID.name)
                        .withAttributeType(PillDataAttribute.ACCOUNT_ID.type),
                new AttributeDefinition()
                        .withAttributeName(PillDataAttribute.TS_PILL_ID.name)
                        .withAttributeType(PillDataAttribute.TS_PILL_ID.type)
        );

        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        return dynamoDBClient.createTable(request);
    }

}
