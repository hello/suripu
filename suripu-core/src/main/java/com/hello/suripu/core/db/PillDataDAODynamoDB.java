package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
 * val      (svm_no_gravity, N)
 * om       (offset_millis, N)
 * lutcts   (local_utc_ts, S)
 * mr       (motion_range, N)
 * kc       (kickoff_count, N)
 * od       (on_duration, N)
 * see https://hello.hackpad.com/Pill-Data-Explained-wgXcyalTFcq
 */
public class PillDataDAODynamoDB implements PillDataIngestDAO {
    private final static Logger LOGGER = LoggerFactory.getLogger(PillDataDAODynamoDB.class);
    private static final int MAX_PUT_ITEMS = 25;
    private static final int MAX_BATCH_WRITE_ATTEMPTS = 5;

    private final AmazonDynamoDB dynamoDBClient;
    private final String tablePrefix;

    public enum PillDataAttribute implements Attribute {
        ACCOUNT_ID ("aid", "N"),
        TS_PILL_ID ("ts|pil", "S"),
        VALUE ("val", "N"),
        OFFSET_MILLIS ("om", "N"),
        LOCAL_UTC_TS ("lutcts", "S"),
        MOTION_RANGE ("mr", "N"),
        KICKOFF_COUNTS ("kc", "N"),
        ON_DURATION ("od", "N");

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
            .add(PillDataAttribute.VALUE)
            .add(PillDataAttribute.OFFSET_MILLIS)
            .add(PillDataAttribute.LOCAL_UTC_TS)
            .add(PillDataAttribute.MOTION_RANGE)
            .add(PillDataAttribute.KICKOFF_COUNTS)
            .add(PillDataAttribute.ON_DURATION).build();

    private static final Set<String> TARGET_ATTRIBUTE_NAMES = ImmutableSet.of(
            PillDataAttribute.ACCOUNT_ID.name,
            PillDataAttribute.TS_PILL_ID.name,
            PillDataAttribute.VALUE.name,
            PillDataAttribute.OFFSET_MILLIS.name,
            PillDataAttribute.LOCAL_UTC_TS.name,
            PillDataAttribute.MOTION_RANGE.name,
            PillDataAttribute.KICKOFF_COUNTS.name,
            PillDataAttribute.ON_DURATION.name);

    // Store everything to the minute level
    private static final DateTimeFormatter DATE_TIME_READ_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ");
    private static final String DATE_TIME_STRING_TEMPLATE = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DATE_TIME_WRITE_FORMATTER = DateTimeFormat.forPattern(DATE_TIME_STRING_TEMPLATE);

    public PillDataDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tablePrefix) {
        this.dynamoDBClient = dynamoDBClient;
        this.tablePrefix = tablePrefix;
    }

    public String getTableName(final DateTime dateTime) {
        final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy_MM");
        return tablePrefix + "_" + dateTime.toString(formatter);
    }

    private static AttributeValue getRangeKey(final Long timestamp, final String pillId) {
        final DateTime dateTime = new DateTime(timestamp, DateTimeZone.UTC).withMillisOfSecond(0);
        return new AttributeValue(dateTime.toString(DATE_TIME_WRITE_FORMATTER) + "|" + pillId);
    }

    private Map<String, AttributeValue> toDynamoDBItem(final TrackerMotion trackerMotion) {
        final Map<String, AttributeValue> item = Maps.newHashMap();
        item.put(PillDataAttribute.ACCOUNT_ID.name, new AttributeValue().withN(String.valueOf(trackerMotion.accountId)));
        item.put(PillDataAttribute.TS_PILL_ID.name, getRangeKey(trackerMotion.timestamp, trackerMotion.externalTrackerId));
        item.put(PillDataAttribute.VALUE.name, new AttributeValue().withN(String.valueOf(trackerMotion.value)));
        item.put(PillDataAttribute.OFFSET_MILLIS.name, new AttributeValue().withN(String.valueOf(trackerMotion.offsetMillis)));

        //TODO: may not need this
        final DateTime localUTCDateTIme = new DateTime(trackerMotion.timestamp, DateTimeZone.UTC).plusMillis(trackerMotion.offsetMillis);
        item.put(PillDataAttribute.LOCAL_UTC_TS.name, new AttributeValue().withS(localUTCDateTIme.toString(DATE_TIME_WRITE_FORMATTER)));

        item.put(PillDataAttribute.MOTION_RANGE.name, new AttributeValue().withN(String.valueOf(trackerMotion.motionRange)));
        item.put(PillDataAttribute.KICKOFF_COUNTS.name, new AttributeValue().withN(String.valueOf(trackerMotion.kickOffCounts)));
        item.put(PillDataAttribute.ON_DURATION.name, new AttributeValue().withN(String.valueOf(trackerMotion.onDurationInSeconds)));
        return item;
    }

