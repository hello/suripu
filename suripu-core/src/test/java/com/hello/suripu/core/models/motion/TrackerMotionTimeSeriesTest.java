package com.hello.suripu.core.models.motion;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by jakepiccolo on 12/15/15.
 */
public class TrackerMotionTimeSeriesTest {

    private static TrackerMotion makeMotion(final Long motionMask, final DateTime dateTime) {
        return new TrackerMotion.Builder()
                .withMotionMask(motionMask)
                .withAccountId(0L)
                .withExternalTrackerId("")
                .withKickOffCounts(3L)
                .withId(0L)
                .withMotionRange(0L)
                .withOnDurationInSeconds(3L)
                .withTimestampMillis(dateTime.getMillis())
                .build();
    }

    // TrackerMotionTimeSeries
    @Test
    public void testAsList() {
        final DateTime startTime = new DateTime(2015, 01, 01, 01, 01, 01, DateTimeZone.UTC);
        final List<TrackerMotion> trackerMotions = ImmutableList.of(
                makeMotion(7L, startTime.plusMinutes(1)),
                makeMotion(10L, startTime.plusMinutes(2)),
                makeMotion(0L, startTime.plusMinutes(3))
        );

        final TrackerMotionTimeSeries timeSeries = TrackerMotionTimeSeries.create(trackerMotions);
        final List<MotionAtSecond> motionAtSeconds = timeSeries.asList();
        assertThat(motionAtSeconds.size(), is(60 * trackerMotions.size()));
        assertThat(motionAtSeconds.get(0).didMove, is(true));
        assertThat(motionAtSeconds.get(0).trackerMotion, is(trackerMotions.get(0)));

        assertThat(motionAtSeconds.get(1).didMove, is(true));
        assertThat(motionAtSeconds.get(1).trackerMotion, is(trackerMotions.get(0)));

        assertThat(motionAtSeconds.get(2).didMove, is(true));
        assertThat(motionAtSeconds.get(2).trackerMotion, is(trackerMotions.get(0)));

        for (int i = 3; i < 60; i++) {
            assertThat(motionAtSeconds.get(i).didMove, is(false));
            assertThat(motionAtSeconds.get(i).trackerMotion, is(trackerMotions.get(0)));
            assertThat(motionAtSeconds.get(i).dateTime, is(startTime.plusSeconds(i)));
        }

        assertThat(motionAtSeconds.get(60).didMove, is(false));
        assertThat(motionAtSeconds.get(60).trackerMotion, is(trackerMotions.get(1)));

        assertThat(motionAtSeconds.get(61).didMove, is(true));
        assertThat(motionAtSeconds.get(61).trackerMotion, is(trackerMotions.get(1)));

        assertThat(motionAtSeconds.get(62).didMove, is(false));
        assertThat(motionAtSeconds.get(62).trackerMotion, is(trackerMotions.get(1)));

        assertThat(motionAtSeconds.get(63).didMove, is(true));
        assertThat(motionAtSeconds.get(63).trackerMotion, is(trackerMotions.get(1)));

        for (int i = 64; i < 120; i++) {
            assertThat(motionAtSeconds.get(i).didMove, is(false));
            assertThat(motionAtSeconds.get(i).trackerMotion, is(trackerMotions.get(1)));
        }

        for (int i = 120; i < 180; i++) {
            assertThat(motionAtSeconds.get(i).didMove, is(false));
            assertThat(motionAtSeconds.get(i).trackerMotion, is(trackerMotions.get(2)));
        }

    }

    @Test
    public void testAsListWithOverlappingSeconds() {
        final DateTime startTime = new DateTime(2015, 01, 01, 00, 00, 02, DateTimeZone.UTC);
        final List<TrackerMotion> trackerMotions = ImmutableList.of(
                makeMotion(15L, startTime.plusMinutes(1)),
                makeMotion(3L | (1L << 59), startTime.plusMinutes(1).plusSeconds(2))
        );
        final TrackerMotionTimeSeries timeSeries = TrackerMotionTimeSeries.create(trackerMotions);
        final List<MotionAtSecond> motionAtSeconds = timeSeries.asList();

        assertThat(motionAtSeconds.size(), is(62));

        for (int i = 0; i < 4; i++) {
            assertThat(motionAtSeconds.get(i).didMove, is(true));
            // Even though they overlapped, we expect to use the first trackerMotion object
            assertThat(motionAtSeconds.get(i).trackerMotion, is(trackerMotions.get(0)));
        }

        for (int i = 4; i < 60; i++) {
            assertThat(motionAtSeconds.get(i).didMove, is(false));
            // Even though they overlapped, we expect to use the first trackerMotion object
            assertThat(motionAtSeconds.get(i).trackerMotion, is(trackerMotions.get(0)));
        }

        assertThat(motionAtSeconds.get(60).didMove, is(false));
        assertThat(motionAtSeconds.get(60).trackerMotion, is(trackerMotions.get(1)));

        assertThat(motionAtSeconds.get(61).didMove, is(true));
        assertThat(motionAtSeconds.get(61).trackerMotion, is(trackerMotions.get(1)));

    }

