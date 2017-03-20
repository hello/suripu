package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;

import java.util.Set;
import java.util.UUID;

/**
 * Created by benjo on 1/20/16.
 */
public interface TimelineAlgorithm {

    Optional<TimelineAlgorithmResult> getTimelinePrediction(final OneDaysSensorData sensorData, final SleepPeriod sleepPeriod, final TimelineLog log, final long accountId, final boolean feedbackChanged, final Set<String> enabledFeatures);
    TimelineAlgorithm cloneWithNewUUID(Optional<UUID> uuid);
}
