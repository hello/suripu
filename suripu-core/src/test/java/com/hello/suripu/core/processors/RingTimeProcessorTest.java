package com.hello.suripu.core.processors;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.event.SleepCycleAlgorithm;
import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.ProgressiveAlarmThresholds;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 2/4/15.
 */
public class RingTimeProcessorTest {

    private final String senseId = "test sense";
    private final long accountId = 1L;
    private final long accountId2 = 2L;

    private List<TrackerMotion> loadTrackerMotionFromCSV(final String resource, final boolean mockMM){
        final URL fixtureCSVFile = Resources.getResource(resource);
        final List<TrackerMotion> trackerMotions = new ArrayList<>();
        try {
            final String csvString = Resources.toString(fixtureCSVFile, Charsets.UTF_8);
            final String[] lines = csvString.split("\\n");
            for(int i = 1; i < lines.length; i++){
                final String[] columns = lines[i].split(",");
                if (!mockMM) {
                    final TrackerMotion trackerMotion = new TrackerMotion.Builder()
                            .withAccountId(accountId)
                            .withKickOffCounts(Long.valueOf(columns[4]))
                            .withValue(Integer.valueOf(columns[11]))
                            .withOnDurationInSeconds(Long.valueOf(columns[7]))
                            .withTimestampMillis(Long.valueOf(columns[12]))
                            .withOffsetMillis(Integer.valueOf(columns[9]))
                            .build();
                    trackerMotions.add(trackerMotion);

                } else{
                    final TrackerMotion trackerMotion = new TrackerMotion.Builder()
                            .withAccountId(accountId)
                            .withKickOffCounts(0L)
                            .withValue(Integer.valueOf(columns[11]))
                            .withOnDurationInSeconds(Long.valueOf(columns[7]))
                            .withMotionMask(Long.valueOf(0L))
                            .withTimestampMillis(Long.valueOf(columns[12]))
                            .withOffsetMillis(Integer.valueOf(columns[9]))
                            .build();
                    trackerMotions.add(trackerMotion);
                }
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }

        return trackerMotions;
    }

    @Test
    public void testGetNextRingTimeForSenseFormOneUser(){
        final List<Alarm> alarmList = new ArrayList<>();
        final HashSet<Integer> daysOfWeek = new HashSet<>();
        daysOfWeek.add(DateTimeConstants.TUESDAY);
        daysOfWeek.add(DateTimeConstants.WEDNESDAY);


        final DateTimeZone userTimeZone = DateTimeZone.forID("America/Los_Angeles");
        DateTime now = new DateTime(2015, 2, 3, 7, 0, 10, userTimeZone);
        alarmList.add(new Alarm.Builder().withDayOfWeek(daysOfWeek)
                .withHour(7)
                .withMinute(30)
                .withId("1")
                .withIsEditable(true)
                .withIsEnabled(true)
                .withIsRepeated(true)
                .withIsSmart(true)
                .build());

        alarmList.add(new Alarm.Builder().withDayOfWeek(daysOfWeek)
                .withHour(8)
                .withMinute(0)
                .withId("2")
                .withIsEditable(true)
                .withIsEnabled(true)
                .withIsRepeated(true)
                .withIsSmart(false)
                .build());

        UserInfo userInfo = new UserInfo(this.senseId, this.accountId,
                alarmList,
                Optional.<RingTime>absent(),
                Optional.of(userTimeZone),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                now.minusMillis(100).getMillis());
        final List<UserInfo> userInfoList = new ArrayList<>();
        userInfoList.add(userInfo);

        RingTime ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, userTimeZone);

        // alarm 1, smart: 7:30
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 7:00
        assertThat(actualRingTime, is(new DateTime(2015, 2, 3, 7, 30, 0, userTimeZone)));
        assertThat(ringTime.fromSmartAlarm, is(true));

        // now: 2015-2-3 7:05
        // worker processed smart alarm
        // smart alarm set at 2015-2-3 7:21
        now = new DateTime(2015, 2, 3, 7, 5, userTimeZone);
        userInfo = new UserInfo(senseId, accountId, alarmList,
                Optional.of(new RingTime(new DateTime(2015,2,3,7,21,0, userTimeZone).getMillis(),
                        new DateTime(2015,2,3,7,30,0,userTimeZone).getMillis(),
                        new long[0],
                        true,
                        Lists.newArrayList())),
                Optional.of(userTimeZone),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                now.minusMillis(100).getMillis()
        );
        userInfoList.set(0, userInfo);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(ringTime.actualRingTimeUTC, is(new DateTime(2015,2,3,7,21,0,userTimeZone).getMillis()));
        assertThat(ringTime.expectedRingTimeUTC, is(new DateTime(2015,2,3,7,30,0,userTimeZone).getMillis()));
        assertThat(ringTime.fromSmartAlarm, is(true));

        // alarm 1, smart: 7:30
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 7:22
        now = new DateTime(2015, 2, 3, 7, 22, userTimeZone);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(ringTime.isEmpty(), is(true));

        // alarm 1, smart: 7:30
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 7:23
        now = new DateTime(2015, 2, 3, 7, 23, userTimeZone);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(ringTime.isEmpty(), is(true));

        // alarm 1, smart: 7:30
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 7:30
        now = new DateTime(2015, 2, 3, 7, 30, userTimeZone);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(ringTime.isEmpty(), is(true));

        // alarm 1, smart: 7:30
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 7:31
        now = new DateTime(2015, 2, 3, 7, 31, userTimeZone);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(ringTime.actualRingTimeUTC, is(new DateTime(2015,2,3,8,0,0,userTimeZone).getMillis()));
        assertThat(ringTime.expectedRingTimeUTC, is(new DateTime(2015,2,3,8,0,0,userTimeZone).getMillis()));
        assertThat(ringTime.fromSmartAlarm, is(false));

        // alarm 1, smart: 7:30
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 8:01
        now = new DateTime(2015, 2, 3, 8, 1, userTimeZone);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(new DateTime(ringTime.actualRingTimeUTC, userTimeZone), is(new DateTime(2015,2,4,7,30,0,userTimeZone)));
        assertThat(new DateTime(ringTime.expectedRingTimeUTC, userTimeZone), is(new DateTime(2015,2,4,7,30,0,userTimeZone)));
        assertThat(ringTime.fromSmartAlarm, is(true));
    }


