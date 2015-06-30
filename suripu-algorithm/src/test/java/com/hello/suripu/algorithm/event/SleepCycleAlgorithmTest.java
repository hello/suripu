package com.hello.suripu.algorithm.event;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 6/30/15.
 */
public class SleepCycleAlgorithmTest {
    @Test
    public void testFakeSmartAlarmWhenCurrentTimeApproachedAlarmSetTime(){
        final DateTime alarmSetTime = new DateTime(2015, 6, 30, 0, 0, 0, DateTimeZone.UTC);
        final DateTime currentTime = alarmSetTime.minusMinutes(2);

        final DateTime fakeSmartAlarmTime = SleepCycleAlgorithm.fakeSmartAlarm(currentTime.getMillis(), alarmSetTime.getMillis());
        assertThat(fakeSmartAlarmTime.isAfter(alarmSetTime), is(false));
    }

    @Test
    public void  testFakeSmartAlarm(){
        final DateTime alarmSetTime = new DateTime(2015, 6, 30, 0, 0, 0, DateTimeZone.UTC);
        final DateTime currentTime = alarmSetTime.minusMinutes(40);

        final DateTime fakeSmartAlarmTime = SleepCycleAlgorithm.fakeSmartAlarm(currentTime.getMillis(), alarmSetTime.getMillis());
        assertThat(fakeSmartAlarmTime.isAfter(alarmSetTime), is(false));
        assertThat(fakeSmartAlarmTime.isBefore(alarmSetTime.minusMinutes(15)), is(false));

    }
}
