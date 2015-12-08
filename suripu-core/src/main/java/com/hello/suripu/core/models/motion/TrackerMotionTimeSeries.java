package com.hello.suripu.core.models.motion;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.util.List;

/**
 * Created by jakepiccolo on 12/15/15.
 */
public class TrackerMotionTimeSeries {
    private final List<TrackerMotion> trackerMotions;
    private final List<Long> alignedMotionMasks;
    private final DateTime start;


    protected TrackerMotionTimeSeries(final List<TrackerMotion> trackerMotions,
                                      final List<Long> alignedMotionMasks,
                                      final DateTime start) {
        this.trackerMotions = trackerMotions;
        this.alignedMotionMasks = alignedMotionMasks;
        this.start = start;
    }


    private static void checkNotEmpty(final List<TrackerMotion> trackerMotions) {
        if (trackerMotions.isEmpty()) {
            throw new IllegalArgumentException("TrackerMotions cannot be empty");
        }
    }

    static DateTime alignedStartTime(final List<TrackerMotion> trackerMotions) {
        return trackerMotions.get(0).dateTimeUTC().withSecondOfMinute(0).minusMinutes(1);
    }

    static DateTime alignedEndTime(final List<TrackerMotion> trackerMotions) {
        return trackerMotions.get(trackerMotions.size() - 1).dateTimeUTC().withSecondOfMinute(0).plusMinutes(1);
    }

    public static TrackerMotionTimeSeries create(final List<TrackerMotion> trackerMotions) {
        checkNotEmpty(trackerMotions);

        // Align to beginning of minute. Also, motion masks are for minute preceding trackerMotion timestamp
        final DateTime start = alignedStartTime(trackerMotions);
        final DateTime end = alignedEndTime(trackerMotions);
        return create(trackerMotions, start, end);
    }


    static TrackerMotionTimeSeries create(final List<TrackerMotion> trackerMotions,
                                          final DateTime start,
                                          final DateTime end) {
        final Long mask = 0x0FFFFFFFFFFFFFFFL;
        final long[] alignedMotionMasks = new long[Minutes.minutesBetween(start, end).getMinutes()];

        DateTime currMotionTime;
        for (final TrackerMotion trackerMotion : trackerMotions) {
            if (!trackerMotion.motionMask.isPresent()) {
                continue;
            }

            currMotionTime = trackerMotion.dateTimeUTC();
            final int secondsOff = currMotionTime.getSecondOfMinute();
            final int currIndex = Minutes.minutesBetween(start, currMotionTime).getMinutes() - 1;

            alignedMotionMasks[currIndex] |= (trackerMotion.motionMask.get() << secondsOff) & mask;
            alignedMotionMasks[currIndex + 1] |= trackerMotion.motionMask.get() >> (60 - secondsOff);
        }

        final List<Long> asList = Lists.newArrayListWithCapacity(alignedMotionMasks.length);
        for (final long x : alignedMotionMasks) {
            asList.add(x);
        }

        return new TrackerMotionTimeSeries(trackerMotions, asList, start);
    }


    /**
     * Get a sparse list of MotionAtSecond objects for each second covered by this time series.
     *
     * The list is sparse in that there will only be elements for seconds where there is a TrackerMotion object,
     * not for seconds in between.
     *
     * If one second is covered by multiple TrackerMotion objects, the first TrackerMotion object will be used.
     *
     * @return List of MotionAtSecond objects
     */
    public List<MotionAtSecond> asList() {
        final List<MotionAtSecond> result = Lists.newArrayListWithExpectedSize(60 * trackerMotions.size());
        DateTime latestDateTime = start.minusMinutes(1); // Guaranteed to be before any trackerMotion time to start
        for (final TrackerMotion trackerMotion : trackerMotions) {
            final DateTime trackerMotionDateTime = trackerMotion.dateTimeUTC();
            final List<Boolean> motionsForSeconds = trackerMotion.motionsForSeconds();
            for (int i = 0; i < motionsForSeconds.size(); i++) {
                final DateTime currSecondDateTime = trackerMotionDateTime.minusSeconds(60 - i);
                if (!currSecondDateTime.isAfter(latestDateTime)) {
                    // Only take the first TrackerMotion for a given second if they overlap
                    continue;
                }
                final Boolean didMove = motionsForSeconds.get(i);
                final MotionAtSecond mas = new MotionAtSecond(trackerMotion, currSecondDateTime, didMove);
                result.add(mas);
                latestDateTime = currSecondDateTime;
            }
        }
        return result;
    }

    private int indexOfMinute(final DateTime dateTime) {
        return Minutes.minutesBetween(start, dateTime).getMinutes();
    }


    /**
     * Same as `countMovementInRange` but using the entire time series.
     *
     * @return The number of seconds where motion data was recorded in the time series.
     */
    public int countMovement() {
        return countMovementInRange(getStart(), getEnd());
    }


    /**
     * @param startDateTime inclusive
     * @param endDateTime   inclusive
     * @return All "on" bits between startDateTime and endDateTime.
     */
    public int countMovementInRange(final DateTime startDateTime, final DateTime endDateTime) {
        final int startIndex = indexOfMinute(startDateTime);

        final int endIndex = indexOfMinute(endDateTime);

        // Fill in the remaining seconds of the first minute
        int count = Long.bitCount(alignedMotionMasks.get(startIndex) >> startDateTime.getSecondOfMinute());

        for (int i = startIndex + 1; i < endIndex; i++) {
            count += Long.bitCount(alignedMotionMasks.get(i));
        }

        // Fill in remaining seconds of last minute
        count += Long.bitCount(alignedMotionMasks.get(endIndex) << (4 + (59 - endDateTime.getSecondOfMinute())));

        return count;
    }


    /**
     * @param dateTime - Time of movement. Rounds down to the nearest second.
     * @return Whether or not there was movement at the given second.
     */
    public Boolean didMoveAtSecond(final DateTime dateTime) {
        final int index = indexOfMinute(dateTime);
        final int secondOffset = dateTime.getSecondOfMinute();
        final Boolean didMove = ((alignedMotionMasks.get(index) >> secondOffset) & 1) == 1;
        return didMove;
    }


    protected List<Long> getAlignedMotionMasks() {
        return alignedMotionMasks;
    }


    /**
     * @return The time of the first second of this time series (inclusive)
     */
    public DateTime getStart() {
        return start;
    }


    /**
     * @return The time of the last second of this time series (inclusive)
     */
    public DateTime getEnd() {
        return start.plusMinutes(alignedMotionMasks.size()).minusSeconds(1);
    }


    public List<TrackerMotion> getTrackerMotions() {
        return trackerMotions;
    }

}
