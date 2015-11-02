package com.hello.suripu.core.algorithmintegration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TrackerMotion;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    public final SensorDataTimeSpanInfo sensorDataTimeSpanInfo;

    public OneDaysSensorData(final AllSensorSampleList allSensorSampleList,
                             final ImmutableList<TrackerMotion> trackerMotions, final ImmutableList<TrackerMotion> partnerMotions,
                             final ImmutableList<TimelineFeedback> feedbackList,
                             final ImmutableList<TrackerMotion> originalTrackerMotions, final ImmutableList<TrackerMotion> originalPartnerTrackerMotions) {
        this.allSensorSampleList = allSensorSampleList;
        this.trackerMotions = trackerMotions;
        this.partnerMotions = partnerMotions;
        this.feedbackList = feedbackList;
        this.originalTrackerMotions = originalTrackerMotions;
        this.originalPartnerTrackerMotions = originalPartnerTrackerMotions;
        this.sensorDataTimeSpanInfo = getTimeInfo(allSensorSampleList);
    }

    public OneDaysSensorData(final AllSensorSampleList allSensorSampleList,
                             final ImmutableList<TrackerMotion> trackerMotions, final ImmutableList<TrackerMotion> partnerMotions,
                             final ImmutableList<TimelineFeedback> feedbackList) {
        this.allSensorSampleList = allSensorSampleList;
        this.trackerMotions = trackerMotions;
        this.partnerMotions = partnerMotions;
        this.feedbackList = feedbackList;
        this.originalTrackerMotions = trackerMotions;
        this.originalPartnerTrackerMotions = partnerMotions;
        this.sensorDataTimeSpanInfo = getTimeInfo(allSensorSampleList);
    }


    private SensorDataTimeSpanInfo getTimeInfo(final AllSensorSampleList allSensorSampleList) {
        final List<Sample> light = allSensorSampleList.get(Sensor.LIGHT);
        long t0 = 0;

        if (light.isEmpty()) {
            return new SensorDataTimeSpanInfo(new SensorDataTimeSpanInfo.OffsetJump(0,0,0),0,0);
        }

        SensorDataTimeSpanInfo.OffsetJump offsetJump = new SensorDataTimeSpanInfo.OffsetJump(light.get(0).offsetMillis,light.get(0).offsetMillis,0);

        //assumes data is in chronological order, which is probably safe
        Sample lastSample = null;
        for (Iterator<Sample> it = light.iterator(); it.hasNext(); ) {
            final Sample sample = it.next();

            if (lastSample == null) {
                lastSample = sample;
                t0 = sample.dateTime;
            }

            if (!sample.offsetMillis.equals(lastSample.offsetMillis)) {
                offsetJump = new SensorDataTimeSpanInfo.OffsetJump(lastSample.offsetMillis,sample.offsetMillis,sample.dateTime - 1L);
            }

            lastSample = sample;
        }

        return new SensorDataTimeSpanInfo(offsetJump,t0,lastSample.dateTime);

    }

}