    @Test
    public void testGetNextRingTimeForSenseFormTwoUsers(){
        final List<Alarm> alarmList = new ArrayList<>();
        final HashSet<Integer> daysOfWeek = new HashSet<>();
        daysOfWeek.add(DateTimeConstants.TUESDAY);
        daysOfWeek.add(DateTimeConstants.WEDNESDAY);


        final DateTimeZone userTimeZone = DateTimeZone.forID("America/Los_Angeles");
        DateTime now = new DateTime(2015, 2, 3, 7, 0, 10, userTimeZone);
        alarmList.add(new Alarm.Builder().withDayOfWeek(daysOfWeek)
                .withHour(7)
                .withMinute(30)
                .withId("1")
                .withIsEditable(true)
                .withIsEnabled(true)
                .withIsRepeated(true)
                .withIsSmart(true)
                .build());

        alarmList.add(new Alarm.Builder().withDayOfWeek(daysOfWeek)
                .withHour(8)
                .withMinute(0)
                .withId("2")
                .withIsEditable(true)
                .withIsEnabled(true)
                .withIsRepeated(true)
                .withIsSmart(false)
                .build());

        UserInfo userInfo = new UserInfo(this.senseId, this.accountId,
                alarmList,
                Optional.<RingTime>absent(),
                Optional.of(userTimeZone),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                now.minusMillis(100).getMillis());

        final List<Alarm> alarmList2 = new ArrayList<>();
        alarmList2.add(new Alarm.Builder().withDayOfWeek(daysOfWeek)
                .withHour(7)
                .withMinute(40)
                .withId("3")
                .withIsEditable(true)
                .withIsEnabled(true)
                .withIsRepeated(true)
                .withIsSmart(true)
                .build());
        UserInfo userInfo2 = new UserInfo(this.senseId, this.accountId2,
                alarmList2,
                Optional.<RingTime>absent(),
                Optional.of(userTimeZone),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                now.minusMillis(100).getMillis());

        final List<UserInfo> userInfoList = new ArrayList<>();
        userInfoList.add(userInfo);
        userInfoList.add(userInfo2);

        RingTime ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, userTimeZone);

