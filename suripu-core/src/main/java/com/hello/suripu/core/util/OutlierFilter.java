package com.hello.suripu.core.util;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.TrackerMotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by benjo on 1/26/16.
 */
public class OutlierFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlierFilter.class);
    final public static long MIN_MOTION = 1L;
    final public static int MIN_MOTION_MAGNITUDE = 300;

    protected static class MotionGroupsWithDuration implements  Comparable<MotionGroupsWithDuration> {
        public final long duration;
        public final List<TrackerMotion> motions;

        // ASSUMES MOTIONS ARE ALREADY TIME-SORTED
        public MotionGroupsWithDuration(final List<TrackerMotion> motions) {
            this.motions = motions;

            if (this.motions.isEmpty()) {
                duration = 0;
            }
            else {
                duration = motions.get(motions.size() - 1).timestamp - motions.get(0).timestamp;
            }
        }


        @Override
        public int compareTo(MotionGroupsWithDuration o) {
            if (o.duration < this.duration) {
                return -1;
            }

            if (o.duration > this.duration) {
                return 1;
            }

            return 0;
        }
    }

    public static List<TrackerMotion> removeOutliers(final List<TrackerMotion> trackerMotions, final long outlierGuardDurationMillis, final long dominantGroupDuration) {

        if (trackerMotions.isEmpty()) {
            return Lists.newArrayList();
        }


        // FILTER OUT ALL "SMALL" MOTIONS"
        final List<TrackerMotion> realPoints = Lists.newArrayList();
        for (final Iterator<TrackerMotion> it = trackerMotions.iterator(); it.hasNext(); ) {
            final TrackerMotion m = it.next();

            if (m.onDurationInSeconds > MIN_MOTION || m.value > MIN_MOTION_MAGNITUDE) {
                realPoints.add(m);
            }

        }

        //I managed to filter out all the points, good job.
        if (realPoints.isEmpty()) {
            return Lists.newArrayList();
        }

        final List<List<TrackerMotion>> groups = Lists.newArrayList();

        List<TrackerMotion> currentGroup = Lists.newArrayList();

        TrackerMotion prev = null;
        for (final Iterator<TrackerMotion> it = realPoints.iterator(); it.hasNext(); ) {
            final TrackerMotion m = it.next();



            if (prev != null) {
                final long tdiff = m.timestamp - prev.timestamp;

                if (tdiff  > outlierGuardDurationMillis) {
                    groups.add(currentGroup);
                    currentGroup = Lists.newArrayList();
                }

            }

            prev = m;
            currentGroup.add(m);

        }

        groups.add(currentGroup);

        //sort groups
        List<MotionGroupsWithDuration> groupsWithDurations = Lists.newArrayList();

        for (final List<TrackerMotion> motions : groups) {
            groupsWithDurations.add(new MotionGroupsWithDuration(motions));
        }

        Collections.sort(groupsWithDurations);

        //THE RULE IS -- IF YOU HAVE A GROUP WITH DURATION > N HOURS as the longest duration, discard the rest.
        if (groupsWithDurations.size() > 1) {
            if (groupsWithDurations.get(0).duration > dominantGroupDuration) {

                int pointcount = 0;

                for (int i = 1; i < groupsWithDurations.size(); i++) {
                    pointcount += groupsWithDurations.get(i).motions.size();
                }

                LOGGER.info("action=discard_my_tracker_motion num_points={}",pointcount);

                return groupsWithDurations.get(0).motions;
            }
        }

        return trackerMotions;
    }
}
