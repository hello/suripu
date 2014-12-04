package com.hello.suripu.core.processors;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.MergedAlarmInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmInfo;
import com.hello.suripu.core.models.AlarmSound;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.TrackerMotion;
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
public class RingProcessorSingleUserTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(RingProcessorSingleUserTest.class);

    private final AlarmDAODynamoDB alarmDAODynamoDB = mock(AlarmDAODynamoDB.class);

    private final RingTimeDAODynamoDB ringTimeDAODynamoDB = mock(RingTimeDAODynamoDB.class);
    private final MergedAlarmInfoDynamoDB mergedAlarmInfoDynamoDB = mock(MergedAlarmInfoDynamoDB.class);

    private final TrackerMotionDAO trackerMotionDAO = mock(TrackerMotionDAO.class);

    private final String testDeviceId = "test morpheus";
    private final List<AlarmInfo> alarmInfoList1 = new ArrayList<>();
    private final List<AlarmInfo> alarmInfoList2 = new ArrayList<>();


    @Before
    public void setUp(){

        // Preset the test environment
        // In this settings, a user already has a regular alarm set and waiting the smart alarm to be computed.
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                true, true, true,
                new AlarmSound(100, "The Star Spangled Banner")));

        final RingTime ringTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplates(alarmList,
                new DateTime(2014, 9, 23, 8, 0, 0, DateTimeZone.forID("America/Los_Angeles")).getMillis(),
                DateTimeZone.forID("America/Los_Angeles")
        );




        alarmInfoList1.add(new AlarmInfo(testDeviceId, 1L, alarmList, Optional.of(ringTime), Optional.of(DateTimeZone.forID("America/Los_Angeles"))));

        when(this.mergedAlarmInfoDynamoDB.getInfo(testDeviceId)).thenReturn(alarmInfoList1);

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
            LOGGER.error("Failed parsing CSV: {}", ioe.getMessage());
        }

        final DateTime alarmDeadlineLocalUTC = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.UTC);
        final DateTime dataCollectionTimeLocalUTC = alarmDeadlineLocalUTC.minusMinutes(20);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(motions));

        when(this.ringTimeDAODynamoDB.getNextRingTime(testDeviceId))
                .thenReturn(RingTime.createEmpty());

    }

    @After
    public void cleanUp(){
        this.alarmInfoList1.clear();
        this.alarmInfoList2.clear();
        setUp();
    }

    @Test
    public void testSmartAlarmOn_09_23_2014_Update(){
        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));

        final RingTime nextRingTime = new RingTime(deadline.getMillis(),
                deadline.getMillis(),
                100);

        final AlarmInfo alarmInfo1 = alarmInfoList1.get(0);
        alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId, alarmInfo1.alarmList,
                Optional.of(nextRingTime), alarmInfo1.timeZone));

        RingTime ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);


        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.isSmart(), is(false));

        alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId, alarmInfo1.alarmList,
                Optional.of(ringTime), alarmInfo1.timeZone));


        // For minute that triggered smart alarm computation
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
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
        assertThat(ringTime.isSmart(), is(true));



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
        RingTime ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
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
        assertThat(ringTime.isRegular(), is(true));

        final AlarmInfo alarmInfo1 = alarmInfoList1.get(0);
        alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId, alarmInfo1.alarmList,
                Optional.of(ringTime), alarmInfo1.timeZone));

        // For the minute trigger smart alarm computation
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.getMillis(), is(deadline.getMillis()));
        assertThat(ringTime.isRegular(), is(true));
    }


    @Test
    public void testNoneRepeatedSmartAlarmOn_09_23_2014_Update(){
        // Test scenario when computation get triggered, an ring time from previous alarm settings is set,
        // but user updated his/her next alarm to non-repeated after the last ring was computed.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true,
                new AlarmSound(100, "The Star Spangled Banner")));

        when(this.alarmDAODynamoDB.getAlarms(1)).thenReturn(ImmutableList.copyOf(alarmList));

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));

        // For moments that not yet trigger smart alarm computation
        RingTime ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
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
        assertThat(ringTime.isSmart(), is(false));

        final AlarmInfo alarmInfo1 = alarmInfoList1.get(0);
        alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId, alarmInfo1.alarmList,
                Optional.of(ringTime), alarmInfo1.timeZone));

        // For moments that triggered smart alarm computation
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
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
        assertThat(ringTime.isSmart(), is(true));
    }


    @Test
    public void testDisabledAlarmOn_09_23_2014_Update(){
        // Test scenario when computation get triggered, an ring time from previous alarm settings is set,
        // but user disabled all his/her alarms after the last ring was computed.

        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                true, false, true,
                new AlarmSound(100, "The Star Spangled Banner")));

        final AlarmInfo alarmInfo1 = this.alarmInfoList1.get(0);
        this.alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId,
                alarmList,
                alarmInfo1.ringTime,
                alarmInfo1.timeZone));


        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
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
        final AlarmInfo alarmInfo1 = this.alarmInfoList1.get(0);
        this.alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId,
                Collections.EMPTY_LIST,
                alarmInfo1.ringTime,
                alarmInfo1.timeZone));

        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
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
        final AlarmInfo alarmInfo1 = this.alarmInfoList1.get(0);
        this.alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId,
                Collections.<Alarm>emptyList(),
                Optional.of(RingTime.createEmpty()),
                alarmInfo1.timeZone));


        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
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

        when(this.ringTimeDAODynamoDB.getNextRingTime(testDeviceId)).thenReturn(RingTime.createEmpty());
        final DateTime alarmDeadlineLocalUTC = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.UTC);
        final DateTime dataCollectionTimeLocalUTC = alarmDeadlineLocalUTC.minusMinutes(20);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(Collections.<TrackerMotion>emptyList()));

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);

        final DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.getMillis(), is(deadline.getMillis()));
        assertThat(ringTime.isRegular(), is(true));
    }


    @Test
    public void testNoneRepeatedAlarmOn_09_23_2014_InitWithData(){
        // Test scenario when computation get triggered all the alarm is non-repeated and not expired.
        // pill has data.

        when(this.ringTimeDAODynamoDB.getNextRingTime(testDeviceId)).thenReturn(RingTime.createEmpty());
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true,
                new AlarmSound(100, "The Star Spangled Banner")));

        when(this.alarmDAODynamoDB.getAlarms(1)).thenReturn(ImmutableList.copyOf(alarmList));

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));


        // Minutes before smart alarm triggered.
        RingTime ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);


        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.isRegular(), is(true));

        when(this.ringTimeDAODynamoDB.getNextRingTime(testDeviceId)).thenReturn(ringTime);

        // Minutes after smart alarm triggered but before deadline.
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
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
        assertThat(ringTime.isSmart(), is(true));
    }

    @Test
    public void testNoneRepeatedAlarmOn_09_23_2014_InitWithNoData(){
        // Test scenario when computation get triggered all the alarm is non-repeated and not expired.
        // pill has no data.

        when(this.ringTimeDAODynamoDB.getNextRingTime(testDeviceId)).thenReturn(RingTime.createEmpty());
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true,
                new AlarmSound(100, "The Star Spangled Banner")));

        when(this.alarmDAODynamoDB.getAlarms(1)).thenReturn(ImmutableList.copyOf(alarmList));

        // pill has no data
        final DateTime alarmDeadlineLocalUTC = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.UTC);
        final DateTime dataCollectionTimeLocalUTC = alarmDeadlineLocalUTC.minusMinutes(20);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.trackerMotionDAO.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(Collections.<TrackerMotion>emptyList()));

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                dataCollectionTime,
                20,
                15,
                0.2f,
                null);

        final DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.isRegular(), is(true));
    }



    @Test
    public void testNoneRepeatedExpiredAlarmOn_09_23_2014_Init(){
        // Test scenario when computation get triggered all the alarm is non-repeated and expired.

        when(this.ringTimeDAODynamoDB.getNextRingTime(testDeviceId)).thenReturn(RingTime.createEmpty());
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.MONDAY);

        alarmList.add(new Alarm(2014, 9, 22, 8, 20, dayOfWeek,
                false, true, true,
                new AlarmSound(100, "The Star Spangled Banner")));

        final AlarmInfo alarmInfo1 = this.alarmInfoList1.get(0);
        this.alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId,
                alarmList,
                alarmInfo1.ringTime,
                alarmInfo1.timeZone));

        final DateTime deadline = new DateTime(2014, 9, 22, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
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
                false, true, true,
                new AlarmSound(100, "The Star Spangled Banner")));

        final AlarmInfo alarmInfo1 = this.alarmInfoList1.get(0);
        this.alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId,
                alarmList,
                alarmInfo1.ringTime,
                alarmInfo1.timeZone));

        final DateTime deadline = new DateTime(2014, 9, 22, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
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

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true,
                new AlarmSound(100, "The Star Spangled Banner")));

        final HashSet<Integer> dayOfWeek2 = new HashSet<Integer>();
        dayOfWeek2.add(DateTimeConstants.WEDNESDAY);
        alarmList.add(new Alarm(2014, 9, 24, 9, 20, dayOfWeek2,
                false, true, true,
                new AlarmSound(100, "The Star Spangled Banner")));


        AlarmInfo alarmInfo1 = this.alarmInfoList1.get(0);
        this.alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId,
                alarmList,
                alarmInfo1.ringTime,
                alarmInfo1.timeZone));


        DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));

        // Minutes before alarm triggered
        RingTime ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
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
        assertThat(ringTime.isRegular(), is(true));

        when(this.ringTimeDAODynamoDB.getNextRingTime(testDeviceId)).thenReturn(ringTime);

        alarmInfo1 = this.alarmInfoList1.get(0);
        this.alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId,
                alarmInfo1.alarmList,
                Optional.of(ringTime),
                alarmInfo1.timeZone));

        // Minute that trigger smart alarm processing
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
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
        assertThat(ringTime.isSmart(), is(true));

        alarmInfo1 = this.alarmInfoList1.get(0);
        this.alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId,
                alarmInfo1.alarmList,
                Optional.of(ringTime),
                alarmInfo1.timeZone));

        // Minutes after smart alarm processing but before next smart alarm process triggered.
        deadline = new DateTime(2014, 9, 24, 9, 20, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
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
        assertThat(ringTime.isRegular(), is(true));
    }


    @Test
    public void testSmartAlarmAlreadySetOn_09_23_2014_Update(){
        // Test scenario when computation get triggered, a smart alarm in the future is set.
        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));


        final RingTime nextRingTime = new RingTime(deadline.minusMinutes(3).getMillis(),
                deadline.getMillis(),
                100);

        AlarmInfo alarmInfo1 = alarmInfoList1.get(0);
        alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId, alarmInfo1.alarmList,
                Optional.of(nextRingTime), alarmInfo1.timeZone));


        // For moments that not yet trigger smart alarm computation
        RingTime ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 7, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);


        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline.minusMinutes(3)), is(true));
        assertThat(ringTime.isSmart(), is(true));

        alarmInfo1 = alarmInfoList1.get(0);
        alarmInfoList1.set(0, new AlarmInfo(alarmInfo1.deviceId, alarmInfo1.accountId, alarmInfo1.alarmList,
                Optional.of(ringTime), alarmInfo1.timeZone));

        // For moments that triggered smart alarm computation
        ringTime = RingProcessor.updateNextRingTime(this.mergedAlarmInfoDynamoDB,
                this.ringTimeDAODynamoDB,
                this.trackerMotionDAO,
                this.testDeviceId,
                new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles")),
                20,
                15,
                0.2f,
                null);


        assertThat(ringTime.actualRingTimeUTC, is(deadline.minusMinutes(3).getMillis()));
        assertThat(ringTime.isSmart(), is(true));
    }
}
