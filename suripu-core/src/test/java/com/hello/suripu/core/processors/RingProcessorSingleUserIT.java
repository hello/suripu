package com.hello.suripu.core.processors;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.ScheduledRingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmSound;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by pangwu on 9/24/14.
 */
public class RingProcessorSingleUserIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(RingProcessorSingleUserIT.class);

    private final AlarmDAODynamoDB alarmDAODynamoDB = mock(AlarmDAODynamoDB.class);

    private ScheduledRingTimeHistoryDAODynamoDB scheduledRingTimeHistoryDAODynamoDB;
    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = mock(MergedUserInfoDynamoDB.class);

    private final TrackerMotionDAO trackerMotionDAO = mock(TrackerMotionDAO.class);

    private final String testDeviceId = "test morpheus";
    private final String ringTimeTableName = "ringtime_test2";

    private final List<UserInfo> userInfoList1 = new ArrayList<>();
    private final List<UserInfo> userInfoList2 = new ArrayList<>();


    @Before
    public void setUp(){

        // Preset the test environment
        // In this settings, a user already has a regular alarm set and waiting the smart alarm to be computed.
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                true, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));

        final RingTime ringTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList,
                new DateTime(2014, 9, 23, 8, 0, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );




        userInfoList1.add(new UserInfo(testDeviceId, 1L,
                alarmList,
                Optional.of(ringTime),
                Optional.of(DateTimeZone.forID("America/Los_Angeles")),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                0));

        when(this.mergedUserInfoDynamoDB.getInfo(testDeviceId)).thenReturn(userInfoList1);

        final URL url = Resources.getResource("fixtures/algorithm/pill_data_09_23_2014_pang.csv");
        final List<TrackerMotion> motions = new ArrayList<TrackerMotion>();
        try {
            final String csvString = Resources.toString(url, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(final String line:lines){
                final String[] columns = line.split("\\t");
                long timestamp = Long.valueOf(columns[0]);
                long value = Long.valueOf(columns[1]);
                int offsetMillis = DateTimeZone.forID("America/Los_Angeles").getOffset(timestamp);
                motions.add(new TrackerMotion(1L, 1L, 1L, timestamp, (int)value, offsetMillis, 0L, 0L, 0L));
            }
        }catch (IOException ioe){
            LOGGER.error("Failed parsing CSV: {}", ioe.getMessage());
        }

        final DateTime alarmDeadlineLocalUTC = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.UTC);
        final DateTime dataCollectionTimeLocalUTC = alarmDeadlineLocalUTC.minusMinutes(20);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(motions));

        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        try {
            ScheduledRingTimeHistoryDAODynamoDB.createTable(ringTimeTableName, this.amazonDynamoDBClient);
            this.scheduledRingTimeHistoryDAODynamoDB = new ScheduledRingTimeHistoryDAODynamoDB(
                    this.amazonDynamoDBClient,
                    ringTimeTableName
            );


        }catch (ResourceInUseException rie){
            LOGGER.warn("Can not create existing table");
        }

    }

    @After
    public void cleanUp(){
        this.userInfoList1.clear();
        this.userInfoList2.clear();
        //setUp();
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(ringTimeTableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            LOGGER.warn("Can not delete non existing table");
        }
    }

    @Test
    public void testSmartAlarmOn_09_23_2014_Update(){
        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));

        final RingTime nextRingTime = new RingTime(deadline.getMillis(),
                deadline.getMillis(),
                100,
                true);

        final UserInfo userInfo1 = userInfoList1.get(0);
        userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId, userInfo1.alarmList,
                Optional.of(nextRingTime), userInfo1.timeZone, userInfo1.pillColor,
                0));

        RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);


        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));

        userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId, userInfo1.alarmList,
                Optional.of(ringTime), userInfo1.timeZone, userInfo1.pillColor,
                0));


        // For minute that triggered smart alarm computation
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);


        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(actualRingTime.isAfter(dataCollectionTime), is(true));
        assertThat(ringTime.processed(), is(true));



    }

    @Test
    public void testRegularRepeatedAlarmOn_09_23_2014_Update(){
        // Test scenario when computation get triggered, an ring time from previous alarm settings is set,
        // but user updated his/her next alarm to non-repeated after the last ring was computed.
        // and user's pill has no data.

        final DateTime alarmDeadlineLocalUTC = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.UTC);
        final DateTime dataCollectionTimeLocalUTC = alarmDeadlineLocalUTC.minusMinutes(20);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(Collections.<TrackerMotion>emptyList()));

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));

        // For minutes that not yet trigger smart alarm computation
        RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));

        final UserInfo userInfo1 = userInfoList1.get(0);
        userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId, userInfo1.alarmList,
                Optional.of(ringTime), userInfo1.timeZone, userInfo1.pillColor,
                0));

        // For the minute trigger smart alarm computation
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.getMillis(), is(deadline.getMillis()));
        assertThat(ringTime.processed(), is(false));
    }


    @Test
    public void testNoneRepeatedSmartAlarmOn_09_23_2014_Update(){
        // Test scenario when computation get triggered, an ring time from previous alarm settings is set,
        // but user updated his/her next alarm to non-repeated after the last ring was computed.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));

        when(this.alarmDAODynamoDB.getAlarms(1)).thenReturn(ImmutableList.copyOf(alarmList));

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));

        // For moments that not yet trigger smart alarm computation
        RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));

        final UserInfo userInfo1 = userInfoList1.get(0);
        userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId, userInfo1.alarmList,
                Optional.of(ringTime), userInfo1.timeZone, userInfo1.pillColor,
                0));

        // For moments that triggered smart alarm computation
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(actualRingTime.isAfter(new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"))), is(true));
        assertThat(ringTime.processed(), is(true));
    }


    @Test
    public void testDisabledAlarmOn_09_23_2014_Update(){
        // Test scenario when computation get triggered, an ring time from previous alarm settings is set,
        // but user disabled all his/her alarms after the last ring was computed.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                true, false, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));

        final UserInfo userInfo1 = this.userInfoList1.get(0);
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                alarmList,
                userInfo1.ringTime,
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));


        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);


        assertThat(ringTime.isEmpty(), is(true));
    }


    @Test
    public void testNoAlarmOn_09_23_2014_Update(){
        // Test scenario when computation get triggered, an ring time from previous alarm settings is set,
        // but user clear all his/her alarms after the last ring was computed.
        final UserInfo userInfo1 = this.userInfoList1.get(0);
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                Collections.EMPTY_LIST,
                userInfo1.ringTime,
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));

        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);


        assertThat(ringTime.isEmpty(), is(true));
    }


    @Test
    public void testNoAlarmOn_09_23_2014_Init(){
        // Test scenario when computation get triggered there is no alarm for that device.
        final UserInfo userInfo1 = this.userInfoList1.get(0);
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                Collections.<Alarm>emptyList(),
                Optional.of(RingTime.createEmpty()),
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));


        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);


        assertThat(ringTime.isEmpty(), is(true));
    }


    @Test
    public void testRegularRepeatedAlarmOn_09_23_2014_Init(){
        // Test scenario when computation get triggered all the alarm is repeated and,
        // no alarm is set yet.
        // And pill has no data upload.

        final DateTime alarmDeadlineLocalUTC = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.UTC);
        final DateTime dataCollectionTimeLocalUTC = alarmDeadlineLocalUTC.minusMinutes(20);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(Collections.<TrackerMotion>emptyList()));

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);

        final DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.getMillis(), is(deadline.getMillis()));
        assertThat(ringTime.processed(), is(false));
    }


    @Test
    public void testNoneRepeatedAlarmOn_09_23_2014_InitWithData(){
        // Test scenario when computation get triggered all the alarm is non-repeated and not expired.
        // pill has data.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));

        when(this.alarmDAODynamoDB.getAlarms(1)).thenReturn(ImmutableList.copyOf(alarmList));

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));


        // Minutes before smart alarm triggered.
        RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);


        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));

        // Minutes after smart alarm triggered but before deadline.
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(ringTime.processed(), is(true));
    }

    @Test
    public void testNoneRepeatedAlarmOn_09_23_2014_InitWithNoData(){
        // Test scenario when computation get triggered all the alarm is non-repeated and not expired.
        // pill has no data.
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));

        when(this.alarmDAODynamoDB.getAlarms(1)).thenReturn(ImmutableList.copyOf(alarmList));

        // pill has no data
        final DateTime alarmDeadlineLocalUTC = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.UTC);
        final DateTime dataCollectionTimeLocalUTC = alarmDeadlineLocalUTC.minusMinutes(20);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(Collections.<TrackerMotion>emptyList()));

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);

        final DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));
    }



    @Test
    public void testNoneRepeatedExpiredAlarmOn_09_23_2014_Init(){
        // Test scenario when computation get triggered all the alarm is non-repeated and expired.
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.MONDAY);

        alarmList.add(new Alarm(2014, 9, 22, 8, 20, dayOfWeek,
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));

        final UserInfo userInfo1 = this.userInfoList1.get(0);
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                alarmList,
                userInfo1.ringTime,
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));

        final DateTime deadline = new DateTime(2014, 9, 22, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);

        assertThat(ringTime.isEmpty(), is(true));
    }


    @Test
    public void testNoneRepeatedExpiredAlarmOn_09_23_2014_Update(){
        // Test scenario when computation get triggered, a regular alarm from the previous alarm template set.
        // Then user updates his/her alarm template.
        // After alarm update, all the alarm is non-repeated and expired.
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.MONDAY);

        alarmList.add(new Alarm(2014, 9, 22, 8, 20, dayOfWeek,
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));

        final UserInfo userInfo1 = this.userInfoList1.get(0);
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                alarmList,
                userInfo1.ringTime,
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));

        final DateTime deadline = new DateTime(2014, 9, 22, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);

        assertThat(ringTime.isEmpty(), is(true));
    }


    @Test
    public void testAlarmTransitionByTime(){
        // Test how alarm behave when time goes by.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        // 1st alarm: 2014-09-23 08:20
        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));

        final HashSet<Integer> dayOfWeek2 = new HashSet<Integer>();
        dayOfWeek2.add(DateTimeConstants.WEDNESDAY);

        // 2nd alarm: 2014-09-24 09:20
        alarmList.add(new Alarm(2014, 9, 24, 9, 20, dayOfWeek2,
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));


        UserInfo userInfo1 = this.userInfoList1.get(0);
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                alarmList,
                userInfo1.ringTime,
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));


        DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));

        // 1st alarm: 2014-09-23 08:20
        // 2nd alarm: 2014-09-24 09:20
        // Now: 2014-9-23 07:20
        // Minutes before alarm triggered
        RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));

        userInfo1 = this.userInfoList1.get(0);
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));

        // 1st alarm: 2014-09-23 08:20
        // 2nd alarm: 2014-09-24 09:20
        // Now: 2014-9-23 08:00
        // Minute that trigger smart alarm processing
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(ringTime.processed(), is(true));

        userInfo1 = this.userInfoList1.get(0);
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));

        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 09:20
        // Now: [actual ring time + 1 minute]
        // Minutes after smart alarm processing but before next smart alarm process triggered.
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                actualRingTime.plusMinutes(1),
                20,
                15,
                0.2f,
                null);

        assertThat(ringTime.isEmpty(), is(true));

        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 09:20
        // Now: 2014-9-23 08:21
        // 1st smart alarm expected ring time past, but not yet reach teh processing time of 2nd
        // smart alarm.
        deadline = new DateTime(2014, 9, 24, 9, 20, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 21, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.equals(deadline), is(true));
        assertThat(ringTime.processed(), is(false));
    }



    @Test
    public void testAlarmTransitionByTimeAndUpdateAlarmInSmartAlarmRingTimeGap(){
        // Test how alarm behave when time goes by.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        // 1st alarm: 2014-09-23 08:20
        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));

        final HashSet<Integer> dayOfWeek2 = new HashSet<Integer>();
        dayOfWeek2.add(DateTimeConstants.WEDNESDAY);

        // 2nd alarm: 2014-09-24 09:20
        alarmList.add(new Alarm(2014, 9, 24, 9, 20, dayOfWeek2,
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));


        UserInfo userInfo1 = this.userInfoList1.get(0);
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                alarmList,
                userInfo1.ringTime,
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));


        DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));

        // 1st alarm: 2014-09-23 08:20
        // 2nd alarm: 2014-09-24 09:20
        // Now: 2014-9-23 07:20
        // Minutes before alarm triggered
        RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));

        userInfo1 = this.userInfoList1.get(0);
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));

        // 1st alarm: 2014-09-23 08:20
        // 2nd alarm: 2014-09-24 09:20
        // Now: 2014-9-23 08:00
        // Minute that trigger smart alarm processing
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(ringTime.processed(), is(true));

        userInfo1 = this.userInfoList1.get(0);
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));

        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 09:20
        // Now: [actual ring time + 1 minute]
        // Minutes after smart alarm processing but before next smart alarm process triggered.
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                actualRingTime.plusMinutes(1),
                20,
                15,
                0.2f,
                null);

        assertThat(ringTime.isEmpty(), is(true));


        // And now, the user update his/her alarms!!!
        userInfo1 = this.userInfoList1.get(0);
        userInfo1.alarmList.add(new Alarm(2014, 9, 23, 10, 0, new HashSet<Integer>(), false, true, true, false, null, "id"));
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                userInfo1.ringTime,  // Here we simulate no writing the temporary empty alarm into user info table.
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));

        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 09:20
        // 3rd alarm, smart: 2014-09-23 10:00
        // Now: [actual ring time + 2 minute]
        // Minutes after smart alarm processing but before next smart alarm process triggered.
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                actualRingTime.plusMinutes(2),
                20,
                15,
                0.2f,
                null);

        assertThat(ringTime.isEmpty(), is(true));


        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 09:20
        // 3rd alarm, smart: 2014-09-23 10:00
        // Now: [actual ring time + 2 minute]
        // Minutes after smart alarm processing but before next smart alarm process triggered.
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 21, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.equals(new DateTime(2014, 9, 23, 10, 0, DateTimeZone.forID("America/Los_Angeles"))), is(true));
        assertThat(ringTime.fromSmartAlarm, is(false));


        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 09:20
        // Now: 2014-9-23 08:21
        // 1st smart alarm expected ring time past, but not yet reach the processing time of 2nd
        // smart alarm.
        deadline = new DateTime(2014, 9, 24, 9, 20, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 10, 1, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.equals(deadline), is(true));
        assertThat(ringTime.processed(), is(false));
    }


    @Test
    public void testSmartAlarmAlreadySetOn_09_23_2014_Update(){
        // Test scenario when computation get triggered, a smart alarm in the future is set.
        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));


        final RingTime nextRingTime = new RingTime(deadline.minusMinutes(3).getMillis(),
                deadline.getMillis(),
                100,
                true);

        UserInfo userInfo1 = userInfoList1.get(0);
        userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId, userInfo1.alarmList,
                Optional.of(nextRingTime), userInfo1.timeZone, userInfo1.pillColor,
                0));


        // For moments that not yet trigger smart alarm computation
        RingTime ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);


        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline.minusMinutes(3)), is(true));
        assertThat(ringTime.processed(), is(true));

        userInfo1 = userInfoList1.get(0);
        userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId, userInfo1.alarmList,
                Optional.of(ringTime), userInfo1.timeZone, userInfo1.pillColor,
                0));

        // For moments that triggered smart alarm computation
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);


        assertThat(ringTime.actualRingTimeUTC, is(deadline.minusMinutes(3).getMillis()));
        assertThat(ringTime.processed(), is(true));
    }
}
