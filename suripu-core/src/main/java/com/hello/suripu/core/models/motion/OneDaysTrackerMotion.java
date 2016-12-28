package com.hello.suripu.core.models.motion;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.TrackerMotion;

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


}
