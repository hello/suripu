package com.hello.suripu.core.algorithmintegration;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TrackerMotion;
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
    public final double age;
    public final int male;
    public final int female;
    public final double bmi;
    public final int partner;


    public OneDaysSensorData(final AllSensorSampleList allSensorSampleList,
                             final ImmutableList<TrackerMotion> trackerMotions, final ImmutableList<TrackerMotion> partnerMotions,
                             final ImmutableList<TimelineFeedback> feedbackList,
                             final ImmutableList<TrackerMotion> originalTrackerMotions, final ImmutableList<TrackerMotion> originalPartnerTrackerMotions,
                             final ImmutableList<TrackerMotion> nonfilteredOriginalTrackerMotions, final ImmutableList<TrackerMotion> nonFilteredOriginalPartnerTrackerMotions,
                             final DateTime date, final DateTime startTimeLocalUTC, final DateTime endTimeLocalUTC, final DateTime currentTimeUTC,
                             final int timezoneOffsetMillis, final double age, final int male, final int female, final double bmi, final int partner) {
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
        this.age = age;
        this.male = male;
        this.female = female;
        this.bmi = bmi;
        this.partner = partner;
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
        this.age = 0;
        this.male = 0;
        this.female = 0;
        this.bmi = 0;
        this.partner = 0;
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
        this.age = 0;
        this.male = 0;
        this.female = 0;
        this.bmi = 0;
        this.partner = 0;

    }

}
