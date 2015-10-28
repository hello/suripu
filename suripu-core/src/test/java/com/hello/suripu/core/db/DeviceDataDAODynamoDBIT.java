package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.handlers.RequestHandler2;
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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
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
 * Created by jakepiccolo on 10/13/15.
 */
public class DeviceDataDAODynamoDBIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceDataDAODynamoDB.class);

    private BasicAWSCredentials awsCredentials;
    private SimulatedThrottlingDynamoDBClient amazonDynamoDBClient;
    private DeviceDataDAODynamoDB deviceDataDAODynamoDB;

    private static final String TABLE_PREFIX = "integration_test_device_data";
    private static final String OCTOBER_TABLE_NAME = TABLE_PREFIX + "_2015_10";
    private static final String NOVEMBER_TABLE_NAME = TABLE_PREFIX + "_2015_11";

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

        this.deviceDataDAODynamoDB = new DeviceDataDAODynamoDB(amazonDynamoDBClient, TABLE_PREFIX);

        try {
            LOGGER.debug("-------- Creating Table {} ---------", OCTOBER_TABLE_NAME);
            final String octTableName = deviceDataDAODynamoDB.getTableName(new DateTime(2015, 10, 1, 0, 0, DateTimeZone.UTC));
            final CreateTableResult octResult = deviceDataDAODynamoDB.createTable(octTableName);
            LOGGER.debug("Created dynamoDB table {}", octResult.getTableDescription());
            LOGGER.debug("-------- Creating Table {} ---------", NOVEMBER_TABLE_NAME);
            final String novTableName = deviceDataDAODynamoDB.getTableName(new DateTime(2015, 11, 1, 0, 0, DateTimeZone.UTC));
            final CreateTableResult novResult = deviceDataDAODynamoDB.createTable(novTableName);
            LOGGER.debug("Created dynamoDB table {}", novResult.getTableDescription());
        } catch (ResourceInUseException rie){
            LOGGER.warn("Problem creating table");
        }
    }

    @After
    public void tearDown() throws Exception {
        final List<String> tableNames = ImmutableList.of(OCTOBER_TABLE_NAME, NOVEMBER_TABLE_NAME);
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
    public void testBatchInsertAll() {
        final List<DeviceData> deviceDataList = new ArrayList<>();
        final List<Long> accountIds = new ImmutableList.Builder<Long>().add(new Long(1)).add(new Long(2)).build();
        final String deviceId = "100";
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0, DateTimeZone.UTC);
        final int numMinutes = 50;
        for (final Long accountId: accountIds) {
            for (int i = 0; i < numMinutes; i++) {
                deviceDataList.add(new DeviceData.Builder()
                        .withAccountId(accountId)
                        .withExternalDeviceId(deviceId)
                        .withDateTimeUTC(firstTime.plusMinutes(i))
                        .withOffsetMillis(0)
                        .build());
            }
        }

        final int initialItemsInserted = deviceDataDAODynamoDB.batchInsertAll(deviceDataList);
        assertThat(initialItemsInserted, is(deviceDataList.size()));
        assertThat(getTableCount(OCTOBER_TABLE_NAME), is(deviceDataList.size()));

        // Now insert the exact same thing again. Should work fine in DynamoDB.
        final int duplicateItemsInserted = deviceDataDAODynamoDB.batchInsertAll(deviceDataList);
        assertThat(duplicateItemsInserted, is(deviceDataList.size()));
        assertThat(getTableCount(OCTOBER_TABLE_NAME), is(deviceDataList.size()));
    }

    @Test
    public void testBatchInsertAllDuplicateKeys() {
        final DeviceData.Builder builder = new DeviceData.Builder()
                .withAccountId(new Long(0))
                .withExternalDeviceId("1")
                .withDateTimeUTC(new DateTime(2015, 10, 1, 8, 0, DateTimeZone.UTC))
                .withOffsetMillis(0);
        final List<DeviceData> deviceDataList = new ImmutableList.Builder<DeviceData>()
                .add(builder.build())
                .add(builder.build())
                .build();
        final int insertions = deviceDataDAODynamoDB.batchInsertAll(deviceDataList);
        assertThat(insertions, is(1));
        assertThat(getTableCount(OCTOBER_TABLE_NAME), is(1));
    }

    @Test
    public void testBatchInsertWithDuplicateKeys() {
        final DeviceData.Builder builder = new DeviceData.Builder()
                .withAccountId(new Long(0))
                .withExternalDeviceId("1")
                .withDateTimeUTC(new DateTime(2015, 10, 1, 8, 0, DateTimeZone.UTC))
                .withOffsetMillis(0);
        final List<DeviceData> deviceDataList = new ImmutableList.Builder<DeviceData>()
                .add(builder.build())
                .add(builder.build())
                .build();
        final int inserted = deviceDataDAODynamoDB.batchInsert(deviceDataList);
        assertThat(inserted, is(1));
        assertThat(getTableCount(OCTOBER_TABLE_NAME), is(1));
    }

    @Test
    public void testBatchInsertWithSimulatedThrottling() {
        final DeviceData.Builder builder = new DeviceData.Builder()
                .withAccountId(new Long(0))
                .withExternalDeviceId("1")
                .withDateTimeUTC(new DateTime(2015, 10, 1, 8, 0, DateTimeZone.UTC))
                .withOffsetMillis(0);
        final List<DeviceData> deviceDataList = new ImmutableList.Builder<DeviceData>()
                .add(builder.build())
                .build();
        this.amazonDynamoDBClient.throttle = true;
        final int inserted = deviceDataDAODynamoDB.batchInsert(deviceDataList);
        assertThat(inserted, is(1));
        assertThat(getTableCount(OCTOBER_TABLE_NAME), is(1));
    }

    @Test
    public void testBatchInsertMultipleMonths() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0, DateTimeZone.UTC);
        final int insertedThisMonth = addDataForQuerying(accountId, deviceId, offsetMillis, firstTime);

        final DateTime nextMonth = firstTime.plusMonths(1);
        final int insertedNextMonth = addDataForQuerying(accountId, deviceId, offsetMillis, nextMonth);

        assertThat(getTableCount(OCTOBER_TABLE_NAME), is(insertedThisMonth));
        assertThat(getTableCount(NOVEMBER_TABLE_NAME), is(insertedNextMonth));
    }

    private int addDataForQuerying(final Long accountId, final String deviceId, final Integer offsetMillis, final DateTime firstTime) {
        final List<DeviceData> deviceDataList = new ArrayList<>();
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime)
                .withAmbientTemperature(70)
                .withAmbientLight(36996)
                .withAmbientHumidity(100)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(1))
                .withAmbientTemperature(77)
                .withAmbientLight(36996)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(2))
                .withAmbientTemperature(69)
                .withAmbientLight(36996)
                .withAmbientHumidity(100)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(3))
                .withAmbientTemperature(90)
                .withAmbientLight(36996)
                .withAmbientHumidity(100)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(4))
                .withAmbientTemperature(70)
                .withAmbientLight(36996)
                .withAmbientHumidity(100)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(5))
                .withAmbientTemperature(20)
                .withAmbientLight(36996)
                .withAmbientHumidity(100)
                .build());
        // Skip minute 6
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(7))
                .withAmbientTemperature(100)
                .withAmbientLight(36996)
                .withAmbientHumidity(100)
                .build());
        deviceDataDAODynamoDB.batchInsert(deviceDataList);
        return deviceDataList.size();
    }

    @Test
    public void testGetBetweenByAbsoluteTimeAggregateBySlotDuration1Minute() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0, DateTimeZone.UTC);

        addDataForQuerying(accountId, deviceId, offsetMillis, firstTime);

        // From start to start+1, 2 results
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        accountId, deviceId, firstTime, firstTime.plusMinutes(1), 1).size(),
                is(2));
        // from start to start should be 1 result
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        accountId, deviceId, firstTime, firstTime, 1).size(),
                is(1));
        // Account ID unrecognized should be 0 results
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        accountId + 1000, deviceId, firstTime, firstTime.plusMinutes(1), 1).size(),
                is(0));
    }

    @Test
    public void testGetBetweenByAbsoluteTimeAggregateBySlotDurationAcrossMonths() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 0;
        // Very end of the month
        final DateTime firstTime = new DateTime(2015, 10, 31, 23, 58, DateTimeZone.UTC);

        addDataForQuerying(accountId, deviceId, offsetMillis, firstTime);

        // From start to start+1, 2 results
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        accountId, deviceId, firstTime, firstTime.plusMinutes(1), 1).size(),
                is(2));
        // from start to start should be 1 result
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        accountId, deviceId, firstTime, firstTime, 1).size(),
                is(1));
        // Account ID unrecognized should be 0 results
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        accountId + 1000, deviceId, firstTime, firstTime.plusMinutes(1), 1).size(),
                is(0));
    }

    @Test
    public void testGetBetweenByAbsoluteTimeAggregateBySlotDurationMultipleSenseIds() {
        final Long accountId = new Long(1);
        final String deviceId1 = "2";
        final String deviceId2 = "3";
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0, DateTimeZone.UTC);

        addDataForQuerying(accountId, deviceId1, offsetMillis, firstTime);
        addDataForQuerying(accountId, deviceId2, offsetMillis, firstTime);

        assertThat(getTableCount(OCTOBER_TABLE_NAME), is(14));
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        accountId, deviceId1, firstTime, firstTime.plusMinutes(10), 1).size(),
                is(7));
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        accountId, deviceId2, firstTime, firstTime.plusMinutes(10), 1).size(),
                is(7));
    }

    @Test
    public void testGetBetweenByAbsoluteTimeAggregateBySlotDuration5And60Minutes() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0, DateTimeZone.UTC);

        addDataForQuerying(accountId, deviceId, offsetMillis, firstTime);


        // 5-minute results starting at firstTime
        final List<DeviceData> fiveMinuteresults = deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                accountId, deviceId, firstTime, firstTime.plusMinutes(10), 5);
        assertThat(fiveMinuteresults.size(), is(2));
        assertThat(fiveMinuteresults.get(0).ambientTemperature, is(69));
        assertThat(fiveMinuteresults.get(0).dateTimeUTC, is(firstTime.plusMinutes(0)));
        assertThat(fiveMinuteresults.get(1).ambientTemperature, is(20));
        assertThat(fiveMinuteresults.get(1).dateTimeUTC, is(firstTime.plusMinutes(5)));

        // 5-minute results starting at a weird time (firstTime+3)
        final List<DeviceData> offsetFiveMinuteresults = deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                accountId, deviceId, firstTime.plusMinutes(3), firstTime.plusMinutes(10), 5);
        assertThat(offsetFiveMinuteresults.size(), is(2));
        assertThat(offsetFiveMinuteresults.get(0).ambientTemperature, is(70));
        assertThat(offsetFiveMinuteresults.get(0).dateTimeUTC, is(firstTime));
        assertThat(offsetFiveMinuteresults.get(1).ambientTemperature, is(20));
        assertThat(offsetFiveMinuteresults.get(1).dateTimeUTC, is(firstTime.plusMinutes(5)));


        // Aggregate by hour (60 minutes)
        final List<DeviceData> hourlyResults = deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                accountId, deviceId, firstTime, firstTime.plusMinutes(90), 60);
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
        final String deviceId = "2";
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0, DateTimeZone.UTC);

        addDataForQuerying(accountId, deviceId, offsetMillis, firstTime);

        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        accountId, deviceId, firstTime, firstTime.plusMinutes(1), 1).size(),
                is(2));
    }

    private int countSamplesWithFillValue(final List<Sample> samples, final int fillValue) {
        int count = 0;
        for (final Sample sample : samples) {
            if (fillValue == sample.value) {
                count++;
            }
        }
        return count;
    }

    @Test
    public void testGenerateTimeSeriesByUTCTime() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0, DateTimeZone.UTC);

        addDataForQuerying(accountId, deviceId, offsetMillis, firstTime);

        final Optional<Device.Color> colorOptional = Optional.absent();
        final Optional<Calibration> calibrationOptional = Optional.absent();

        final List<Sample> lightSamples = deviceDataDAODynamoDB.generateTimeSeriesByUTCTime(
                firstTime.getMillis(), firstTime.plusMinutes(10).getMillis(), accountId,
                deviceId, 1, "light", -1, colorOptional, calibrationOptional);
        assertThat(lightSamples.size(), is(11));
        assertThat(countSamplesWithFillValue(lightSamples, -1), is(4));

        final List<Sample> tempSamples = deviceDataDAODynamoDB.generateTimeSeriesByUTCTime(
                firstTime.getMillis(), firstTime.plusMinutes(10).getMillis(), accountId,
                deviceId, 1, "temperature", -1, colorOptional, calibrationOptional);
        assertThat(tempSamples.size(), is(11));
        assertThat(countSamplesWithFillValue(tempSamples, -1), is(4));

        final List<Sample> humiditySamples = deviceDataDAODynamoDB.generateTimeSeriesByUTCTime(
                firstTime.getMillis(), firstTime.plusMinutes(10).getMillis(), accountId,
                deviceId, 1, "humidity", -1, colorOptional, calibrationOptional);
        assertThat(humiditySamples.size(), is(11));
        assertThat(countSamplesWithFillValue(humiditySamples, -1), is(4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateTimeSeriesByUTCTimeInvalidSensor() {
        final Optional<Device.Color> colorOptional = Optional.absent();
        final Optional<Calibration> calibrationOptional = Optional.absent();
        deviceDataDAODynamoDB.generateTimeSeriesByUTCTime(new Long(1), new Long(1), new Long(1), "2", 1, "not_a_sensor", 0, colorOptional, calibrationOptional);
    }

}