package com.hello.suripu.core.models.timeline.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.Event;

public class TimelineFeedback {

    public final Long eventTimestamp;
    public final String newTimeOfEvent;
    public final Event.Type eventType;

    private TimelineFeedback(final Long eventTimestamp, final String newTimeOfEvent, final Event.Type eventType) {
        this.eventTimestamp = eventTimestamp;
        this.newTimeOfEvent = newTimeOfEvent;
        this.eventType = eventType;
    }

    @JsonCreator
    public static TimelineFeedback create(
            @JsonProperty("event_timestamp") final Long eventTimestampInSeconds,
            @JsonProperty("new_event_time") final String newTimeOfEvent,
            @JsonProperty("event_type") final String eventTypeString) {

        final Long eventTimestamp = eventTimestampInSeconds * 1000L;
        final Event.Type eventType = Event.Type.fromString(eventTypeString);
        return new TimelineFeedback(eventTimestamp, newTimeOfEvent, eventType);
    }
}
