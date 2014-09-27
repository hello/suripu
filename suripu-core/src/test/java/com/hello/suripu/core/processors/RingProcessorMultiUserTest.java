package com.hello.suripu.core.processors;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmInfo;
import com.hello.suripu.core.models.AlarmSound;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

    private final AlarmDAODynamoDB alarmDAODynamoDB = mock(AlarmDAODynamoDB.class);
    private final TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB = mock(TimeZoneHistoryDAODynamoDB.class);
    private final RingTimeDAODynamoDB ringTimeDAODynamoDB = mock(RingTimeDAODynamoDB.class);
    private final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB = mock(MergedAlarmInfoDynamoDB.class);

    private final DeviceDAO deviceDAO = mock(DeviceDAO.class);
    private final TrackerMotionDAO trackerMotionDAO = mock(TrackerMotionDAO.class);

    private final String testDeviceId = "test morpheus";
    private final List<AlarmInfo> alarmInfoList = new ArrayList<>();


    @Before
    public void setUp(){

        when(this.mergedAlarmInfoDynamoDB.getInfo(testDeviceId)).thenReturn(alarmInfoList);

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
                motions.add(new TrackerMotion(1L, 1L, 1L, timestamp, (int)value, offsetMillis));
            }
        }catch (IOException ioe){
            ioe.printStackTrace();
        }

        final DateTime alarmDeadlineLocalUTC = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.UTC);
        final DateTime dataCollectionTimeLocalUTC = alarmDeadlineLocalUTC.minusMinutes(20);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(motions));

    }

    @After
    public void cleanUp(){
        this.alarmInfoList.clear();
        setUp();
    }




    @Test
    public void testTwoNonRepeatedAlarmsFromDifferentUsersTransitionByTime(){
        // Test how two alarms from different users at the same day behave when time goes by.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true,
                new AlarmSound(100, "The Star Spangled Banner")));

        RingTime ringTime = Alarm.Utils.getNextRingTime(alarmList,
                new DateTime(2014, 9, 23, 8, 20, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        alarmInfoList.add(new AlarmInfo(testDeviceId, 1L,
                Optional.of(alarmList),
                Optional.of(ringTime),
                Optional.of(DateTimeZone.forID("America/Los_Angeles"))));


        final List<Alarm> alarmList2 = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek2 = new HashSet<Integer>();
        dayOfWeek2.add(DateTimeConstants.TUESDAY);
        alarmList2.add(new Alarm(2014, 9, 23, 8, 30, dayOfWeek2,
                false, true, true,
                new AlarmSound(101, "God Save the Queen")));

        final RingTime ringTime2 = Alarm.Utils.getNextRingTime(alarmList2,
                new DateTime(2014, 9, 23, 8, 30, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        alarmInfoList.add(new AlarmInfo(testDeviceId, 2L,
                Optional.of(alarmList2),
                Optional.of(ringTime2),
                Optional.of(DateTimeZone.forID("America/Los_Angeles"))));

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
                motions2.add(new TrackerMotion(1L, 2L, 2L, timestamp, (int)value, offsetMillis));
                motions1.add(new TrackerMotion(1L, 1L, 1L, timestamp, (int)value, offsetMillis));
            }
        }catch (IOException ioe){
            ioe.printStackTrace();
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
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f);

        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.isRegular(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));

        AlarmInfo alarmInfo1 = this.alarmInfoList.get(0);
        this.alarmInfoList.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId,
                alarmInfo1.alarmList,
                Optional.of(ringTime),
                alarmInfo1.timeZone));


        // Minute that trigger 1st smart alarm processing
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(ringTime.isSmart(), is(true));

        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));

        alarmInfo1 = this.alarmInfoList.get(0);
        this.alarmInfoList.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId,
                alarmInfo1.alarmList,
                Optional.of(ringTime),
                alarmInfo1.timeZone));


        // Minute that update 2nd alarm processing
        deadline = new DateTime(2014, 9, 23, 8, 30, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 21, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.isRegular(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{101L}));

        AlarmInfo alarmInfo2 = this.alarmInfoList.get(1);
        this.alarmInfoList.set(1, new AlarmInfo(alarmInfo2.deviceId, alarmInfo2.accountId,
                alarmInfo2.alarmList,
                Optional.of(ringTime),
                alarmInfo2.timeZone));


        // Minute that trigger 2nd smart alarm processing
        deadline = new DateTime(2014, 9, 23, 8, 30, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 22, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(ringTime.isSmart(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{101L}));

        alarmInfo2 = this.alarmInfoList.get(1);
        this.alarmInfoList.set(1, new AlarmInfo(alarmInfo2.deviceId, alarmInfo2.accountId,
                alarmInfo2.alarmList,
                Optional.of(ringTime),
                alarmInfo2.timeZone));


        // Minutes after smart alarm processing but before next smart alarm process triggered.
        deadline = new DateTime(2014, 9, 24, 9, 20, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 24, 7, 20, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f);


        assertThat(ringTime.isEmpty(), is(true));  // the two alarms are non-repeated.
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[0]));
    }


    @Test
    public void testTwoRepeatedAlarmsFromDifferentUsersTransitionByTime(){
        // Test how two alarms from different users at the same day behave when time goes by.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                true, true, true,
                new AlarmSound(100, "The Star Spangled Banner")));

        RingTime ringTime = Alarm.Utils.getNextRingTime(alarmList,
                new DateTime(2014, 9, 23, 8, 20, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        alarmInfoList.add(new AlarmInfo(testDeviceId, 1L,
                Optional.of(alarmList),
                Optional.of(ringTime),
                Optional.of(DateTimeZone.forID("America/Los_Angeles"))));


        final List<Alarm> alarmList2 = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek2 = new HashSet<Integer>();
        dayOfWeek2.add(DateTimeConstants.TUESDAY);
        alarmList2.add(new Alarm(2014, 9, 23, 8, 30, dayOfWeek2,
                true, true, true,
                new AlarmSound(101, "God Save the Queen")));

        final RingTime ringTime2 = Alarm.Utils.getNextRingTime(alarmList2,
                new DateTime(2014, 9, 23, 8, 30, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        alarmInfoList.add(new AlarmInfo(testDeviceId, 2L,
                Optional.of(alarmList2),
                Optional.of(ringTime2),
                Optional.of(DateTimeZone.forID("America/Los_Angeles"))));


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
                motions2.add(new TrackerMotion(1L, 2L, 2L, timestamp, (int)value, offsetMillis));
                motions1.add(new TrackerMotion(1L, 1L, 1L, timestamp, (int)value, offsetMillis));
            }
        }catch (IOException ioe){
            ioe.printStackTrace();
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
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f);

        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.isRegular(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));

        AlarmInfo alarmInfo1 = this.alarmInfoList.get(0);
        this.alarmInfoList.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId,
                alarmInfo1.alarmList,
                Optional.of(ringTime),
                alarmInfo1.timeZone));


        // Minute that trigger 1st smart alarm processing
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(ringTime.isSmart(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));

        alarmInfo1 = this.alarmInfoList.get(0);
        this.alarmInfoList.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId,
                alarmInfo1.alarmList,
                Optional.of(ringTime),
                alarmInfo1.timeZone));

        // Minute that update 2nd alarm processing
        deadline = new DateTime(2014, 9, 23, 8, 30, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 21, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.isRegular(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{101L}));

        AlarmInfo alarmInfo2 = this.alarmInfoList.get(1);
        this.alarmInfoList.set(1, new AlarmInfo(alarmInfo2.deviceId, alarmInfo2.accountId,
                alarmInfo2.alarmList,
                Optional.of(ringTime),
                alarmInfo2.timeZone));

        // Minute that trigger 2nd smart alarm processing
        deadline = new DateTime(2014, 9, 23, 8, 30, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 22, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(ringTime.isSmart(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{101L}));

        alarmInfo2 = this.alarmInfoList.get(1);
        this.alarmInfoList.set(1, new AlarmInfo(alarmInfo2.deviceId, alarmInfo2.accountId,
                alarmInfo2.alarmList,
                Optional.of(ringTime),
                alarmInfo2.timeZone));


        // Minutes after smart alarm processing but before next smart alarm process triggered.
        // Since the alarm is only repeated on Tuesday, the next deadline will be next week.
        deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles")).plusWeeks(1);
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 24, 7, 20, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.isRegular(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L}));
    }




    @Test
    public void testTwoAlarmsFromDifferentUsersAtSameTimeShouldHaveMultipleSoundIds(){
        // Test how two alarms from different users at the same day behave when time goes by.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                true, true, true,
                new AlarmSound(100, "The Star Spangled Banner")));

        RingTime ringTime1 = Alarm.Utils.getNextRingTime(alarmList,
                new DateTime(2014, 9, 23, 8, 20, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        this.alarmInfoList.add(new AlarmInfo(testDeviceId, 1L,
                Optional.of(alarmList),
                Optional.of(ringTime1),
                Optional.of(DateTimeZone.forID("America/Los_Angeles"))));

        final List<Alarm> alarmList2 = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek2 = new HashSet<Integer>();
        dayOfWeek2.add(DateTimeConstants.TUESDAY);
        alarmList2.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek2,
                true, true, true,
                new AlarmSound(101, "God Save the Queen")));

        RingTime ringTime2 = Alarm.Utils.getNextRingTime(alarmList2,
                new DateTime(2014, 9, 23, 8, 20, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );
        this.alarmInfoList.add(new AlarmInfo(testDeviceId, 2L,
                Optional.of(alarmList2),
                Optional.of(ringTime2),
                Optional.of(DateTimeZone.forID("America/Los_Angeles"))));


        RingTime ringTime = RingProcessor.getNextRegularRingTime(this.mergedAlarmInfoDynamoDB,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")));

        final DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime expectRingTime = new DateTime(2014,9,23,8,20, DateTimeZone.forID("America/Los_Angeles"));

        assertThat(actualRingTime, is(expectRingTime));
        assertThat(ringTime.isRegular(), is(true));
        assertThat(Arrays.asList(ringTime.soundIds), containsInAnyOrder(new long[]{100L, 101L}));

    }
}
