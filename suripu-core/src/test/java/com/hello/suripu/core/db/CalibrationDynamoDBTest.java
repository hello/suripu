package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.Calibration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CalibrationDynamoDBTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(AlarmDAODynamoDBIT.class);

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private CalibrationDynamoDB calibrationDAO;
    private final String tableName = "calibration_test";
    private final static String SENSE_ID = "TEST_SENSE";

    @Before
    public void setUp(){
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            CalibrationDynamoDB.createTable(tableName, this.amazonDynamoDBClient);
            this.calibrationDAO = CalibrationDynamoDB.create(this.amazonDynamoDBClient, tableName);
        }catch (ResourceInUseException rie){
            LOGGER.warn("Table already exists");
        }
    }

    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(tableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            LOGGER.warn("Can't delete existing table");
        }
    }

    @Test
    public void testMissingCalibration() {
        final Optional<Calibration> calibration = calibrationDAO.get(SENSE_ID);
        assertThat(calibration.isPresent(), is(false));

        final Optional<Calibration> calibrationOptional = calibrationDAO.getStrict(SENSE_ID);
        assertThat(calibrationOptional.isPresent(), is(false));
    }


    @Test
    public void testCache() throws InterruptedException {

        // Specify short cache duration
        final CalibrationDAO calibrationDAO = CalibrationDynamoDB.createWithCacheConfig(this.amazonDynamoDBClient, tableName, 1);

        // Haven't put anything yet, so expect to be absent
        final Optional<Calibration> calibrationOptional = calibrationDAO.get(SENSE_ID);
        assertThat(calibrationOptional.isPresent(), is(false));

        final Calibration calibration = Calibration.create(SENSE_ID, 88, 123456798L);
        calibrationDAO.put(calibration);

        // Cache hasn't expired yet, so we expect it to still be missing
        final Optional<Calibration> calibrationOptional2 = calibrationDAO.get(SENSE_ID);
        assertThat(calibrationOptional2.isPresent(), is(false));


        // Bypass cache to make sure it's there
        final Optional<Calibration> calibrationOptional3 = calibrationDAO.getStrict(SENSE_ID);
        assertThat(calibrationOptional3.isPresent(), is(true));

        Thread.sleep(1100); // boo sleep in tests

        // Cache should have expired now, so we expect item to be here
        final Optional<Calibration> calibrationOptional4 = calibrationDAO.get(SENSE_ID);
        assertThat(calibrationOptional4.isPresent(), is(true));
        assertThat(calibrationOptional4.get().dustOffset, is(calibration.dustOffset));

    }
}
