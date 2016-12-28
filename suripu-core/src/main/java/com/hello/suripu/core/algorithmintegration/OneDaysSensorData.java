package com.hello.suripu.core.algorithmintegration;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserBioInfo;
import com.hello.suripu.core.models.motion.OneDaysTrackerMotion;
import org.joda.time.DateTime;

/**
 * Created by benjo on 8/20/15.
 */
public class OneDaysSensorData {
    public final AllSensorSampleList allSensorSampleList;
    public final OneDaysTrackerMotion oneDaysTrackerMotion;
    public final OneDaysTrackerMotion oneDaysPartnerMotion;
    public final ImmutableList<TimelineFeedback> feedbackList;
    public final int timezoneOffsetMillis;
    public final DateTime date;
    public final DateTime startTimeLocalUTC;
    public final DateTime endTimeLocalUTC;
    public final DateTime currentTimeUTC;
    public final UserBioInfo userBioInfo;


    public OneDaysSensorData(final AllSensorSampleList allSensorSampleList,
                             final OneDaysTrackerMotion oneDaysTrackerMotion,
                             final OneDaysTrackerMotion oneDaysPartnerMotion,
                             final ImmutableList<TimelineFeedback> feedbackList,
                             final DateTime date, final DateTime startTimeLocalUTC, final DateTime endTimeLocalUTC, final DateTime currentTimeUTC,
                             final int timezoneOffsetMillis, final UserBioInfo userBioInfo) {
        this.allSensorSampleList = allSensorSampleList;
        this.oneDaysTrackerMotion = oneDaysTrackerMotion;
        this.oneDaysPartnerMotion= oneDaysPartnerMotion;
        this.feedbackList = feedbackList;
        this.date = date;
        this.startTimeLocalUTC = startTimeLocalUTC;
        this.endTimeLocalUTC = endTimeLocalUTC;
        this.currentTimeUTC = currentTimeUTC;
        this.timezoneOffsetMillis = timezoneOffsetMillis;
        this.userBioInfo = userBioInfo;
    }


    public OneDaysSensorData(final AllSensorSampleList allSensorSampleList,
                             final OneDaysTrackerMotion oneDaysTrackerMotion,
                             final OneDaysTrackerMotion oneDaysPartnerMotion,
                             final ImmutableList<TimelineFeedback> feedbackList,
                             final DateTime date, final DateTime startTimeLocalUTC, final DateTime endTimeLocalUTC, final DateTime currentTimeUTC,
                             final int timezoneOffsetMillis) {
        this.allSensorSampleList = allSensorSampleList;
        this.oneDaysTrackerMotion = oneDaysTrackerMotion;
        this.oneDaysPartnerMotion= oneDaysPartnerMotion;
        this.feedbackList = feedbackList;
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
        this.oneDaysTrackerMotion = new OneDaysTrackerMotion(trackerMotions);
        this.oneDaysPartnerMotion = new OneDaysTrackerMotion(partnerMotions);
        this.feedbackList = feedbackList;
        this.date = date;
        this.startTimeLocalUTC = startTimeLocalUTC;
        this.endTimeLocalUTC = endTimeLocalUTC;
        this.currentTimeUTC = currentTimeUTC;
        this.timezoneOffsetMillis = timezoneOffsetMillis;
        this.userBioInfo = new UserBioInfo();
    }

}
