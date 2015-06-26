package com.hello.suripu.core.models.timeline.v2;

import com.hello.suripu.core.models.Event;

public class TimelineEvent {

    public final Long timestamp;
    public final Integer timezoneOffset;
    public final Integer duration;
    public final String message;

    public final Integer sleepDepth;
    public final String sleepState;

    public final Event.Type eventType;

    private TimelineEvent(final Long timestamp, final Integer timezoneOffset, final Integer duration, final String message, final Integer sleepDepth, final String sleepState, final Event.Type eventType) {
        this.timestamp = timestamp;
        this.timezoneOffset = timezoneOffset;
        this.duration = duration;
        this.message = message;
        this.sleepDepth = sleepDepth;
        this.sleepState = sleepState;
        this.eventType = eventType;
    }

    public static TimelineEvent create(final Long timestamp, final Integer timezoneOffset, final Integer duration, final String message, final Integer sleepDepth, final String sleepState, final Event.Type eventType) {
        return new TimelineEvent(timestamp, timezoneOffset, duration, message, sleepDepth, sleepState, eventType);
    }
}
