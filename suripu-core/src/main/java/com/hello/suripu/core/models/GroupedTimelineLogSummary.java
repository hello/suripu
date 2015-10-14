package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.api.logging.LoggingProtos;

/**
 * Created by jakepiccolo on 10/5/15.
 */
public class GroupedTimelineLogSummary {

    @JsonProperty("algorithm")
    public final LoggingProtos.TimelineLog.AlgType algorithm;

    @JsonProperty("error")
    public final LoggingProtos.TimelineLog.ErrorType error;

    @JsonProperty("count")
    public final int count;

    @JsonProperty("date")
    public final String date;

    public GroupedTimelineLogSummary(final LoggingProtos.TimelineLog.AlgType algorithm,
                                     final LoggingProtos.TimelineLog.ErrorType error,
                                     final int count,
                                     final String date) {
        this.algorithm = algorithm;
        this.error = error;
        this.count = count;
        this.date = date;
    }

    public GroupedTimelineLogSummary(final int algorithm, final int error, final int count, final String date) {
        this(LoggingProtos.TimelineLog.AlgType.values()[algorithm],
             LoggingProtos.TimelineLog.ErrorType.values()[error],
             count, date);
    }
}
