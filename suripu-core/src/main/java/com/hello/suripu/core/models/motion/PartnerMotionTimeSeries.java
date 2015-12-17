package com.hello.suripu.core.models.motion;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;

import java.util.List;
import java.util.ListIterator;

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

    public List<TrackerMotionWithPartnerMotion> groupByLeft() {
        return groupTimeSeries(leftTimeSeries, rightTimeSeries);
    }

    public List<TrackerMotionWithPartnerMotion> groupByRight() {
        return groupTimeSeries(rightTimeSeries, leftTimeSeries);
    }


    private List<TrackerMotionWithPartnerMotion> groupTimeSeries(final TrackerMotionTimeSeries groupByTimeSeries,
                                                                 final TrackerMotionTimeSeries otherTimeSeries)
    {
        final List<TrackerMotionWithPartnerMotion> results = Lists.newArrayList();

        final List<TrackerMotion> groupByTrackerMotions = groupByTimeSeries.getTrackerMotions();
        final List<MotionAtSecond> otherMotionAtSeconds = otherTimeSeries.asList();

        final ListIterator<MotionAtSecond> otherIterator = otherMotionAtSeconds.listIterator();

        for (final TrackerMotion currTrackerMotion: groupByTrackerMotions) {

            final List<MotionAtSecond> currMotionAtSeconds = TrackerMotionTimeSeries.trackerMotionToMotionAtSeconds(currTrackerMotion);
            final List<PartnerMotionAtSecond> partnerMotionAtSeconds = Lists.newArrayListWithExpectedSize(currMotionAtSeconds.size());

            for (final MotionAtSecond groupByAtSecond: currMotionAtSeconds) {

                while (otherIterator.hasNext()) {
                    final MotionAtSecond otherAtSecond = otherIterator.next();
                    if (otherAtSecond.dateTime.isAfter(groupByAtSecond.dateTime)) {
                        // If we got to this point, it means there's no matching partnerMotion at this second :(
                        partnerMotionAtSeconds.add(new PartnerMotionAtSecond(
                                groupByAtSecond.didMove, false, groupByAtSecond.dateTime, Optional.<TrackerMotion>absent()));

                        // Move the cursor back a step and move on to the next groupByAtSecond
                        otherIterator.previous();
                        break;
                    } else if (otherAtSecond.dateTime.equals(groupByAtSecond.dateTime)) {
                        // We found our match!
                        partnerMotionAtSeconds.add(new PartnerMotionAtSecond(
                                groupByAtSecond.didMove,
                                otherAtSecond.didMove,
                                groupByAtSecond.dateTime,
                                Optional.of(otherAtSecond.trackerMotion)));
                        break;
                    }
                }
            }

            results.add(new TrackerMotionWithPartnerMotion(currTrackerMotion, partnerMotionAtSeconds));
        }

        return results;
    }

}
