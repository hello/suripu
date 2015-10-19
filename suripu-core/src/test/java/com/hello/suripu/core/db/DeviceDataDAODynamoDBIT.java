package com.hello.suripu.core.db;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.google.common.collect.ImmutableList;
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
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private DeviceDataDAODynamoDB deviceDataDAODynamoDB;

    private static final String TABLE_NAME = "integration_test_device_data";


    @Before
    public void setUp() throws Exception {
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
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
    public void testBatchInsertWithFailureFallback() {
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

        final int initialItemsInserted = deviceDataDAODynamoDB.batchInsertWithFailureFallback(deviceDataList);
        assertThat(initialItemsInserted, is(deviceDataList.size()));
        assertThat(getTableCount(), is(deviceDataList.size()));

        // Now insert the exact same thing again. Should work fine in DynamoDB.
        final int duplicateItemsInserted = deviceDataDAODynamoDB.batchInsertWithFailureFallback(deviceDataList);
        assertThat(duplicateItemsInserted, is(deviceDataList.size()));
        assertThat(getTableCount(), is(deviceDataList.size()));
    }

    @Test
    public void testBatchInsertWithFailureFallbackDuplicateKeys() {
        final DeviceData.Builder builder = new DeviceData.Builder()
                .withAccountId(new Long(0))
                .withDeviceId(new Long(0))
                .withDateTimeUTC(new DateTime(2015, 10, 1, 8, 0))
                .withOffsetMillis(0);
        final List<DeviceData> deviceDataList = new ImmutableList.Builder<DeviceData>()
                .add(builder.build())
                .add(builder.build())
                .build();
        final int insertions = deviceDataDAODynamoDB.batchInsertWithFailureFallback(deviceDataList);
        assertThat(insertions, is(1));
        assertThat(getTableCount(), is(1));
    }

    @Test()
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
    }

    @Test
    public void testGetBetweenByAbsoluteTimeAggregateBySlotDuration() {
        final List<DeviceData> deviceDataList = new ArrayList<>();
        final Long accountId = new Long(1);
        final Long deviceId = new Long(1);
        final Integer offsetMillis = 0;
        final DateTime firstTime = new DateTime(2015, 10, 1, 7, 0);
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withDeviceId(deviceId)
                .withDateTimeUTC(firstTime)
                .withOffsetMillis(offsetMillis)
                .build());
        deviceDataList.add(new DeviceData.Builder()
                .withAccountId(accountId)
                .withDeviceId(deviceId)
                .withDateTimeUTC(firstTime.plusMinutes(1))
                .withOffsetMillis(offsetMillis)
                .build());
        deviceDataDAODynamoDB.batchInsert(deviceDataList);

        // From start to start+1, 2 results
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        deviceId, accountId, firstTime, firstTime.plusMinutes(1), 1).size(),
                is(2));
        // From start to start+2, Also 2 results because only 2 in table
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        deviceId, accountId, firstTime, firstTime.plusMinutes(2), 1).size(),
                is(2));
        // from start to start should be 1 result
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        deviceId, accountId, firstTime, firstTime, 1).size(),
                is(1));
        // Account ID unrecognized should be 0 results
        assertThat(deviceDataDAODynamoDB.getBetweenByAbsoluteTimeAggregateBySlotDuration(
                        deviceId, accountId + 1000, firstTime, firstTime, 1).size(),
                is(0));
    }

}