    @Test
    public void testCreate() {
        final DateTime startTime = new DateTime(2015, 01, 01, 00, 00, 02, DateTimeZone.UTC); // 2 seconds after midnight
        final List<TrackerMotion> trackerMotions = ImmutableList.of(
                makeMotion(7L, startTime.plusMinutes(1)), // Motion in first 3 seconds
                makeMotion(10L | (1L << 59), startTime.plusMinutes(2)), // Motion in index 1, 3, and 59 (where 59 is last second)
                makeMotion((3L << 58), startTime.plusMinutes(3))    // Motion for last 2 seconds
        );
        final TrackerMotionTimeSeries timeSeries = TrackerMotionTimeSeries.create(trackerMotions);

        assertThat(timeSeries.getStart(), is(startTime.withSecondOfMinute(0)));
        assertThat(timeSeries.getAlignedMotionMasks().size(), is(4));
        assertThat(timeSeries.getTrackerMotions(), is(trackerMotions));

        assertThat(timeSeries.getAlignedMotionMasks().get(0), is(7L << 2));
        assertThat(timeSeries.getAlignedMotionMasks().get(1), is(10L << 2));
        assertThat(timeSeries.getAlignedMotionMasks().get(2), is(2L));
        assertThat(timeSeries.getAlignedMotionMasks().get(3), is(3L));
    }

    @Test
    public void testCreateWithOverlappingSeconds() {
        final DateTime startTime = new DateTime(2015, 01, 01, 00, 00, 02, DateTimeZone.UTC);
        final List<TrackerMotion> trackerMotions = ImmutableList.of(
                makeMotion(15L, startTime.plusMinutes(1)),
                makeMotion(3L | (1L << 59), startTime.plusMinutes(1).plusSeconds(2))
        );
        final TrackerMotionTimeSeries timeSeries = TrackerMotionTimeSeries.create(trackerMotions);

        assertThat(timeSeries.getStart(), is(startTime.withSecondOfMinute(0)));
        assertThat(timeSeries.getAlignedMotionMasks().size(), is(2));
        assertThat(timeSeries.getAlignedMotionMasks().get(0), is(15L << 2));
        assertThat(timeSeries.getAlignedMotionMasks().get(1), is(1L << 3));
    }

    @Test
    public void testCountMovement() {
        final DateTime startTime = new DateTime(2015, 01, 01, 00, 00, 02, DateTimeZone.UTC); // 2 seconds after midnight
        final List<TrackerMotion> trackerMotions = ImmutableList.of(
                makeMotion(7L, startTime.plusMinutes(1)), // Motion in first 3 seconds
                makeMotion(10L | (1L << 59), startTime.plusMinutes(2)), // Motion in index 1, 3, and 59 (where 59 is last second)
                makeMotion((3L << 58), startTime.plusMinutes(3))    // Motion for last 2 seconds
        );

        final TrackerMotionTimeSeries timeSeries = TrackerMotionTimeSeries.create(trackerMotions);

        assertThat(timeSeries.countMovement(), is(8));
    }

    @Test
    public void testCountMovementInRange() {
        final DateTime startTime = new DateTime(2015, 01, 01, 00, 00, 02, DateTimeZone.UTC); // 2 seconds after midnight
        final List<TrackerMotion> trackerMotions = ImmutableList.of(
                makeMotion(7L, startTime.plusMinutes(1)), // Motion in first 3 seconds
                makeMotion(10L | (1L << 59), startTime.plusMinutes(2)), // Motion in index 1, 3, and 59 (where 59 is last second)
                makeMotion((3L << 58), startTime.plusMinutes(3))    // Motion for last 2 seconds
        );

        final TrackerMotionTimeSeries timeSeries = TrackerMotionTimeSeries.create(trackerMotions);

        assertThat(timeSeries.countMovementInRange(timeSeries.getStart().plusSeconds(3), timeSeries.getEnd()), is(7));
        assertThat(timeSeries.countMovementInRange(timeSeries.getStart().plusSeconds(3), timeSeries.getEnd().minusSeconds(59)), is(6));
    }

    @Test
    public void testDidMoveAtSecond() {
        final DateTime startTime = new DateTime(2015, 01, 01, 00, 00, 05, DateTimeZone.UTC); // 5 seconds after midnight
        final List<TrackerMotion> trackerMotions = ImmutableList.of(
                makeMotion(7L, startTime.plusMinutes(1)),
                makeMotion(8L, startTime.plusMinutes(3))
        );

        final TrackerMotionTimeSeries timeSeries = TrackerMotionTimeSeries.create(trackerMotions);

        for (int i = 1; i <= 5; i++) {
            assertThat(timeSeries.didMoveAtSecond(startTime.minusSeconds(i)), is(false));
        }

        assertThat(timeSeries.didMoveAtSecond(startTime), is(true));
        assertThat(timeSeries.didMoveAtSecond(startTime.plusSeconds(1)), is(true));
        assertThat(timeSeries.didMoveAtSecond(startTime.plusSeconds(2)), is(true));

        for (int i = 3; i < 123; i++) {
            assertThat(timeSeries.didMoveAtSecond(startTime.plusSeconds(i)), is(false));
        }

        assertThat(timeSeries.didMoveAtSecond(startTime.plusMinutes(2).plusSeconds(3)), is(true));

        for (int i = 4; i < 60; i++) {
            assertThat(timeSeries.didMoveAtSecond(startTime.plusMinutes(2).plusSeconds(i)), is(false));
        }
    }

}