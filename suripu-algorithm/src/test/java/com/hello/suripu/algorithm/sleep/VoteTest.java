package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.CSVFixtureTest;
import com.hello.suripu.algorithm.DateTimeTestUtils;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 3/19/15.
 */
public class VoteTest extends CSVFixtureTest {
    @Test
    public void testGetResultPetFiltered(){
        final List<AmplitudeData> input = loadFromResource("fixtures/cl_motion_2015_03_12_raw.csv");
        final List<DateTime> lightOuts = new ArrayList<>();
        lightOuts.add(new DateTime(1426213320000L, DateTimeZone.forOffsetMillis(input.get(0).offsetMillis)));
        lightOuts.add(new DateTime(1426231620000L, DateTimeZone.forOffsetMillis(input.get(0).offsetMillis)));
        final Vote vote = new Vote(input, lightOuts, Optional.<DateTime>absent());
        final SleepEvents<Segment> sleepEvents = vote.getResult(true);
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.goToBed.getStartTimestamp(), sleepEvents.goToBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 03:19:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 04:14:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 11:12:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 11:12:00")));
    }

    @Test
    public void testGetResultNormalFiltered(){
        final List<AmplitudeData> input = loadFromResource("fixtures/jp_motion_2015_03_20_raw.csv");
        final List<DateTime> lightOuts = new ArrayList<>();
        lightOuts.add(new DateTime(1426924080000L, DateTimeZone.forOffsetMillis(input.get(0).offsetMillis)));
        final Vote vote = new Vote(input, lightOuts, Optional.<DateTime>absent());
        final SleepEvents<Segment> sleepEvents = vote.getResult(true);
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.goToBed.getStartTimestamp(), sleepEvents.goToBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-21 00:12:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-21 01:18:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-21 09:00:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-21 10:06:00")));
    }

    @Test
    public void testGetResultShouldNotAdjustWakeUp(){
        final List<AmplitudeData> input = loadFromResource("fixtures/qf_motion_2015_03_12_raw.csv");
        final List<DateTime> lightOuts = new ArrayList<>();
        lightOuts.add(new DateTime(1426218480000L, DateTimeZone.forOffsetMillis(input.get(0).offsetMillis)));
        final Vote vote = new Vote(input, lightOuts, Optional.<DateTime>absent());
        final SleepEvents<Segment> sleepEvents = vote.getResult(true);

        assertThat((float)vote.getMotionClusterAlgorithm().getDensityMean(), is((float)3.65789473684));
        assertThat((float)vote.getMotionClusterAlgorithm().getStd(), is((float)1.88008958353));

        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.goToBed.getStartTimestamp(), sleepEvents.goToBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-12 20:59:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-12 21:32:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 07:59:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 09:05:00")));
    }

    @Test
    public void testGetResultTimeZoneChanged(){
        final List<AmplitudeData> input = loadFromResource("fixtures/km_motion_2015_03_01_raw.csv");
        final List<DateTime> lightOuts = new ArrayList<>();
        lightOuts.add(new DateTime(1425275520000L, DateTimeZone.forOffsetMillis(input.get(0).offsetMillis)));
        final Vote vote = new Vote(input, lightOuts, Optional.of(new DateTime(1425311460000L, DateTimeZone.UTC)));
        final SleepEvents<Segment> sleepEvents = vote.getResult(true);

        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.goToBed.getStartTimestamp(), sleepEvents.goToBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-01 21:26:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-01 23:27:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-02 07:53:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-02 08:04:00")));
    }


    @Test
    public void testGetResultSleepClusterEdgeCase(){
        final List<AmplitudeData> input = loadFromResource("fixtures/rb_motion_2015_03_11_raw.csv");
        final List<DateTime> lightOuts = new ArrayList<>();
        final Vote vote = new Vote(input, lightOuts, Optional.of(new DateTime(1426173060000L, DateTimeZone.UTC)));
        final SleepEvents<Segment> sleepEvents = vote.getResult(true);

        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.goToBed.getStartTimestamp(), sleepEvents.goToBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-11 23:51:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-12 00:57:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-12 04:48:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-12 07:33:00")));
    }
}
