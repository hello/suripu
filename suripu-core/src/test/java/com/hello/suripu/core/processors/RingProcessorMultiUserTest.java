package com.hello.suripu.core.processors;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmSound;
import com.hello.suripu.core.models.DeviceAccountPair;
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
public class RingProcessorMultiUserTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(RingProcessorMultiUserTest.class);

    private final RingTimeDAODynamoDB ringTimeDAODynamoDB = mock(RingTimeDAODynamoDB.class);
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = mock(MergedUserInfoDynamoDB.class);

    private final DeviceDAO deviceDAO = mock(DeviceDAO.class);
    private final TrackerMotionDAO trackerMotionDAO = mock(TrackerMotionDAO.class);

    private final String testDeviceId = "test morpheus";
    private final List<UserInfo> userInfoList = new ArrayList<>();


    @Before
    public void setUp(){

        when(this.mergedUserInfoDynamoDB.getInfo(testDeviceId)).thenReturn(userInfoList);

        final URL url = Resources.getResource("pill_data_09_23_2014_pang.csv");
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

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(motions));

    }

    @After
    public void cleanUp(){
        this.userInfoList.clear();
        setUp();
    }




//    @Test
    public void testTwoNonRepeatedAlarmsFromDifferentUsersTransitionByTime(){
        // Test how two alarms from different users at the same day behave when time goes by.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner")));

        RingTime ringTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList,
                new DateTime(2014, 9, 23, 8, 20, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        userInfoList.add(new UserInfo(testDeviceId, 1L,
                alarmList,
                Optional.of(ringTime),
                Optional.of(DateTimeZone.forID("America/Los_Angeles")),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent()));


        final List<Alarm> alarmList2 = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek2 = new HashSet<Integer>();
        dayOfWeek2.add(DateTimeConstants.TUESDAY);
        alarmList2.add(new Alarm(2014, 9, 23, 8, 30, dayOfWeek2,
                false, true, true, true,
                new AlarmSound(101, "God Save the Queen")));

        final RingTime ringTime2 = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList2,
                new DateTime(2014, 9, 23, 8, 30, 0, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        userInfoList.add(new UserInfo(testDeviceId, 2L,
                alarmList2,
                Optional.of(ringTime2),
                Optional.of(DateTimeZone.forID("America/Los_Angeles")),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent()));

        final URL url = Resources.getResource("pill_data_09_23_2014_pang.csv");
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

        when(this.trackerMotionDAO.getBetweenLocalUTC(2, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(motions2));

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(motions1));


        final DateTime dataCollectionTimeLocalUTC1 = new DateTime(2014, 9, 23, 8, 0, 0, 0, DateTimeZone.UTC);
        final DateTime startQueryTimeLocalUTC1 = dataCollectionTimeLocalUTC1.minusHours(8);

        when(this.trackerMotionDAO.getBetweenLocalUTC(2, startQueryTimeLocalUTC1, dataCollectionTimeLocalUTC1))
                .thenReturn(ImmutableList.copyOf(motions2));

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC1, dataCollectionTimeLocalUTC1))
                .thenReturn(ImmutableList.copyOf(motions1));



        final DateTime dataCollectionTimeLocalUTC3 = new DateTime(2014, 9, 23, 8, 22, 0, 0, DateTimeZone.UTC);
        final DateTime startQueryTimeLocalUTC3 = dataCollectionTimeLocalUTC3.minusHours(8);

        when(this.trackerMotionDAO.getBetweenLocalUTC(2, startQueryTimeLocalUTC3, dataCollectionTimeLocalUTC3))
                .thenReturn(ImmutableList.copyOf(motions2));

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC3, dataCollectionTimeLocalUTC3))
                .thenReturn(ImmutableList.copyOf(motions1));




        final List<DeviceAccountPair> deviceAccountPairs = new ArrayList<DeviceAccountPair>();
        deviceAccountPairs.add(new DeviceAccountPair(1L, 1L, testDeviceId));
        deviceAccountPairs.add(new DeviceAccountPair(2L, 1L, testDeviceId));

        when(this.deviceDAO.getAccountIdsForDeviceId(testDeviceId)).thenReturn(ImmutableList.copyOf(deviceAccountPairs));

        DateTime deadline = new DateTime(2014, 9, 23, 8, 20, 0, 0, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, 0, 0, DateTimeZone.forID("America/Los_Angeles"));

        // Minutes before alarm triggered
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 20, 0, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));

        UserInfo userInfo1 = this.userInfoList.get(0);
        this.userInfoList.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor));


        // Minute that trigger 1st smart alarm processing
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.ringTimeDAODynamoDB,
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

        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));

        userInfo1 = this.userInfoList.get(0);
        this.userInfoList.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor));


        // Minute that update 2nd alarm processing
        deadline = new DateTime(2014, 9, 23, 8, 30, 0, 0, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 21, 0, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{101L}));

        UserInfo userInfo2 = this.userInfoList.get(1);
        this.userInfoList.set(1, new UserInfo(userInfo2.deviceId, userInfo2.accountId,
                userInfo2.alarmList,
                Optional.of(ringTime),
                userInfo2.timeZone,
                userInfo2.pillColor));


        // Minute that trigger 2nd smart alarm processing
        deadline = new DateTime(2014, 9, 23, 8, 30, 0, 0, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 22, 0, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(ringTime.processed(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{101L}));

        userInfo2 = this.userInfoList.get(1);
        this.userInfoList.set(1, new UserInfo(userInfo2.deviceId, userInfo2.accountId,
                userInfo2.alarmList,
                Optional.of(ringTime),
                userInfo2.timeZone,
                userInfo2.pillColor));


        // Minutes after smart alarm processing but before next smart alarm process triggered.
        deadline = new DateTime(2014, 9, 24, 9, 20, 0, 0, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 24, 7, 20, 0, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);


        assertThat(ringTime.isEmpty(), is(true));  // the two alarms are non-repeated.
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[0]));
    }


