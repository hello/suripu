package com.hello.suripu.core.util;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTimeConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jarredheinrich on 2/2/17.
 */
public class RestrictEventWindow {

    public static boolean motionDuringWindow(final List<TrackerMotion> trackerMotions, final Long windowStartTime, final int windowDurationMinutes, final int newMotionCountThreshold){
        final int newMotionTimeWindow = windowDurationMinutes * DateTimeConstants.MILLIS_PER_MINUTE;
        int newMotionCount = 0;
        for(final TrackerMotion trackermotion : trackerMotions){
            if (trackermotion.timestamp > windowStartTime && trackermotion.timestamp <= windowStartTime + newMotionTimeWindow){
                newMotionCount +=1;
            }
        }
        if (newMotionCount > newMotionCountThreshold){
            return true;
        }

        return false;
    }

    public static ImmutableList<TrackerMotion> removeSubsequentMotions(final ImmutableList<TrackerMotion> trackerMotions, final long endTime){
        final List<TrackerMotion> truncatedTrackerMotions = new ArrayList<>();
        for (final TrackerMotion trackerMotion : trackerMotions){
            if(trackerMotion.timestamp <= endTime){
                truncatedTrackerMotions.add(trackerMotion);
            }
        }

        return ImmutableList.copyOf(truncatedTrackerMotions);
    }

}
