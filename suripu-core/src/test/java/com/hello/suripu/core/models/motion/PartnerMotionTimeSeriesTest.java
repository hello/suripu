package com.hello.suripu.core.models.motion;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jakepiccolo on 12/8/15.
 */
public class PartnerMotionTimeSeriesTest {

    private static TrackerMotion makeMotion(final Long accountId, final Long motionMask, final DateTime dateTime) {
        return new TrackerMotion.Builder()
                .withMotionMask(motionMask)
                .withAccountId(accountId)
                .withExternalTrackerId("")
                .withKickOffCounts(3L)
                .withId(0L)
                .withMotionRange(0L)
                .withOnDurationInSeconds(3L)
                .withTimestampMillis(dateTime.getMillis())
                .build();
    }

    @Test
    public void testCreate() {
        final DateTime startTime = new DateTime(2015, 01, 01, 00, 00, DateTimeZone.UTC); // Midnight on Jan 1
        final int myMotionOffset = 2;
        final int partnerMotionOffset = 5;

        final List<TrackerMotion> myMotions = ImmutableList.of(
                makeMotion(1L, 7L, startTime.plusSeconds(myMotionOffset).plusMinutes(1)),
                makeMotion(1L, 1L << 40, startTime.plusSeconds(myMotionOffset).plusMinutes(2)),
                makeMotion(1L, 8L, startTime.plusSeconds(myMotionOffset).plusMinutes(4))
        );

        final List<TrackerMotion> partnerMotions = ImmutableList.of(
                makeMotion(2L, 31L << 36, startTime.plusSeconds(partnerMotionOffset).plusMinutes(2)),
                makeMotion(2L, 15L, startTime.plusSeconds(partnerMotionOffset).plusMinutes(3))
        );

        final PartnerMotionTimeSeries timeSeries = PartnerMotionTimeSeries.create(myMotions, partnerMotions);

        assertThat(timeSeries.getLeftTimeSeries().getStart(), is(timeSeries.getRightTimeSeries().getStart()));
        assertThat(timeSeries.getLeftTimeSeries().getEnd(), is(timeSeries.getRightTimeSeries().getEnd()));
        assertThat(timeSeries.getLeftTimeSeries().getAlignedMotionMasks().size(),
                is(timeSeries.getRightTimeSeries().getAlignedMotionMasks().size()));
    }


    @Test
    public void testGroupByLeft() {
        final DateTime startTime = new DateTime(2015, 01, 01, 00, 00, DateTimeZone.UTC); // Midnight on Jan 1
        final int myMotionOffset = 2;
        final int partnerMotionOffset = 5;

        final List<TrackerMotion> myMotions = ImmutableList.of(
                makeMotion(1L, 7L, startTime.plusSeconds(myMotionOffset).plusMinutes(1)),
                makeMotion(1L, 1L << 40, startTime.plusSeconds(myMotionOffset).plusMinutes(2)),
                makeMotion(1L, 8L, startTime.plusSeconds(myMotionOffset).plusMinutes(3))
        );

        final List<TrackerMotion> partnerMotions = ImmutableList.of(
                makeMotion(2L, 31L << 36, startTime.plusSeconds(partnerMotionOffset).plusMinutes(2)),
                makeMotion(2L, 15L, startTime.plusSeconds(partnerMotionOffset).plusMinutes(3))
        );

        final PartnerMotionTimeSeries timeSeries = PartnerMotionTimeSeries.create(myMotions, partnerMotions);

        final List<TrackerMotionWithPartnerMotion> grouped = timeSeries.groupByLeft();
        assertThat(grouped.size(), is(myMotions.size()));
        assertThat(grouped.get(0).trackerMotion, is(myMotions.get(0)));
        assertThat(grouped.get(1).trackerMotion, is(myMotions.get(1)));
        assertThat(grouped.get(2).trackerMotion, is(myMotions.get(2)));

        // Inspect first trackerMotion. Should have no partner motions
        for (final PartnerMotionAtSecond x: grouped.get(0).partnerMotionAtSeconds) {
            assertThat(x.partnerMotion, is(Optional.<TrackerMotion>absent()));
            assertThat(x.didPartnerMove, is(false));
            // First 3 seconds should be true
            if (x.dateTime.isBefore(startTime.plusSeconds(myMotionOffset + 3))) {
                assertThat(x.didMove, is(true));
            }
        }

        // Second one is a bit more complex. Should have some motion overlap.
        for (final PartnerMotionAtSecond x : grouped.get(1).partnerMotionAtSeconds) {
            assertThat(x.dateTime.isAfter(startTime.plusSeconds(myMotionOffset - 1).plusMinutes(1)), is(true));
            assertThat(x.dateTime.isBefore(startTime.plusSeconds(myMotionOffset).plusMinutes(2)), is(true));

            // Should be the only "on" bit for this TrackerMotion
            if (x.dateTime.equals(startTime.plusMinutes(1).plusSeconds(myMotionOffset + 40))) {
                assertThat(x.didMove, is(true));
            } else {
                assertThat(x.didMove, is(false));
            }

            // there should be 5 "on" bits for the partner motion
            if (x.dateTime.isAfter(startTime.plusSeconds(myMotionOffset + 38).plusMinutes(1)) &&
                    x.dateTime.isBefore(startTime.plusSeconds(myMotionOffset + 44).plusMinutes(1)))
            {
                assertThat(x.didPartnerMove, is(true));
            } else {
                assertThat(x.didPartnerMove, is(false));
            }

            if (x.dateTime.isBefore(startTime.plusMinutes(1).plusSeconds(partnerMotionOffset))) {
                assertThat(x.partnerMotion, is(Optional.<TrackerMotion>absent()));
            } else {
                assertThat(x.partnerMotion.get(), is(partnerMotions.get(0)));
            }
        }

        // Third one is a real doozy
        for (final PartnerMotionAtSecond x : grouped.get(2).partnerMotionAtSeconds) {
            assertThat(x.dateTime.isAfter(startTime.plusSeconds(myMotionOffset - 1).plusMinutes(2)), is(true));
            assertThat(x.dateTime.isBefore(startTime.plusSeconds(myMotionOffset).plusMinutes(3)), is(true));

            // Should be the only "on" bit for this TrackerMotion
            if (x.dateTime.equals(startTime.plusMinutes(2).plusSeconds(myMotionOffset + 3))) {
                assertThat(x.didMove, is(true));
            } else {
                assertThat(x.didMove, is(false));
            }

            // there should be 4 "on" bits for the partner motion
            if (x.dateTime.isAfter(startTime.plusSeconds(partnerMotionOffset - 1).plusMinutes(2)) &&
                    x.dateTime.isBefore(startTime.plusSeconds(partnerMotionOffset + 4).plusMinutes(2)))
            {
                assertThat(x.didPartnerMove, is(true));
            } else {
                assertThat(x.didPartnerMove, is(false));
            }

            if (x.dateTime.isBefore(startTime.plusMinutes(2).plusSeconds(partnerMotionOffset))) {
                assertThat(x.partnerMotion.get(), is(partnerMotions.get(0)));
            } else {
                assertThat(x.partnerMotion.get(), is(partnerMotions.get(1)));
            }
        }
    }

}