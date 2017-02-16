package com.hello.suripu.core.algorithmintegration;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.TrackerMotion;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jarredheinrich on 12/27/16.
 */
public class OneDaysTrackerMotion {
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




    public OneDaysTrackerMotion getMotionsForTimeWindow(final Long startTime, final Long endTime) {
        final List<TrackerMotion> currentPeriodProcessedtrackerMotions = new ArrayList<>();
        for (final TrackerMotion trackerMotion : this.processedtrackerMotions){
            if (trackerMotion.timestamp >=startTime && trackerMotion.timestamp < endTime){
                currentPeriodProcessedtrackerMotions.add(trackerMotion);
            }
        }
        final List<TrackerMotion> currentPeriodFilteredOriginalTrackerMotions = new ArrayList<>();
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

        return new OneDaysTrackerMotion(ImmutableList.copyOf(currentPeriodProcessedtrackerMotions), ImmutableList.copyOf(currentPeriodFilteredOriginalTrackerMotions), ImmutableList.copyOf(currentPeriodOriginalTrackerMotions));
    }
}
