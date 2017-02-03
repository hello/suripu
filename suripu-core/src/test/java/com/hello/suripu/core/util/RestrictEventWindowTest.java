package com.hello.suripu.core.util;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.TrackerMotion;
import org.junit.Test;

import java.util.List;

/**
 * Created by jarredheinrich on 2/2/17.
 */
public class RestrictEventWindowTest {
    @Test
    public void testMotionDuringWindow(){
        final List<TrackerMotion> trackerMotions = CSVLoader.loadTrackerMotionFromCSV("fixtures/tracker_motion/nn_raw_tracker_motion.csv");
        final Long checkTime1 = 1463817198000L;
        final Long checkTime2 = 1463852896000L;
        final int windowDuration = 60;
        final int newMotionCountThreshold = 2;
        boolean hasMotionDuringWindow = RestrictEventWindow.motionDuringWindow(trackerMotions,checkTime1, windowDuration, newMotionCountThreshold);
        assert(hasMotionDuringWindow);
        hasMotionDuringWindow = RestrictEventWindow.motionDuringWindow(trackerMotions,checkTime2, windowDuration, newMotionCountThreshold);
        assert(!hasMotionDuringWindow);
    }

    @Test
    public void testRemoveSubsequentMotions(){
        List<TrackerMotion> trackerMotions = CSVLoader.loadTrackerMotionFromCSV("fixtures/tracker_motion/nn_raw_tracker_motion.csv");
        final Long checkTime2 = 1463852896000L;
        assert(trackerMotions.size() == 85);
        trackerMotions = RestrictEventWindow.removeSubsequentMotions(ImmutableList.copyOf(trackerMotions),checkTime2);
        assert(trackerMotions.size() == 83);
    }

}
