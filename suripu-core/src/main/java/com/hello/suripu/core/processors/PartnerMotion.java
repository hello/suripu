package com.hello.suripu.core.processors;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.MotionEvent;
import com.hello.suripu.core.models.ParterMotionEvent;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.TimelineUtils;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 10/13/14.
 */
public class PartnerMotion {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartnerMotion.class);

    // TODO: tune these thresholds when we have real data
    final private static int PARTNER_DEPTH_THRESHOLD = 60; // large movement
    final private static int ACCOUNT_DEPTH_THRESHOLD = 90; // small movement
    final private static int CHECK_PRECEDING_MINS = 2; // make sure user has no movement in the previous 2 mins

    @Timed
    public static List<ParterMotionEvent> getPartnerData(final List<MotionEvent> originalSegments, final List<TrackerMotion> partnerMotions, int threshold) {

        final boolean createMotionlessSegment = false;
        final List<MotionEvent> partnerSegments = TimelineUtils.generateMotionEvents(partnerMotions);

        // convert list of TrackerMotion to hash map for easy lookup
        final Map<Long, MotionEvent> originalMotionMap = new HashMap<>();
        for (final MotionEvent segment : originalSegments) {
            originalMotionMap.put(segment.startTimestamp, segment);
        }

        final List<ParterMotionEvent> affectedSegments = new ArrayList<>();
        for (final MotionEvent partnerSegment : partnerSegments) {
            if (!originalMotionMap.containsKey(partnerSegment.startTimestamp)) {
                continue;
            }

            final MotionEvent originalSegment = originalMotionMap.get(partnerSegment.startTimestamp);
            final int mySleepDepth = TimelineUtils.normalizeSleepDepth(originalSegment.amplitude, originalSegment.maxAmplitude);
            final int partnerSleepDepth = TimelineUtils.normalizeSleepDepth(partnerSegment.amplitude, partnerSegment.maxAmplitude);
            if (mySleepDepth >= ACCOUNT_DEPTH_THRESHOLD ||  // original user not moving much, not affected.
                    partnerSleepDepth > PARTNER_DEPTH_THRESHOLD || // or, partner movement is not huge, no effects.
                    mySleepDepth < partnerSleepDepth) { // or, user movement is larger than partner's
                continue;
            }

            LOGGER.debug("user {}, partner {}", mySleepDepth, partnerSleepDepth);

            // check if there's any user movement in the preceding minutes
            boolean noPriorMovement = true;
            for (int i = 1; i <= CHECK_PRECEDING_MINS; i++) {
                final MotionEvent priorSegment = originalMotionMap.get(partnerSegment.startTimestamp - i * DateTimeConstants.MILLIS_PER_MINUTE);
                final int priorSleepDepth = TimelineUtils.normalizeSleepDepth(priorSegment.amplitude, priorSegment.maxAmplitude);
                if (priorSegment != null && priorSleepDepth < ACCOUNT_DEPTH_THRESHOLD) {
                    LOGGER.debug("{} prior movement {} {}", partnerSegment.startTimestamp, priorSegment.startTimestamp, priorSleepDepth);
                    noPriorMovement = false;
                    break;
                }
            }

            if (noPriorMovement) {
                affectedSegments.add(new ParterMotionEvent(
                                originalSegment.startTimestamp,
                                originalSegment.endTimestamp,
                                originalSegment.timezoneOffset,
                                originalSegment.amplitude,
                                originalSegment.maxAmplitude
                ));
            }

        }

        //return TimelineUtils.categorizeSleepDepth(affectedSegments);  // normalized
        return ImmutableList.copyOf(affectedSegments);
    }
}
