package com.hello.suripu.core.algorithmintegration;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserBioInfo;
import org.joda.time.DateTime;

/**
 * Created by benjo on 8/20/15.
 */
public class OneDaysSensorData {
    public final AllSensorSampleList allSensorSampleList;
    public final ImmutableList<TrackerMotion> trackerMotions;
    public final ImmutableList<TrackerMotion> partnerMotions;
    public final ImmutableList<TimelineFeedback> feedbackList;
    public final ImmutableList<TrackerMotion> originalTrackerMotions;
    public final ImmutableList<TrackerMotion> originalPartnerTrackerMotions;
    public final ImmutableList<TrackerMotion> nonfilteredOriginalTrackerMotions;
    public final ImmutableList<TrackerMotion> nonfilteredOriginalPartnerTrackerMotions;
    public final int timezoneOffsetMillis;
    public final DateTime date;
    public final DateTime startTimeLocalUTC;
    public final DateTime endTimeLocalUTC;
    public final DateTime currentTimeUTC;
    public final UserBioInfo userBioInfo;


    public OneDaysSensorData(final AllSensorSampleList allSensorSampleList,
                             final ImmutableList<TrackerMotion> trackerMotions, final ImmutableList<TrackerMotion> partnerMotions,
                             final ImmutableList<TimelineFeedback> feedbackList,
                             final ImmutableList<TrackerMotion> originalTrackerMotions, final ImmutableList<TrackerMotion> originalPartnerTrackerMotions,
                             final ImmutableList<TrackerMotion> nonfilteredOriginalTrackerMotions, final ImmutableList<TrackerMotion> nonFilteredOriginalPartnerTrackerMotions,
                             final DateTime date, final DateTime startTimeLocalUTC, final DateTime endTimeLocalUTC, final DateTime currentTimeUTC,
                             final int timezoneOffsetMillis, final UserBioInfo userBioInfo) {
        this.allSensorSampleList = allSensorSampleList;
        this.trackerMotions = trackerMotions;
        this.partnerMotions = partnerMotions;
        this.feedbackList = feedbackList;
        this.originalTrackerMotions = originalTrackerMotions;
        this.originalPartnerTrackerMotions = originalPartnerTrackerMotions;
        this.nonfilteredOriginalTrackerMotions = nonfilteredOriginalTrackerMotions;
        this.nonfilteredOriginalPartnerTrackerMotions = nonFilteredOriginalPartnerTrackerMotions;
        this.date = date;
        this.startTimeLocalUTC = startTimeLocalUTC;
        this.endTimeLocalUTC = endTimeLocalUTC;
        this.currentTimeUTC = currentTimeUTC;
        this.timezoneOffsetMillis = timezoneOffsetMillis;
        this.userBioInfo = userBioInfo;
    }


    public OneDaysSensorData(final AllSensorSampleList allSensorSampleList,
                             final ImmutableList<TrackerMotion> trackerMotions, final ImmutableList<TrackerMotion> partnerMotions,
                             final ImmutableList<TimelineFeedback> feedbackList,
                             final ImmutableList<TrackerMotion> originalTrackerMotions, final ImmutableList<TrackerMotion> originalPartnerTrackerMotions,
                             final DateTime date, final DateTime startTimeLocalUTC, final DateTime endTimeLocalUTC, final DateTime currentTimeUTC,
                             final int timezoneOffsetMillis) {
        this.allSensorSampleList = allSensorSampleList;
        this.trackerMotions = trackerMotions;
        this.partnerMotions = partnerMotions;
        this.feedbackList = feedbackList;
        this.originalTrackerMotions = originalTrackerMotions;
        this.originalPartnerTrackerMotions = originalPartnerTrackerMotions;
        this.nonfilteredOriginalTrackerMotions = originalTrackerMotions;
        this.nonfilteredOriginalPartnerTrackerMotions = originalPartnerTrackerMotions;
        this.date = date;
        this.startTimeLocalUTC = startTimeLocalUTC;
        this.endTimeLocalUTC = endTimeLocalUTC;
        this.currentTimeUTC = currentTimeUTC;
        this.timezoneOffsetMillis = timezoneOffsetMillis;
        this.userBioInfo = new UserBioInfo();
    }

    public OneDaysSensorData(final AllSensorSampleList allSensorSampleList,
                             final ImmutableList<TrackerMotion> trackerMotions, final ImmutableList<TrackerMotion> partnerMotions,
                             final ImmutableList<TimelineFeedback> feedbackList,
                             final DateTime date,final DateTime startTimeLocalUTC, final DateTime endTimeLocalUTC, final DateTime currentTimeUTC,
                             final int timezoneOffsetMillis) {
        this.allSensorSampleList = allSensorSampleList;
        this.trackerMotions = trackerMotions;
        this.partnerMotions = partnerMotions;
        this.feedbackList = feedbackList;
        this.originalTrackerMotions = trackerMotions;
        this.originalPartnerTrackerMotions = partnerMotions;
        this.nonfilteredOriginalTrackerMotions = originalTrackerMotions;
        this.nonfilteredOriginalPartnerTrackerMotions = originalPartnerTrackerMotions;
        this.date = date;
        this.startTimeLocalUTC = startTimeLocalUTC;
        this.endTimeLocalUTC = endTimeLocalUTC;
        this.currentTimeUTC = currentTimeUTC;
        this.timezoneOffsetMillis = timezoneOffsetMillis;
        this.userBioInfo = new UserBioInfo();

    }

}
