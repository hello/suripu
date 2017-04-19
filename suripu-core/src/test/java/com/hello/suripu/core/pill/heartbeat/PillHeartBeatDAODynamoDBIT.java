package com.hello.suripu.core.pill.heartbeat;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PillHeartBeatDAODynamoDBIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceDataDAODynamoDB.class);

    private BasicAWSCredentials awsCredentials;
    private SimulatedThrottlingDynamoDBClient amazonDynamoDBClient;
    private PillHeartBeatDAODynamoDB pillHeartBeatDAODynamoDB;

    private static final String TABLE_NAME = "integration_test_pill_heartbeat";

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

        try {
            LOGGER.debug("-------- Creating Table {} ---------", TABLE_NAME);
            final CreateTableResult result = PillHeartBeatDAODynamoDB.createTable(TABLE_NAME, amazonDynamoDBClient);
            LOGGER.debug("Created dynamoDB table {}", result.getTableDescription());
        } catch (ResourceInUseException rie){
            LOGGER.warn("Problem creating table");
        }
        this.pillHeartBeatDAODynamoDB = PillHeartBeatDAODynamoDB.create(amazonDynamoDBClient, TABLE_NAME);
    }

    @After
    public void tearDown() throws Exception {
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(TABLE_NAME);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            LOGGER.warn("Can not delete non existing table");
        }
    }


    @Test
    public void testQuerySingleHeartBeatPresent() {
        final String pillId = "ABC";
        pillHeartBeatDAODynamoDB.put(PillHeartBeat.create(pillId, 0, 0, 0, new DateTime(2015,1,1,0,0,0, DateTimeZone.UTC)));

        final Optional<PillHeartBeat> pillHeartBeats = pillHeartBeatDAODynamoDB.get(pillId);
        assertThat(pillHeartBeats.isPresent(), is(true));
    }

    @Test
    public void testQuerySingleHeartBeatAbsent() {
        final String pillId = "ABC";
        final Optional<PillHeartBeat> pillHeartBeats = pillHeartBeatDAODynamoDB.get(pillId);
        assertThat(pillHeartBeats.isPresent(), is(false));
    }

    @Test
    public void testQueryAlmostTooOld() {
        final String pillId = "ABC";
        final DateTime now = DateTime.now().withTimeAtStartOfDay();
        final DateTime then = now.minusDays(7).plusMinutes(1);
        pillHeartBeatDAODynamoDB.put(PillHeartBeat.create(pillId, 0, 0, 0, then));

        final List<PillHeartBeat> pillHeartBeats = pillHeartBeatDAODynamoDB.get(pillId, now);
        assertThat(pillHeartBeats.isEmpty(), is(false));
    }

    @Test
    public void testQueryOneTooOld() {
        final String pillId = "ABC";
        final DateTime now = DateTime.now().withTimeAtStartOfDay();
        final PillHeartBeat heartBeat1 = PillHeartBeat.create(pillId, 0, 0, 0, now.minusHours(1));
        final PillHeartBeat heartBeat2 = PillHeartBeat.create(pillId, 0, 0, 0, now.minusDays(7).minusHours(1));
        Set<PillHeartBeat> pillHeartBeatSet = ImmutableSet.of(heartBeat1, heartBeat2);
        pillHeartBeatDAODynamoDB.put(pillHeartBeatSet);

        final List<PillHeartBeat> pillHeartBeats = pillHeartBeatDAODynamoDB.get(pillId, now);
        assertThat(pillHeartBeats.isEmpty(), is(false));
    }

    @Test
    public void testQuery() {
        final String pillId = "ABC";
        final DateTime now = DateTime.now().withTimeAtStartOfDay();
        final PillHeartBeat heartBeat1 = PillHeartBeat.create(pillId, 0, 0, 0, now.minusHours(1));
        final PillHeartBeat heartBeat2 = PillHeartBeat.create(pillId, 0, 0, 0, now.minusHours(2));
        Set<PillHeartBeat> pillHeartBeatSet = ImmutableSet.of(heartBeat1, heartBeat2);
        pillHeartBeatDAODynamoDB.put(pillHeartBeatSet);

        final List<PillHeartBeat> pillHeartBeats = pillHeartBeatDAODynamoDB.get(pillId, now);
        assertThat(pillHeartBeats.size(), is(pillHeartBeatSet.size()));
    }

    @Test
    public void testInsertDuplicates() {
        final String pillId = "ABC";
        final DateTime now = DateTime.now().withTimeAtStartOfDay();
        final PillHeartBeat heartBeat1 = PillHeartBeat.create(pillId, 0, 0, 0, now.minusHours(1));
        final PillHeartBeat heartBeat2 = PillHeartBeat.create(pillId, 10, 0, 0, now.minusHours(1));
        Set<PillHeartBeat> pillHeartBeatSet = ImmutableSet.of(heartBeat1, heartBeat2);
        pillHeartBeatDAODynamoDB.put(pillHeartBeatSet);

        final List<PillHeartBeat> pillHeartBeats = pillHeartBeatDAODynamoDB.get(pillId, now);
        assertThat(pillHeartBeats.size(), is(1));
    }

    @Test
    public void testInsertOutOfRange() {
        final String pillId = "ABC";
        final DateTime now = DateTime.now().withTimeAtStartOfDay();
        final PillHeartBeat heartBeat1 = PillHeartBeat.create(pillId, 0, 0, 0, now.minusHours(1));

        // simulates having most recent item in DB outside the query range
        final PillHeartBeat heartBeat2 = PillHeartBeat.create(pillId, 100, 0, 0, now.plusHours(1));
        Set<PillHeartBeat> pillHeartBeatSet = ImmutableSet.of(heartBeat1, heartBeat2);
        pillHeartBeatDAODynamoDB.put(pillHeartBeatSet);

        final List<PillHeartBeat> pillHeartBeats = pillHeartBeatDAODynamoDB.get(pillId, now);
        assertThat(pillHeartBeats.size(), is(1));
        assertThat(pillHeartBeats.get(0).batteryLevel, is(heartBeat1.batteryLevel));
    }

    @Test
    public void testThrottledWrites() {
        final String pillId = "ABC123";
        final DateTime now = DateTime.now().withTimeAtStartOfDay();
        final Set<PillHeartBeat> heartBeats = Sets.newHashSet();
        for (int i = 0; i < 100; i++ ) {
            heartBeats.add(PillHeartBeat.create(pillId, 100 - i, 0, 0, now.plusMinutes(i)));
        }
        this.amazonDynamoDBClient.throttle = true;
        final Set<PillHeartBeat> unprocessed = pillHeartBeatDAODynamoDB.put(heartBeats);
        assertThat(unprocessed.isEmpty(), is(false));
    }
}
