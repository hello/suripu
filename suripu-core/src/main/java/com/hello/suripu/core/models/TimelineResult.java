package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by benjo on 4/9/15.
 */
public class TimelineResult {

    @JsonProperty("timelines")
    public final ImmutableList<Timeline> timelines;

    @JsonProperty("log")
    public final TimelineLog log;

    @JsonCreator
    public static TimelineResult create(@JsonProperty("timelines") final List<Timeline> timelines,
                                        @JsonProperty("log") final TimelineLog log) {

        return new TimelineResult(ImmutableList.copyOf(timelines),log);
    }


    static public TimelineResult createEmpty() {
        final Timeline timeline = Timeline.createEmpty();
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return new TimelineResult(ImmutableList.copyOf(timelines),TimelineLog.createEmpty());
    }

    static public TimelineResult createEmpty(String message) {
        final Timeline timeline = Timeline.createEmpty(message);
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return new TimelineResult(ImmutableList.copyOf(timelines), TimelineLog.createEmpty());
    }


    private TimelineResult(ImmutableList<Timeline> timelines, TimelineLog log) {
        this.timelines = timelines;
        this.log = log;
    }
}
