package com.hello.suripu.core.pill.data;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by jyfan on 9/7/16.
 */
public class AggStatTrackerMotion {

    public final DateTime localTime;

    public AggStatTrackerMotion(final DateTime localTime) {
        this.localTime = localTime;
    }

    public static List<AggStatTrackerMotion> createAggStatTrackerMotionList(final List<TrackerMotion> trackerMotions) {

        final List<AggStatTrackerMotion> aggStatTrackerMotions = Lists.newArrayList();

        for (final TrackerMotion trackerMotion : trackerMotions) {

            final AggStatTrackerMotion aggStatTrackerMotionDynamo = new AggStatTrackerMotion(
                    trackerMotion.localTime());

            aggStatTrackerMotions.add(aggStatTrackerMotionDynamo);
        }

        return aggStatTrackerMotions;
    }

}
