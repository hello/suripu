package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;

import java.util.Collections;
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

        return "";
    }

    @JsonIgnore
    public final ImmutableList<TimelineLog> logV2List;

    @JsonIgnore
    public final DataCompleteness dataCompleteness;

    @JsonCreator
    public static TimelineResult create(@JsonProperty("timelines") final List<Timeline> timelines,
                                        @JsonProperty("logV2") final String log) {


        if (log == null || log.equals("")) {
            //older record.... the v2 information will not be there
            return new TimelineResult(ImmutableList.copyOf(timelines),Optional.<TimelineLog>absent(),ImmutableList.copyOf(Collections.EMPTY_LIST), DataCompleteness.ENOUGH_DATA);
        }

        return new TimelineResult(ImmutableList.copyOf(timelines), TimelineLog.createFromProtobuf(log), ImmutableList.copyOf(Collections.EMPTY_LIST), DataCompleteness.ENOUGH_DATA);

    }

    public static TimelineResult create(final List<Timeline> timelines,
                                        final TimelineLog log) {
        return new TimelineResult(ImmutableList.copyOf(timelines),Optional.of(log),ImmutableList.copyOf(Collections.EMPTY_LIST), DataCompleteness.ENOUGH_DATA);
    }

    public static TimelineResult create(final List<Timeline> timelines,
                                        final List<TimelineLog> logs) {

        //hack: set log to last log in list.
        final Optional<TimelineLog> logOptional;
        if(!logs.isEmpty()){
            logOptional = Optional.of(logs.get(logs.size()-1));
        } else{
            logOptional = Optional.absent();
        }
        return new TimelineResult(ImmutableList.copyOf(timelines), logOptional, ImmutableList.copyOf(logs), DataCompleteness.ENOUGH_DATA);
    }


    static public TimelineResult createEmpty() {
        final Timeline timeline = Timeline.createEmpty();
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return new TimelineResult(ImmutableList.copyOf(timelines), Optional.<TimelineLog>absent(), ImmutableList.copyOf(Collections.EMPTY_LIST), DataCompleteness.ENOUGH_DATA);
    }

    static public TimelineResult createEmpty(final TimelineLog logV2) {
        final Timeline timeline = Timeline.createEmpty();
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return new TimelineResult(ImmutableList.copyOf(timelines),Optional.of(logV2),ImmutableList.copyOf(Collections.EMPTY_LIST), DataCompleteness.ENOUGH_DATA);
    }

    static public TimelineResult createEmpty(final TimelineLog logV2, final String message) {
        return createEmpty(logV2, message, DataCompleteness.ENOUGH_DATA);
    }

    static public TimelineResult createEmpty(final TimelineLog logV2, final String message, final DataCompleteness dataCompleteness) {
        final Timeline timeline = Timeline.createEmpty(message);
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return new TimelineResult(ImmutableList.copyOf(timelines), Optional.of(logV2), ImmutableList.copyOf(Collections.EMPTY_LIST),  dataCompleteness);
    }

    private TimelineResult(final ImmutableList<Timeline> timelines,final Optional<TimelineLog> log, final ImmutableList<TimelineLog> logs, final DataCompleteness completeness) {
        this.timelines = timelines;
        this.logV2 = log;
        this.logV2List = logs;
        this.dataCompleteness = completeness;
    }
}