//    @Test
    public void testTwoRepeatedAlarmsFromDifferentUsersTransitionByTime(){
        // Test how two alarms from different users at the same day behave when time goes by.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                true, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner")));

        RingTime ringTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList,
                new DateTime(2014, 9, 23, 8, 20, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        userInfoList.add(new UserInfo(testDeviceId, 1L,
                alarmList,
                Optional.of(ringTime),
                Optional.of(DateTimeZone.forID("America/Los_Angeles")),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent()));


        final List<Alarm> alarmList2 = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek2 = new HashSet<Integer>();
        dayOfWeek2.add(DateTimeConstants.TUESDAY);
        alarmList2.add(new Alarm(2014, 9, 23, 8, 30, dayOfWeek2,
                true, true, true, true,
                new AlarmSound(101, "God Save the Queen")));

        final RingTime ringTime2 = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList2,
                new DateTime(2014, 9, 23, 8, 30, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        userInfoList.add(new UserInfo(testDeviceId, 2L,
                alarmList2,
                Optional.of(ringTime2),
                Optional.of(DateTimeZone.forID("America/Los_Angeles")),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent()));


        final URL url = Resources.getResource("pill_data_09_23_2014_pang.csv");
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

        final DateTime dataCollectionTimeLocalUTC = new DateTime(2014, 9, 23, 8, 21, DateTimeZone.UTC);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.trackerMotionDAO.getBetweenLocalUTC(2, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(motions2));

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(motions1));


        final DateTime dataCollectionTimeLocalUTC1 = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.UTC);
        final DateTime startQueryTimeLocalUTC1 = dataCollectionTimeLocalUTC1.minusHours(8);

        when(this.trackerMotionDAO.getBetweenLocalUTC(2, startQueryTimeLocalUTC1, dataCollectionTimeLocalUTC1))
                .thenReturn(ImmutableList.copyOf(motions2));

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC1, dataCollectionTimeLocalUTC1))
                .thenReturn(ImmutableList.copyOf(motions1));



        final DateTime dataCollectionTimeLocalUTC3 = new DateTime(2014, 9, 23, 8, 22, DateTimeZone.UTC);
        final DateTime startQueryTimeLocalUTC3 = dataCollectionTimeLocalUTC3.minusHours(8);

        when(this.trackerMotionDAO.getBetweenLocalUTC(2, startQueryTimeLocalUTC3, dataCollectionTimeLocalUTC3))
                .thenReturn(ImmutableList.copyOf(motions2));

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC3, dataCollectionTimeLocalUTC3))
                .thenReturn(ImmutableList.copyOf(motions1));




        final List<DeviceAccountPair> deviceAccountPairs = new ArrayList<DeviceAccountPair>();
        deviceAccountPairs.add(new DeviceAccountPair(1L, 1L, testDeviceId));
        deviceAccountPairs.add(new DeviceAccountPair(2L, 1L, testDeviceId));

        when(this.deviceDAO.getAccountIdsForDeviceId(testDeviceId)).thenReturn(ImmutableList.copyOf(deviceAccountPairs));

        DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));

        // Minutes before alarm triggered
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.ringTimeDAODynamoDB,
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
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));

        UserInfo userInfo1 = this.userInfoList.get(0);
        this.userInfoList.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor));


        // Minute that trigger 1st smart alarm processing
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.ringTimeDAODynamoDB,
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
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));

        userInfo1 = this.userInfoList.get(0);
        this.userInfoList.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor));

        // Minute that update 2nd alarm processing
        deadline = new DateTime(2014, 9, 23, 8, 30, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 21, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{101L}));

        UserInfo userInfo2 = this.userInfoList.get(1);
        this.userInfoList.set(1, new UserInfo(userInfo2.deviceId, userInfo2.accountId,
                userInfo2.alarmList,
                Optional.of(ringTime),
                userInfo2.timeZone,
                userInfo2.pillColor));

        // Minute that trigger 2nd smart alarm processing
        deadline = new DateTime(2014, 9, 23, 8, 30, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 22, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(ringTime.processed(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{101L}));

        userInfo2 = this.userInfoList.get(1);
        this.userInfoList.set(1, new UserInfo(userInfo2.deviceId, userInfo2.accountId,
                userInfo2.alarmList,
                Optional.of(ringTime),
                userInfo2.timeZone,
                userInfo2.pillColor));


        // Minutes after smart alarm processing but before next smart alarm process triggered.
        // Since the alarm is only repeated on Tuesday, the next deadline will be next week.
        deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles")).plusWeeks(1);
        ringTime = RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 24, 7, 20, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(true));
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
                new AlarmSound(100, "The Star Spangled Banner")));

        RingTime ringTime1 = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList,
                new DateTime(2014, 9, 23, 8, 20, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        this.userInfoList.add(new UserInfo(testDeviceId, 1L,
                alarmList,
                Optional.of(ringTime1),
                Optional.of(DateTimeZone.forID("America/Los_Angeles")),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent()));

        final List<Alarm> alarmList2 = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek2 = new HashSet<Integer>();
        dayOfWeek2.add(DateTimeConstants.TUESDAY);
        alarmList2.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek2,
                true, true, true, true,
                new AlarmSound(101, "God Save the Queen")));

        RingTime ringTime2 = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(alarmList2,
                new DateTime(2014, 9, 23, 8, 20, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        this.userInfoList.add(new UserInfo(testDeviceId, 2L,
                alarmList2,
                Optional.of(ringTime2),
                Optional.of(DateTimeZone.forID("America/Los_Angeles")),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent()));


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
