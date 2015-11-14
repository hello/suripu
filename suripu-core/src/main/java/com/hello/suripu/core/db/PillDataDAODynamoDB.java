package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.TrackerMotion;
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
 */
public class PillDataDAODynamoDB implements PillDataIngestDAO {
    private final static Logger LOGGER = LoggerFactory.getLogger(PillDataDAODynamoDB.class);
    private static final int MAX_PUT_ITEMS = 25;
    private static final int MAX_BATCH_WRITE_ATTEMPTS = 5;

    private final AmazonDynamoDB dynamoDBClient;
    private final String tablePrefix;

    public static final String ACCOUNT_ID_ATTRIBUTE_NAME = "aid";
    public static final String TS_PILL_ID_ATTRIBUTE_NAME = "ts|pil"; // S
    public static final String VALUE_ATTRIBUTE_NAME = "val";
    public static final String OFFSET_MILLIS_ATTRIBUTE_NAME = "om";
    public static final String LOCAL_UTC_TS_ATTRIBUTE_NAME = "lutcts"; // S
    public static final String MOTION_RANGE_ATTRIBUTE_NAME = "mr";
    public static final String KICKOFF_COUNTS_ATTRIBUTE_NAME = "kc";
    public static final String ON_DURATION_ATTRIBUTE_NAME = "od";

    private static final Set<String> TARGET_ATTRIBUTES = ImmutableSet.of(
            ACCOUNT_ID_ATTRIBUTE_NAME,
            TS_PILL_ID_ATTRIBUTE_NAME,
            VALUE_ATTRIBUTE_NAME,
            OFFSET_MILLIS_ATTRIBUTE_NAME,
            LOCAL_UTC_TS_ATTRIBUTE_NAME,
            MOTION_RANGE_ATTRIBUTE_NAME,
            KICKOFF_COUNTS_ATTRIBUTE_NAME,
            ON_DURATION_ATTRIBUTE_NAME
    );


    // Store everything to the minute level
    private static final DateTimeFormatter DATE_TIME_READ_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:00Z");
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

    private static  AttributeValue getRangeKey(final Long timestamp, final String pillId) {
        final DateTime dateTime = new DateTime(timestamp, DateTimeZone.UTC).withMillisOfSecond(0);
        return new AttributeValue(dateTime.toString(DATE_TIME_WRITE_FORMATTER) + "|" + pillId);
    }

    private Map<String, AttributeValue> toDynamoDBItem(final TrackerMotion trackerMotion) {
        final Map<String, AttributeValue> item = Maps.newHashMap();
        item.put(ACCOUNT_ID_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(trackerMotion.accountId)));
        item.put(TS_PILL_ID_ATTRIBUTE_NAME, getRangeKey(trackerMotion.timestamp, trackerMotion.externalTrackerId));
        item.put(VALUE_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(trackerMotion.value)));
        item.put(OFFSET_MILLIS_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(trackerMotion.offsetMillis)));

        //TODO: may not need this
        final DateTime localUTCDateTIme = new DateTime(trackerMotion.timestamp, DateTimeZone.UTC).plusMillis(trackerMotion.offsetMillis);
        item.put(LOCAL_UTC_TS_ATTRIBUTE_NAME, new AttributeValue().withS(localUTCDateTIme.toString(DATE_TIME_WRITE_FORMATTER)));

        item.put(MOTION_RANGE_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(trackerMotion.motionRange)));
        item.put(KICKOFF_COUNTS_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(trackerMotion.kickOffCounts)));
        item.put(ON_DURATION_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(trackerMotion.onDurationInSeconds)));
        return item;
    }

    private String externalTrackerIdFromDDBItem(final Map<String, AttributeValue> item) {
        return item.get(TS_PILL_ID_ATTRIBUTE_NAME).getS().substring(DATE_TIME_STRING_TEMPLATE.length() + 1);
    }

    private DateTime timestampFromDDBItem(final Map<String, AttributeValue> item) {
        final String dateString = item.get(TS_PILL_ID_ATTRIBUTE_NAME).getS().substring(0, DATE_TIME_STRING_TEMPLATE.length());
        return DateTime.parse(dateString + ":00Z", DATE_TIME_READ_FORMATTER).withZone(DateTimeZone.UTC);
    }

    private TrackerMotion fromDynamoDBItem(final Map<String, AttributeValue> item) {
        return new TrackerMotion.Builder()
                .withAccountId(Long.parseLong(item.get(ACCOUNT_ID_ATTRIBUTE_NAME).getN()))
                .withExternalTrackerId(externalTrackerIdFromDDBItem(item))
                .withTimestampMillis(timestampFromDDBItem(item).getMillis())
                .withValue(Integer.parseInt(item.get(VALUE_ATTRIBUTE_NAME).getN()))
                .withOffsetMillis(Integer.parseInt(item.get(OFFSET_MILLIS_ATTRIBUTE_NAME).getN()))
                .withMotionRange(Long.parseLong(item.get(MOTION_RANGE_ATTRIBUTE_NAME).getN()))
                .withKickOffCounts(Long.parseLong(item.get(KICKOFF_COUNTS_ATTRIBUTE_NAME).getN()))
                .withOnDurationInSeconds(Long.parseLong(item.get(ON_DURATION_ATTRIBUTE_NAME).getN()))
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
            final String hashRangeKey = item.get(ACCOUNT_ID_ATTRIBUTE_NAME).getN() + item.get(TS_PILL_ID_ATTRIBUTE_NAME).getS();
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
     * Insert a list of TrackerMotion of size MAX_PUT_ITEMS
     * @param trackerMotionList
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
     * @param trackerMotionList
     * @return
     */
    public int batchInsertTrackerMotionData(final List<TrackerMotion> trackerMotionList, int batchSize) {
        final List<List<TrackerMotion>> dataList = Lists.partition(trackerMotionList, MAX_PUT_ITEMS);
        int numberInserted = 0;
        final Boolean retry = true;

        for (final List<TrackerMotion> trackerMotions : dataList) {
            try {
                final List<TrackerMotion> remaining = batchInsert(trackerMotions, retry);
                numberInserted += (trackerMotions.size() - remaining.size());
            } catch (AmazonClientException e) {
                LOGGER.error("Got exception while attempting to batchInsert to DynamoDB: {}", e);
            }
        }
        return numberInserted;
    }

    @Override
    public Class name() {
        return  PillDataDAODynamoDB.class;
    }

    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDB dynamoDBClient){
        final CreateTableRequest request = new CreateTableRequest().withTableName(tableName);

        request.withKeySchema(
                new KeySchemaElement().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(TS_PILL_ID_ATTRIBUTE_NAME).withKeyType(KeyType.RANGE)
        );

        request.withAttributeDefinitions(
                new AttributeDefinition().withAttributeName(ACCOUNT_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.N),
                new AttributeDefinition().withAttributeName(TS_PILL_ID_ATTRIBUTE_NAME).withAttributeType(ScalarAttributeType.S)
        );

        request.setProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L));

        final CreateTableResult result = dynamoDBClient.createTable(request);
        return result;
    }

}
