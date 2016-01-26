package com.hello.suripu.core.util;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.TrackerMotion;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.Test;

import java.util.List;

/**
 * Created by benjo on 1/26/16.
 */
public class OutlierFilterTest {


    @Test
    public void testOutlierFilter() {
        final List<TrackerMotion> nominalTrackerMotion = Lists.newArrayList();
        final long t0 = 1453831241000L;

        for (int i = 0; i < 8; i++) {
            nominalTrackerMotion.add(new TrackerMotion(0L,0L,0L,t0 + i*DateTimeConstants.MILLIS_PER_HOUR,4999,0,0L,0L,3L));
        }

        final List<TrackerMotion> separatedTrackerMotions = Lists.newArrayList();

        for (int i = 0; i < 4; i++) {
            separatedTrackerMotions.add(new TrackerMotion(0L,0L,0L,t0 + i*DateTimeConstants.MILLIS_PER_HOUR,5000,0,0L,0L,3L));
        }

        for (int i = 0; i < 9; i++) {
            separatedTrackerMotions.add(new TrackerMotion(0L,0L,0L,t0 + i*DateTimeConstants.MILLIS_PER_HOUR + DateTimeConstants.MILLIS_PER_DAY,5001,0,0L,0L,3L));
        }

        for (int i = 0; i < 2; i++) {
            separatedTrackerMotions.add(new TrackerMotion(0L,0L,0L,t0 + i*DateTimeConstants.MILLIS_PER_HOUR +  2*DateTimeConstants.MILLIS_PER_DAY,5002,0,0L,0L,3L));
        }


        final List<TrackerMotion> motionsNominal = OutlierFilter.removeOutliers(nominalTrackerMotion,DateTimeConstants.MILLIS_PER_HOUR*5,DateTimeConstants.MILLIS_PER_HOUR*3);
        TestCase.assertTrue(motionsNominal.size() == 8);

        final List<TrackerMotion> motionsSeparated = OutlierFilter.removeOutliers(separatedTrackerMotions,DateTimeConstants.MILLIS_PER_HOUR*5,DateTimeConstants.MILLIS_PER_HOUR*3);
        TestCase.assertTrue(motionsSeparated.size() == 9);
        for (int i = 0; i < 9; i++) {
            TestCase.assertTrue(motionsSeparated.get(i).value == 5001);
        }


        separatedTrackerMotions.add(new TrackerMotion(0L,0L,0L,t0 -DateTimeConstants.MILLIS_PER_HOUR  + DateTimeConstants.MILLIS_PER_DAY,100,0,0L,0L,1L));
        final List<TrackerMotion> motionsSeparated2 = OutlierFilter.removeOutliers(separatedTrackerMotions,DateTimeConstants.MILLIS_PER_HOUR*5,DateTimeConstants.MILLIS_PER_HOUR*3);
        TestCase.assertTrue(motionsSeparated.size() == 9);

    }

}
