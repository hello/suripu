package com.hello.suripu.core.models.motion;

import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by jakepiccolo on 12/7/15.
 */
public class PartnerMotionTimeSeries {

    private final TrackerMotionTimeSeries leftTimeSeries;
    private final TrackerMotionTimeSeries rightTimeSeries;

    protected PartnerMotionTimeSeries(final TrackerMotionTimeSeries leftTimeSeries,
                                      final TrackerMotionTimeSeries rightTimeSeries)
    {
        this.leftTimeSeries = leftTimeSeries;
        this.rightTimeSeries = rightTimeSeries;
    }

    public static PartnerMotionTimeSeries create(final List<TrackerMotion> leftMotions,
                                                 final List<TrackerMotion> rightMotions)
    {
        final DateTime leftStart = TrackerMotionTimeSeries.alignedStartTime(leftMotions);
        final DateTime rightStart = TrackerMotionTimeSeries.alignedStartTime(rightMotions);
        final DateTime leftEnd = TrackerMotionTimeSeries.alignedEndTime(leftMotions);
        final DateTime rightEnd = TrackerMotionTimeSeries.alignedEndTime(rightMotions);

        final DateTime start = leftStart.isBefore(rightStart) ? leftStart : rightStart;
        final DateTime end = leftEnd.isAfter(rightEnd) ? leftEnd : rightEnd;

        final TrackerMotionTimeSeries leftTimeSeries = TrackerMotionTimeSeries.create(leftMotions, start, end);
        final TrackerMotionTimeSeries rightTimeSeries = TrackerMotionTimeSeries.create(rightMotions, start, end);

        return new PartnerMotionTimeSeries(leftTimeSeries, rightTimeSeries);
    }

    public TrackerMotionTimeSeries getLeftTimeSeries() {
        return leftTimeSeries;
    }

    public TrackerMotionTimeSeries getRightTimeSeries() {
        return rightTimeSeries;
    }

}
