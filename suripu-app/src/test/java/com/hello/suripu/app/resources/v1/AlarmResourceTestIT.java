package com.hello.suripu.app.resources.v1;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmSound;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by pangwu on 9/17/14.
 */
public class AlarmResourceTestIT {


    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private AlarmDAODynamoDB alarmDAODynamoDB;

    private AlarmResource alarmResource;
    private final String tableName = "alarm_test";
    private final String alarmInfoTableName = "alarm_info_test";


    private final List<Alarm> validList = new ArrayList<Alarm>();
    private final List<Alarm> tooMuchAlarmList = new ArrayList<Alarm>();
    private final List<Alarm> twoAlarmInADayList = new ArrayList<Alarm>();

    private final AccessToken token = new AccessToken.Builder()
            .withAccountId(1L)
            .withCreatedAt(DateTime.now())
            .withExpiresIn(DateTime.now().plusHours(1).getMillis())
            .withRefreshToken(UUID.randomUUID())
            .withToken(UUID.randomUUID())
            .withScopes(new OAuthScope[]{ OAuthScope.ALARM_READ, OAuthScope.ALARM_WRITE })
            .withAppId(1L)
            .build();


    @Before
    public void setUp(){
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            AlarmDAODynamoDB.createTable(this.tableName, this.amazonDynamoDBClient);
            this.alarmDAODynamoDB = new AlarmDAODynamoDB(
                    this.amazonDynamoDBClient,
                    this.tableName
            );

            MergedAlarmInfoDynamoDB.createTable(this.alarmInfoTableName, this.amazonDynamoDBClient);
            final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB = new MergedAlarmInfoDynamoDB(
                    this.amazonDynamoDBClient,
                    this.alarmInfoTableName
            );


            final DeviceDAO deviceDAO = mock(DeviceDAO.class);
            when(deviceDAO.getDeviceIdFromAccountId(1L)).thenReturn(Optional.of("test morpheus"));

            this.alarmResource = new AlarmResource(this.alarmDAODynamoDB, mergedAlarmInfoDynamoDB, deviceDAO);


        }catch (ResourceInUseException rie){
            rie.printStackTrace();
        }


        // Create an alarm list that contains valid alarms
        final Alarm.Builder builder = new Alarm.Builder();
        final DateTime now = DateTime.now();

        for(int i = 0; i < 7; i++){
            final DateTime targetDate = now.plusDays(i);
            final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
            dayOfWeek.add(targetDate.getDayOfWeek());

            builder.withYear(targetDate.getYear())
                    .withMonth(targetDate.getMonthOfYear())
                    .withDay(targetDate.getDayOfMonth())
                    .withDayOfWeek(dayOfWeek)
                    .withHour(targetDate.getHourOfDay())
                    .withMinute(targetDate.getMinuteOfHour())
                    .withIsRepeated(false)
                    .withIsEnabled(true)
                    .withIsEditable(true)
                    .withAlarmSound(new AlarmSound(1, "La Marseillaise"));

            this.validList.add(builder.build());

        }


        // Create an alarm list that contains 8 alarms
        for(int i = 0; i < 8; i++){
            final DateTime targetDate = now.plusDays(i);
            final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
            dayOfWeek.add(targetDate.getDayOfWeek());

            builder.withYear(targetDate.getYear())
                    .withMonth(targetDate.getMonthOfYear())
                    .withDay(targetDate.getDayOfMonth())
                    .withDayOfWeek(dayOfWeek)
                    .withHour(targetDate.getHourOfDay())
                    .withMinute(targetDate.getMinuteOfHour())
                    .withIsRepeated(false)
                    .withIsEnabled(true)
                    .withIsEditable(true)
                    .withAlarmSound(new AlarmSound(1, "La Marseillaise"));

            this.tooMuchAlarmList.add(builder.build());

        }


        // Create an alarm list that contains 2 alarms in the same day
        for(int i = 0; i < 2; i++){
            final DateTime targetDate = now.withTimeAtStartOfDay().plusHours(i);
            final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
            dayOfWeek.add(targetDate.getDayOfWeek());

            builder.withYear(targetDate.getYear())
                    .withMonth(targetDate.getMonthOfYear())
                    .withDay(targetDate.getDayOfMonth())
                    .withDayOfWeek(dayOfWeek)
                    .withHour(targetDate.getHourOfDay())
                    .withMinute(targetDate.getMinuteOfHour())
                    .withIsRepeated(false)
                    .withIsEnabled(true)
                    .withIsEditable(true)
                    .withAlarmSound(new AlarmSound(1, "La Marseillaise"));

            this.twoAlarmInADayList.add(builder.build());

        }
    }


    @After
    public void cleanUp(){

        try {
            this.amazonDynamoDBClient.deleteTable(new DeleteTableRequest()
                    .withTableName(this.tableName));


        }catch (ResourceNotFoundException ex){
            ex.printStackTrace();
        }

        try {
            this.amazonDynamoDBClient.deleteTable(new DeleteTableRequest()
                    .withTableName(this.alarmInfoTableName));


        }catch (ResourceNotFoundException ex){
            ex.printStackTrace();
        }

        this.validList.clear();
        this.tooMuchAlarmList.clear();
        this.twoAlarmInADayList.clear();
    }


    @Test
    public void testSetAndGetValidAlarm(){
        this.alarmResource.setAlarms(this.token, DateTime.now().getMillis(), this.validList);
        final List<Alarm> actual = this.alarmResource.getAlarms(this.token);
        final List<Alarm> expected = this.validList;

        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @Test(expected = WebApplicationException.class)
    public void testSetTooMuchAlarm(){
        this.alarmResource.setAlarms(this.token, DateTime.now().getMillis(), this.tooMuchAlarmList);
    }

    @Test(expected = WebApplicationException.class)
    public void testSetTwoAlarmInADay(){
        this.alarmResource.setAlarms(this.token, DateTime.now().getMillis(), this.twoAlarmInADayList);
    }

    @Test(expected = WebApplicationException.class)
    public void testInvalidClientTime(){
        this.alarmResource.setAlarms(this.token, DateTime.now().minusMinutes(2).getMillis(), this.validList);
    }

}
