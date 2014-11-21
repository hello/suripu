package com.hello.suripu.core.processors;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SensorReading;
import com.hello.suripu.core.models.SleepSegment;
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
    public static List<SleepSegment> getPartnerData(final List<SleepSegment> originalSegments, final List<TrackerMotion> partnerMotions, int threshold) {

        final boolean createMotionlessSegment = false;
        final List<SleepSegment> partnerSegments = TimelineUtils.generateSleepSegments(partnerMotions, threshold, createMotionlessSegment);

        // convert list of TrackerMotion to hash map for easy lookup
        final Map<Long, SleepSegment> originalMotionMap = new HashMap<>();
        for (final SleepSegment segment : originalSegments) {
            originalMotionMap.put(segment.getTimestamp(), segment);
        }

        final List<SleepSegment> affectedSegments = new ArrayList<>();
        for (final SleepSegment partnerSegment : partnerSegments) {
            if (!originalMotionMap.containsKey(partnerSegment.getTimestamp())) {
                continue;
            }

            final SleepSegment originalSegment = originalMotionMap.get(partnerSegment.getTimestamp());

            if (originalSegment.sleepDepth >= ACCOUNT_DEPTH_THRESHOLD ||  // original user not moving much, not affected.
                    partnerSegment.sleepDepth > PARTNER_DEPTH_THRESHOLD || // or, partner movement is not huge, no effects.
                    originalSegment.sleepDepth < partnerSegment.sleepDepth) { // or, user movement is larger than partner's
                continue;
            }

            LOGGER.debug("user {}, partner {}", originalSegment.sleepDepth, partnerSegment.sleepDepth);

            // check if there's any user movement in the preceding minutes
            boolean noPriorMovement = true;
            for (int i = 1; i <= CHECK_PRECEDING_MINS; i++) {
                final SleepSegment priorSegment = originalMotionMap.get(partnerSegment.getTimestamp() - i * DateTimeConstants.MILLIS_PER_MINUTE);
                if (priorSegment != null && priorSegment.sleepDepth < ACCOUNT_DEPTH_THRESHOLD) {
                    LOGGER.debug("{} prior movement {} {}", partnerSegment.getTimestamp(), priorSegment.getTimestamp(), priorSegment.sleepDepth);
                    noPriorMovement = false;
                    break;
                }
            }

            if (noPriorMovement) {
                affectedSegments.add(new SleepSegment(
                                originalSegment.id,
                                originalSegment.getTimestamp(),
                                originalSegment.timezoneOffset,
                                60,
                                originalSegment.sleepDepth,
                                Event.Type.PARTNER_MOTION,
                                new ArrayList<SensorReading>(),
                                null //soundInfo
                ));
            }

        }

        //return TimelineUtils.categorizeSleepDepth(affectedSegments);  // normalized
        return affectedSegments;
    }
}
