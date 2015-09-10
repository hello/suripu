package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;

import java.util.List;

/**
 * Created by benjo on 4/9/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimelineResult {

    @JsonProperty("timelines")
    public final ImmutableList<Timeline> timelines;

    public final Optional<TimelineLog> logV2;

    @JsonProperty("logV2")
    public String getTimelineLogV2() {

        if (logV2.isPresent()) {
            return logV2.get().toProtobufBase64();
        }
        else {
            return "";
        }

    }

    @JsonIgnore
    public final boolean notEnoughData;

    @JsonCreator
    public static TimelineResult create(@JsonProperty("timelines") final List<Timeline> timelines,
                                        @JsonProperty("logV2") final String log) {


        if (log == null || log.equals("")) {
            //older record.... the v2 information will not be there
            return new TimelineResult(ImmutableList.copyOf(timelines),Optional.<TimelineLog>absent(),false);
        }

        return new TimelineResult(ImmutableList.copyOf(timelines), TimelineLog.createFromProtobuf(log),false);

    }

    public static TimelineResult create(final List<Timeline> timelines,
                                        final TimelineLog log) {
        return new TimelineResult(ImmutableList.copyOf(timelines),Optional.of(log),false);
    }


    static public TimelineResult createEmpty() {
        final Timeline timeline = Timeline.createEmpty();
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return new TimelineResult(ImmutableList.copyOf(timelines), Optional.<TimelineLog>absent(),false);
    }

    static public TimelineResult createEmpty(TimelineLog logV2) {
        final Timeline timeline = Timeline.createEmpty();
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return new TimelineResult(ImmutableList.copyOf(timelines),Optional.of(logV2),false);
    }

    static public TimelineResult createEmpty(TimelineLog logV2, String message) {
        final Timeline timeline = Timeline.createEmpty(message);
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return new TimelineResult(ImmutableList.copyOf(timelines), Optional.of(logV2),false);
    }

    static public TimelineResult createEmpty(TimelineLog logV2, String message,boolean notEnoughData) {
        final Timeline timeline = Timeline.createEmpty(message);
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return new TimelineResult(ImmutableList.copyOf(timelines), Optional.of(logV2),notEnoughData);
    }


    private TimelineResult(ImmutableList<Timeline> timelines, Optional<TimelineLog> log, boolean notEnoughData) {
        this.timelines = timelines;
        this.logV2 = log;
        this.notEnoughData =  notEnoughData;
    }


}
