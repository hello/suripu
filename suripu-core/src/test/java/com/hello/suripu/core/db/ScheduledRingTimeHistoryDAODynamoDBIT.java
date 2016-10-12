package com.hello.suripu.core.db;

import com.google.common.collect.Lists;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello.suripu.core.models.RingTime;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by pangwu on 9/19/14.
 */
public class ScheduledRingTimeHistoryDAODynamoDBIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(ScheduledRingTimeHistoryDAODynamoDBIT.class);

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private ScheduledRingTimeHistoryDAODynamoDB scheduledRingTimeHistoryDAODynamoDB;
    private final String tableName = "scheduled_ring_time_history_test";

    @Before
    public void setUp(){

        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            ScheduledRingTimeHistoryDAODynamoDB.createTable(tableName, this.amazonDynamoDBClient);
            this.scheduledRingTimeHistoryDAODynamoDB = new ScheduledRingTimeHistoryDAODynamoDB(
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
        final RingTime ringTime1 = new RingTime(actualTime1.getMillis(), alarmTime1.getMillis(), 0, true, Lists.newArrayList());

        final DateTime alarmTime2 = new DateTime(2014, 9, 24, 9, 10, 0, localTimeZone);
        final DateTime actualTime2 = new DateTime(2014, 9, 24, 9, 0, 0, localTimeZone);
        final RingTime ringTime2 = new RingTime(actualTime2.getMillis(), alarmTime2.getMillis(), 0, true, Lists.newArrayList());

        this.scheduledRingTimeHistoryDAODynamoDB.setNextRingTime(deviceId, ringTime1);
        this.scheduledRingTimeHistoryDAODynamoDB.setNextRingTime(deviceId, ringTime2);

        final RingTime nextRingTime = this.scheduledRingTimeHistoryDAODynamoDB.getNextRingTime(deviceId);
        final RingTime expected = new RingTime(actualTime2.getMillis(), alarmTime2.getMillis(), 0, true, Lists.newArrayList());

        assertThat(nextRingTime, is(expected));
    }


    @Test
    public void testGetEmptyRingTime(){
        final RingTime ringTimeOptional = this.scheduledRingTimeHistoryDAODynamoDB.getNextRingTime("test morpheus");
        assertThat(ringTimeOptional, is(RingTime.createEmpty()));
    }

    @Test
    public void testJSONBackwardCompatibility(){
        //{\"actual_ring_time_utc\":1418311800000,\"expected_ring_time_utc\":1418311800000,\"sound_ids\":[0]}
        final String deviceId = "morpheus";
        final String ringTimeJSON = "{\"actual_ring_time_utc\":1418311800000,\"expected_ring_time_utc\":1418311800000,\"sound_ids\":[0]}";
        final HashMap<String, AttributeValue> items = new HashMap<String, AttributeValue>();
        items.put(ScheduledRingTimeHistoryDAODynamoDB.MORPHEUS_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));
        final ObjectMapper mapper = new ObjectMapper();

        items.put(ScheduledRingTimeHistoryDAODynamoDB.CREATED_AT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(DateTime.now().getMillis())));
        items.put(ScheduledRingTimeHistoryDAODynamoDB.RING_TIME_ATTRIBUTE_NAME, new AttributeValue().withS(ringTimeJSON));

        final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, items);
        final PutItemResult result = this.amazonDynamoDBClient.putItem(putItemRequest);

        final RingTime actual = this.scheduledRingTimeHistoryDAODynamoDB.getNextRingTime(deviceId);
        assertThat(actual.actualRingTimeUTC, is(1418311800000L));
        assertThat(actual.expectedRingTimeUTC, is(1418311800000L));
        assertThat(actual.fromSmartAlarm, is(false));
    }

}
