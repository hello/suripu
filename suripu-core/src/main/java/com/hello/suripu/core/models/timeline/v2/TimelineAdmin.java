package com.hello.suripu.core.models.timeline.v2;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.TimelineLog;

public class TimelineAdmin {

    @JsonProperty("timeline")
    private final Timeline timeline;

    @JsonProperty("timeline_log")
    private final TimelineLog timelineLog;

    @JsonCreator
    public TimelineAdmin(final Timeline timeline, final TimelineLog timelineLog) {
        this.timeline = timeline;
        this.timelineLog = timelineLog;
    }
}
