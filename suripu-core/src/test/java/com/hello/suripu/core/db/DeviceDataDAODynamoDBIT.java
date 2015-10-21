package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.DeviceData;
import org.joda.time.DateTime;
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
 * Created by jakepiccolo on 10/13/15.
 */
public class DeviceDataDAODynamoDBIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceDataDAODynamoDB.class);

    private BasicAWSCredentials awsCredentials;
    private SimulatedThrottlingDynamoDBClient amazonDynamoDBClient;
    private DeviceDataDAODynamoDB deviceDataDAODynamoDB;

    private static final String TABLE_NAME = "integration_test_device_data";

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
            final CreateTableResult result = DeviceDataDAODynamoDB.createTable(TABLE_NAME, amazonDynamoDBClient);
            LOGGER.debug("Created dynamoDB table {}", result.getTableDescription());
        } catch (ResourceInUseException rie){
            LOGGER.warn("Problem creating table");
        }
        this.deviceDataDAODynamoDB = new DeviceDataDAODynamoDB(amazonDynamoDBClient, TABLE_NAME);
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

    private int getTableCount() {
        final ScanRequest scanRequest = new ScanRequest().withTableName(TABLE_NAME);
        final ScanResult scanResult = amazonDynamoDBClient.scan(scanRequest);
        return scanResult.getCount();
    }

    @Test
    public void testBatchInsertAll() {
        final List<DeviceData> deviceDataList = new ArrayList<>();
        final List<Long> accountIds = new ImmutableList.Builder<Long>().add(new Long(1)).add(new Long(2)).build();
        final Long deviceId = new Long(100);
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0);
        final int numMinutes = 50;
        for (final Long accountId: accountIds) {
            for (int i = 0; i < numMinutes; i++) {
                deviceDataList.add(new DeviceData.Builder()
                        .withAccountId(accountId)
                        .withDeviceId(deviceId)
                        .withDateTimeUTC(firstTime.plusMinutes(i))
                        .withOffsetMillis(0)
                        .build());
            }
        }

        final int initialItemsInserted = deviceDataDAODynamoDB.batchInsertAll(deviceDataList);
        assertThat(initialItemsInserted, is(deviceDataList.size()));
        assertThat(getTableCount(), is(deviceDataList.size()));

        // Now insert the exact same thing again. Should work fine in DynamoDB.
        final int duplicateItemsInserted = deviceDataDAODynamoDB.batchInsertAll(deviceDataList);
        assertThat(duplicateItemsInserted, is(deviceDataList.size()));
        assertThat(getTableCount(), is(deviceDataList.size()));
    }

    @Test
    public void testBatchInsertAllDuplicateKeys() {
        final DeviceData.Builder builder = new DeviceData.Builder()
                .withAccountId(new Long(0))
                .withDeviceId(new Long(0))
                .withDateTimeUTC(new DateTime(2015, 10, 1, 8, 0))
                .withOffsetMillis(0);
        final List<DeviceData> deviceDataList = new ImmutableList.Builder<DeviceData>()
                .add(builder.build())
                .add(builder.build())
                .build();
        final int insertions = deviceDataDAODynamoDB.batchInsertAll(deviceDataList);
        assertThat(insertions, is(1));
        assertThat(getTableCount(), is(1));
    }

    @Test
    public void testBatchInsertWithDuplicateKeys() {
        final DeviceData.Builder builder = new DeviceData.Builder()
                .withAccountId(new Long(0))
                .withDeviceId(new Long(0))
                .withDateTimeUTC(new DateTime(2015, 10, 1, 8, 0))
                .withOffsetMillis(0);
        final List<DeviceData> deviceDataList = new ImmutableList.Builder<DeviceData>()
                .add(builder.build())
                .add(builder.build())
                .build();
        final int inserted = deviceDataDAODynamoDB.batchInsert(deviceDataList);
        assertThat(inserted, is(1));
        assertThat(getTableCount(), is(1));
    }

    @Test
    public void testBatchInsertWithSimulatedThrottling() {
        final DeviceData.Builder builder = new DeviceData.Builder()
                .withAccountId(new Long(0))
                .withDeviceId(new Long(0))
                .withDateTimeUTC(new DateTime(2015, 10, 1, 8, 0))
                .withOffsetMillis(0);
        final List<DeviceData> deviceDataList = new ImmutableList.Builder<DeviceData>()
                .add(builder.build())
                .build();
        this.amazonDynamoDBClient.throttle = true;
        final int inserted = deviceDataDAODynamoDB.batchInsert(deviceDataList);
        assertThat(inserted, is(1));
        assertThat(getTableCount(), is(1));
    }

    private void addDataForQuerying(final Long accountId, final Long deviceId, final Integer offsetMillis, final DateTime firstTime) {
        final List<DeviceData> deviceDataList = new ArrayList<>();
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime)
                .withAmbientTemperature(70)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(1))
                .withAmbientTemperature(77)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(2))
                .withAmbientTemperature(69)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(3))
                .withAmbientTemperature(90)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(4))
                .withAmbientTemperature(70)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(5))
                .withAmbientTemperature(20)
                .build());
        // Skip minute 6
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(7))
                .withAmbientTemperature(100)
                .build());
        deviceDataDAODynamoDB.batchInsert(deviceDataList);
    }

    @Test
    public void testGetBetweenByAbsoluteTimeAggregateBySlotDuration1Minute() {
        final Long accountId = new Long(1);
        final Long deviceId = new Long(1);
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0);

        addDataForQuerying(accountId, deviceId, offsetMillis, firstTime);

        // From start to start+1, 2 results
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        deviceId, accountId, firstTime, firstTime.plusMinutes(1), 1).size(),
                is(2));
        // from start to start should be 1 result
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        deviceId, accountId, firstTime, firstTime, 1).size(),
                is(1));
        // Account ID unrecognized should be 0 results
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        deviceId, accountId + 1000, firstTime, firstTime.plusMinutes(1), 1).size(),
                is(0));
    }

    @Test
    public void testGetBetweenByAbsoluteTimeAggregateBySlotDuration5And60Minutes() {
        final Long accountId = new Long(1);
        final Long deviceId = new Long(1);
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0);

        addDataForQuerying(accountId, deviceId, offsetMillis, firstTime);


        // 5-minute results starting at firstTime
        final List<DeviceData> fiveMinuteresults = deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                deviceId, accountId, firstTime, firstTime.plusMinutes(10), 5);
        assertThat(fiveMinuteresults.size(), is(2));
        assertThat(fiveMinuteresults.get(0).ambientTemperature, is(69));
        assertThat(fiveMinuteresults.get(0).dateTimeUTC, is(firstTime.plusMinutes(0)));
        assertThat(fiveMinuteresults.get(1).ambientTemperature, is(20));
        assertThat(fiveMinuteresults.get(1).dateTimeUTC, is(firstTime.plusMinutes(5)));

        // 5-minute results starting at a weird time (firstTime+3)
        final List<DeviceData> offsetFiveMinuteresults = deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                deviceId, accountId, firstTime.plusMinutes(3), firstTime.plusMinutes(10), 5);
        assertThat(offsetFiveMinuteresults.size(), is(2));
        assertThat(offsetFiveMinuteresults.get(0).ambientTemperature, is(70));
        assertThat(offsetFiveMinuteresults.get(0).dateTimeUTC, is(firstTime));
        assertThat(offsetFiveMinuteresults.get(1).ambientTemperature, is(20));
        assertThat(offsetFiveMinuteresults.get(1).dateTimeUTC, is(firstTime.plusMinutes(5)));


        // Aggregate by hour (60 minutes)
        final List<DeviceData> hourlyResults = deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                deviceId, accountId, firstTime, firstTime.plusMinutes(90), 60);
        assertThat(hourlyResults.size(), is(1));
        assertThat(hourlyResults.get(0).ambientTemperature, is(20));
        assertThat(hourlyResults.get(0).dateTimeUTC, is(firstTime));
    }

    @Test
    public void testGetBetweenByAbsoluteTimeAggregateBySlotDurationThroughputExceeded() {
        amazonDynamoDBClient.addRequestHandler(new RequestHandler2() {
            int numTries = 0;

            @Override
            public void beforeRequest(Request<?> request) {
                if (request.getOriginalRequest() instanceof QueryRequest &&  numTries < 2) {
                    numTries++;
                    LOGGER.info("Injecting ProvisionedThroughputExceededException");
                    throw new ProvisionedThroughputExceededException("Injected Error");
                }
                LOGGER.info("nailed it");
            }

            @Override
            public void afterResponse(Request<?> request, Response<?> response) {

            }

            @Override
            public void afterError(Request<?> request, Response<?> response, Exception e) {

            }
        });

        final Long accountId = new Long(1);
        final Long deviceId = new Long(1);
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0);

        addDataForQuerying(accountId, deviceId, offsetMillis, firstTime);

        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        deviceId, accountId, firstTime, firstTime.plusMinutes(1), 1).size(),
                is(2));
    }

}