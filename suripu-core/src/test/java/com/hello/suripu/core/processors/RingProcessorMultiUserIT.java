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
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillDataDAODynamoDB;
import com.hello.suripu.core.db.ScheduledRingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SmartAlarmLoggerDynamoDB;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmSound;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.util.DateTimeUtil;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by pangwu on 9/24/14.
 */
public class RingProcessorMultiUserIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(RingProcessorMultiUserIT.class);

    private ScheduledRingTimeHistoryDAODynamoDB scheduledRingTimeHistoryDAODynamoDB;
    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = mock(MergedUserInfoDynamoDB.class);
    private final SmartAlarmLoggerDynamoDB smartAlarmLoggerDynamoDB = mock(SmartAlarmLoggerDynamoDB.class);

    private final DeviceDAO deviceDAO = mock(DeviceDAO.class);
    private final PillDataDAODynamoDB pillDataDAODynamoDB = mock(PillDataDAODynamoDB.class);

    private final String testDeviceId = "test morpheus";
    private final List<UserInfo> userInfoList = new ArrayList<>();
    private final String ringTimeTableName = "ringtime_test";

    @Before
    public void setUp(){

        when(this.mergedUserInfoDynamoDB.getInfo(testDeviceId)).thenReturn(userInfoList);

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
            LOGGER.error("Failed parsing CSV");
        }

        final DateTime alarmDeadlineLocalUTC = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.UTC);
        final DateTime dataCollectionTimeLocalUTC = alarmDeadlineLocalUTC.minusMinutes(20);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
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
        this.userInfoList.clear();
        //init();
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(ringTimeTableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            LOGGER.warn("Can not delete non existing table");
        }
    }

    private RingTime updateRingTime(final DateTime dataCollectionTime){
        return RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.smartAlarmLoggerDynamoDB,
                this.pillDataDAODynamoDB,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);
    }


