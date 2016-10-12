package com.hello.suripu.core.db;

import com.google.common.collect.Lists;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by pangwu on 2/26/15.
 */
public class RingTimeHistoryDAODynamoDBIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(RingTimeHistoryDAODynamoDBIT.class);

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB;
    private final String tableName = "ring_time_history_test";

    @Before
    public void setUp(){

        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            RingTimeHistoryDAODynamoDB.createTable(tableName, this.amazonDynamoDBClient);
            this.ringTimeHistoryDAODynamoDB = new RingTimeHistoryDAODynamoDB(
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

        this.ringTimeHistoryDAODynamoDB.setNextRingTime(deviceId, 1L, ringTime1);
        this.ringTimeHistoryDAODynamoDB.setNextRingTime(deviceId, 1L, ringTime2);

        List<RingTime> nextRingTime = this.ringTimeHistoryDAODynamoDB.getRingTimesBetween(deviceId, 1L, alarmTime1.minusDays(1), alarmTime1);
        assertThat(nextRingTime.size(), is(1));
        assertThat(nextRingTime.get(0), is(ringTime1));

        nextRingTime = this.ringTimeHistoryDAODynamoDB.getRingTimesBetween(deviceId, 1L, alarmTime1, alarmTime2);
        assertThat(nextRingTime.size(), is(2));
        assertThat(nextRingTime.get(0), is(ringTime1));
        assertThat(nextRingTime.get(1), is(ringTime2));

    }

    protected void setNextRingTime(final String deviceId, final RingTime ringTime, final DateTime currentTime){


        final HashMap<String, AttributeValue> items = new HashMap<>();
        items.put(RingTimeHistoryDAODynamoDB.MORPHEUS_ID_ATTRIBUTE_NAME, new AttributeValue().withS(deviceId));

        items.put(RingTimeHistoryDAODynamoDB.CREATED_AT_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(currentTime.getMillis())));
        items.put(RingTimeHistoryDAODynamoDB.ACTUAL_RING_TIME_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(ringTime.actualRingTimeUTC)));
        items.put(RingTimeHistoryDAODynamoDB.EXPECTED_RING_TIME_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(ringTime.expectedRingTimeUTC)));


        try {
            final ObjectMapper mapper = new ObjectMapper();
            final String ringTimeJSON = mapper.writeValueAsString(ringTime);
            items.put(RingTimeHistoryDAODynamoDB.RINGTIME_OBJECT_ATTRIBUTE_NAME, new AttributeValue().withS(ringTimeJSON));
            final PutItemRequest putItemRequest = new PutItemRequest(this.tableName, items);
            final PutItemResult result = this.amazonDynamoDBClient.putItem(putItemRequest);
        } catch (JsonProcessingException e) {
            LOGGER.error("set next ringtime for device {} failed: {}", deviceId, e.getMessage());
        }catch (AmazonServiceException awsServiceExp){
            LOGGER.error("set next ringtime for device {} failed due to service exception: {}",
                    deviceId, awsServiceExp.getMessage());
        } catch (AmazonClientException awsClientExp){
            LOGGER.error("set next ringtime for device {} failed due to client exception: {}",
                    deviceId, awsClientExp.getMessage());
        } catch (Exception ex){
            LOGGER.error("set next ringtime for device {} failed due to general exception: {}",
                    deviceId, ex.getMessage());
        }


    }

    @Test
    public void testSetTwoIdenticalSmartAlarmWithTwoUsers(){
        final String deviceId = "test morpheus";
        final DateTimeZone localTimeZone = DateTimeZone.forID("America/Los_Angeles");

        final DateTime alarmTime1 = new DateTime(2014, 9, 23, 8, 20, 0, localTimeZone);
        final DateTime actualTime1 = new DateTime(2014, 9, 23, 8, 10, 0, localTimeZone);
        final RingTime ringTime1 = new RingTime(actualTime1.getMillis(), alarmTime1.getMillis(), 0, true, Lists.newArrayList());

        final DateTime alarmTime2 = new DateTime(2014, 9, 23, 8, 20, 0, localTimeZone);
        final DateTime actualTime2 = new DateTime(2014, 9, 23, 8, 9, 0, localTimeZone);
        final RingTime ringTime2 = new RingTime(actualTime2.getMillis(), alarmTime2.getMillis(), 0, true, Lists.newArrayList());

        this.ringTimeHistoryDAODynamoDB.setNextRingTime(deviceId, 1L, ringTime1);
        this.ringTimeHistoryDAODynamoDB.setNextRingTime(deviceId, 2L, ringTime2);

        List<RingTime> nextRingTime = this.ringTimeHistoryDAODynamoDB.getRingTimesBetween(deviceId, 1L, alarmTime1.minusDays(1), alarmTime1);
        assertThat(nextRingTime.size(), is(1));
        assertThat(nextRingTime.get(0).actualRingTimeUTC, is(ringTime1.actualRingTimeUTC));

        nextRingTime = this.ringTimeHistoryDAODynamoDB.getRingTimesBetween(deviceId, 2L, alarmTime2.minusDays(1), alarmTime2);
        assertThat(nextRingTime.size(), is(1));
        assertThat(nextRingTime.get(0).actualRingTimeUTC, is(ringTime2.actualRingTimeUTC));
    }

    @Test
    public void testSetTwoIdenticalRegularAlarmWithTwoUsers(){
        final String deviceId = "test morpheus";
        final DateTimeZone localTimeZone = DateTimeZone.forID("America/Los_Angeles");

        final DateTime alarmTime1 = new DateTime(2014, 9, 23, 8, 20, 0, localTimeZone);
        final DateTime actualTime1 = new DateTime(2014, 9, 23, 8, 20, 0, localTimeZone);
        final RingTime ringTime1 = new RingTime(actualTime1.getMillis(), alarmTime1.getMillis(), 0, false, Lists.newArrayList());

        final DateTime alarmTime2 = new DateTime(2014, 9, 23, 8, 20, 0, localTimeZone);
        final DateTime actualTime2 = new DateTime(2014, 9, 23, 8, 20, 0, localTimeZone);
        final RingTime ringTime2 = new RingTime(actualTime2.getMillis(), alarmTime2.getMillis(), 0, false, Lists.newArrayList());

        this.ringTimeHistoryDAODynamoDB.setNextRingTime(deviceId, 1L, ringTime1);
        this.ringTimeHistoryDAODynamoDB.setNextRingTime(deviceId, 2L, ringTime2);

        List<RingTime> nextRingTime = this.ringTimeHistoryDAODynamoDB.getRingTimesBetween(deviceId, 1L, alarmTime1.minusDays(1), alarmTime1);
        assertThat(nextRingTime.size(), is(1));
        assertThat(nextRingTime.get(0).actualRingTimeUTC, is(ringTime1.actualRingTimeUTC));

        nextRingTime = this.ringTimeHistoryDAODynamoDB.getRingTimesBetween(deviceId, 2L, alarmTime2.minusDays(1), alarmTime2);
        assertThat(nextRingTime.size(), is(1));
        assertThat(nextRingTime.get(0).actualRingTimeUTC, is(ringTime2.actualRingTimeUTC));
    }

    @Test
    public void testSetTwoIdenticalRingTime(){
        final String deviceId = "test morpheus";
        final DateTimeZone localTimeZone = DateTimeZone.forID("America/Los_Angeles");

        final DateTime alarmTime1 = new DateTime(2014, 9, 23, 8, 20, 0, localTimeZone);
        final DateTime actualTime1 = new DateTime(2014, 9, 23, 8, 10, 0, localTimeZone);
        final RingTime ringTime1 = new RingTime(actualTime1.getMillis(), alarmTime1.getMillis(), 0, true, Lists.newArrayList());

        final DateTime alarmTime2 = new DateTime(2014, 9, 23, 8, 20, 0, localTimeZone);
        final DateTime actualTime2 = new DateTime(2014, 9, 23, 8, 9, 0, localTimeZone);
        final RingTime ringTime2 = new RingTime(actualTime2.getMillis(), alarmTime2.getMillis(), 0, true, Lists.newArrayList());

        this.ringTimeHistoryDAODynamoDB.setNextRingTime(deviceId, 1L, ringTime1);
        this.ringTimeHistoryDAODynamoDB.setNextRingTime(deviceId, 1L, ringTime2);

        List<RingTime> nextRingTime = this.ringTimeHistoryDAODynamoDB.getRingTimesBetween(deviceId, 1L, alarmTime1.minusDays(1), alarmTime1);
        assertThat(nextRingTime.size(), is(1));
        assertThat(nextRingTime.get(0).actualRingTimeUTC, is(ringTime2.actualRingTimeUTC));

        nextRingTime = this.ringTimeHistoryDAODynamoDB.getRingTimesBetween(deviceId, 100L, alarmTime1.minusDays(1), alarmTime1);
        assertThat(nextRingTime.size(), is(0));

    }


    @Test
    public void testGetEmptyRingTime(){
        final List<RingTime> ringTimes = this.ringTimeHistoryDAODynamoDB.getRingTimesBetween("test morpheus", 1L, DateTime.now(), DateTime.now().plusDays(1));
        assertThat(ringTimes.size(), is(0));
    }

}
