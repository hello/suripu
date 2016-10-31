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
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillDataDAODynamoDB;
import com.hello.suripu.core.db.ScheduledRingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SmartAlarmLoggerDynamoDB;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.AlarmSound;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserInfo;
import com.librato.rollout.RolloutClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
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
    private final SmartAlarmLoggerDynamoDB smartAlarmLoggerDynamoDB = mock(SmartAlarmLoggerDynamoDB.class);

    private ScheduledRingTimeHistoryDAODynamoDB scheduledRingTimeHistoryDAODynamoDB;
    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = mock(MergedUserInfoDynamoDB.class);

    private final PillDataDAODynamoDB pillDataDAODynamoDB = mock(PillDataDAODynamoDB.class);

    private final String testDeviceId = "test morpheus";
    private final String ringTimeTableName = "ringtime_test2";

    private final List<UserInfo> userInfoList1 = new ArrayList<>();
    private final List<UserInfo> userInfoList2 = new ArrayList<>();





    private static RolloutClient featureFlipOn() {
        final Long FAKE_ACCOUNT_ID = 1L;

        RolloutClient mockFeatureFlipper = Mockito.mock(RolloutClient.class);
        Mockito.when(mockFeatureFlipper.userFeatureActive(FeatureFlipper.SMART_ALARM_REFACTORED, FAKE_ACCOUNT_ID, Collections.EMPTY_LIST)).thenReturn(Boolean.TRUE);

        return mockFeatureFlipper;
    }

    private void setAlarm(final boolean isRepeated, final boolean isSmart){
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                isRepeated, true, true, isSmart,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));

        final UserInfo userInfo1 = userInfoList1.get(0);
        userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId, alarmList,
                userInfo1.ringTime, userInfo1.timeZone, userInfo1.pillColor,
                0));
    }

    private void mockMotionData20150923OffsetByDay(final int dayOffset){
        final URL url = Resources.getResource("fixtures/algorithm/pill_data_09_23_2014_pang.csv");
        final List<TrackerMotion> motions = new ArrayList<TrackerMotion>();
        final int offset = DateTimeConstants.MILLIS_PER_DAY * dayOffset;

        try {
            final String csvString = Resources.toString(url, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(final String line:lines){
                final String[] columns = line.split("\\t");
                long timestamp = Long.valueOf(columns[0]) + offset;
                long value = Long.valueOf(columns[1]);
                int offsetMillis = DateTimeZone.forID("America/Los_Angeles").getOffset(timestamp);
                motions.add(new TrackerMotion(1L, 1L, 1L, timestamp, (int)value, offsetMillis, 0L, 0L, 0L));
            }
        }catch (IOException ioe){
            LOGGER.error("Failed parsing CSV: {}", ioe.getMessage());
        }

        final DateTime alarmDeadlineLocalUTC = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.UTC).plusDays(dayOffset);
        final DateTime dataCollectionTimeLocalUTC = alarmDeadlineLocalUTC.minusMinutes(20);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(motions));
    }

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

        mockMotionData20150923OffsetByDay(0);

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
        //init();
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(ringTimeTableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            LOGGER.warn("Can not delete non existing table");
        }
    }

    private RingTime updateAndReturnNextRingTime(final DateTime dataCollectionTime, final RolloutClient features){
        return RingProcessor.updateAndReturnNextRingTimeForSense(this.mergedUserInfoDynamoDB,
                this.scheduledRingTimeHistoryDAODynamoDB,
                this.smartAlarmLoggerDynamoDB,
                this.pillDataDAODynamoDB,
                this.testDeviceId,
                dataCollectionTime,
                30,
                15,
                0.2f,
                features);
    }



    private void mockProgressiveMotionData(final DateTime queryStart, final DateTime queryEnd){
        final List<TrackerMotion> last5MinsMotion = new ArrayList<>();
        DateTime currentTime = queryEnd;
        while (currentTime.isAfter(queryStart)){
            last5MinsMotion.add(new TrackerMotion(0, 1L, 1L, currentTime.getMillis(), 7000, 0, 0L, 0L, 0L));
            currentTime = currentTime.minusMinutes(1);
        }
        when(pillDataDAODynamoDB.getBetween(1L, queryStart, queryEnd.plusMinutes(1))).thenReturn(ImmutableList.copyOf(last5MinsMotion));
    }

    @Test
    @Ignore
    public void testProgressiveSmartAlarmWithFakeDataForTwoDays(){
        // Test the case user has repeated smart alarm and has data for two days in a row
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);
        dayOfWeek.add(DateTimeConstants.WEDNESDAY);

        // 1st alarm: 2014-09-23 08:20, repeated on TUE and WED
        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                true, true, true, true,
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
        // 2nd alarm: 2014-09-24 08:20
        // Now: 2014-9-23 07:20
        // Minutes before alarm triggered
        RingTime ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")), null);

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
        // 2nd alarm: 2014-09-24 08:20
        // Now: 2014-9-23 08:00
        // Minute that trigger smart alarm processing
        ringTime = updateAndReturnNextRingTime(dataCollectionTime, null);

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
        // 2nd alarm, smart: 2014-09-24 08:20
        // Now: [actual ring time - 3 minute]
        // Minutes after smart alarm processing but before alarm take off time -11 minutes.
        // Progressive alarm processing triggered
        DateTime now = actualRingTime.minusMinutes(3);
        mockProgressiveMotionData(now.minusMinutes(5), now);
        ringTime = updateAndReturnNextRingTime(now, null);

        assertThat(ringTime.isEmpty(), is(false));
        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, userInfo1.timeZone.get());
        LOGGER.debug("=========> Now: {}, actual: {}, expected: {}",
                now,
                actualRingTime,
                new DateTime(ringTime.expectedRingTimeUTC));

        assertThat(actualRingTime.equals(now.plusMinutes(RingProcessor.PROGRESSIVE_SAFE_GAP_MIN)), is(true));

        userInfo1 = this.userInfoList1.get(0);
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));

        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 08:20
        // Now: [actual ring time + 1 minute]
        // Minutes after smart alarm processing but before alarm take off time -11 minutes.
        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, actualRingTime.getZone());
        ringTime = updateAndReturnNextRingTime(actualRingTime.plusMinutes(1), null);

        assertThat(ringTime.isEmpty(), is(true));

        mockMotionData20150923OffsetByDay(1);

        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 08:20
        // Now: 2014-9-24 07:00
        // 1st smart alarm expected ring time past, but not yet reach the processing time of 2nd
        // smart alarm.
        deadline = new DateTime(2014, 9, 24, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 24, 7, 0, DateTimeZone.forID("America/Los_Angeles")), null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.equals(deadline), is(true));
        assertThat(ringTime.processed(), is(false));


        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 08:20
        // Now: 2014-9-24 08:00
        // 1st smart alarm expected ring time past, 2nd smart alarm triggered.
        deadline = new DateTime(2014, 9, 24, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 24, 8, 0, DateTimeZone.forID("America/Los_Angeles")), null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(new DateTime(ringTime.expectedRingTimeUTC, DateTimeZone.forID("America/Los_Angeles")).equals(deadline), is(true));
        assertThat(ringTime.processed(), is(true));

        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));

        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 08:20
        // Now: 2014-9-24 08:21
        // Both alarm expired for this week
        deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles")).plusWeeks(1);
        ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 24, 8, 21, DateTimeZone.forID("America/Los_Angeles")), null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.equals(deadline), is(true));
        assertThat(ringTime.processed(), is(false));
    }

    @Test
    public void testRepeatedSmartAlarmWithFakeDataForTwoDays(){
        // Test the case user has repeated smart alarm and has data for two days in a row
        final List<Alarm> alarmList = new ArrayList<Alarm>();
        final HashSet<Integer> dayOfWeek = new HashSet<Integer>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);
        dayOfWeek.add(DateTimeConstants.WEDNESDAY);

        // 1st alarm: 2014-09-23 08:20, repeated on TUE and WED
        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                true, true, true, true,
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
        // 2nd alarm: 2014-09-24 08:20
        // Now: 2014-9-23 07:20
        // Minutes before alarm triggered
        RingTime ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")), null);

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
        // 2nd alarm: 2014-09-24 08:20
        // Now: 2014-9-23 08:00
        // Minute that trigger smart alarm processing
        ringTime = updateAndReturnNextRingTime(dataCollectionTime, null);

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
        // 2nd alarm, smart: 2014-09-24 08:20
        // Now: [actual ring time + 1 minute]
        // Minutes after smart alarm processing but before next smart alarm process triggered.
        ringTime = updateAndReturnNextRingTime(actualRingTime.plusMinutes(1), null);

        assertThat(ringTime.isEmpty(), is(true));


        mockMotionData20150923OffsetByDay(1);

        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 08:20
        // Now: 2014-9-24 07:00
        // 1st smart alarm expected ring time past, but not yet reach the processing time of 2nd
        // smart alarm.
        deadline = new DateTime(2014, 9, 24, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 24, 7, 0, DateTimeZone.forID("America/Los_Angeles")), null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.equals(deadline), is(true));
        assertThat(ringTime.processed(), is(false));


        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 08:20
        // Now: 2014-9-24 08:00
        // 1st smart alarm expected ring time past, 2nd smart alarm triggered.
        deadline = new DateTime(2014, 9, 24, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 24, 8, 0, DateTimeZone.forID("America/Los_Angeles")), null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(new DateTime(ringTime.expectedRingTimeUTC, DateTimeZone.forID("America/Los_Angeles")).equals(deadline), is(true));
        assertThat(ringTime.processed(), is(true));

        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                userInfo1.alarmList,
                Optional.of(ringTime),
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));

        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 08:20
        // Now: 2014-9-24 08:21
        // Both alarm expired for this week
        deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles")).plusWeeks(1);
        ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 24, 8, 21, DateTimeZone.forID("America/Los_Angeles")), null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.equals(deadline), is(true));
        assertThat(ringTime.processed(), is(false));
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

        RingTime ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 7, 0, DateTimeZone.forID("America/Los_Angeles")), null);


        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));

        userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId, userInfo1.alarmList,
                Optional.of(ringTime), userInfo1.timeZone, userInfo1.pillColor,
                0));


        // For minute that triggered smart alarm computation
        ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles")), null);


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

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(Collections.<TrackerMotion>emptyList()));

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));

        // For minutes that not yet trigger smart alarm computation
        RingTime ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")), null);

        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));


        final List<Alarm> alarmList = new ArrayList<>();
        final HashSet<Integer> dayOfWeek = new HashSet<>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));

        final UserInfo userInfo1 = userInfoList1.get(0);
        userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId, alarmList,
                Optional.of(ringTime), userInfo1.timeZone, userInfo1.pillColor,
                0));

        // For the minute trigger smart alarm computation
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = updateAndReturnNextRingTime(dataCollectionTime, null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));

        // User has no pill data, everything is regular
        assertThat(actualRingTime.getMillis(), is(deadline.getMillis()));
        assertThat(ringTime.processed(), is(false));
    }


    @Test
    public void testNoneRepeatedSmartAlarmOn_09_23_2014_Update(){
        // Test scenario when computation get triggered, an ring time from previous alarm settings is set,
        // but user updated his/her next alarm to non-repeated after the last ring was computed.
        final List<Alarm> alarmList = new ArrayList<>();
        final HashSet<Integer> dayOfWeek = new HashSet<>();
        dayOfWeek.add(DateTimeConstants.TUESDAY);

        alarmList.add(new Alarm(2014, 9, 23, 8, 20, dayOfWeek,
                false, true, true, true,
                new AlarmSound(100, "The Star Spangled Banner"), "id"));

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));

        // For moments that not yet trigger smart alarm computation
        RingTime ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")), null);

        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));

        final UserInfo userInfo1 = userInfoList1.get(0);
        userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId, alarmList,
                Optional.of(ringTime), userInfo1.timeZone, userInfo1.pillColor,
                0));

        // For moments that triggered smart alarm computation
        ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles")), null);

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
        final RingTime ringTime = updateAndReturnNextRingTime(dataCollectionTime, null);


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
        final RingTime ringTime = updateAndReturnNextRingTime(dataCollectionTime, null);


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
        final RingTime ringTime = updateAndReturnNextRingTime(dataCollectionTime, null);


        assertThat(ringTime.isEmpty(), is(true));
    }


    @Test
    public void testRegularRepeatedAlarmOn_09_23_2014_Init(){
        // Test scenario when computation get triggered all the alarm is repeated and,
        // no alarm is set yet.
        // And pill has no data upload.
        setAlarm(true, false);

        final DateTime alarmDeadlineLocalUTC = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.UTC);
        final DateTime dataCollectionTimeLocalUTC = alarmDeadlineLocalUTC.minusMinutes(20);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(Collections.<TrackerMotion>emptyList()));

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = updateAndReturnNextRingTime(dataCollectionTime, null);

        final DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.getMillis(), is(deadline.getMillis()));
        assertThat(ringTime.processed(), is(false));
    }


    @Test
    public void testNoneRepeatedAlarmOn_09_23_2014_InitWithData(){
        // Test scenario when computation get triggered all the alarm is non-repeated and not expired.
        // pill has data.
        setAlarm(false, true);

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));


        // Minutes before smart alarm triggered.
        RingTime ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 7, 0, DateTimeZone.forID("America/Los_Angeles")), null);


        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isEqual(deadline), is(true));
        assertThat(ringTime.processed(), is(false));

        // Minutes after smart alarm triggered but before deadline.
        ringTime = updateAndReturnNextRingTime(dataCollectionTime, null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.isBefore(deadline), is(true));
        assertThat(ringTime.processed(), is(true));
    }

    @Test
    public void testNoneRepeatedAlarmOn_09_23_2014_InitWithNoData(){
        // Test scenario when computation get triggered all the alarm is non-repeated and not expired.
        // pill has no data.
        setAlarm(false, true);

        // pill has no data
        final DateTime alarmDeadlineLocalUTC = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.UTC);
        final DateTime dataCollectionTimeLocalUTC = alarmDeadlineLocalUTC.minusMinutes(20);
        final DateTime startQueryTimeLocalUTC = dataCollectionTimeLocalUTC.minusHours(8);

        when(this.pillDataDAODynamoDB.getBetweenLocalUTC(1, startQueryTimeLocalUTC, dataCollectionTimeLocalUTC))
                .thenReturn(ImmutableList.copyOf(Collections.<TrackerMotion>emptyList()));

        final DateTime deadline = new DateTime(2014, 9, 23, 8, 20, DateTimeZone.forID("America/Los_Angeles"));
        final DateTime dataCollectionTime = new DateTime(2014, 9, 23, 8, 0, DateTimeZone.forID("America/Los_Angeles"));
        final RingTime ringTime = updateAndReturnNextRingTime(dataCollectionTime, null);

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
        final RingTime ringTime = updateAndReturnNextRingTime(dataCollectionTime, null);

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
        final RingTime ringTime = updateAndReturnNextRingTime(dataCollectionTime, null);

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
        RingTime ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")), null);

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
        ringTime = updateAndReturnNextRingTime(dataCollectionTime, null);

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
        ringTime = updateAndReturnNextRingTime(actualRingTime.plusMinutes(1), null);

        assertThat(ringTime.isEmpty(), is(true));

        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 09:20
        // Now: 2014-9-23 08:21
        // 1st smart alarm expected ring time past, but not yet reach the processing time of 2nd
        // smart alarm.
        deadline = new DateTime(2014, 9, 24, 9, 20, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 8, 21, DateTimeZone.forID("America/Los_Angeles")), null);

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
        RingTime ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")), null);

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
        ringTime = updateAndReturnNextRingTime(dataCollectionTime, null);

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
        ringTime = updateAndReturnNextRingTime(actualRingTime.plusMinutes(1), null);

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
        ringTime = updateAndReturnNextRingTime(actualRingTime.plusMinutes(2),null);

        assertThat(ringTime.isEmpty(), is(true));


        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 09:20
        // 3rd alarm, smart: 2014-09-23 10:00
        // Now: [actual ring time + 2 minute]
        // Minutes after smart alarm processing but before next smart alarm process triggered.
        ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 8, 21, DateTimeZone.forID("America/Los_Angeles")), null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.equals(new DateTime(2014, 9, 23, 10, 0, DateTimeZone.forID("America/Los_Angeles"))), is(true));
        assertThat(ringTime.fromSmartAlarm, is(false));


        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 09:20
        // Now: 2014-9-23 08:21
        // 1st smart alarm expected ring time past, but not yet reach the processing time of 2nd
        // smart alarm.
        deadline = new DateTime(2014, 9, 24, 9, 20, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 10, 1, DateTimeZone.forID("America/Los_Angeles")),null);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.equals(deadline), is(true));
        assertThat(ringTime.processed(), is(false));
    }

    @Test
    public void testProgressiveAlarmTransitionByTimeAndUpdateAlarmInSmartAlarmRingTimeGap(){
        // Test how alarm behave when time goes by.
        final RolloutClient features = featureFlipOn();
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
                false, true, true, false,
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
        RingTime ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 7, 20, DateTimeZone.forID("America/Los_Angeles")), features);

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
        mockProgressiveMotionData(dataCollectionTime.withZone(DateTimeZone.UTC).minusMinutes(7), dataCollectionTime.withZone(DateTimeZone.UTC));

        ringTime = updateAndReturnNextRingTime(dataCollectionTime, features);

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
        ringTime = updateAndReturnNextRingTime(actualRingTime.plusMinutes(1), features);

        assertThat(ringTime.isEmpty(), is(true));


        // And now, the user update his/her alarms!!!
        userInfo1 = this.userInfoList1.get(0);
        List<Alarm> alarmList2 = this.userInfoList1.get(0).alarmList;
        alarmList2.add(new Alarm(2014, 9, 23, 8, 19, dayOfWeek, false, true, true, false,  new AlarmSound(100, "The Star Spangled Banner"), "id"));
        this.userInfoList1.set(0, new UserInfo(userInfo1.deviceId, userInfo1.accountId,
                alarmList2,
                userInfo1.ringTime,  // Here we simulate no writing the temporary empty alarm into user info table.
                userInfo1.timeZone,
                userInfo1.pillColor,
                0));

        // 1st alarm, smart: 2014-09-23 08:20
        // New Alarm, regular: 2014-09-23 08:19
        // 2nd alarm, smart: 2014-09-24 09:20
        // 3rd alarm, smart: 2014-09-23 10:00
        // Now: [actual ring time + 2 minute]
        // Minutes after smart alarm processing but before next smart alarm process triggered.
        RingTime gapRingTime = Alarm.Utils.generateNextRingTimeFromAlarmTemplatesForUser(userInfo1.alarmList, dataCollectionTime.getMillis(), userInfo1.timeZone.get());
        ringTime = updateAndReturnNextRingTime(actualRingTime.plusMinutes(2), features);

        assertThat(ringTime, is(gapRingTime));

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
        ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 8, 21, DateTimeZone.forID("America/Los_Angeles")),features);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.equals(new DateTime(2014, 9, 23, 10, 0, DateTimeZone.forID("America/Los_Angeles"))), is(true));
        assertThat(ringTime.fromSmartAlarm, is(false));


        // 1st alarm, smart: 2014-09-23 08:20
        // 2nd alarm, smart: 2014-09-24 09:20
        // Now: 2014-9-23 08:21
        // 1st smart alarm expected ring time past, but not yet reach the processing time of 2nd
        // smart alarm.
        deadline = new DateTime(2014, 9, 24, 9, 20, DateTimeZone.forID("America/Los_Angeles"));
        ringTime = updateAndReturnNextRingTime(new DateTime(2014, 9, 23, 10, 1, DateTimeZone.forID("America/Los_Angeles")), features);

        actualRingTime = new DateTime(ringTime.actualRingTimeUTC, DateTimeZone.forID("America/Los_Angeles"));
        assertThat(actualRingTime.equals(deadline), is(true));
        assertThat(ringTime.processed(), is(false));
    }
}
