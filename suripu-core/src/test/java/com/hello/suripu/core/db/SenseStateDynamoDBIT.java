package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.hello.suripu.api.input.State;
import com.hello.suripu.core.models.SenseStateAtTime;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by jakepiccolo on 2/19/16.
 */
public class SenseStateDynamoDBIT  {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseStateDynamoDB.class);

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDB amazonDynamoDBClient;
    private SenseStateDynamoDB senseStateDynamoDB;

    private static String TABLE_NAME = "integration_test_sense_state";

    @Before
    public void setUp() throws Exception {
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        this.senseStateDynamoDB = new SenseStateDynamoDB(amazonDynamoDBClient, TABLE_NAME);

        tearDown();

        try {
            LOGGER.debug("-------- Creating Table {} ---------", TABLE_NAME);
            final CreateTableResult result = senseStateDynamoDB.createTable(2L, 2L);
            LOGGER.debug("Created dynamoDB table {}", result.getTableDescription());
        } catch (ResourceInUseException rie){
            LOGGER.warn("Problem creating table");
        }
    }

    @After
    public void tearDown() throws Exception {
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(TABLE_NAME);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            System.out.println("Can not delete non existing table " + TABLE_NAME);
        }
    }

    @Test
    public void testGetState() throws Exception {

    }

    private State.SenseState newState(final String senseId) {
        return State.SenseState.newBuilder().setSenseId(senseId).build();
    }

    @Test
    public void testUpdateState() throws Exception {
        final String senseId = "sense";
        final DateTime origTime = new DateTime(2016, 1, 1, 0, 0, 0, DateTimeZone.UTC);

        final Optional<SenseStateAtTime> missingStateOptional = senseStateDynamoDB.getState(senseId);
        assertThat(missingStateOptional.isPresent(), is(false));

        final SenseStateAtTime state1 = new SenseStateAtTime(newState(senseId), origTime);
        senseStateDynamoDB.updateState(state1);
        final Optional<SenseStateAtTime> stateOptional1 = senseStateDynamoDB.getState(senseId);
        assertThat(stateOptional1.isPresent(), is(true));
        assertThat(stateOptional1.get().timestamp, is(origTime));
    }


}