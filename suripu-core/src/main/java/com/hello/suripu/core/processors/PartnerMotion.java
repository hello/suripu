package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SensorReading;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.TimelineUtils;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 10/13/14.
 */
public class PartnerMotion {
    // TODO: tune theses thresholds
    final private static int PARTNER_DEPTH_THRESHOLD = 60; // large movement
    final private static int ACCOUNT_DEPTH_THRESHOLD = 90; // small movement

    @Timed
    public static List<SleepSegment> getPartnerData(final long accountID, final List<SleepSegment> originalSegments, final DateTime startTime, final DateTime endTime, final DeviceDAO deviceDAO, final TrackerMotionDAO trackerMotionDAO, int threshold) {

        // check if accountID has a partner pill pill paired with the same sense
        final Optional<Long> optionalPartnerAccountId = deviceDAO.getPartnerAccountId(accountID);
        if (!optionalPartnerAccountId.isPresent()) {
            return Collections.emptyList();
        }

        final Long partnerAccountId = optionalPartnerAccountId.get();

        // get tracker motions for partner
        // note that start and end time is in UTC, not local_utc
        final List<TrackerMotion> partnerMotions = trackerMotionDAO.getBetween(partnerAccountId, startTime, endTime);

        if (partnerMotions.size() == 0) {
            return Collections.emptyList();
        }

        final int groupBy = 1;
        final boolean createMotionless = false;
        final List<SleepSegment> partnerSegments = TimelineUtils.generateSleepSegments(partnerMotions, threshold, groupBy, createMotionless);

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
            if (partnerSegment.sleepDepth <= PARTNER_DEPTH_THRESHOLD) {
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
        // for instances where m
        return affectedSegments;
    }
}
