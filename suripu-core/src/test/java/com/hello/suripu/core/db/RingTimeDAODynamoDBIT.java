package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.hello.suripu.core.models.RingTime;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by pangwu on 9/19/14.
 */
public class RingTimeDAODynamoDBIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(RingTimeDAODynamoDBIT.class);

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private RingTimeDAODynamoDB ringTimeDAODynamoDB;
    private final String tableName = "ring_time_test";

    @Before
    public void setUp(){

        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            RingTimeDAODynamoDB.createTable(tableName, this.amazonDynamoDBClient);
            this.ringTimeDAODynamoDB = new RingTimeDAODynamoDB(
                    this.amazonDynamoDBClient,
                    tableName
            );


        }catch (ResourceInUseException rie){
            LOGGER.error("Can not create existing table");
        }
    }

    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(tableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        } catch (ResourceNotFoundException ex){
            LOGGER.error("Can not delete non existing table");
        }
    }


    @Test
    public void testSetAndGetRingTime(){
        final String deviceId = "test morpheus";
        final DateTimeZone localTimeZone = DateTimeZone.forID("America/Los_Angeles");

        final DateTime alarmTime1 = new DateTime(2014, 9, 23, 8, 20, 0, localTimeZone);
        final DateTime actualTime1 = new DateTime(2014, 9, 23, 8, 10, 0, localTimeZone);
        final RingTime ringTime1 = new RingTime(actualTime1.getMillis(), alarmTime1.getMillis(), 0, true);

        final DateTime alarmTime2 = new DateTime(2014, 9, 24, 9, 10, 0, localTimeZone);
        final DateTime actualTime2 = new DateTime(2014, 9, 24, 9, 0, 0, localTimeZone);
        final RingTime ringTime2 = new RingTime(actualTime2.getMillis(), alarmTime2.getMillis(), 0, true);

        this.ringTimeDAODynamoDB.setNextRingTime(deviceId, ringTime1);
        this.ringTimeDAODynamoDB.setNextRingTime(deviceId, ringTime2);

        final RingTime nextRingTime = this.ringTimeDAODynamoDB.getNextRingTime(deviceId);
        final RingTime expected = new RingTime(actualTime2.getMillis(), alarmTime2.getMillis(), 0, true);

        assertThat(nextRingTime, is(expected));
    }


    @Test
    public void testGetEmptyRingTime(){
        final RingTime ringTimeOptional = this.ringTimeDAODynamoDB.getNextRingTime("test morpheus");
        assertThat(ringTimeOptional, is(RingTime.createEmpty()));
    }

}
