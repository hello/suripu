package com.hello.suripu.core.algorithmintegration;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TrackerMotion;

/**
 * Created by benjo on 8/20/15.
 */
public class OneDaysSensorData {
    public final AllSensorSampleList allSensorSampleList;
    public final ImmutableList<TrackerMotion> trackerMotions;
    public final ImmutableList<TrackerMotion> partnerMotions;
    public final ImmutableList<TimelineFeedback> feedbackList;
    public final int timezoneOffsetMillis;
    public final long timeOfQueryUTC;

    public OneDaysSensorData(AllSensorSampleList allSensorSampleList,final ImmutableList<TrackerMotion> trackerMotions,final ImmutableList<TrackerMotion> partnerMotions,
                             final ImmutableList<TimelineFeedback> feedbackList, final int timezoneOffsetMillis, final long timeOfQueryUTC) {
        this.allSensorSampleList = allSensorSampleList;
        this.trackerMotions = trackerMotions;
        this.partnerMotions = partnerMotions;
        this.feedbackList = feedbackList;
        this.timezoneOffsetMillis = timezoneOffsetMillis;
        this.timeOfQueryUTC = timeOfQueryUTC;
    }
}
