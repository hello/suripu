package com.hello.suripu.core.models.motion;

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

}