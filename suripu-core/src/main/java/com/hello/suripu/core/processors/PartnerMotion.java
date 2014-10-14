package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TrackerMotion;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 10/13/14.
 */
public class PartnerMotion {
    @Timed
    public static List<SleepSegment> getPartnerData(final long accountID, final List<TrackerMotion> trackerMotions, final DeviceDAO deviceDAO, final TrackerMotionDAO trackerMotionDAO, int threshold) {

        // check if accountID has a partner pill pill paired with the same sense
        final Optional<Long> optionalPartnerAccountId = deviceDAO.getPartnerAccountId(accountID);
        if (!optionalPartnerAccountId.isPresent()) {
            return Collections.emptyList();
        }

        final Long partnerAccountId = optionalPartnerAccountId.get();

        // get tracker motions for partner
        // note that start and end time is in UTC, not local_utc
        final DateTime startTime = new DateTime(trackerMotions.get(0).timestamp, DateTimeZone.UTC);
        final DateTime endTime = new DateTime(trackerMotions.get(trackerMotions.size() - 1).timestamp, DateTimeZone.UTC);
        final List<TrackerMotion> partnerMotions = trackerMotionDAO.getBetween(partnerAccountId, startTime, endTime);

        if (partnerMotions.size() == 0) {
            return Collections.emptyList();
        }

        final List<SleepSegment> partnerSegments = new ArrayList<>();

        // convert list of TrackerMotion to hash map for easy lookup
        final Map<Long, TrackerMotion> motionMap = new HashMap<>();
        for (final TrackerMotion trackerMotion : trackerMotions) {
            motionMap.put(trackerMotion.timestamp, trackerMotion);
        }
        return partnerSegments;
    }
}
