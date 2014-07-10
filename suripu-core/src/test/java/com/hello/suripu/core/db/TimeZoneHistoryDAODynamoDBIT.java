package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.TimeZoneHistory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by pangwu on 6/16/14.
 */
public class TimeZoneHistoryDAODynamoDBIT {

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB;
    private final String tableName = "timezone_history_test";

    @Before
    public void setUp(){

        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            TimeZoneHistoryDAODynamoDB.createTable(tableName, this.amazonDynamoDBClient);
            this.timeZoneHistoryDAODynamoDB = new TimeZoneHistoryDAODynamoDB(
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
    public void testUpdateTimeZone(){
        final long accountId = 1;
        int offsetMillis  = DateTimeZone.getDefault().getOffset(DateTime.now());
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId, offsetMillis);

        final Optional<TimeZoneHistory> updated = this.timeZoneHistoryDAODynamoDB.getLastTimeZoneOffset(accountId);
        assertThat(updated.isPresent(), is(true));

        final TimeZoneHistory actual = updated.get();
        assertThat(actual.offsetMillis, is(offsetMillis));


    }

    @Test
    public void testUpdateTimeZones(){
        final long accountId = 1;
        int offsetMillis  = DateTimeZone.getDefault().getOffset(DateTime.now());
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId, offsetMillis);

        try {
            Thread.sleep(1000);
        }catch (Exception ex){

        }


        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId, offsetMillis + 1);

        final Optional<TimeZoneHistory> updated = this.timeZoneHistoryDAODynamoDB.getLastTimeZoneOffset(accountId);
        assertThat(updated.isPresent(), is(true));

        final TimeZoneHistory actual = updated.get();
        assertThat(actual.offsetMillis, is(offsetMillis + 1));


    }
}