        // alarm 1, smart: 7:30
        // alarm 3, smart: 7:40
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 7:00
        assertThat(actualRingTime, is(new DateTime(2015, 2, 3, 7, 30, 0, userTimeZone)));
        assertThat(ringTime.fromSmartAlarm, is(true));

        // now: 2015-2-3 7:05
        // worker processed smart alarm
        // smart alarm set at 2015-2-3 7:21
        now = new DateTime(2015, 2, 3, 7, 5, userTimeZone);
        userInfo = new UserInfo(senseId, accountId, alarmList,
                Optional.of(new RingTime(new DateTime(2015,2,3,7,21,0, userTimeZone).getMillis(),
                        new DateTime(2015,2,3,7,30,0,userTimeZone).getMillis(),
                        new long[0],
                        true,
                        Lists.newArrayList())),
                Optional.of(userTimeZone),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                now.minusMillis(100).getMillis()
        );
        userInfoList.set(0, userInfo);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(ringTime.actualRingTimeUTC, is(new DateTime(2015, 2, 3, 7, 21, 0, userTimeZone).getMillis()));
        assertThat(ringTime.expectedRingTimeUTC, is(new DateTime(2015,2,3,7,30,0,userTimeZone).getMillis()));
        assertThat(ringTime.fromSmartAlarm, is(true));

        // alarm 1, smart: 7:30 - ring at 2015-2-3 7:21
        // alarm 3, smart: 7:40
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 7:22
        now = new DateTime(2015, 2, 3, 7, 22, userTimeZone);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(ringTime.isEmpty(), is(true));

        // alarm 1, smart: 7:30 - 2015-2-3 7:21
        // alarm 3, smart: 7:40
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 7:23
        now = new DateTime(2015, 2, 3, 7, 23, userTimeZone);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(ringTime.isEmpty(), is(true));

        // alarm 1, smart: 7:30
        // alarm 3, smart: 7:40
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 7:30
        now = new DateTime(2015, 2, 3, 7, 30, userTimeZone);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(ringTime.isEmpty(), is(true));

        // alarm 1, smart: 7:30
        // alarm 3, smart: 7:40
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 7:31
        now = new DateTime(2015, 2, 3, 7, 31, userTimeZone);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(ringTime.actualRingTimeUTC, is(new DateTime(2015,2,3,7,40,0,userTimeZone).getMillis()));
        assertThat(ringTime.expectedRingTimeUTC, is(new DateTime(2015,2,3,7,40,0,userTimeZone).getMillis()));
        assertThat(ringTime.fromSmartAlarm, is(true));

        // alarm 1, smart: 7:30
        // alarm 3, smart: 7:40
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 7:41
        now = new DateTime(2015, 2, 3, 7, 41, userTimeZone);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(new DateTime(ringTime.actualRingTimeUTC, userTimeZone), is(new DateTime(2015,2,3,8,0,0,userTimeZone)));
        assertThat(new DateTime(ringTime.expectedRingTimeUTC, userTimeZone), is(new DateTime(2015,2,3,8,0,0,userTimeZone)));
        assertThat(ringTime.fromSmartAlarm, is(false));

