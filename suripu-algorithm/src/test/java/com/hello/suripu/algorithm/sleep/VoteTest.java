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
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 03:21:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 04:16:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 11:13:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 11:13:00")));
    }

    @Test
    public void testGetResultNormalFiltered(){
        final List<AmplitudeData> input = loadFromResource("fixtures/jp_motion_2015_03_20_raw.csv");
        final List<DateTime> lightOuts = new ArrayList<>();
        lightOuts.add(new DateTime(1426924080000L, DateTimeZone.forOffsetMillis(input.get(0).offsetMillis)));
        final Vote vote = new Vote(input, lightOuts, Optional.<DateTime>absent());
        final SleepEvents<Segment> sleepEvents = vote.getResult(true);
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.goToBed.getStartTimestamp(), sleepEvents.goToBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-21 00:14:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-21 01:20:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-21 09:02:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-21 10:08:00")));
    }

    @Test
    public void testGetResultShouldNotAdjustWakeUp(){
        final List<AmplitudeData> input = loadFromResource("fixtures/qf_motion_2015_03_12_raw.csv");
        final List<DateTime> lightOuts = new ArrayList<>();
        lightOuts.add(new DateTime(1426218480000L, DateTimeZone.forOffsetMillis(input.get(0).offsetMillis)));
        final Vote vote = new Vote(input, lightOuts, Optional.<DateTime>absent());
        final SleepEvents<Segment> sleepEvents = vote.getResult(true);

        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.goToBed.getStartTimestamp(), sleepEvents.goToBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-12 20:28:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-12 21:34:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 08:01:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-13 08:56:00")));
    }

    @Test
    public void testGetResultTimeZoneChanged(){
        final List<AmplitudeData> input = loadFromResource("fixtures/km_motion_2015_03_01_raw.csv");
        final List<DateTime> lightOuts = new ArrayList<>();
        lightOuts.add(new DateTime(1425275520000L, DateTimeZone.forOffsetMillis(input.get(0).offsetMillis)));
        final Vote vote = new Vote(input, lightOuts, Optional.of(new DateTime(1425311460000L, DateTimeZone.UTC)));
        final SleepEvents<Segment> sleepEvents = vote.getResult(true);

        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.goToBed.getStartTimestamp(), sleepEvents.goToBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-01 21:28:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-01 23:29:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-02 07:55:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-02 08:17:00")));
    }


    @Test
    public void testGetResultSleepClusterEdgeCase(){
        final List<AmplitudeData> input = loadFromResource("fixtures/rb_motion_2015_03_11_raw.csv");
        final List<DateTime> lightOuts = new ArrayList<>();
        final Vote vote = new Vote(input, lightOuts, Optional.of(new DateTime(1426173060000L, DateTimeZone.UTC)));
        final SleepEvents<Segment> sleepEvents = vote.getResult(true);

        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.goToBed.getStartTimestamp(), sleepEvents.goToBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-11 23:53:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-12 00:59:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-12 04:17:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-12 07:35:00")));
    }

    @Test
    public void testGetResultSmoothCluster(){
        final List<AmplitudeData> input = loadFromResource("fixtures/ksg_motion_2015_03_22_raw.csv");
        final List<DateTime> lightOuts = new ArrayList<>();
        lightOuts.add(new DateTime(1427093280000L, DateTimeZone.UTC));
        final Vote vote = new Vote(input, lightOuts, Optional.<DateTime>absent());
        final SleepEvents<Segment> sleepEvents = vote.getResult(true);

        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.goToBed.getStartTimestamp(), sleepEvents.goToBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-22 23:53:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-23 00:15:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-23 08:19:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-23 09:47:00")));
    }

    @Test
    public void testGetResultTestKSGScript(){
        final List<AmplitudeData> input = loadFromResource("fixtures/km_motion_2015_03_23_raw.csv");
        final List<DateTime> lightOuts = new ArrayList<>();
        lightOuts.add(new DateTime(1427174340000L, DateTimeZone.UTC));
        final Vote vote = new Vote(input, lightOuts, Optional.<DateTime>absent());
        final SleepEvents<Segment> sleepEvents = vote.getResult(true);

        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.goToBed.getStartTimestamp(), sleepEvents.goToBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-23 21:49:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-23 23:28:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-24 07:32:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-24 07:54:00")));
    }
}
