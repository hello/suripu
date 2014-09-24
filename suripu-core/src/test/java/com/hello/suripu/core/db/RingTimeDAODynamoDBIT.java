package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by pangwu on 9/19/14.
 */
public class RingTimeDAODynamoDBIT {
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
            rie.printStackTrace();
        }

    }

    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(tableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            ex.printStackTrace();
        }
    }


    @Test
    public void testSetAndGetRingTime(){
        final String deviceId = "test morpheus";
        final DateTimeZone localTimeZone = DateTimeZone.forID("America/Los_Angeles");

        final DateTime day1LocalUTC = new DateTime(2014, 9, 23, 0, 0, 0, DateTimeZone.UTC);
        final DateTime ringTime1 = new DateTime(2014, 9, 23, 8, 20, 0, localTimeZone);

        final DateTime day2LocalUTC = new DateTime(2014, 9, 24, 0, 0, 0, DateTimeZone.UTC);
        final DateTime ringTime2 = new DateTime(2014, 9, 24, 9, 0, 0, localTimeZone);

        this.ringTimeDAODynamoDB.setRingTime(deviceId, day1LocalUTC, ringTime1, localTimeZone);
        this.ringTimeDAODynamoDB.setRingTime(deviceId, day2LocalUTC, ringTime2, localTimeZone);

        final Optional<DateTime> actualRingTime1 = this.ringTimeDAODynamoDB.getRingTime(deviceId, day1LocalUTC);
        final Optional<DateTime> actualRingTime2 = this.ringTimeDAODynamoDB.getRingTime(deviceId, day2LocalUTC);

        assertThat(actualRingTime1.isPresent(), is(true));
        assertThat(actualRingTime2.isPresent(), is(true));

        assertThat(actualRingTime1.get().getMillis(), is(ringTime1.getMillis()));
        assertThat(actualRingTime2.get().getMillis(), is(ringTime2.getMillis()));
    }

    @Test
    public void testUpdateRingTime(){
        final String deviceId = "test morpheus";
        final DateTimeZone localTimeZone = DateTimeZone.forID("America/Los_Angeles");

        final DateTime day1LocalUTC = new DateTime(2014, 9, 23, 0, 0, 0, DateTimeZone.UTC);
        final DateTime ringTime1 = new DateTime(2014, 9, 23, 8, 20, 0, localTimeZone);

        final DateTime day2LocalUTC = new DateTime(2014, 9, 23, 0, 0, 0, DateTimeZone.UTC);
        final DateTime ringTime2 = new DateTime(2014, 9, 23, 9, 0, 0, localTimeZone);

        this.ringTimeDAODynamoDB.setRingTime(deviceId, day1LocalUTC, ringTime1, localTimeZone);
        this.ringTimeDAODynamoDB.setRingTime(deviceId, day2LocalUTC, ringTime2, localTimeZone);

        final Optional<DateTime> actualRingTime1 = this.ringTimeDAODynamoDB.getRingTime(deviceId, day1LocalUTC);
        assertThat(actualRingTime1.isPresent(), is(true));
        assertThat(actualRingTime1.get().getMillis(), is(ringTime2.getMillis()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInvalidRingTime(){
        final String deviceId = "test morpheus";
        final DateTimeZone localTimeZone = DateTimeZone.forID("America/Los_Angeles");

        final DateTime day1LocalUTC = new DateTime(2014, 9, 23, 0, 0, 0, DateTimeZone.UTC);
        final DateTime ringTime1 = new DateTime(2014, 9, 24, 8, 20, 0, localTimeZone);

        this.ringTimeDAODynamoDB.setRingTime(deviceId, day1LocalUTC, ringTime1, localTimeZone);

    }

}