    private String externalTrackerIdFromDDBItem(final Map<String, AttributeValue> item) {
        return item.get(PillDataAttribute.TS_PILL_ID.name).getS().substring(DATE_TIME_STRING_TEMPLATE.length() + 1);
    }

    private DateTime timestampFromDDBItem(final Map<String, AttributeValue> item) {
        final String dateString = item.get(PillDataAttribute.TS_PILL_ID.name).getS().substring(0, DATE_TIME_STRING_TEMPLATE.length());
        return DateTime.parse(dateString + "Z", DATE_TIME_READ_FORMATTER).withZone(DateTimeZone.UTC);
    }

    private TrackerMotion fromDynamoDBItem(final Map<String, AttributeValue> item) {
        return new TrackerMotion.Builder()
                .withAccountId(PillDataAttribute.ACCOUNT_ID.getLong(item))
                .withExternalTrackerId(externalTrackerIdFromDDBItem(item))
                .withTimestampMillis(timestampFromDDBItem(item).getMillis())
                .withValue(PillDataAttribute.VALUE.getInteger(item))
                .withOffsetMillis(PillDataAttribute.OFFSET_MILLIS.getInteger(item))
                .withMotionRange(PillDataAttribute.MOTION_RANGE.getLong(item))
                .withKickOffCounts(PillDataAttribute.KICKOFF_COUNTS.getLong(item))
                .withOnDurationInSeconds(PillDataAttribute.ON_DURATION.getLong(item))
                .build();
    }

    private static void backoff(int numberOfAttempts) {
        try {
            long sleepMillis = (long) Math.pow(2, numberOfAttempts) * 50;
            LOGGER.warn("Throttled by DynamoDB, sleeping for {} ms.", sleepMillis);
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while attempting exponential backoff.");
        }
    }

    public Map<String, List<WriteRequest>> toWriteRequestItems(final List<TrackerMotion> trackerMotionList) {
        final Map<String, Map<String, WriteRequest>> writeRequestMap = Maps.newHashMap(); // table -> requests
        for (final TrackerMotion trackerMotion: trackerMotionList) {
            final DateTime dateTimeUTC = new DateTime(trackerMotion.timestamp, DateTimeZone.UTC).withMillisOfSecond(0);
            final String tableName = getTableName(dateTimeUTC);

            // convert trackerMotion to DynamoDB item
            final Map<String, AttributeValue> item = toDynamoDBItem(trackerMotion);

            // make sure the batch of write requests has unique hash-range keys, over-write
            final String hashRangeKey = item.get(PillDataAttribute.ACCOUNT_ID.name).getN() + item.get(PillDataAttribute.TS_PILL_ID.name).getS();
            final WriteRequest request = new WriteRequest().withPutRequest(new PutRequest().withItem(item));
            if (writeRequestMap.containsKey(tableName)) {
                writeRequestMap.get(tableName).put(hashRangeKey, request);
            } else {
                final Map<String, WriteRequest> newMap = Maps.newHashMap();
                newMap.put(hashRangeKey, request);
                writeRequestMap.put(tableName, newMap);
            }
        }

        // flatten
        final Map<String, List<WriteRequest>> requestItems = Maps.newHashMapWithExpectedSize(writeRequestMap.size());
        for (final Map.Entry<String, Map<String, WriteRequest>> entry : writeRequestMap.entrySet()) {
            requestItems.put(entry.getKey(), Lists.newArrayList(entry.getValue().values()));
        }
        return requestItems;
    }

