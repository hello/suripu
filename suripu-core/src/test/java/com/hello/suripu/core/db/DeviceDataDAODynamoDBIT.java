package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        final List<DeviceData> insertedThisMonth = addDataForQuerying(accountId, deviceId, offsetMillis, firstTime);

        final DateTime nextMonth = firstTime.plusMonths(1);
        final List<DeviceData> insertedNextMonth = addDataForQuerying(accountId, deviceId, offsetMillis, nextMonth);

        assertThat(getTableCount(OCTOBER_TABLE_NAME), is(insertedThisMonth.size()));
        assertThat(getTableCount(NOVEMBER_TABLE_NAME), is(insertedNextMonth.size()));
    }

    private List<DeviceData> addDataForQuerying(final Long accountId, final String deviceId, final Integer offsetMillis, final DateTime firstTime) {
        final List<DeviceData> deviceDataList = new ArrayList<>();
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime)
                .withAmbientTemperature(2499)
                .withAmbientLight(10)
                .withAmbientLightVariance(100)
                .withAmbientHumidity(4662)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(1))
                .withAmbientTemperature(2498)
                .withAmbientLight(10)
                .withAmbientLightVariance(10)
                .withAmbientHumidity(4666)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(2))
                .withAmbientTemperature(2500)
                .withAmbientLight(8)
                .withAmbientLightVariance(8)
                .withAmbientHumidity(4665)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(3))
                .withAmbientTemperature(2500)
                .withAmbientLight(12)
                .withAmbientLightVariance(12)
                .withAmbientHumidity(4662)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(4))
                .withAmbientTemperature(2501)
                .withAmbientLight(9)
                .withAmbientLightVariance(9)
                .withAmbientHumidity(4667)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(5))
                .withAmbientTemperature(2502)
                .withAmbientLight(10)
                .withAmbientLightVariance(10)
                .withAmbientHumidity(4669)
                .build());
        // Skip minute 6
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withDateTimeUTC(firstTime.plusMinutes(7))
                .withAmbientTemperature(2503)
                .withAmbientLight(15)
                .withAmbientLightVariance(15)
                .withAmbientHumidity(4658)
                .build());
        deviceDataDAODynamoDB.batchInsert(deviceDataList);
        return deviceDataList;
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
    public void testGetBetweenByAbsoluteTimeAggregateBySlotDurationDifferentOffsetMillis() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer firstBatchOffsetMillis = 0;
        final Integer secondBatchOffsetMillis = 1000 * 60 * 60; // 1 hour
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0, DateTimeZone.UTC);

        final List<DeviceData> firstBatch = addDataForQuerying(accountId, deviceId, firstBatchOffsetMillis, firstTime);
        final DateTime secondBatchFirstTime = last(firstBatch).dateTimeUTC.plusMinutes(1);
        final List<DeviceData> secondBatch = addDataForQuerying(accountId, deviceId, secondBatchOffsetMillis, secondBatchFirstTime);

        // Aggregate to 5 minutes, should get 3 batches
        final List<DeviceData> results = deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                accountId, deviceId, firstTime, firstTime.plusMinutes(14), 5);

        assertThat(results.size(), is(3));
        assertThat(results.get(0).offsetMillis, is(firstBatchOffsetMillis));
        assertThat(results.get(1).offsetMillis, is(firstBatchOffsetMillis));
        assertThat(results.get(2).offsetMillis, is(secondBatchOffsetMillis));
    }

    @Test
    public void testGetBetweenByAbsoluteTimeAggregateBySlotDurationNoShard() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        // No table created for this date
        final DateTime firstTime = new DateTime(2015, 9, 1, 7, 0, DateTimeZone.UTC);

        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        accountId, deviceId, firstTime, firstTime.plusMinutes(1), 1).size(),
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
        assertThat(fiveMinuteresults.get(0).ambientTemperature, is(2498));
        assertThat(fiveMinuteresults.get(0).dateTimeUTC, is(firstTime.plusMinutes(0)));
        assertThat(fiveMinuteresults.get(0).ambientLightVariance, is(28));
        assertThat(fiveMinuteresults.get(1).ambientTemperature, is(2502));
        assertThat(fiveMinuteresults.get(1).dateTimeUTC, is(firstTime.plusMinutes(5)));

        // 5-minute results starting at a weird time (firstTime+3)
        final List<DeviceData> offsetFiveMinuteresults = deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                accountId, deviceId, firstTime.plusMinutes(3), firstTime.plusMinutes(10), 5);
        assertThat(offsetFiveMinuteresults.size(), is(2));
        assertThat(offsetFiveMinuteresults.get(0).ambientTemperature, is(2500));
        assertThat(offsetFiveMinuteresults.get(0).dateTimeUTC, is(firstTime));
        assertThat(offsetFiveMinuteresults.get(1).ambientTemperature, is(2502));
        assertThat(offsetFiveMinuteresults.get(1).dateTimeUTC, is(firstTime.plusMinutes(5)));


        // Aggregate by hour (60 minutes)
        final List<DeviceData> hourlyResults = deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                accountId, deviceId, firstTime, firstTime.plusMinutes(90), 60);
        assertThat(hourlyResults.size(), is(1));
        assertThat(hourlyResults.get(0).ambientTemperature, is(2498));
        assertThat(hourlyResults.get(0).dateTimeUTC, is(firstTime));
    }

    @Test
    public void testGetBetweenByAbsoluteTimeAggregateBySlotDurationThroughputExceeded() {
        amazonDynamoDBClient.addRequestHandler(new RequestHandler2() {
            int numTries = 0;

            @Override
            public void beforeRequest(Request<?> request) {
                if (request.getOriginalRequest() instanceof QueryRequest && numTries < 2) {
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
        final Optional<Calibration> calibrationOptional = Optional.of(Calibration.createDefault(deviceId));

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

        final List<Sample> soundSamples = deviceDataDAODynamoDB.generateTimeSeriesByUTCTime(
                firstTime.getMillis(), firstTime.plusMinutes(10).getMillis(), accountId,
                deviceId, 1, "sound", -1, colorOptional, calibrationOptional);
        assertThat(soundSamples.size(), is(11));
        assertThat(countSamplesWithFillValue(soundSamples, -1), is(4));

        final List<Sample> particulateSamples = deviceDataDAODynamoDB.generateTimeSeriesByUTCTime(
                firstTime.getMillis(), firstTime.plusMinutes(10).getMillis(), accountId,
                deviceId, 1, "particulates", -1, colorOptional, calibrationOptional);
        assertThat(particulateSamples.size(), is(11));
        assertThat(countSamplesWithFillValue(particulateSamples, -1), is(4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateTimeSeriesByUTCTimeInvalidSensor() {
        final Optional<Device.Color> colorOptional = Optional.absent();
        final Optional<Calibration> calibrationOptional = Optional.absent();
        deviceDataDAODynamoDB.generateTimeSeriesByUTCTime(new Long(1), new Long(1), new Long(1), "2", 1, "not_a_sensor", 0, colorOptional, calibrationOptional);
    }

    @Test
    public void testGenerateTimeSeriesByUTCTimeAllSensors() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0, DateTimeZone.UTC);

        addDataForQuerying(accountId, deviceId, offsetMillis, firstTime);

        final Optional<Device.Color> colorOptional = Optional.absent();
        final Optional<Calibration> calibrationOptional = Optional.absent();

        final AllSensorSampleList sampleList = deviceDataDAODynamoDB.generateTimeSeriesByUTCTimeAllSensors(
                firstTime.getMillis(), firstTime.plusMinutes(10).getMillis(), accountId,
                deviceId, 1, -1, colorOptional, calibrationOptional);
        assertThat(sampleList.get(Sensor.LIGHT).size(), is(11));
        assertThat(sampleList.get(Sensor.TEMPERATURE).size(), is(11));
        assertThat(sampleList.get(Sensor.HUMIDITY).size(), is(11));
    }

    @Test
    public void testGetMostRecentByAccountId() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0, DateTimeZone.UTC);

        final List<DeviceData> inserted = addDataForQuerying(accountId, deviceId, offsetMillis, firstTime);
        final DeviceData mostRecent = deviceDataDAODynamoDB.getMostRecent(accountId, firstTime).get();
        assertThat(mostRecent.dateTimeUTC, is(inserted.get(inserted.size() - 1).dateTimeUTC));
    }

    @Test
    public void testGetMostRecentByAccountIdNotPresent() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0, DateTimeZone.UTC);

        addDataForQuerying(accountId, deviceId, offsetMillis, firstTime);

        assertThat(deviceDataDAODynamoDB.getMostRecent(accountId, firstTime.plusMonths(1)).isPresent(), is(false));
        assertThat(deviceDataDAODynamoDB.getMostRecent(2L, firstTime).isPresent(), is(false));
    }

    private DeviceData last(final List<DeviceData> list) {
        return list.get(list.size() - 1);
    }

    @Test
    public void testGetMostRecentByAccountAndDeviceId() {
        final Long accountId = new Long(1);
        final String deviceId1 = "2";
        final String deviceId2 = "3";
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0, DateTimeZone.UTC);

        final List<DeviceData> firstDeviceFirstMonthBatch = addDataForQuerying(accountId, deviceId1, offsetMillis, firstTime);
        final List<DeviceData> secondDeviceFirstMonthBatch = addDataForQuerying(accountId, deviceId2, offsetMillis, firstTime);
        final List<DeviceData> firstDeviceSecondMonthBatch = addDataForQuerying(accountId, deviceId1, offsetMillis, firstTime.plusMonths(1));

        final Optional<DeviceData> firstDeviceFirstMonth = deviceDataDAODynamoDB.getMostRecent(accountId, deviceId1, firstTime.plusMinutes(10), firstTime);
        assertThat(
                firstDeviceFirstMonth.get().dateTimeUTC,
                is(last(firstDeviceFirstMonthBatch).dateTimeUTC));
        assertThat(
                firstDeviceFirstMonth.get().externalDeviceId,
                is(last(firstDeviceFirstMonthBatch).externalDeviceId));

        final Optional<DeviceData> secondDeviceFirstMonth = deviceDataDAODynamoDB.getMostRecent(accountId, deviceId2, firstTime.plusMinutes(10), firstTime);
        assertThat(
                secondDeviceFirstMonth.get().dateTimeUTC,
                is(last(secondDeviceFirstMonthBatch).dateTimeUTC));
        assertThat(
                secondDeviceFirstMonth.get().externalDeviceId,
                is(last(secondDeviceFirstMonthBatch).externalDeviceId));

        final Optional<DeviceData> firstDeviceSecondMonth = deviceDataDAODynamoDB.getMostRecent(accountId, deviceId1, firstTime.plusMonths(1).plusMinutes(10), firstTime);
        assertThat(
                firstDeviceSecondMonth.get().dateTimeUTC,
                is(last(firstDeviceSecondMonthBatch).dateTimeUTC));
        assertThat(
                firstDeviceSecondMonth.get().externalDeviceId,
                is(last(firstDeviceSecondMonthBatch).externalDeviceId));

        final Optional<DeviceData> secondDeviceFirstMonthStartingFromNextMonth = deviceDataDAODynamoDB.getMostRecent(accountId, deviceId2, firstTime.plusMonths(1).plusMinutes(10), firstTime);
        assertThat(
                secondDeviceFirstMonthStartingFromNextMonth.get().dateTimeUTC,
                is(last(secondDeviceFirstMonthBatch).dateTimeUTC));
        assertThat(
                secondDeviceFirstMonthStartingFromNextMonth.get().externalDeviceId,
                is(last(secondDeviceFirstMonthBatch).externalDeviceId));

        assertThat(
                deviceDataDAODynamoDB.getMostRecent(accountId, deviceId2, firstTime.plusMonths(1).plusMinutes(10), firstTime.plusMonths(1)).isPresent(),
                is(false));
    }

    @Test
    public void testGetLightByBetweenHourDateByTS() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 1000 * 60 * 60 * 8; // 8 hours
        final DateTime firstUTCTime = new DateTime(2015, 10, 1, 10, 0, DateTimeZone.UTC).minusMillis(offsetMillis);
        final DateTime minLocalTime = new DateTime(2015, 10, 1, 10, 1, DateTimeZone.forOffsetMillis(offsetMillis));
        final DateTime maxLocalTime = new DateTime(2015, 10, 1, 15, 0, DateTimeZone.forOffsetMillis(offsetMillis));
        final int minAmbientLight = 100;

        final DeviceData.Builder builder = new DeviceData.Builder()
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withAccountId(accountId);

        final List<DeviceData> data = Lists.newArrayList();
        // NO: Enough ambient light, date time too early
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(0))
                .withAmbientLight(minAmbientLight + 2).build());
        // YES: Enough ambient light, good time
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(1))
                .withAmbientLight(minAmbientLight + 3).build());
        // YES
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(2))
                .withAmbientLight(minAmbientLight + 3).build());
        // YES
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(4))
                .withAmbientLight(minAmbientLight + 3).build());
        // YES
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(8))
                .withAmbientLight(minAmbientLight + 3).build());
        // NO, ambient light too low
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(3))
                .withAmbientLight(minAmbientLight).build());
        // NO, wrong hour
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusHours(2))
                .withAmbientLight(minAmbientLight + 3).build());
        // NO, bad ID
        data.add(builder
                .withExternalDeviceId("badId")
                .withDateTimeUTC(firstUTCTime.plusMinutes(7))
                .withAmbientLight(minAmbientLight + 3).build());

        deviceDataDAODynamoDB.batchInsert(data);

        final List<DeviceData> results = deviceDataDAODynamoDB.getLightByBetweenHourDateByTS(accountId, DeviceId.create(deviceId), minAmbientLight, firstUTCTime, firstUTCTime.plusMinutes(200), minLocalTime, maxLocalTime, 20, 11);
        LOGGER.debug("results: {}", results);
        for (final DeviceData result : results) {
            LOGGER.debug("ambient light: {}, device id: {}, datetime: {}, localtime: {}", result.ambientLight, result.externalDeviceId, result.dateTimeUTC, result.localTime());
        }
        assertThat(results.size(), is(4));
        assertThat(results.get(0).dateTimeUTC, is(firstUTCTime.plusMinutes(1)));
        assertThat(results.get(1).dateTimeUTC, is(firstUTCTime.plusMinutes(2)));
        assertThat(results.get(2).dateTimeUTC, is(firstUTCTime.plusMinutes(4)));
        assertThat(results.get(3).dateTimeUTC, is(firstUTCTime.plusMinutes(8)));
    }

    @Test
    public void testGetBetweenLocalTime() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 1000 * 60 * 60 * 8; // 8 hours
        final DateTime firstUTCTime = new DateTime(2015, 10, 1, 10, 0, DateTimeZone.UTC).minusMillis(offsetMillis);
        final DateTime minLocalTime = new DateTime(2015, 10, 1, 10, 1, DateTimeZone.forOffsetMillis(offsetMillis));
        final DateTime maxLocalTime = new DateTime(2015, 10, 1, 15, 0, DateTimeZone.forOffsetMillis(offsetMillis));

        final DeviceData.Builder builder = new DeviceData.Builder()
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withAccountId(accountId);

        final List<DeviceData> data = Lists.newArrayList();
        // NO: date time too early
        data.add(builder.withDateTimeUTC(firstUTCTime.plusMinutes(0)).build());
        // YES
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(1)).build());
        // YES
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(8)).build());
        // NO, bad ID
        data.add(builder
                .withExternalDeviceId("badId")
                .withDateTimeUTC(firstUTCTime.plusMinutes(7)).build());

        deviceDataDAODynamoDB.batchInsert(data);

        final List<DeviceData> results = deviceDataDAODynamoDB.getBetweenLocalTime(accountId, DeviceId.create(deviceId), firstUTCTime, firstUTCTime.plusMinutes(200), minLocalTime, maxLocalTime, deviceDataDAODynamoDB.ALL_ATTRIBUTES);
        LOGGER.debug("results: {}", results);
        for (final DeviceData result : results) {
            LOGGER.debug("ambient light: {}, device id: {}, datetime: {}, localtime: {}", result.ambientLight, result.externalDeviceId, result.dateTimeUTC, result.localTime());
        }
        assertThat(results.size(), is(2));
        assertThat(results.get(0).dateTimeUTC, is(firstUTCTime.plusMinutes(1)));
        assertThat(results.get(1).dateTimeUTC, is(firstUTCTime.plusMinutes(8)));
    }

    @Test
    public void testGetBetweenHourDateByTSSameDay() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 1000 * 60 * 60 * 8; // 8 hours
        final DateTime firstUTCTime = new DateTime(2015, 10, 1, 10, 0, DateTimeZone.UTC).minusMillis(offsetMillis);
        final DateTime minLocalTime = new DateTime(2015, 10, 1, 10, 1, DateTimeZone.forOffsetMillis(offsetMillis));
        final DateTime maxLocalTime = new DateTime(2015, 10, 1, 15, 0, DateTimeZone.forOffsetMillis(offsetMillis));

        final DeviceData.Builder builder = new DeviceData.Builder()
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withAccountId(accountId);

        final List<DeviceData> data = Lists.newArrayList();
        // NO: date time too early
        data.add(builder.withDateTimeUTC(firstUTCTime.plusMinutes(0)).build());
        // YES
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(1)).build());
        // YES
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(8)).build());
        // NO, bad ID
        data.add(builder
                .withExternalDeviceId("badId")
                .withDateTimeUTC(firstUTCTime.plusMinutes(7)).build());
        // NO, wrong hour
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusHours(2))
                .build());

        deviceDataDAODynamoDB.batchInsert(data);

        final List<DeviceData> results = deviceDataDAODynamoDB.getBetweenHourDateByTSSameDay(accountId, DeviceId.create(deviceId), firstUTCTime, firstUTCTime.plusMinutes(200), minLocalTime, maxLocalTime, 10, 11);
        LOGGER.debug("results: {}", results);
        for (final DeviceData result : results) {
            LOGGER.debug("ambient light: {}, device id: {}, datetime: {}, localtime: {}", result.ambientLight, result.externalDeviceId, result.dateTimeUTC, result.localTime());
        }
        assertThat(results.size(), is(2));
        assertThat(results.get(0).dateTimeUTC, is(firstUTCTime.plusMinutes(1)));
        assertThat(results.get(1).dateTimeUTC, is(firstUTCTime.plusMinutes(8)));
    }

    @Test
    public void testGetBetweenHourDateByTS() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 1000 * 60 * 60 * 8; // 8 hours
        final DateTime firstUTCTime = new DateTime(2015, 10, 1, 10, 0, DateTimeZone.UTC).minusMillis(offsetMillis);
        final DateTime minLocalTime = new DateTime(2015, 10, 1, 10, 1, DateTimeZone.forOffsetMillis(offsetMillis));
        final DateTime maxLocalTime = new DateTime(2015, 10, 1, 15, 0, DateTimeZone.forOffsetMillis(offsetMillis));

        final DeviceData.Builder builder = new DeviceData.Builder()
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withAccountId(accountId);

        final List<DeviceData> data = Lists.newArrayList();
        // NO: date time too early
        data.add(builder.withDateTimeUTC(firstUTCTime.plusMinutes(0)).build());
        // YES
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(1)).build());
        // YES
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(8)).build());
        // NO, bad ID
        data.add(builder
                .withExternalDeviceId("badId")
                .withDateTimeUTC(firstUTCTime.plusMinutes(7)).build());
        // NO, wrong hour
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusHours(2))
                .build());

        deviceDataDAODynamoDB.batchInsert(data);

        final List<DeviceData> results = deviceDataDAODynamoDB.getBetweenHourDateByTS(accountId, DeviceId.create(deviceId), firstUTCTime, firstUTCTime.plusMinutes(200), minLocalTime, maxLocalTime, 11, 20);
        LOGGER.debug("results: {}", results);
        for (final DeviceData result : results) {
            LOGGER.debug("ambient light: {}, device id: {}, datetime: {}, localtime: {}", result.ambientLight, result.externalDeviceId, result.dateTimeUTC, result.localTime());
        }
        assertThat(results.size(), is(2));
        assertThat(results.get(0).dateTimeUTC, is(firstUTCTime.plusMinutes(1)));
        assertThat(results.get(1).dateTimeUTC, is(firstUTCTime.plusMinutes(8)));
    }

    @Test
    public void testGetBetweenByLocalHourAggregateBySlotDuration() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 1000 * 60 * 60 * 8; // 8 hours
        final Integer slotDuration = 5;
        final DateTime firstUTCTime = new DateTime(2015, 10, 1, 10, 0, DateTimeZone.UTC).minusMillis(offsetMillis);
        final DateTime minLocalTime = new DateTime(2015, 10, 1, 10, 1, DateTimeZone.forOffsetMillis(offsetMillis));
        final DateTime maxLocalTime = new DateTime(2015, 10, 1, 15, 0, DateTimeZone.forOffsetMillis(offsetMillis));

        final DeviceData.Builder builder = new DeviceData.Builder()
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withAccountId(accountId);

        final List<DeviceData> data = Lists.newArrayList();
        // NO: date time too early
        data.add(builder.withDateTimeUTC(firstUTCTime.plusMinutes(0)).build());
        // YES
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(1)).build());
        // YES
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(8)).build());
        // NO, bad ID
        data.add(builder
                .withExternalDeviceId("badId")
                .withDateTimeUTC(firstUTCTime.plusMinutes(7)).build());
        // NO, wrong hour
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusHours(2))
                .build());

        deviceDataDAODynamoDB.batchInsert(data);

        final List<DeviceData> results = deviceDataDAODynamoDB.getBetweenByLocalHourAggregateBySlotDuration(accountId, DeviceId.create(deviceId), firstUTCTime, firstUTCTime.plusMinutes(200), minLocalTime, maxLocalTime, 11, 20, slotDuration);
        LOGGER.debug("results: {}", results);
        for (final DeviceData result : results) {
            LOGGER.debug("ambient light: {}, device id: {}, datetime: {}, localtime: {}", result.ambientLight, result.externalDeviceId, result.dateTimeUTC, result.localTime());
        }
        assertThat(results.size(), is(2));
        assertThat(results.get(0).dateTimeUTC, is(firstUTCTime.plusMinutes(0)));
        assertThat(results.get(1).dateTimeUTC, is(firstUTCTime.plusMinutes(5)));
    }

    @Test
    public void testGetAirQualityRawList() {
        final Long accountId = new Long(1);
        final String deviceId = "2";
        final Integer offsetMillis = 1000 * 60 * 60 * 8; // 8 hours
        final DateTime firstUTCTime = new DateTime(2015, 10, 1, 10, 0, DateTimeZone.UTC).minusMillis(offsetMillis);
        final DateTime minLocalTime = new DateTime(2015, 10, 1, 10, 1, DateTimeZone.forOffsetMillis(offsetMillis));
        final DateTime maxLocalTime = new DateTime(2015, 10, 2, 15, 0, DateTimeZone.forOffsetMillis(offsetMillis));

        final DeviceData.Builder builder = new DeviceData.Builder()
                .withExternalDeviceId(deviceId)
                .withOffsetMillis(offsetMillis)
                .withAccountId(accountId);

        final List<DeviceData> data = Lists.newArrayList();
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(1))
                .withAmbientAirQualityRaw(10).build());
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusMinutes(20))
                .withAmbientAirQualityRaw(20).build());
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusDays(1).plusMinutes(0))
                .withAmbientAirQualityRaw(100).build());
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusDays(1).plusMinutes(1))
                .withAmbientAirQualityRaw(200).build());
        // Won't be included, outside local time window
        data.add(builder
                .withDateTimeUTC(firstUTCTime.plusDays(1).plusHours(7).plusMinutes(0))
                .withAmbientAirQualityRaw(300).build());

        deviceDataDAODynamoDB.batchInsert(data);

        final List<Integer> results = deviceDataDAODynamoDB.getAirQualityRawList(accountId, DeviceId.create(deviceId), firstUTCTime, firstUTCTime.plusDays(3), minLocalTime, maxLocalTime);
        assertThat(results.size(), is(2));
        assertThat(results.get(0), is(15));
        assertThat(results.get(1), is(150));
    }

    @Test
    public void benchmarkAggregation() {
        final List<Map<String, AttributeValue>> items = Lists.newArrayList();
        final Integer slotDuration = 60;
        final DateTime firstTime = new DateTime(2015, 10, 10, 1, 1);
        final String DATE_TIME_STRING_TEMPLATE = "yyyy-MM-dd HH:mm";
        final DateTimeFormatter DATE_TIME_WRITE_FORMATTER = DateTimeFormat.forPattern(DATE_TIME_STRING_TEMPLATE);
        for (int i = 0; i < 10000; i++) {
            final Map<String, AttributeValue> item = Maps.newHashMap();
            final DateTime currTime = firstTime.plusMinutes(i);
            item.put(DeviceDataDAODynamoDB.Attribute.ACCOUNT_ID.name, new AttributeValue().withN("0"));
            item.put(DeviceDataDAODynamoDB.Attribute.LOCAL_UTC_TIMESTAMP.name, new AttributeValue().withS(currTime.toString(DATE_TIME_WRITE_FORMATTER)));
            item.put(DeviceDataDAODynamoDB.Attribute.OFFSET_MILLIS.name, new AttributeValue().withN("0"));
            item.put(DeviceDataDAODynamoDB.Attribute.RANGE_KEY.name, new AttributeValue().withS(currTime.toString(DATE_TIME_WRITE_FORMATTER) + "|" + "hi"));
            item.put(DeviceDataDAODynamoDB.Attribute.AMBIENT_LIGHT.name, new AttributeValue().withN(String.valueOf(i)));
            items.add(item);
        }

        for (int i = 0; i < 100; i++) {
            final long start = System.nanoTime();
            deviceDataDAODynamoDB.aggregateDynamoDBItemsToDeviceData(items, slotDuration);
            final long end = System.nanoTime();
            System.out.println("Time: " + (end - start) / 1000000.0);
        }
    }

}