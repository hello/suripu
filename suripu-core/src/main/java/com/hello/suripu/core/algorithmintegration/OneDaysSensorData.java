package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserBioInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

/**
 * Created by benjo on 8/20/15.
 */
public class OneDaysSensorData {
    public static final int OOB_UNCERTAINTY_WINDOW= DateTimeConstants.MILLIS_PER_HOUR * 4; //ignore motion for 2 hour after OOB;
    //UNCERTAINTY TO OUTLIER FILTER//

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


    //remove motion data affiliated with previous period +
    public OneDaysSensorData getForSleepPeriod(final Optional<Long> prevOutOfBedTimeOptional, final SleepPeriod sleepPeriod, final boolean useOutlierFilter){
        final DateTime newEndTimeLocalUTC = sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.END_DATA, this.timezoneOffsetMillis);
        final DateTime newStartTimeLocalUTC = sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.START, this.timezoneOffsetMillis);

        final Long prevOutOfBedTime;
        if(prevOutOfBedTimeOptional.isPresent()){
            prevOutOfBedTime = prevOutOfBedTimeOptional.get() + OOB_UNCERTAINTY_WINDOW;
        } else {
            prevOutOfBedTime = 0L;
        }
        final long newStartTimeBounded = Math.max(prevOutOfBedTime, newStartTimeLocalUTC.getMillis());

        final AllSensorSampleList allSensorSampleListCurrentPeriod = this.allSensorSampleList.getSensorDataForTimeWindow(newStartTimeBounded, newEndTimeLocalUTC.getMillis());
        final OneDaysTrackerMotion oneDaysTrackerMotionCurrentPeriod = this.oneDaysTrackerMotion.getMotionsForTimeWindow(newStartTimeBounded, newEndTimeLocalUTC.getMillis(), useOutlierFilter);
        final OneDaysTrackerMotion oneDaysPartnerMotionCurrentPeriod = this.oneDaysPartnerMotion.getMotionsForTimeWindow(newStartTimeBounded, newEndTimeLocalUTC.getMillis(),useOutlierFilter);

        return new OneDaysSensorData(allSensorSampleListCurrentPeriod, oneDaysTrackerMotionCurrentPeriod, oneDaysPartnerMotionCurrentPeriod, this.feedbackList,
                this.date,newStartTimeLocalUTC, newEndTimeLocalUTC, this.currentTimeUTC, this.timezoneOffsetMillis, this.userBioInfo);
    }




}
