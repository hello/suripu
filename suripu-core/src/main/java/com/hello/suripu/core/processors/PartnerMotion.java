package com.hello.suripu.core.processors;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SensorReading;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.TimelineUtils;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 10/13/14.
 */
public class PartnerMotion {
    // TODO: tune these thresholds
    final private static int PARTNER_DEPTH_THRESHOLD = 60; // large movement
    final private static int ACCOUNT_DEPTH_THRESHOLD = 90; // small movement

    @Timed
    public static List<SleepSegment> getPartnerData(final List<SleepSegment> originalSegments, final List<TrackerMotion> partnerMotions, int threshold) {

        final boolean createMotionlessSegment = false;
        final List<SleepSegment> partnerSegments = TimelineUtils.generateSleepSegments(partnerMotions, threshold, createMotionlessSegment);

        // convert list of TrackerMotion to hash map for easy lookup
        final Map<Long, SleepSegment> partnerMotionMap = new HashMap<>();
        for (final SleepSegment segment : partnerSegments) {
            partnerMotionMap.put(segment.timestamp, segment);
        }

        final List<SleepSegment> affectedSegments = new ArrayList<>();
        for (final SleepSegment segment : originalSegments) {
            if (segment.sleepDepth > ACCOUNT_DEPTH_THRESHOLD || !partnerMotionMap.containsKey(segment.timestamp)) {
                continue;
            }
            final SleepSegment partnerSegment = partnerMotionMap.get(segment.timestamp);
            if (partnerSegment.sleepDepth <= PARTNER_DEPTH_THRESHOLD && partnerSegment.sleepDepth <= segment.sleepDepth) {
                affectedSegments.add(new SleepSegment(
                                segment.id,
                                segment.timestamp,
                                segment.offsetMillis,
                                60,
                                segment.sleepDepth,
                                Event.Type.PARTNER_MOTION.toString(),
                                Event.getMessage(Event.Type.PARTNER_MOTION, new DateTime(segment.timestamp)),
                                new ArrayList<SensorReading>())
                );
            }
        }
        //return TimelineUtils.categorizeSleepDepth(affectedSegments);  // normalized
        return affectedSegments;
    }
}
