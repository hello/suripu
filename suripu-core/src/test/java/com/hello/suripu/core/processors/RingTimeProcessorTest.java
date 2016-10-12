package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.core.models.Alarm;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.UserInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

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
}
