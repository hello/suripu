package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hello.suripu.core.metrics.DeviceEvents;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class SenseEventsDAOIT {

    private AmazonDynamoDB amazonDynamoDB;
    private String tableName = "test-sense-events";

    @Before
    public void setUp() throws InterruptedException {
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        amazonDynamoDB = new AmazonDynamoDBClient(new DefaultAWSCredentialsProviderChain(), clientConfiguration);
        amazonDynamoDB.setEndpoint("http://localhost:7777");
        CreateTableResult createTableResult = SenseEventsDynamoDB.createTable(tableName, amazonDynamoDB);
        Thread.sleep(2000L);
    }

    @After public void tearDown() {
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(tableName);
        try {
            amazonDynamoDB.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){

        }
    }
    @Test public void query() {
        final SenseEventsDynamoDB senseEventsDynamoDB = new SenseEventsDynamoDB(amazonDynamoDB, tableName);
        final DateTime dateTime = DateTime.now(DateTimeZone.UTC);
        final Long millis = dateTime.getMillis();
        final DeviceEvents deviceEvents = new DeviceEvents("AAA", dateTime, Sets.newHashSet("hello : world"));

        final DateTime dateTime2 = dateTime.plusSeconds(5);
        final DeviceEvents deviceEvents2 = new DeviceEvents("AAA", dateTime2, Sets.newHashSet("hello! : world!"));
        final List<DeviceEvents> eventsList = Lists.newArrayList(deviceEvents, deviceEvents2);
        senseEventsDynamoDB.write(eventsList);

        final List<DeviceEvents> deviceEventsList = senseEventsDynamoDB.get("AAA", dateTime, 1);
        assertThat(deviceEventsList.size(), is(1));

        final List<DeviceEvents> deviceEventsList2 = senseEventsDynamoDB.get("AAA", dateTime2, 1);
        assertThat(deviceEventsList2.size(), is(1));

        final List<String> eventsFromDeviceList1 = Lists.newArrayList(deviceEventsList.get(0).events);
        final List<String> eventsFromDeviceList2 = Lists.newArrayList(deviceEventsList2.get(0).events);
        assertThat(eventsFromDeviceList1.get(0), not(eventsFromDeviceList2.get(0)));
    }

    @Test public void queryMultiple() {
        final SenseEventsDynamoDB senseEventsDynamoDB = new SenseEventsDynamoDB(amazonDynamoDB, tableName);
        final DateTime dateTime = DateTime.now(DateTimeZone.UTC);
        final Long millis = dateTime.getMillis();
        final DeviceEvents deviceEvents = new DeviceEvents("AAA", dateTime, Sets.newHashSet("hello : world"));

        final DateTime dateTime2 = dateTime.plusSeconds(5);
        final DeviceEvents deviceEvents2 = new DeviceEvents("AAA", dateTime2, Sets.newHashSet("hello! : world!"));
        final List<DeviceEvents> eventsList = Lists.newArrayList(deviceEvents, deviceEvents2);
        senseEventsDynamoDB.write(eventsList);

        final List<DeviceEvents> deviceEventsList = senseEventsDynamoDB.get("AAA", dateTime2);
        assertThat(deviceEventsList.size(), is(eventsList.size()));
    }

    @Test public void queryAlarms() {
        final SenseEventsDynamoDB senseEventsDynamoDB = new SenseEventsDynamoDB(amazonDynamoDB, tableName);
        final String senseId = "sense-id";
        final DateTime now = new DateTime(2017,1,19,1,0,0, DateTimeZone.UTC);
        final DeviceEvents deviceEvents = new DeviceEvents(senseId, now, Sets.newHashSet("alarm:ring"));
        senseEventsDynamoDB.write(Lists.newArrayList(deviceEvents));


        final List<DeviceEvents> deviceEventsList = senseEventsDynamoDB.getAlarms(senseId, now.minusMinutes(5), now.plusMinutes(1));
        assertEquals("alarms found", 1, deviceEventsList.size());
    }
}
