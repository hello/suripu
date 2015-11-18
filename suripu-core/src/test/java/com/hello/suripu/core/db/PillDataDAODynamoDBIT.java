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
        final List<String> tableNames = ImmutableList.of(NOVEMBER_TABLE_NAME, DECEMBER_TABLE_NAME);
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

        final List<TrackerMotion> results = pillDataDAODynamoDB.getSinglePillData(accountId, "ABCDEFG0", firstTime);
        assertThat(results.isEmpty(), is(false));
        assertThat(results.get(0).value, is (trackerMotionList.get(0).value));
    }
}
