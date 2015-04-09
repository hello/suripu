package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.TimelineLogDAO;

import java.util.List;

/**
 * Created by benjo on 4/9/15.
 */
public class TimelineResult {
    public final ImmutableList<Timeline> timelines;
    public final Optional<TimelineLog> log;

    public TimelineResult(ImmutableList<Timeline> timelines, TimelineLog log) {
        this.timelines = timelines;
        this.log = Optional.of(log);
    }

    public TimelineResult(ImmutableList<Timeline> timelines) {
        this.timelines = timelines;
        this.log = Optional.absent();
    }

    static public TimelineResult createEmpty() {
        final Timeline timeline = Timeline.createEmpty();
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return new TimelineResult(ImmutableList.copyOf(timelines));
    }
}
