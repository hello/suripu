package com.hello.suripu.core.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.TrackerMotion;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Created by benjo on 2/11/16.
 */
public class TrackerMotionUtilsTest {

    @Test
    public void testGetIndex() {
        final int periodInMinutes = 5;
        final long t0 = (DateTime.now().getMillis() / (long)DateTimeConstants.MILLIS_PER_MINUTE) * (long)DateTimeConstants.MILLIS_PER_MINUTE;
        final int max = 192;
        final long period= DateTimeConstants.MILLIS_PER_MINUTE * periodInMinutes;
        for (int i = - 20; i < max*periodInMinutes + 20; i++) {
            final long tstamp = t0 + i*DateTimeConstants.MILLIS_PER_MINUTE;

            final int idx = TrackerMotionUtils.getIndex(tstamp,t0,period,max);

            int refIdx = i / periodInMinutes;

            //invalid indicies should be a -1
            if (refIdx < 0) {
                refIdx = -1;
            }

            if (refIdx >= max) {
                refIdx = -1;
            }

            TestCase.assertEquals(refIdx,idx);

        }
//    public static int getIndex(final Long timestamp, final Long t0,final Long period, final int max) {

    }

    @Test
    public void testFillTrackerMotionBins() {
        final int periodInMinutes = 5;
        final long t0 = (DateTime.now().getMillis() / (long)DateTimeConstants.MILLIS_PER_MINUTE) * (long)DateTimeConstants.MILLIS_PER_MINUTE;
        final int max = 10;
        final long period= DateTimeConstants.MILLIS_PER_MINUTE * periodInMinutes;

        final Double [] bins = new Double[10];
        Arrays.fill(bins,0.0);

        List<TrackerMotion> trackerMotionList = Lists.newArrayList();
/*
        public TrackerMotion(@JsonProperty("id") final long id,
        @JsonProperty("account_id") final long accountId,
        @JsonProperty("tracker_id") final Long trackerId,
        @JsonProperty("timestamp") final long timestamp,
        @JsonProperty("value") final int value,
        @JsonProperty("timezone_offset") final int timeZoneOffset,
        final Long motionRange,
        final Long kickOffCounts,
        final Long onDurationInSeconds) {
        */
        trackerMotionList.add(new TrackerMotion(0,0L,0L,t0 + 0*DateTimeConstants.MILLIS_PER_MINUTE,0,0,0L,0L,3L));
        trackerMotionList.add(new TrackerMotion(0,0L,0L,t0 + 2*DateTimeConstants.MILLIS_PER_MINUTE,0,0,0L,0L,3L));

        trackerMotionList.add(new TrackerMotion(0,0L,0L,t0 + 5*DateTimeConstants.MILLIS_PER_MINUTE,0,0,0L,0L,3L));


        TrackerMotionUtils.fillBinsWithTrackerDurations(bins,t0,period, ImmutableList.copyOf(trackerMotionList),1,false);

        TestCase.assertEquals(6.0,bins[0]);
        TestCase.assertEquals(3.0,bins[1]);
        TestCase.assertEquals(0.0,bins[2]);


        Arrays.fill(bins,0.0);

        TrackerMotionUtils.fillBinsWithTrackerDurations(bins,t0,period, ImmutableList.copyOf(trackerMotionList),1,true);

        TestCase.assertEquals(7.0,bins[0]);
        TestCase.assertEquals(2.0,bins[1]);
        TestCase.assertEquals(0.0,bins[2]);



    }
}