    /**
     * Insert a list of TrackerMotion of size 25 (current DynamoDB max batch size)
     * @param trackerMotionList list of pill data
     * @param retry try to insert again if throttled, backoff appropriately
     * @return list of unsuccessful inserts
     */
    public List<TrackerMotion> batchInsert(final List<TrackerMotion> trackerMotionList, final Boolean retry) {
        if (trackerMotionList.size() > MAX_PUT_ITEMS) {
            return trackerMotionList;
        }

        Map<String, List<WriteRequest>> requestItems = toWriteRequestItems(trackerMotionList);
        int numAttempts = 0;
        do {
            if (numAttempts > 0) {
                // Being throttled! Back off, buddy.
                backoff(numAttempts);
            }

            numAttempts++;
            final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest().withRequestItems(requestItems);
            final BatchWriteItemResult result = this.dynamoDBClient.batchWriteItem(batchWriteItemRequest);

            // check for unprocessed items
            requestItems = result.getUnprocessedItems();
        } while (retry && !requestItems.isEmpty() && (numAttempts < MAX_BATCH_WRITE_ATTEMPTS));

        // convert write request items back to tracker motion
        final List<TrackerMotion> remainingTrackerMotions = Lists.newArrayList();
        for (final List<WriteRequest> writeRequests : requestItems.values()) {
            for (final WriteRequest request : writeRequests) {
                final TrackerMotion trackerMotion = fromDynamoDBItem(request.getPutRequest().getItem());
                remainingTrackerMotions.add(trackerMotion);
            }
        }

        return remainingTrackerMotions;
    }

    /**
     * Insert a list of any size to DDB, if size > 25, will be partitioned
     * @param trackerMotionList list of pill data to insert
     * @return number of successful inserts
     */
    public int batchInsertTrackerMotionData(final List<TrackerMotion> trackerMotionList, int batchSize) {
        final List<List<TrackerMotion>> dataList = Lists.partition(trackerMotionList, MAX_PUT_ITEMS);
        int numberInserted = 0;

        for (final List<TrackerMotion> trackerMotions : dataList) {
            try {
                final List<TrackerMotion> remaining = batchInsert(trackerMotions, true);
                numberInserted += (trackerMotions.size() - remaining.size());
            } catch (AmazonClientException e) {
                LOGGER.error("Got exception while attempting to batchInsert to DynamoDB: {}", e);
            }
        }
        return numberInserted;
    }

    public List<TrackerMotion> getSinglePillData(final Long accountId, final String externalPillId, final DateTime queryDateTime) {
        final Map<String, Condition> queryConditions = Maps.newHashMap();

        queryConditions.put(PillDataAttribute.ACCOUNT_ID.name, new Condition()
                        .withComparisonOperator(ComparisonOperator.EQ)
                        .withAttributeValueList(new AttributeValue().withN(accountId.toString())));

        final AttributeValue rangeKey = getRangeKey(queryDateTime.getMillis(), externalPillId);
        queryConditions.put(PillDataAttribute.TS_PILL_ID.name, new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(rangeKey));

        final String tableName = getTableName(queryDateTime);
        final QueryRequest queryRequest = new QueryRequest()
                .withTableName(tableName)
                .withKeyConditions(queryConditions)
                .withAttributesToGet(TARGET_ATTRIBUTE_NAMES)
                .withLimit(1);

        final QueryResult queryResult = this.dynamoDBClient.query(queryRequest);

        final List<Map<String, AttributeValue>> items = queryResult.getItems();

        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        final TrackerMotion trackerMotion = fromDynamoDBItem(items.get(0));
        return Lists.newArrayList(trackerMotion);
    }

    @Override
    public Class name() {
        return  PillDataDAODynamoDB.class;
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
                new AttributeDefinition().withAttributeName(PillDataAttribute.ACCOUNT_ID.name).withAttributeType(PillDataAttribute.ACCOUNT_ID.type),
                new AttributeDefinition().withAttributeName(PillDataAttribute.TS_PILL_ID.name).withAttributeType(PillDataAttribute.TS_PILL_ID.type)
        );

        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        return dynamoDBClient.createTable(request);
    }

}
