package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmSound;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;


/**
 * Created by pangwu on 9/16/14.
 */
public class AlarmDAODynamoDBIT {

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private AlarmDAODynamoDB alarmDAODynamoDB;
    private final String tableName = "alarm_test";

    @Before
    public void setUp(){
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            AlarmDAODynamoDB.createTable(tableName, this.amazonDynamoDBClient);
            this.alarmDAODynamoDB = new AlarmDAODynamoDB(
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
    public void testGetEmptyAlarmList(){
        long accountId = 1;
        final ImmutableList<Alarm> actual = this.alarmDAODynamoDB.getAlarms(accountId);
        final ImmutableList<Alarm> expected = ImmutableList.copyOf(Collections.EMPTY_LIST);
        assertThat(expected, containsInAnyOrder(actual.toArray()));
        assertThat(expected.size(), is(actual.size()));
    }

    @Test
    public void testSetEmptyAlarmList(){
        long accountId = 1;
        this.alarmDAODynamoDB.setAlarms(accountId, new ArrayList<Alarm>());
        final ImmutableList<Alarm> expected = ImmutableList.copyOf(Collections.EMPTY_LIST);

        final ImmutableList<Alarm> actual = this.alarmDAODynamoDB.getAlarms(accountId);
        assertThat(expected, containsInAnyOrder(actual.toArray()));
        assertThat(expected.size(), is(actual.size()));
    }


    @Test
    public void testSetValidAlarmList(){
        long accountId = 1;
        final List<Alarm> expected = new ArrayList<Alarm>();

        final DateTime now = DateTime.now();

        final Alarm.Builder builder = new Alarm.Builder();

        HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.MONDAY);

        builder.withYear(2014)
                .withMonth(9)
                .withDay(15)
                .withDayOfWeek(dayOfWeek)
                .withHour(0)
                .withMinute(1)
                .withIsRepeated(true)
                .withIsEnabled(true)
                .withIsEditable(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        expected.add(builder.build());

        dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        builder.withYear(2014)
                .withMonth(9)
                .withDay(16)
                .withDayOfWeek(dayOfWeek)
                .withHour(0)
                .withMinute(1)
                .withIsRepeated(false)
                .withIsEnabled(true)
                .withIsEditable(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));

        expected.add(builder.build());

        this.alarmDAODynamoDB.setAlarms(accountId, expected);
        final ImmutableList<Alarm> actual = this.alarmDAODynamoDB.getAlarms(accountId);
        assertThat(expected, containsInAnyOrder(actual.toArray()));
        assertThat(actual.size(), is(expected.size()));
    }

    @Test(expected = RuntimeException.class)
    public void testSetTooMuchAlarms(){
        long accountId = 1;
        final List<Alarm> expected = new ArrayList<Alarm>();
        final Alarm.Builder builder = new Alarm.Builder();

        for(int i = 0; i < AlarmDAODynamoDB.MAX_ALARM_COUNT + 1; i++) {
            HashSet<Integer> dayOfWeek = new HashSet<Integer>();
            dayOfWeek.add((DateTimeConstants.MONDAY + i) % 7);

            builder.withYear(2014)
                    .withMonth(9)
                    .withDay(14)
                    .withDayOfWeek(dayOfWeek)
                    .withHour(1)
                    .withMinute(1)
                    .withIsRepeated(false)
                    .withIsEnabled(true)
                    .withIsEditable(true)
                    .withAlarmSound(new AlarmSound(1, "god save the queen"));

            expected.add(builder.build());
        }

        this.alarmDAODynamoDB.setAlarms(accountId, expected);
    }


    @Test(expected = RuntimeException.class)
    public void testTwoAlarmsInOneDay(){
        long accountId = 1;
        final List<Alarm> expected = new ArrayList<Alarm>();

        final Alarm.Builder builder = new Alarm.Builder();
        final DateTime now = DateTime.now();

        HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(now.getDayOfWeek());

        builder.withYear(now.getYear())
                .withMonth(now.getMonthOfYear())
                .withDay(now.getDayOfMonth())
                .withDayOfWeek(dayOfWeek)
                .withHour(1)
                .withMinute(1)
                .withIsRepeated(false)
                .withIsEnabled(true)
                .withIsEditable(true)
                .withAlarmSound(new AlarmSound(1, "god save the queen"));


        expected.add(builder.build());

        builder.withHour(2);

        expected.add(builder.build());
        this.alarmDAODynamoDB.setAlarms(accountId, expected);
    }


}
