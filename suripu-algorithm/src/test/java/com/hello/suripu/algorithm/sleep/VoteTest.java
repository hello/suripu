package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.CSVFixtureTest;
import com.hello.suripu.algorithm.DateTimeTestUtils;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import org.joda.time.DateTime;
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
    public void testGetResultNormalFiltered(){
        final List<AmplitudeData> input = loadAmpFromResource("fixtures/jp_motion_2015_03_23_raw.csv");
        final List<AmplitudeData> kickoffs = loadKickOffFromResource("fixtures/jp_motion_2015_03_23_raw.csv");

        final List<DateTime> lightOuts = new ArrayList<>();
        //lightOuts.add(new DateTime(1426924080000L, DateTimeZone.forOffsetMillis(input.get(0).offsetMillis)));
        final Vote vote = new Vote(input, kickoffs, lightOuts, Optional.<DateTime>absent());
        final SleepEvents<Segment> sleepEvents = vote.getResult(true);


        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.goToBed.getStartTimestamp(), sleepEvents.goToBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-23 22:18:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-23 23:24:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-24 08:45:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-24 08:56:00")));
    }


    @Test
    public void testGetResultDebug(){
        final List<AmplitudeData> input = loadAmpFromResource("fixtures/debug.csv");
        final List<AmplitudeData> kickoffs = loadKickOffFromResource("fixtures/debug.csv");

        final List<DateTime> lightOuts = new ArrayList<>();
        //lightOuts.add(new DateTime(1426924080000L, DateTimeZone.forOffsetMillis(input.get(0).offsetMillis)));
        final Vote vote = new Vote(input, kickoffs, lightOuts, Optional.<DateTime>absent());
        final SleepEvents<Segment> sleepEvents = vote.getResult(true);

        /*
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.goToBed.getStartTimestamp(), sleepEvents.goToBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-23 22:18:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.fallAsleep.getStartTimestamp(), sleepEvents.fallAsleep.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-23 23:24:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.wakeUp.getStartTimestamp(), sleepEvents.wakeUp.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-24 08:45:00")));
        assertThat(DateTimeTestUtils.millisToLocalUTC(sleepEvents.outOfBed.getStartTimestamp(), sleepEvents.outOfBed.getOffsetMillis()),
                is(DateTimeTestUtils.stringToLocalUTC("2015-03-24 08:56:00")));
                */
    }
}
