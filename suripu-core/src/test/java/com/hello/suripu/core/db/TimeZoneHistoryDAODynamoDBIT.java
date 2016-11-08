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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

/**
 * Created by pangwu on 6/16/14.
 */
public class TimeZoneHistoryDAODynamoDBIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(TimeZoneHistoryDAODynamoDBIT.class);

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
            LOGGER.warn("Can't create existing table");
        }
    }

    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(tableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            LOGGER.error("Can't delete non existing table");
        }
    }


    @Test
    public void testUpdateTimeZone(){
        final long accountId = 1;
        int offsetMillis  = DateTimeZone.getDefault().getOffset(DateTime.now());
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId,  DateTime.now(), DateTime.now().getZone().getID(), DateTime.now().getZone().getOffset(DateTime.now()));

        final Optional<TimeZoneHistory> updated = this.timeZoneHistoryDAODynamoDB.getCurrentTimeZone(accountId);
        assertThat(updated.isPresent(), is(true));

        assertThat(updated.get().offsetMillis, is(offsetMillis));
    }

    @Test
    public void testUpdateTimeZones(){
        final long accountId = 1;
        int offsetMillis  = DateTime.now().getZone().getOffset(DateTime.now());
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId,
                DateTime.now(),
                DateTime.now().getZone().getID(),
                offsetMillis);

        try {
            Thread.sleep(1000);
        }catch (Exception ex){

        }


        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId, DateTime.now(), DateTimeZone.UTC.getID(), DateTimeZone.UTC.getOffset(DateTime.now()));

        final Optional<TimeZoneHistory> updated = this.timeZoneHistoryDAODynamoDB.getCurrentTimeZone(accountId);
        assertThat(updated.isPresent(), is(true));


        assertThat(updated.get().offsetMillis, is(0));
    }


    @Test
    public void testGetCustomTimeZone(){
        final long accountId = 1;
        int offsetMillis  = DateTime.now().getZone().getOffset(DateTime.now());
        final DateTimeZone zoneFromOffset = DateTimeZone.forOffsetMillis(offsetMillis - 1);
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId, DateTime.now(), zoneFromOffset.getID(), offsetMillis - 1);


        final Optional<TimeZoneHistory> updated = this.timeZoneHistoryDAODynamoDB.getCurrentTimeZone(accountId);
        assertThat(updated.isPresent(), is(true));

        assertThat(updated.get().offsetMillis, is(offsetMillis - 1));
        assertThat(updated.get().timeZoneId, is(zoneFromOffset.getID()));

        // This timezone, created from offset millis, should not appear in the standard timezone id list.
        assertThat(DateTimeZone.getAvailableIDs().contains(zoneFromOffset.getID()), is(false));
    }


    @Test
    public void testInvalidLocalOffsetMillis(){
        // Test the scenario that mobile has a wrong time but has a correct timezone id.
        // server should return the correct timezone offset.

        final long accountId = 1;
        int offsetMillis  = DateTime.now().getZone().getOffset(DateTime.now());
        final DateTimeZone timeZone = DateTimeZone.getDefault();
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId, DateTime.now(), timeZone.getID(), offsetMillis - 100);


        final Optional<TimeZoneHistory> updated = this.timeZoneHistoryDAODynamoDB.getCurrentTimeZone(accountId);
        assertThat(updated.isPresent(), is(true));

        assertThat(updated.get().offsetMillis, is(offsetMillis));
        assertThat(updated.get().timeZoneId, is(timeZone.getID()));
    }


    @Test
    public void testInvalidTimeZoneId(){
        // Test the scenario that mobile provides a wrong time zone id.
        // server should use the offset to create local timezone.

        final long accountId = 1;
        int offsetMillis  = DateTime.now().getZone().getOffset(DateTime.now());
        final DateTimeZone timeZone = DateTimeZone.getDefault();
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId,  DateTime.now(), "Invalid time zone id", offsetMillis);


        final Optional<TimeZoneHistory> updated = this.timeZoneHistoryDAODynamoDB.getCurrentTimeZone(accountId);
        assertThat(updated.isPresent(), is(true));

        assertThat(updated.get().offsetMillis, is(offsetMillis));
        assertThat(updated.get().timeZoneId, not(timeZone.getID()));
    }

    @Test
    public void testRetrieveEmptyHistory(){
        long accountId = 1;
        final Optional<TimeZoneHistory> optional = this.timeZoneHistoryDAODynamoDB.getCurrentTimeZone(accountId);
        assertThat(optional.isPresent(), is(false));

    }

    @Test
    public void testTimeZoneQueryLimit(){
        final long accountId = 1;
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId,  now, now.getZone().getID(), now.getZone().getOffset(now));
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId,  now.minusDays(10), now.getZone().getID(), now.getZone().getOffset(now.minusDays(10)));
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId,  now.minusDays(20), now.getZone().getID(), now.getZone().getOffset(now.minusDays(20)));
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId,  now.minusDays(30), now.getZone().getID(), now.getZone().getOffset(now.minusDays(30)));
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId,  now.minusDays(40), now.getZone().getID(), now.getZone().getOffset(now.minusDays(40)));
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId,  now.minusDays(50), now.getZone().getID(), now.getZone().getOffset(now.minusDays(50)));
        this.timeZoneHistoryDAODynamoDB.updateTimeZone(accountId,  now.minusDays(60), now.getZone().getID(), now.getZone().getOffset(now.minusDays(60)));


        List<TimeZoneHistory> timeZoneHistorylist= this.timeZoneHistoryDAODynamoDB.getMostRecentTimeZoneHistory(accountId, now.minusDays(3), 1);
        assertThat(timeZoneHistorylist.get(0).updatedAt, is(now.minusDays(10).getMillis()));
        timeZoneHistorylist= this.timeZoneHistoryDAODynamoDB.getMostRecentTimeZoneHistory(accountId, now.minusDays(13), 1);
        assertThat(timeZoneHistorylist.get(0).updatedAt, is(now.minusDays(20).getMillis()));
    }
}
