package com.hello.suripu.core.util;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.TrackerMotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Created by benjo on 1/26/16.
 */
public class OutlierFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlierFilter.class);
    final public static long MIN_MOTION = 2L;
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

            //if it passes ANY of these criteria, keep the point
            if (m.onDurationInSeconds >= MIN_MOTION || m.value > MIN_MOTION_MAGNITUDE) {
                realPoints.add(m);
            }

        }

        //I managed to filter out all the points, good job.
        if (realPoints.isEmpty()) {
            return Lists.newArrayList();
        }


        List<TrackerMotion> currentGroupMotions = Lists.newArrayList();

        TrackerMotion prev = null;
        final List<MotionGroupsWithDuration> groupsWithDurations = Lists.newArrayList();
        //creates motion groups separated by 2 hours
        for (final Iterator<TrackerMotion> it = realPoints.iterator(); it.hasNext(); ) {
            final TrackerMotion m = it.next();

            if (prev != null) {
                final long tdiff = m.timestamp - prev.timestamp;
                //tdiff > 2 hours
                if (tdiff  > outlierGuardDurationMillis) {
                    groupsWithDurations.add(new MotionGroupsWithDuration(currentGroupMotions));
                    currentGroupMotions = Lists.newArrayList();
                }
            }

            prev = m;
            currentGroupMotions.add(m);

        }

        groupsWithDurations.add(new MotionGroupsWithDuration(currentGroupMotions));


        //find max group
        long maxDuration = 0;
        int maxIdx = 0;
        for (int i = 0; i < groupsWithDurations.size(); i++) {
            final long duration = groupsWithDurations.get(i).duration;

            if (duration > maxDuration) {
                maxIdx = i;
                maxDuration = duration;
            }
        }


        List<TrackerMotion> returnedPoints = trackerMotions;

        //THE RULE IS -- IF YOU HAVE A GROUP WITH DURATION > N HOURS as the longest duration, discard what comes after
        //unless the group that comes after is too large
        if (maxDuration > dominantGroupDuration) {
            //add everything before and including the dominant group -- only discarding on the tail end
            final List<TrackerMotion> motions = Lists.newArrayList();
            for (int i = 0; i <= maxIdx; i++) {
                motions.addAll(groupsWithDurations.get(i).motions);
            }

            returnedPoints = motions;
        }

        LOGGER.info("action=discard_pill_points num_points={} num_groups={}",trackerMotions.size() - returnedPoints.size(),groupsWithDurations.size() - maxIdx - 1);

        return returnedPoints;
    }
}
