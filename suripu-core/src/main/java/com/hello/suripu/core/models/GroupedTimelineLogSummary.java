package com.hello.suripu.core.models;

import com.hello.suripu.api.logging.LoggingProtos;

/**
 * Created by jakepiccolo on 10/5/15.
 */
public class GroupedTimelineLogSummary {
    public final LoggingProtos.TimelineLog.AlgType algorithm;
    public final LoggingProtos.TimelineLog.ErrorType error;
    public final int count;

    public GroupedTimelineLogSummary(final LoggingProtos.TimelineLog.AlgType algorithm,
                                     final LoggingProtos.TimelineLog.ErrorType error,
                                     final int count) {
        this.algorithm = algorithm;
        this.error = error;
        this.count = count;
    }

    public GroupedTimelineLogSummary(final int algorithm, final int error, final int count) {
        this(LoggingProtos.TimelineLog.AlgType.values()[algorithm],
             LoggingProtos.TimelineLog.ErrorType.values()[error],
             count);
    }
}
