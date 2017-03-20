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
    public OneDaysSensorData getForSleepPeriod(final Optional<Long> prevOutOfBedTimeOptional, final SleepPeriod sleepPeriod){
        final int uncertaintyWindow = DateTimeConstants.MILLIS_PER_HOUR * 2; //ignore motion for 2 hour after OOB;
        final DateTime newEndTimeLocalUTC;
        //check if sleep period end time is after the current time
        if (this.currentTimeUTC.isAfter(sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.END_DATA, this.timezoneOffsetMillis))){
            newEndTimeLocalUTC = sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.END_DATA, this.timezoneOffsetMillis);
        }else {
            newEndTimeLocalUTC = this.currentTimeUTC;
        }

        final Long prevOutOfBedTime;
        if(prevOutOfBedTimeOptional.isPresent()){
            prevOutOfBedTime = prevOutOfBedTimeOptional.get() + uncertaintyWindow;
        } else {
            prevOutOfBedTime = sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.START, this.timezoneOffsetMillis).getMillis();
        }

        final AllSensorSampleList allSensorSampleListCurrentPeriod = this.allSensorSampleList.getSensorDataForTimeWindow(prevOutOfBedTime, newEndTimeLocalUTC.getMillis());
        final OneDaysTrackerMotion oneDaysTrackerMotionCurrentPeriod = this.oneDaysTrackerMotion.getMotionsForTimeWindow(prevOutOfBedTime, newEndTimeLocalUTC.getMillis());
        final OneDaysTrackerMotion oneDaysPartnerMotionCurrentPeriod = this.oneDaysPartnerMotion.getMotionsForTimeWindow(prevOutOfBedTime, newEndTimeLocalUTC.getMillis());

        return new OneDaysSensorData(allSensorSampleListCurrentPeriod, oneDaysTrackerMotionCurrentPeriod, oneDaysPartnerMotionCurrentPeriod, this.feedbackList,
                this.date, sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.START, this.timezoneOffsetMillis), newEndTimeLocalUTC, this.currentTimeUTC, this.timezoneOffsetMillis, this.userBioInfo);
    }




}
