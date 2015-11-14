package com.hello.suripu.core.db;

import com.hello.suripu.core.models.TrackerMotion;

import java.util.List;

/**
 * Created by kingshy on 11/12/15.
 */
public interface PillDataIngestDAO {
    int batchInsertTrackerMotionData(final List<TrackerMotion> trackerMotionList, final int batchSize);
    Class name();
}