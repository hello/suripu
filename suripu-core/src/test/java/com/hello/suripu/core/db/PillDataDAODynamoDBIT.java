package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by kingshy on 11/17/15.
 */
public class PillDataDAODynamoDBIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(PillDataDAODynamoDB.class);
    private BasicAWSCredentials awsCredentials;
    private SimulatedThrottlingDynamoDBClient amazonDynamoDBClient;
    private PillDataDAODynamoDB pillDataDAODynamoDB;

    private static final String TABLE_PREFIX = "test_pill_data";
    private static final String OCTOBER_TABLE_NAME = TABLE_PREFIX + "_2015_10";
    private static final String NOVEMBER_TABLE_NAME = TABLE_PREFIX + "_2015_11";
    private static final String DECEMBER_TABLE_NAME = TABLE_PREFIX + "_2015_12";


    private class SimulatedThrottlingDynamoDBClient extends AmazonDynamoDBClient {
        public SimulatedThrottlingDynamoDBClient(final BasicAWSCredentials awsCredentials, final ClientConfiguration clientConfiguration) {
            super(awsCredentials, clientConfiguration);
        }

        public int numTries = 0;
        public boolean throttle = false;

        @Override
        public BatchWriteItemResult batchWriteItem(BatchWriteItemRequest batchWriteItemRequest) {
            // Force caller to try this a couple of times.
            numTries++;
            if (throttle && (numTries < 3)) {
                LOGGER.info("Simulating throttling...");
                return new BatchWriteItemResult().withUnprocessedItems(batchWriteItemRequest.getRequestItems());
            }
            return super.batchWriteItem(batchWriteItemRequest);
        }
    }

    @Before
    public void setUp() throws Exception {
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new SimulatedThrottlingDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        tearDown();

        this.pillDataDAODynamoDB = new PillDataDAODynamoDB(amazonDynamoDBClient, TABLE_PREFIX);

        try {
            LOGGER.debug("-------- Creating Table {} ---------", OCTOBER_TABLE_NAME);
            final String octTableName = pillDataDAODynamoDB.getTableName(new DateTime(2015, 10, 1, 0, 0, DateTimeZone.UTC));
            final CreateTableResult octResult = pillDataDAODynamoDB.createTable(octTableName);
            LOGGER.debug("Created dynamoDB table {}", octResult.getTableDescription());

            LOGGER.debug("-------- Creating Table {} ---------", NOVEMBER_TABLE_NAME);
            final String novTableName = pillDataDAODynamoDB.getTableName(new DateTime(2015, 11, 1, 0, 0, DateTimeZone.UTC));
            final CreateTableResult novResult = pillDataDAODynamoDB.createTable(novTableName);
            LOGGER.debug("Created dynamoDB table {}", novResult.getTableDescription());

            LOGGER.debug("-------- Creating Table {} ---------", DECEMBER_TABLE_NAME);
            final String decTableName = pillDataDAODynamoDB.getTableName(new DateTime(2015, 12, 1, 0, 0, DateTimeZone.UTC));
            final CreateTableResult decResult = pillDataDAODynamoDB.createTable(decTableName);
            LOGGER.debug("Created dynamoDB table {}", decResult.getTableDescription());
        } catch (ResourceInUseException rie){
            LOGGER.warn("Problem creating table");
        }
    }

    @After
    public void tearDown() throws Exception {
        final List<String> tableNames = ImmutableList.of(OCTOBER_TABLE_NAME, NOVEMBER_TABLE_NAME, DECEMBER_TABLE_NAME);
        for (final String name: tableNames) {
            final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                    .withTableName(name);
            try {
                this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
            }catch (ResourceNotFoundException ex){
                LOGGER.warn("Can not delete non existing table {}", name);
            }
        }
    }

    private int getTableCount(final String tableName) {
        final ScanRequest scanRequest = new ScanRequest().withTableName(tableName);
        final ScanResult scanResult = amazonDynamoDBClient.scan(scanRequest);
        return scanResult.getCount();
    }

    @Test
    public void testBatchInsertTrackerMotionData() {
        final List<TrackerMotion> trackerMotionList = new ArrayList<>();
        final int dataSize = 80;
        final DateTime firstTime = new DateTime(2015, 11, 17, 7, 0, DateTimeZone.UTC);
        Long accountId = 1L;

        for (int i = 0; i < dataSize; i++) {
            trackerMotionList.add(
                    new TrackerMotion.Builder()
                            .withAccountId(accountId + i)
                            .withTimestampMillis(firstTime.plusMinutes(i).getMillis())
                            .withExternalTrackerId(String.format("ABCDEFG%d", i))
                            .withOffsetMillis(-28800000)
                            .withValue(900 + i)
                            .build()
            );
        }

        final int successfulInserts = pillDataDAODynamoDB.batchInsertTrackerMotionData(trackerMotionList, trackerMotionList.size());
        assertThat(successfulInserts, is(trackerMotionList.size()));
        assertThat(getTableCount(NOVEMBER_TABLE_NAME), is(trackerMotionList.size()));

        final int successfulDupeInserts = pillDataDAODynamoDB.batchInsertTrackerMotionData(trackerMotionList, trackerMotionList.size());
        assertThat(successfulDupeInserts, is(trackerMotionList.size()));
        assertThat(getTableCount(NOVEMBER_TABLE_NAME), is(trackerMotionList.size()));

        final Optional<TrackerMotion> results = pillDataDAODynamoDB.getSinglePillData(accountId, "ABCDEFG0", firstTime);
        assertThat(results.isPresent(), is(true));
        assertThat(results.get().value, is (trackerMotionList.get(0).value));
    }

    @Test
    public void crossMonthBatchInsert() {
        final List<TrackerMotion> trackerMotionList = new ArrayList<>();
        final int dataSize = 80;
        final DateTime firstTime = new DateTime(2015, 11, 30, 23, 59, DateTimeZone.UTC);
        Long accountId = 1L;
        for (int i = 0; i < dataSize; i++) {
            trackerMotionList.add(
                    new TrackerMotion.Builder()
                            .withAccountId(accountId + i)
                            .withTimestampMillis(firstTime.plusMinutes(i).getMillis())
                            .withExternalTrackerId(String.format("ABCDEFG%d", i))
                            .withOffsetMillis(-28800000)
                            .withValue(900 + i)
                            .build()
            );
        }

        final int successfulInserts = pillDataDAODynamoDB.batchInsertTrackerMotionData(trackerMotionList, trackerMotionList.size());
        assertThat(successfulInserts, is(trackerMotionList.size()));
        assertThat(getTableCount(NOVEMBER_TABLE_NAME), is(1));
        assertThat(getTableCount(DECEMBER_TABLE_NAME), is(trackerMotionList.size() - 1));

        final List<TrackerMotion> results = pillDataDAODynamoDB.getBetween(accountId + 1, firstTime.plusMinutes(1), firstTime.plusMinutes(2));
        assertThat(results.isEmpty(), is(false));
        assertThat(results.get(0).value, is (trackerMotionList.get(1).value));
    }

    @Test
    public void testGetBetween() {
        final List<TrackerMotion> trackerMotionList = new ArrayList<>();
        final int dataSize = 500;
        final Long accountId = 1L;
        final String externalPillId = "ABCDEFG";
        final DateTime firstTime = new DateTime(2015, 11, 1, 1, 0, DateTimeZone.UTC);
        for (int i = 0; i < dataSize; i++) {
            trackerMotionList.add(
                    new TrackerMotion.Builder()
                            .withAccountId(accountId)
                            .withTimestampMillis(firstTime.plusMinutes(i).getMillis())
                            .withExternalTrackerId(externalPillId)
                            .withOffsetMillis(-28800000)
                            .withValue(900 + i)
                            .build()
            );
        }

        final int successfulInserts = pillDataDAODynamoDB.batchInsertTrackerMotionData(trackerMotionList, trackerMotionList.size());
        assertThat(successfulInserts, is(trackerMotionList.size()));

        final int numMinutes = 100;
        final DateTime queryStartTimeUTC = firstTime.plusMinutes(10);
        final DateTime queryEndTimeUTC = firstTime.plusMinutes(10 + numMinutes);

        final List<TrackerMotion> results = pillDataDAODynamoDB.getBetween(accountId, queryStartTimeUTC, queryEndTimeUTC, externalPillId);
        assertThat(results.size(), is(numMinutes));
        assertThat(results.get(results.size() - 1).cosTheta.isPresent(), is(false));
        assertThat(results.get(results.size() - 1).motionMask.isPresent(), is(false));
    }

    @Test
    public void testGetBetweenWithOptionalFields() {
        final List<TrackerMotion> trackerMotionList = new ArrayList<>();
        final int dataSize = 500;
        final Long accountId = 1L;
        final String externalPillId = "ABCDEFG";
        final DateTime firstTime = new DateTime(2015, 11, 1, 1, 0, DateTimeZone.UTC);
        for (int i = 0; i < dataSize; i++) {
            trackerMotionList.add(
                    new TrackerMotion.Builder()
                            .withAccountId(accountId)
                            .withTimestampMillis(firstTime.plusMinutes(i).getMillis())
                            .withExternalTrackerId(externalPillId)
                            .withOffsetMillis(-28800000)
                            .withValue(900 + i)
                            .withCosTheta(new Long(i))
                            .withMotionMask(new Long(i + 1))
                            .build()
            );
        }

        final int successfulInserts = pillDataDAODynamoDB.batchInsertTrackerMotionData(trackerMotionList, trackerMotionList.size());
        assertThat(successfulInserts, is(trackerMotionList.size()));

        final int numMinutes = 100;
        final DateTime queryStartTimeUTC = firstTime.plusMinutes(10);
        final DateTime queryEndTimeUTC = firstTime.plusMinutes(10 + numMinutes);

        final List<TrackerMotion> results = pillDataDAODynamoDB.getBetween(accountId, queryStartTimeUTC, queryEndTimeUTC, externalPillId);
        assertThat(results.size(), is(numMinutes));
        assertThat(results.get(results.size() - 1).cosTheta.get(), is(new Long(numMinutes + 9)));
        assertThat(results.get(results.size() - 1).motionMask.get(), is(new Long(numMinutes + 10)));
    }

    @Test
    public void testGetBetweenLocalUTC() {
        final List<TrackerMotion> trackerMotionList = new ArrayList<>();
        final int dataSize = 500;
        final Long accountId = 1L;
        final int startValue = 900;
        final int offsetMillis = -28800000;
        final DateTime firstTime = new DateTime(2015, 11, 1, 1, 0, DateTimeZone.UTC);
        for (int i = 0; i < dataSize; i++) {
            trackerMotionList.add(
                    new TrackerMotion.Builder()
                            .withAccountId(accountId)
                            .withTimestampMillis(firstTime.plusMinutes(i).getMillis())
                            .withExternalTrackerId("ABCDEFG")
                            .withOffsetMillis(offsetMillis)
                            .withValue(startValue + i)
                            .build()
            );
        }

        final int successfulInserts = pillDataDAODynamoDB.batchInsertTrackerMotionData(trackerMotionList, trackerMotionList.size());
        assertThat(successfulInserts, is(trackerMotionList.size()));

        final int offset = 10;
        final int numMinutes = 50;
        final DateTime queryStartLocalUTC = firstTime.plusMinutes(offset).plusMillis(offsetMillis);
        final DateTime queryEndLocalUTC = queryStartLocalUTC.plusMinutes(numMinutes);

        final List<TrackerMotion> results = pillDataDAODynamoDB.getBetweenLocalUTC(accountId, queryStartLocalUTC, queryEndLocalUTC);
        assertThat(results.size(), is(numMinutes+1)); // inclusive, add one

        final int correctValue = startValue + offset;
        assertThat(results.get(0).value, is(correctValue));
    }

    @Test
    public void testGetDataCountBetweenLocalUTC() {
        final List<TrackerMotion> trackerMotionList = new ArrayList<>();
        final int dataSize = 500;
        final Long accountId = 1L;
        final int startValue = 900;
        final int offsetMillis = -28800000;
        final DateTime firstTime = new DateTime(2015, 11, 1, 1, 0, DateTimeZone.UTC);
        for (int i = 0; i < dataSize; i++) {
            trackerMotionList.add(
                    new TrackerMotion.Builder()
                            .withAccountId(accountId)
                            .withTimestampMillis(firstTime.plusMinutes(i).getMillis())
                            .withExternalTrackerId("ABCDEFG")
                            .withOffsetMillis(offsetMillis)
                            .withValue(startValue + i)
                            .build()
            );
        }

        final int successfulInserts = pillDataDAODynamoDB.batchInsertTrackerMotionData(trackerMotionList, trackerMotionList.size());
        assertThat(successfulInserts, is(trackerMotionList.size()));

        final int offset = 10;
        final int numMinutes = 75;
        final DateTime queryStartLocalUTC = firstTime.plusMinutes(offset).plusMillis(offsetMillis);
        final DateTime queryEndLocalUTC = queryStartLocalUTC.plusMinutes(numMinutes);

        final Integer dataCount = pillDataDAODynamoDB.getDataCountBetweenLocalUTC(accountId, queryStartLocalUTC, queryEndLocalUTC);
        assertThat(dataCount, is(numMinutes+1)); // inclusive, add one
    }

    @Test
    public void testGetMostRecent() {
        final Long accountId = 1L;
        final String id1 = "id1";
        final String id2 = "id2";
        final int offsetMillis = -28800000;
        final DateTime firstTime = new DateTime(2015, 11, 1, 1, 0, DateTimeZone.UTC);
        final List<TrackerMotion> trackerMotions = ImmutableList.of(
                new TrackerMotion.Builder()
                        .withAccountId(accountId)
                        .withExternalTrackerId(id1)
                        .withTimestampMillis(firstTime.getMillis())
                        .withOffsetMillis(offsetMillis)
                        .withValue(1)
                        .build(),
                new TrackerMotion.Builder()
                        .withAccountId(accountId)
                        .withExternalTrackerId(id2)
                        .withTimestampMillis(firstTime.plusMinutes(1).getMillis())
                        .withOffsetMillis(offsetMillis)
                        .withValue(2)
                        .build(),
                new TrackerMotion.Builder()
                        .withAccountId(accountId)
                        .withExternalTrackerId(id1)
                        .withTimestampMillis(firstTime.plusMinutes(2).getMillis())
                        .withOffsetMillis(offsetMillis)
                        .withValue(3)
                        .build(),
                new TrackerMotion.Builder()
                        .withAccountId(accountId)
                        .withExternalTrackerId(id2)
                        .withTimestampMillis(firstTime.plusMinutes(3).getMillis())
                        .withOffsetMillis(offsetMillis)
                        .withValue(4)
                        .build()
        );

        final int successfulInserts = pillDataDAODynamoDB.batchInsertTrackerMotionData(trackerMotions, trackerMotions.size());
        assertThat(successfulInserts, is(trackerMotions.size()));

        final TrackerMotion latestId2 = pillDataDAODynamoDB.getMostRecent(id2, accountId, firstTime.plusMinutes(5)).get();
        assertThat(latestId2.externalTrackerId, is(id2));
        assertThat(latestId2.dateTimeUTC(), is(firstTime.plusMinutes(3)));

        final TrackerMotion latestId1 = pillDataDAODynamoDB.getMostRecent(id1, accountId, firstTime.plusMinutes(5)).get();
        assertThat(latestId1.externalTrackerId, is(id1));
        assertThat(latestId1.dateTimeUTC(), is(firstTime.plusMinutes(2)));

        assertThat(pillDataDAODynamoDB.getMostRecent(id1, accountId, firstTime.minusMinutes(1)), is(Optional.<TrackerMotion>absent()));
    }

}
