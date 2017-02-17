package com.hello.suripu.algorithm.event;

import com.hello.suripu.algorithm.core.AmplitudeData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 6/30/15.
 */
public class SleepCycleAlgorithmTest {
    @Test
    public void testFakeSmartAlarmWhenCurrentTimeApproachedAlarmSetTime(){
        final DateTime alarmSetTime = new DateTime(2015, 6, 30, 0, 0, 0, DateTimeZone.UTC);
        final DateTime currentTime = alarmSetTime;

        final DateTime fakeSmartAlarmTime = SleepCycleAlgorithm.fakeSmartAlarm(currentTime.getMillis(), alarmSetTime.getMillis());
        assertThat(fakeSmartAlarmTime.isAfter(alarmSetTime), is(false));
    }

    @Test
    public void  testFakeSmartAlarm(){
        final DateTime alarmSetTime = new DateTime(2015, 6, 30, 0, 0, 0, DateTimeZone.UTC);
        final DateTime currentTime = alarmSetTime.minusMinutes(30);

        final DateTime fakeSmartAlarmTime = SleepCycleAlgorithm.fakeSmartAlarm(currentTime.getMillis(), alarmSetTime.getMillis());
        assertThat(fakeSmartAlarmTime.isAfter(alarmSetTime), is(false));

    }

    @Test
    public void testIsUserAwakeInGivenDataSpan() {
        List<AmplitudeData> amplitudeDatasAwakeOD = new ArrayList<>();
        List<AmplitudeData> amplitudeDatasAwakeKickOffs = new ArrayList<>();
        List<AmplitudeData> amplitudeDatasAwakeMotion = new ArrayList<>();
        List<AmplitudeData> amplitudeDatasSleeping = new ArrayList<>();

        for (int i = 0; i<7; i+=1){
            final AmplitudeData amplitudeDataOD = new AmplitudeData(0L, SleepCycleAlgorithm.AWAKE_ON_DURATION_THRESHOLD +1, 0);
            final AmplitudeData amplitudeDataKickoff = new AmplitudeData(0L, SleepCycleAlgorithm.AWAKE_KICKOFF_THRESHOLD +1, 0);
            final AmplitudeData amplitudeDataMotion = new AmplitudeData(0L, SleepCycleAlgorithm.AWAKE_AMPLITUDE_THRESHOLD_MILLIG -4 + i, 0);
            final AmplitudeData amplitudeDataSleeping = new AmplitudeData(0L, 0.0, 0);

            amplitudeDatasAwakeOD.add(amplitudeDataOD);
            amplitudeDatasAwakeKickOffs.add(amplitudeDataKickoff);
            amplitudeDatasAwakeMotion.add(amplitudeDataMotion);
            amplitudeDatasSleeping.add(amplitudeDataSleeping);
        }

        boolean isAwake = SleepCycleAlgorithm.isUserAwakeInGivenDataSpan(amplitudeDatasSleeping,amplitudeDatasSleeping,amplitudeDatasSleeping,true);
        assertThat(isAwake, is(false));
        isAwake = SleepCycleAlgorithm.isUserAwakeInGivenDataSpan(amplitudeDatasAwakeMotion,amplitudeDatasSleeping,amplitudeDatasSleeping,true);
        assertThat(isAwake, is(true));
        isAwake = SleepCycleAlgorithm.isUserAwakeInGivenDataSpan(amplitudeDatasSleeping,amplitudeDatasAwakeKickOffs,amplitudeDatasSleeping,true);
        assertThat(isAwake, is(true));
        isAwake = SleepCycleAlgorithm.isUserAwakeInGivenDataSpan(amplitudeDatasSleeping,amplitudeDatasSleeping,amplitudeDatasAwakeOD,true);
        assertThat(isAwake, is(true));
        isAwake = SleepCycleAlgorithm.isUserAwakeInGivenDataSpan(amplitudeDatasSleeping,amplitudeDatasSleeping,amplitudeDatasAwakeOD,false);
        assertThat(isAwake, is(false));
    }

}