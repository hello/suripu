package com.hello.suripu.core.algorithmintegration;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.OutlierFilter;
import org.joda.time.DateTimeConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jarredheinrich on 12/27/16.
 */
public class OneDaysTrackerMotion {
    final private static long OUTLIER_GUARD_DURATION = (long) (DateTimeConstants.MILLIS_PER_HOUR * 2.0); //min spacing between motion groups
    final private static long DOMINANT_GROUP_DURATION = (long) (DateTimeConstants.MILLIS_PER_HOUR * 6.0); //num hours in a motion group to be considered the dominant one

    public final ImmutableList<TrackerMotion> processedtrackerMotions;
    public final ImmutableList<TrackerMotion> filteredOriginalTrackerMotions;
    public final ImmutableList<TrackerMotion> originalTrackerMotions;

    public OneDaysTrackerMotion (final ImmutableList<TrackerMotion> processedtrackerMotions,ImmutableList<TrackerMotion> filteredOriginalTrackerMotions, ImmutableList<TrackerMotion> originalTrackerMotions ){
        this.processedtrackerMotions = processedtrackerMotions;
        this.filteredOriginalTrackerMotions = filteredOriginalTrackerMotions;
        this.originalTrackerMotions = originalTrackerMotions;
    }

    public OneDaysTrackerMotion (final ImmutableList<TrackerMotion>  trackerMotions ){
        this.processedtrackerMotions = trackerMotions;
        this.filteredOriginalTrackerMotions = trackerMotions;
        this.originalTrackerMotions = trackerMotions;
    }


    public OneDaysTrackerMotion getMotionsForTimeWindow(final Long startTime, final Long endTime, final boolean useOutlierFilter) {
        List<TrackerMotion> currentPeriodProcessedtrackerMotions = new ArrayList<>();
        for (final TrackerMotion trackerMotion : this.processedtrackerMotions){
            if (trackerMotion.timestamp >=startTime && trackerMotion.timestamp < endTime){
                currentPeriodProcessedtrackerMotions.add(trackerMotion);
            }
        }
        List<TrackerMotion> currentPeriodFilteredOriginalTrackerMotions = new ArrayList<>();
        for (final TrackerMotion trackerMotion : this.filteredOriginalTrackerMotions){
            if (trackerMotion.timestamp >=startTime && trackerMotion.timestamp < endTime){
                currentPeriodFilteredOriginalTrackerMotions.add(trackerMotion);
            }
        }
        final List<TrackerMotion> currentPeriodOriginalTrackerMotions = new ArrayList<>();
        for (final TrackerMotion trackerMotion : this.originalTrackerMotions){
            if (trackerMotion.timestamp  >=startTime && trackerMotion.timestamp < endTime){
                currentPeriodOriginalTrackerMotions.add(trackerMotion);
            }
        }
        if(useOutlierFilter) {
            if (currentPeriodFilteredOriginalTrackerMotions.size() > 0) {
                currentPeriodFilteredOriginalTrackerMotions = OutlierFilter.removeOutliers(currentPeriodFilteredOriginalTrackerMotions, OUTLIER_GUARD_DURATION, DOMINANT_GROUP_DURATION);
            }
            if (currentPeriodProcessedtrackerMotions.size() > 0) {
                currentPeriodProcessedtrackerMotions = OutlierFilter.removeOutliers(currentPeriodProcessedtrackerMotions, OUTLIER_GUARD_DURATION, DOMINANT_GROUP_DURATION);
            }
        }
        return new OneDaysTrackerMotion(ImmutableList.copyOf(currentPeriodProcessedtrackerMotions), ImmutableList.copyOf(currentPeriodFilteredOriginalTrackerMotions), ImmutableList.copyOf(currentPeriodOriginalTrackerMotions));
    }
}
