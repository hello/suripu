package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.core.logging.TimelineLogV2;

import java.util.List;

/**
 * Created by benjo on 4/9/15.
 */
public class TimelineResult {

    @JsonProperty("timelines")
    public final ImmutableList<Timeline> timelines;


    public final TimelineLogV2 logV2;

    @JsonProperty("logV2")
    public String getTimelineLogV2() {
        return logV2.toProtobufBase64();
    }

    @JsonCreator
    public static TimelineResult create(@JsonProperty("timelines") final List<Timeline> timelines,
                                        @JsonProperty("logV2") final String log) throws JsonMappingException {

        try {
            return new TimelineResult(ImmutableList.copyOf(timelines),TimelineLogV2.createFromProtobuf(log));

        }
        catch(InvalidProtocolBufferException exception) {
            throw new JsonMappingException("could not deserialized stored protobuf");
        }


    }


    static public TimelineResult createEmpty() {
        final Timeline timeline = Timeline.createEmpty();
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        final TimelineLogV2 emptyLog = new TimelineLogV2(0L);
        return new TimelineResult(ImmutableList.copyOf(timelines), emptyLog);

    }

    static public TimelineResult createEmpty(TimelineLogV2 logV2) {
        final Timeline timeline = Timeline.createEmpty();
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return new TimelineResult(ImmutableList.copyOf(timelines),logV2);
    }

    static public TimelineResult createEmpty(TimelineLogV2 logV2, String message) {
        final Timeline timeline = Timeline.createEmpty(message);
        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return new TimelineResult(ImmutableList.copyOf(timelines), logV2);
    }


    public TimelineResult(ImmutableList<Timeline> timelines, TimelineLogV2 log) {
        this.timelines = timelines;
        this.logV2 = log;

    }
}
