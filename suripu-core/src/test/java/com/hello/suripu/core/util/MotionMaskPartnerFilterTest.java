package com.hello.suripu.core.util;

import com.hello.suripu.core.models.TrackerMotion;
import org.junit.Test;

import java.util.List;

/**
 * Created by jarredheinrich on 1/30/17.
 */
public class MotionMaskPartnerFilterTest {
    @Test
    public void testPartnerFiltering() {
        final List<TrackerMotion> trackerMotions = CSVLoader.loadTrackerMotionFromCSV("fixtures/tracker_motion/mm_pill_data1.csv");
        final List<TrackerMotion> partnerTrackerMotions = CSVLoader.loadTrackerMotionFromCSV("fixtures/tracker_motion/mm_pill_data2.csv");
        final List<TrackerMotion> filteredTrackerMotions = MotionMaskPartnerFilter.partnerFiltering(trackerMotions, partnerTrackerMotions);
        assert(filteredTrackerMotions.size() == 72);
    }

}