        // alarm 1, smart: 7:30
        // alarm 3, smart: 7:40
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 8:01
        now = new DateTime(2015, 2, 3, 8, 1, userTimeZone);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(new DateTime(ringTime.actualRingTimeUTC, userTimeZone), is(new DateTime(2015,2,4,7,30,0,userTimeZone)));
        assertThat(new DateTime(ringTime.expectedRingTimeUTC, userTimeZone), is(new DateTime(2015,2,4,7,30,0,userTimeZone)));
        assertThat(ringTime.fromSmartAlarm, is(true));
    }



    @Test
    public void testGetNextRingTimeUserChangeTimeZoneAfter1stSmartRingProcessedByWorker(){
        final List<Alarm> alarmList = new ArrayList<>();
        final HashSet<Integer> daysOfWeek = new HashSet<>();
        daysOfWeek.add(DateTimeConstants.TUESDAY);
        daysOfWeek.add(DateTimeConstants.WEDNESDAY);


        DateTimeZone userTimeZone = DateTimeZone.forID("Asia/Hong_Kong");
        DateTime now = new DateTime(2015, 2, 3, 7, 0, 10, userTimeZone);
        alarmList.add(new Alarm.Builder().withDayOfWeek(daysOfWeek)
                .withHour(7)
                .withMinute(30)
                .withId("1")
                .withIsEditable(true)
                .withIsEnabled(true)
                .withIsRepeated(true)
                .withIsSmart(true)
                .build());

        alarmList.add(new Alarm.Builder().withDayOfWeek(daysOfWeek)
                .withHour(8)
                .withMinute(0)
                .withId("2")
                .withIsEditable(true)
                .withIsEnabled(true)
                .withIsRepeated(true)
                .withIsSmart(false)
                .build());

        UserInfo userInfo = new UserInfo(this.senseId, this.accountId,
                alarmList,
                Optional.<RingTime>absent(),
                Optional.of(userTimeZone),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                now.minusMillis(100).getMillis());
        final List<UserInfo> userInfoList = new ArrayList<>();
        userInfoList.add(userInfo);

        RingTime ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        DateTime actualRingTime = new DateTime(ringTime.actualRingTimeUTC, userTimeZone);

        // alarm 1, smart: 7:30
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 7:00
        assertThat(actualRingTime, is(new DateTime(2015, 2, 3, 7, 30, 0, userTimeZone)));
        assertThat(ringTime.fromSmartAlarm, is(true));

        // now: 2015-2-3 7:05
        // worker processed smart alarm
        // smart alarm set at 2015-2-3 7:21
        now = new DateTime(2015, 2, 3, 7, 5, userTimeZone);
        userInfo = new UserInfo(senseId, accountId, alarmList,
                Optional.of(new RingTime(new DateTime(2015,2,3,7,21,0, userTimeZone).getMillis(),
                        new DateTime(2015,2,3,7,30,0,userTimeZone).getMillis(),
                        new long[0],
                        true,
                        Lists.newArrayList())),
                Optional.of(userTimeZone),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                now.minusMillis(100).getMillis()
        );
        userInfoList.set(0, userInfo);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(ringTime.actualRingTimeUTC, is(new DateTime(2015,2,3,7,21,0,userTimeZone).getMillis()));
        assertThat(ringTime.expectedRingTimeUTC, is(new DateTime(2015,2,3,7,30,0,userTimeZone).getMillis()));
        assertThat(ringTime.fromSmartAlarm, is(true));


        // Now the user rides a rocket to USA with the be-loved Sense and changes the time zone.
        // now: 2015-2-3 7:15
        // worker processed smart alarm
        // smart alarm set at 2015-2-3 7:21
        now = new DateTime(2015, 2, 3, 7, 15, userTimeZone);
        userTimeZone = DateTimeZone.forID("America/Los_Angeles");
        userInfo = new UserInfo(senseId, accountId, alarmList,
                Optional.of(RingTime.createEmpty()),   // When user changes time zone, the worker ringtime will be reset to empty
                Optional.of(userTimeZone),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                now.minusMillis(100).getMillis()
        );
        userInfoList.set(0, userInfo);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(ringTime.actualRingTimeUTC, is(new DateTime(2015,2,3,7,30,0,userTimeZone).getMillis()));  // should be reset to expected ringtime
        assertThat(ringTime.expectedRingTimeUTC, is(new DateTime(2015,2,3,7,30,0,userTimeZone).getMillis()));
        assertThat(ringTime.fromSmartAlarm, is(true));



        // alarm 1, smart: 7:30
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 7:31
        now = new DateTime(2015, 2, 3, 7, 31, userTimeZone);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(ringTime.actualRingTimeUTC, is(new DateTime(2015,2,3,8,0,0,userTimeZone).getMillis()));
        assertThat(ringTime.expectedRingTimeUTC, is(new DateTime(2015,2,3,8,0,0,userTimeZone).getMillis()));
        assertThat(ringTime.fromSmartAlarm, is(false));

        // alarm 1, smart: 7:30
        // alarm 2, stupid: 8:00
        // now: 2015-2-3 8:01
        now = new DateTime(2015, 2, 3, 8, 1, userTimeZone);
        ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);
        assertThat(new DateTime(ringTime.actualRingTimeUTC, userTimeZone), is(new DateTime(2015,2,4,7,30,0,userTimeZone)));
        assertThat(new DateTime(ringTime.expectedRingTimeUTC, userTimeZone), is(new DateTime(2015,2,4,7,30,0,userTimeZone)));
        assertThat(ringTime.fromSmartAlarm, is(true));
    }

    @Test
    public void testGetProgressiveRingTime() {
        final long updatedTimeAwake =  1476954780000000000L;
        final long updatedTimeAsleep = 147695430000000000L;

        final List<Alarm> alarmList = new ArrayList<>();
        final HashSet<Integer> daysOfWeek = new HashSet<>();
        daysOfWeek.add(DateTimeConstants.TUESDAY);
        daysOfWeek.add(DateTimeConstants.WEDNESDAY);
        daysOfWeek.add(DateTimeConstants.THURSDAY);

        final DateTimeZone userTimeZone = DateTimeZone.forID("America/Vancouver");
        DateTime now = new DateTime(2016, 10, 20, 2, 30, 00, userTimeZone);
        alarmList.add(new Alarm.Builder().withDayOfWeek(daysOfWeek)
                .withHour(2)
                .withMinute(30)
                .withId("1")
                .withIsEditable(true)
                .withIsEnabled(true)
                .withIsRepeated(true)
                .withIsSmart(true)
                .build());

        UserInfo userInfo = new UserInfo(this.senseId, this.accountId,
                alarmList,
                Optional.<RingTime>absent(),
                Optional.of(userTimeZone),
                Optional.<OutputProtos.SyncResponse.PillSettings>absent(),
                now.minusMillis(100).getMillis());
        final List<UserInfo> userInfoList = new ArrayList<>();
        userInfoList.add(userInfo);

        RingTime ringTime = RingProcessor.getNextRingTimeForSense(this.senseId, userInfoList, now);

        List<TrackerMotion> trackerMotionsWithoutMM = loadTrackerMotionFromCSV("fixtures/tracker_motion/smart_alarm_test.csv", false);
        List<TrackerMotion> trackerMotionsMockedMM = loadTrackerMotionFromCSV("fixtures/tracker_motion/smart_alarm_test.csv", true);
        List<TrackerMotion> motionWithinWindow = new ArrayList<>();
        for (TrackerMotion trackerMotion : trackerMotionsWithoutMM) {
            if (trackerMotion.timestamp <= updatedTimeAsleep && trackerMotion.timestamp > updatedTimeAsleep - 60000000000L * RingProcessor.PROGRESSIVE_MOTION_WINDOW_MIN) {
                motionWithinWindow.add(trackerMotion);
            }
        }
        Optional<RingTime> testRing = RingProcessor.getProgressiveRingTime(userInfo.accountId, new DateTime(2016, 10, 20, 2, 5, 0, userTimeZone), ringTime, motionWithinWindow, false);
        assertThat(testRing.isPresent(), is(false));

        motionWithinWindow = new ArrayList<>();
        for (TrackerMotion trackerMotion : trackerMotionsWithoutMM) {
            if (trackerMotion.timestamp <= updatedTimeAwake && trackerMotion.timestamp > updatedTimeAwake - 60000000000L * RingProcessor.PROGRESSIVE_MOTION_WINDOW_MIN) {
                motionWithinWindow.add(trackerMotion);
            }
        }
        testRing = RingProcessor.getProgressiveRingTime(userInfo.accountId, new DateTime(2016, 10, 20, 2, 12, 0, userTimeZone), ringTime, motionWithinWindow, false);
        assertThat(testRing.isPresent(), is(true));

        motionWithinWindow = new ArrayList<>();
        for (TrackerMotion trackerMotion : trackerMotionsMockedMM) {
            if (trackerMotion.timestamp <= updatedTimeAsleep && trackerMotion.timestamp > updatedTimeAsleep - 60000000000L * RingProcessor.PROGRESSIVE_MOTION_WINDOW_MIN) {
                motionWithinWindow.add(trackerMotion);
            }
        }
        testRing = RingProcessor.getProgressiveRingTime(userInfo.accountId, new DateTime(2016, 10, 20, 2, 5, 0, userTimeZone), ringTime, motionWithinWindow, false);
        assertThat(testRing.isPresent(), is(false));

        motionWithinWindow = new ArrayList<>();
        for (TrackerMotion trackerMotion : trackerMotionsMockedMM) {
            if (trackerMotion.timestamp <= updatedTimeAwake && trackerMotion.timestamp > updatedTimeAwake - 60000000000L * RingProcessor.PROGRESSIVE_MOTION_WINDOW_MIN) {
                motionWithinWindow.add(trackerMotion);
            }
        }
        testRing = RingProcessor.getProgressiveRingTime(userInfo.accountId, new DateTime(2016, 10, 20, 2, 12, 0, userTimeZone), ringTime, motionWithinWindow, false);
        assertThat(testRing.isPresent(), is(true));
    }

    @Test
    public void testDecayThreshold(){
        final long ringTime = 1479420000000L;
        long currentTime = 1479418200000L;

        ProgressiveAlarmThresholds progressiveThreshold = ProgressiveAlarmThresholds.getDecayingThreshold(currentTime, ringTime, false);
        assertThat(progressiveThreshold.amplitudeThreshold, is(SleepCycleAlgorithm.AWAKE_AMPLITUDE_THRESHOLD_MILLIG));
        assertThat(progressiveThreshold.amplitudeThresholdCountLimit, is(SleepCycleAlgorithm.AWAKE_AMPLITUDE_THRESHOLD_COUNT_LIMIT));
        assertThat(progressiveThreshold.kickoffCountThreshold, is(SleepCycleAlgorithm.AWAKE_KICKOFF_THRESHOLD));
        assertThat(progressiveThreshold.onDurationThreshold, is(SleepCycleAlgorithm.AWAKE_ON_DURATION_THRESHOLD));

        currentTime = currentTime + 4 * DateTimeConstants.MILLIS_PER_MINUTE;

        progressiveThreshold = ProgressiveAlarmThresholds.getDecayingThreshold(currentTime, ringTime, true);
        assertThat(progressiveThreshold.amplitudeThreshold, is(SleepCycleAlgorithm.AWAKE_AMPLITUDE_THRESHOLD_MILLIG));
        assertThat(progressiveThreshold.amplitudeThresholdCountLimit, is(SleepCycleAlgorithm.AWAKE_AMPLITUDE_THRESHOLD_COUNT_LIMIT));
        assertThat(progressiveThreshold.kickoffCountThreshold, is(SleepCycleAlgorithm.AWAKE_KICKOFF_THRESHOLD));
        assertThat(progressiveThreshold.onDurationThreshold, is(SleepCycleAlgorithm.AWAKE_ON_DURATION_THRESHOLD));

        currentTime = currentTime + 6 * DateTimeConstants.MILLIS_PER_MINUTE;
        progressiveThreshold = ProgressiveAlarmThresholds.getDecayingThreshold(currentTime, ringTime, true);
        assertThat(progressiveThreshold.amplitudeThreshold, is(1830));
        assertThat(progressiveThreshold.amplitudeThresholdCountLimit, is(2));
        assertThat(progressiveThreshold.kickoffCountThreshold, is(3));
        assertThat(progressiveThreshold.onDurationThreshold, is(5));

        currentTime = currentTime + 6 * DateTimeConstants.MILLIS_PER_MINUTE;
        progressiveThreshold = ProgressiveAlarmThresholds.getDecayingThreshold(currentTime, ringTime, true);
        assertThat(progressiveThreshold.amplitudeThreshold, is(228));
        assertThat(progressiveThreshold.amplitudeThresholdCountLimit, is(1));
        assertThat(progressiveThreshold.kickoffCountThreshold, is(2));
        assertThat(progressiveThreshold.onDurationThreshold, is(2));
    }

}