//    @Test
    public void testTwoNonRepeatedAlarmsFromDifferentUsersTransitionByTime(){
        // Test how two alarms from different users at the same day behave when time goes by.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), ""));

        RingTime ringTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList,
                new DateTime(2014, 9, 23, 8, 20, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        userInfoList.add(new UserInfo(testDeviceId, 1L,
                alarmList,
                Optional.of(ringTime),
                Optional.of(DateTimeZone.forID("America/Los_Angeles")),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                0));


        final List<Alarm> alarmList2 = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek2 = new HashSet<Integer>();
        dayOfWeek2.add(DateTimeConstants.TUESDAY);
        alarmList2.add(new Alarm(2014, 9, 23, 8, 30, dayOfWeek2,
                false, true, true, true,
                new AlarmSound(101, "God Save the Queen"), ""));

        final RingTime ringTime2 = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList2,
                new DateTime(2014, 9, 23, 8, 30, 0, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        userInfoList.add(new UserInfo(testDeviceId, 2L,
                alarmList2,
                Optional.of(ringTime2),
                Optional.of(DateTimeZone.forID("America/Los_Angeles")),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                0));

        final URL url = Resources.getResource("fixtures/algorithm/pill_data_09_23_2014_pang.csv");
        final List<TrackerMotion> motions1 = new ArrayList<TrackerMotion>();
        final List<TrackerMotion> motions2 = new ArrayList<TrackerMotion>();

        try {
            final String csvString = Resources.toString(url, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(final String line:lines){
                final String[] columns = line.split("\\t");
                long timestamp = Long.valueOf(columns[0]);
                long value = Long.valueOf(columns[1]);
                int offsetMillis = DateTimeZone.forID("America/Los_Angeles").getOffset(timestamp);
                motions2.add(new TrackerMotion(1L, 2L, 2L, timestamp, (int)value, offsetMillis,0L, 0L, 0L));
                motions1.add(new TrackerMotion(1L, 1L, 1L, timestamp, (int)value, offsetMillis,0L, 0L, 0L));
            }
        }catch (IOException ioe){
            LOGGER.error("Failed parsing CSV");
        }

        final DateTime dataCollectionTimeLocalUTC = new DateTime(2014, 9, 23, 8, 21, 0, 0, DateTimeZone.UTC);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(2, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(motions2));

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(motions1));


        final DateTime dataCollectionTimeLocalUTC1 = new DateTime(2014, 9, 23, 8, 0, 0, 0, DateTimeZone.UTC);
        final DateTime startQueryTimeLocalUTC1 = dataCollectionTimeLocalUTC1.minusHours(8);

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(2, startQueryTimeLocalUTC1, dataCollectionTimeLocalUTC1))
                .thenReturn(ImmutableList.copyOf(motions2));

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(1, startQueryTimeLocalUTC1, dataCollectionTimeLocalUTC1))
                .thenReturn(ImmutableList.copyOf(motions1));



        final DateTime dataCollectionTimeLocalUTC3 = new DateTime(2014, 9, 23, 8, 22, 0, 0, DateTimeZone.UTC);
        final DateTime startQueryTimeLocalUTC3 = dataCollectionTimeLocalUTC3.minusHours(8);

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(2, startQueryTimeLocalUTC3, dataCollectionTimeLocalUTC3))
                .thenReturn(ImmutableList.copyOf(motions2));

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(1, startQueryTimeLocalUTC3, dataCollectionTimeLocalUTC3))
                .thenReturn(ImmutableList.copyOf(motions1));




        final List<DeviceAccountPair> deviceAccountPairs = new ArrayList<DeviceAccountPair>();
        deviceAccountPairs.add(new DeviceAccountPair(1L, 1L, testDeviceId, DateTimeUtil.MORPHEUS_DAY_ONE));
        deviceAccountPairs.add(new DeviceAccountPair(2L, 1L, testDeviceId, DateTimeUtil.MORPHEUS_DAY_ONE));

        when(this.deviceDAO.getAccountIdsForDeviceId(testDeviceId)).thenReturn(ImmutableList.copyOf(deviceAccountPairs));

        DateTime deadline = new DateTime(2014, 9, 23, 8, 20, 0, 0, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, 0, 0, DateTimeZone.forID("America/Los_Angeles"));

        // Minutes before alarm triggered
        ringTime = updateRingTime(new DateTime(2014, 9, 23, 7, 20, 0, 0, DateTimeZone.forID("America/Los_Angeles")));

        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));

        UserInfo userInfo1 = this.userInfoList.get(0);
        this.userInfoList.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));


        // Minute that trigger 1st smart alarm processing
        ringTime = updateRingTime(dataCollectionTime);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(ringTime.processed(), is(true));

        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));

        userInfo1 = this.userInfoList.get(0);
        this.userInfoList.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));


        // Minute that update 2nd alarm processing
        deadline = new DateTime(2014, 9, 23, 8, 30, 0, 0, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = updateRingTime(new DateTime(2014, 9, 23, 8, 21, 0, 0, DateTimeZone.forID("America/Los_Angeles")));

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{101L}));

        UserInfo userInfo2 = this.userInfoList.get(1);
        this.userInfoList.set(1, new UserInfo(userInfo2.deviceId, userInfo2.accountId,
                userInfo2.alarmList,
                Optional.of(ringTime),
                userInfo2.timeZone,
                userInfo2.pillColor,
                0));


        // Minute that trigger 2nd smart alarm processing
        deadline = new DateTime(2014, 9, 23, 8, 30, 0, 0, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = updateRingTime(new DateTime(2014, 9, 23, 8, 22, 0, 0, DateTimeZone.forID("America/Los_Angeles")));

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(ringTime.processed(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{101L}));

        userInfo2 = this.userInfoList.get(1);
        this.userInfoList.set(1, new UserInfo(userInfo2.deviceId, userInfo2.accountId,
                userInfo2.alarmList,
                Optional.of(ringTime),
                userInfo2.timeZone,
                userInfo2.pillColor,
                0));


        // Minutes after smart alarm processing but before next smart alarm process triggered.
        deadline = new DateTime(2014, 9, 24, 9, 20, 0, 0, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = updateRingTime(new DateTime(2014, 9, 24, 7, 20, 0, 0, DateTimeZone.forID("America/Los_Angeles")));


        assertThat(ringTime.isEmpty(), is(true));  // the two alarms are non-repeated.
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[0]));
    }


    @Test
    public void testTwoRepeatedAlarmsFromDifferentUsersTransitionByTime(){
        // Test how two alarms from different users at the same day behave when time goes by.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        // 1st alarm, smart, 2014-09-23 8:20
        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                true, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), ""));

        RingTime ringTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList,
                new DateTime(2014, 9, 23, 8, 20, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        userInfoList.add(new UserInfo(testDeviceId, 1L,
                alarmList,
                Optional.of(ringTime),
                Optional.of(DateTimeZone.forID("America/Los_Angeles")),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                0));


        final List<Alarm> alarmList2 = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek2 = new HashSet<Integer>();
        dayOfWeek2.add(DateTimeConstants.TUESDAY);

        // 1st alarm, smart, 2014-09-23 8:30
        alarmList2.add(new Alarm(2014, 9, 23, 8, 30, dayOfWeek2,
                true, true, true, true,
                new AlarmSound(101, "God Save the Queen"), ""));

        final RingTime ringTime2 = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList2,
                new DateTime(2014, 9, 23, 8, 30, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        userInfoList.add(new UserInfo(testDeviceId, 2L,
                alarmList2,
                Optional.of(ringTime2),
                Optional.of(DateTimeZone.forID("America/Los_Angeles")),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                0));


        final URL url = Resources.getResource("fixtures/algorithm/pill_data_09_23_2014_pang.csv");
        final List<TrackerMotion> motions1 = new ArrayList<TrackerMotion>();
        final List<TrackerMotion> motions2 = new ArrayList<TrackerMotion>();

        try {
            final String csvString = Resources.toString(url, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(final String line:lines){
                final String[] columns = line.split("\\t");
                long timestamp = Long.valueOf(columns[0]);
                long value = Long.valueOf(columns[1]);
                int offsetMillis = DateTimeZone.forID("America/Los_Angeles").getOffset(timestamp);
                motions2.add(new TrackerMotion(1L, 2L, 2L, timestamp, (int)value, offsetMillis,0L, 0L, 0L));
                motions1.add(new TrackerMotion(1L, 1L, 1L, timestamp, (int)value, offsetMillis,0L, 0L, 0L));
            }
        }catch (IOException ioe){
            LOGGER.error("Failed parsing CSV");
        }

        final DateTime dataCollectionTimeLocalUTC = new DateTime(2014, 9, 23, 8, 21, 0, 0, DateTimeZone.UTC);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(2, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(motions2));

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(motions1));


        final DateTime dataCollectionTimeLocalUTC1 = new DateTime(2014, 9, 23, 8, 0, 0, 0, DateTimeZone.UTC);
        final DateTime startQueryTimeLocalUTC1 = dataCollectionTimeLocalUTC1.minusHours(8);

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(2, startQueryTimeLocalUTC1, dataCollectionTimeLocalUTC1))
                .thenReturn(ImmutableList.copyOf(motions2));

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(1, startQueryTimeLocalUTC1, dataCollectionTimeLocalUTC1))
                .thenReturn(ImmutableList.copyOf(motions1));



        final DateTime dataCollectionTimeLocalUTC3 = new DateTime(2014, 9, 23, 8, 22, 0, 0, DateTimeZone.UTC);
        final DateTime startQueryTimeLocalUTC3 = dataCollectionTimeLocalUTC3.minusHours(8);

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(2, startQueryTimeLocalUTC3, dataCollectionTimeLocalUTC3))
                .thenReturn(ImmutableList.copyOf(motions2));

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(1, startQueryTimeLocalUTC3, dataCollectionTimeLocalUTC3))
                .thenReturn(ImmutableList.copyOf(motions1));




        final List<DeviceAccountPair> deviceAccountPairs = new ArrayList<DeviceAccountPair>();
        deviceAccountPairs.add(new DeviceAccountPair(1L, 1L, testDeviceId, DateTimeUtil.MORPHEUS_DAY_ONE));
        deviceAccountPairs.add(new DeviceAccountPair(2L, 1L, testDeviceId, DateTimeUtil.MORPHEUS_DAY_ONE));

        when(this.deviceDAO.getAccountIdsForDeviceId(testDeviceId)).thenReturn(ImmutableList.copyOf(deviceAccountPairs));

        DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, 0, 0, DateTimeZone.forID("America/Los_Angeles"));

        // 1st alarm, smart, 2014-09-23 8:20
        // 2nd alarm, smart, 2014-09-23 8:30
        // Now: 2014-09-23 07:20
        // Minutes before alarm triggered
        ringTime = updateRingTime(new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")));

        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime, is(deadline));
        assertThat(ringTime.processed(), is(false));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));

        UserInfo userInfo1 = this.userInfoList.get(0);
        this.userInfoList.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));


        // 1st alarm, smart, 2014-09-23 8:20
        // 2nd alarm, smart, 2014-09-23 8:30
        // Now: 2014-09-23 8:00
        // Minute that trigger 1st smart alarm processing
        ringTime = updateRingTime(dataCollectionTime);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(ringTime.processed(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));

        userInfo1 = this.userInfoList.get(0);
        this.userInfoList.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));


        // 1st alarm, smart, [actual ring returned above]
        // 2nd alarm, smart, 2014-09-23 8:30
        // Now: [1st alarm's actual ring + 1 minute]
        // Minute 2nd alarm ring time
        deadline = new DateTime(2014, 9, 23, 8, 30, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTimeLocalUTCAfterSmartRing = new DateTime(actualRingTime.getYear(), actualRingTime.getMonthOfYear(),
                actualRingTime.getDayOfMonth(), actualRingTime.getHourOfDay(),
                actualRingTime.getMinuteOfHour() + 1, 0, 0, DateTimeZone.UTC);
        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(2, dataCollectionTimeLocalUTCAfterSmartRing.minusHours(8),
                dataCollectionTimeLocalUTCAfterSmartRing))
                .thenReturn(ImmutableList.copyOf(motions1));

        ringTime = updateRingTime(actualRingTime.plusMinutes(1));
        final DateTime nextSmartRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(nextSmartRingTime.isAfter(deadline), is(false));  // this is NOT empty because we have another user!
        assertThat(nextSmartRingTime.isBefore(actualRingTime.plusMinutes(1)), is(false));
        assertThat(ringTime.processed(), is(ringTime.actualRingTimeUTC != ringTime.expectedRingTimeUTC));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{101L}));


        // 1st alarm, smart, 2014-09-23 8:20 -- past
        // 2nd alarm, smart, 2014-09-23 8:30
        // Now: 2014-09-23 8:21
        // Minute that update 2nd alarm processing
        ringTime = updateRingTime(new DateTime(2014, 9, 23, 8, 21, DateTimeZone.forID("America/Los_Angeles")));

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime, is(deadline));
        assertThat(ringTime.processed(), is(false));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{101L}));

        UserInfo userInfo2 = this.userInfoList.get(1);
        this.userInfoList.set(1, new UserInfo(userInfo2.deviceId, userInfo2.accountId,
                userInfo2.alarmList,
                Optional.of(ringTime),
                userInfo2.timeZone,
                userInfo2.pillColor,
                0));

        // 1st alarm, smart, 2014-09-23 8:20 -- past
        // 2nd alarm, smart, 2014-09-23 8:30
        // Now: 2014-09-23 8:22 -- within 10 minutes bound, do nothing
        // Minute that trigger 2nd smart alarm processing
        deadline = new DateTime(2014, 9, 23, 8, 30, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = updateRingTime(new DateTime(2014, 9, 23, 8, 22, DateTimeZone.forID("America/Los_Angeles")));

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{101L}));

        userInfo2 = this.userInfoList.get(1);
        this.userInfoList.set(1, new UserInfo(userInfo2.deviceId, userInfo2.accountId,
                userInfo2.alarmList,
                Optional.of(ringTime),
                userInfo2.timeZone,
                userInfo2.pillColor,
                0));


        // 1st alarm, smart, 2014-09-23 8:20 -- past
        // 2nd alarm, smart, 2014-09-23 8:30 -- past
        // Now: 2014-09-24 7:20
        // Minutes after smart alarm processing but before next smart alarm process triggered.
        // Since the alarm is only repeated on Tuesday, the next deadline will be next week.
        deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles")).plusWeeks(1);
        ringTime = updateRingTime(new DateTime(2014, 9, 24, 7, 20, DateTimeZone.forID("America/Los_Angeles")));

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));
    }




    @Test
    public void testTwoAlarmsFromDifferentUsersAtSameTimeShouldHaveMultipleSoundIds(){
        // Test how two alarms from different users at the same day behave when time goes by.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                true, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), ""));

        RingTime ringTime1 = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList,
                new DateTime(2014, 9, 23, 8, 20, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        this.userInfoList.add(new UserInfo(testDeviceId, 1L,
                alarmList,
                Optional.of(ringTime1),
                Optional.of(DateTimeZone.forID("America/Los_Angeles")),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                0));

        final List<Alarm> alarmList2 = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek2 = new HashSet<Integer>();
        dayOfWeek2.add(DateTimeConstants.TUESDAY);
        alarmList2.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek2,
                true, true, true, true,
                new AlarmSound(101, "God Save the Queen"), ""));

        RingTime ringTime2 = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList2,
                new DateTime(2014, 9, 23, 8, 20, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        this.userInfoList.add(new UserInfo(testDeviceId, 2L,
                alarmList2,
                Optional.of(ringTime2),
                Optional.of(DateTimeZone.forID("America/Los_Angeles")),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                0));


        RingTime ringTime = RingProcessor.getNextRingTimeFromAlarmTemplateForSense(this.userInfoList,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")));

        final DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime expectRingTime = new DateTime(2014,9,23,8,20, DateTimeZone.forID("America/Los_Angeles"));

        assertThat(actualRingTime, is(expectRingTime));
        assertThat(ringTime.processed(), is(false));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L, 101L}));

    }
}
