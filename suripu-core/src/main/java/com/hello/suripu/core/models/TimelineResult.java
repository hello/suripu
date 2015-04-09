package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.TimelineLogDAO;

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
    public static TimelineResult create(@JsonProperty("timelines") final ImmutableList<Timeline> timelines,
                                        @JsonProperty("log") final TimelineLog log) {

        return new TimelineResult(timelines,log);
    }

    public TimelineResult(ImmutableList<Timeline> timelines, TimelineLog log) {
        this.timelines = timelines;
        this.log = log;
    }


    static public TimelineResult createEmpty() {
        final Timeline timeline = Timeline.createEmpty();
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return new TimelineResult(ImmutableList.copyOf(timelines),TimelineLog.createEmpty());
    }
}
