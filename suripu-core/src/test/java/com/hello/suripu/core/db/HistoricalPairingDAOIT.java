package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HistoricalPairingDAOIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(HistoricalPairingDAOIT.class);

    private DeviceDataDAODynamoDB deviceDataDAODynamoDB;
    private DeviceReadDAO deviceReadDAO;
    private AWSCredentials awsCredentials;
    private AmazonDynamoDB amazonDynamoDBClient;

    private static final String TABLE_PREFIX = "integration_test_device_data";
    private static final String OCTOBER_TABLE_NAME = TABLE_PREFIX + "_2015_10";
    private static final String NOVEMBER_TABLE_NAME = TABLE_PREFIX + "_2015_11";

    @Before
    public void setUp() {
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
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

        this.deviceReadDAO = mock(DeviceReadDAO.class);
    }


    @After
    public void tearDown() {
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


    private List<DeviceData> makeDeviceData(final Long accountId, final String senseId, final DateTime ref) {
        final DeviceData deviceData = new DeviceData.Builder()
                .withExternalDeviceId(senseId)
                .withAccountId(accountId)
                .withDateTimeUTC(ref)
                .withOffsetMillis(0)
                .build();
        return Lists.newArrayList(deviceData);
    }

    @Test
    public void testSenseNeverPaired() {
        final Long accountId = 1L;
        when(deviceReadDAO.getMostRecentSensePairByAccountId(accountId)).thenReturn(Optional.absent());
        final PairingDAO pairingDAO = new HistoricalPairingDAO(deviceReadDAO, deviceDataDAODynamoDB);
        final Optional<String> senseIdOptional = pairingDAO.senseId(
                accountId,
                DateTime.now(DateTimeZone.UTC),
                DateTime.now(DateTimeZone.UTC)
        );
        assertFalse("senseId is present", senseIdOptional.isPresent());
    }

    @Test
    public void testSenseNeverPairedDuringDateRange() {
        final Long accountId = 1L;
        when(deviceReadDAO.getMostRecentSensePairByAccountId(accountId)).thenReturn(Optional.absent());
        final PairingDAO pairingDAO = new HistoricalPairingDAO(deviceReadDAO, deviceDataDAODynamoDB);

        final DateTime nowCurrentMonth = new DateTime(2015,10,1, 0,0,0,0, DateTimeZone.UTC);
        final List<DeviceData> deviceDataList = makeDeviceData(accountId, "sense", nowCurrentMonth);

        deviceDataDAODynamoDB.batchInsertAll(deviceDataList);

        final Optional<String> senseIdOptional = pairingDAO.senseId(
                accountId,
                DateTime.now(DateTimeZone.UTC).minusDays(1),
                DateTime.now(DateTimeZone.UTC)
        );
        assertFalse("senseId is present", senseIdOptional.isPresent());
    }

    @Test
    public void testSenseWasPairedDuringDateRange() {
        final Long accountId = 1L;
        when(deviceReadDAO.getMostRecentSensePairByAccountId(accountId)).thenReturn(Optional.absent());
        final PairingDAO pairingDAO = new HistoricalPairingDAO(deviceReadDAO, deviceDataDAODynamoDB);

        final String senseId = "sense";
        final DateTime nowCurrentMonth = new DateTime(2015,10,1, 0,0,0,0, DateTimeZone.UTC);
        final List<DeviceData> deviceDataList = makeDeviceData(accountId, senseId, nowCurrentMonth);

        deviceDataDAODynamoDB.batchInsertAll(deviceDataList);

        final Optional<String> senseIdOptional = pairingDAO.senseId(
                accountId,
                nowCurrentMonth,
                nowCurrentMonth.plusDays(1)
        );
        assertTrue("senseId is present", senseIdOptional.isPresent());
        assertEquals("senseId match", senseId, senseIdOptional.get());
    }

    @Test
    public void testMultipleSenseWerePairedDuringDateRange() {
        final Long accountId = 1L;
        when(deviceReadDAO.getMostRecentSensePairByAccountId(accountId)).thenReturn(Optional.absent());
        final PairingDAO pairingDAO = new HistoricalPairingDAO(deviceReadDAO, deviceDataDAODynamoDB);

        final String senseId = "sense";
        final String otherSenseId = "other_sense";
        final DateTime nowCurrentMonth = new DateTime(2015,10,1, 0,0,0,0, DateTimeZone.UTC);
        final List<DeviceData> deviceDataList = makeDeviceData(accountId, senseId, nowCurrentMonth);
        final List<DeviceData> otherSenseDeviceDataList = makeDeviceData(accountId, otherSenseId, nowCurrentMonth.plusMinutes(10));

        deviceDataDAODynamoDB.batchInsertAll(deviceDataList);
        deviceDataDAODynamoDB.batchInsertAll(otherSenseDeviceDataList);

        final Optional<String> senseIdOptional = pairingDAO.senseId(
                accountId,
                nowCurrentMonth,
                nowCurrentMonth.plusDays(1)
        );
        assertTrue("senseId is present", senseIdOptional.isPresent());
        // senseId returned for that date range should be the first one for which we have data
        assertEquals("senseId match", senseId, senseIdOptional.get());
    }

    @Test
    public void testCurrentSenseNewerThanDateRange() {
        final Long accountId = 1L;
        final DateTime createdAt = DateTime.now(DateTimeZone.UTC);
        final String newSense = "just_paired";
        final String senseId = "sense";

        final DeviceAccountPair pair = new DeviceAccountPair(accountId, 0L, newSense, createdAt);

        when(deviceReadDAO.getMostRecentSensePairByAccountId(accountId)).thenReturn(Optional.of(pair));
        final PairingDAO pairingDAO = new HistoricalPairingDAO(deviceReadDAO, deviceDataDAODynamoDB);

        final DateTime nowCurrentMonth = new DateTime(2015,10,1, 0,0,0,0, DateTimeZone.UTC);
        final List<DeviceData> deviceDataList = makeDeviceData(accountId, senseId, nowCurrentMonth);

        deviceDataDAODynamoDB.batchInsertAll(deviceDataList);

        final Optional<String> senseIdOptional = pairingDAO.senseId(
                accountId,
                nowCurrentMonth,
                nowCurrentMonth.plusDays(1)
        );
        assertTrue("senseId is present", senseIdOptional.isPresent());
        // senseId returned for that date range should be the historical sense, not the one currently paired
        assertEquals("senseId match", senseId, senseIdOptional.get());
    }

    @Test
    public void testCurrentSenseOlderThanDateRange() {
        final Long accountId = 1L;
        final DateTime createdAt = new DateTime(2015,1,1,0,0,0, DateTimeZone.UTC);
        final String pairedSense = "paired_sense";
        final String senseId = "sense";

        final DeviceAccountPair pair = new DeviceAccountPair(accountId, 0L, pairedSense, createdAt);

        when(deviceReadDAO.getMostRecentSensePairByAccountId(accountId)).thenReturn(Optional.of(pair));
        final PairingDAO pairingDAO = new HistoricalPairingDAO(deviceReadDAO, deviceDataDAODynamoDB);

        final DateTime nowCurrentMonth = new DateTime(2015,10,1, 0,0,0,0, DateTimeZone.UTC);
        final List<DeviceData> deviceDataList = makeDeviceData(accountId, senseId, nowCurrentMonth);

        deviceDataDAODynamoDB.batchInsertAll(deviceDataList);

        final Optional<String> senseIdOptional = pairingDAO.senseId(
                accountId,
                nowCurrentMonth,
                nowCurrentMonth.plusDays(1)
        );
        assertTrue("senseId is present", senseIdOptional.isPresent());
        // senseId should be the current sense that is paired
        assertEquals("senseId match", pairedSense, senseIdOptional.get());
    }
}
