package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class TimelineFeedback {

    public final DateTime day;
    public final Event.Type eventType;
    public final DateTime eventDateTime;

    private TimelineFeedback(final DateTime day, final Event.Type eventType, final DateTime eventDateTime) {
        this.day = day;
        this.eventType = eventType;
        this.eventDateTime = eventDateTime;
    }

    @JsonCreator
    public static TimelineFeedback create(
            @JsonProperty("day") final String day,
            @JsonProperty("event_type") final String eventTypeString,
            @JsonProperty("ts") final Long timestampUTC) {

        final DateTime date = DateTime.parse(day).withZone(DateTimeZone.UTC);
        final DateTime eventDateTime = new DateTime(timestampUTC, DateTimeZone.UTC);
        final Event.Type eventType = Event.Type.fromString(eventTypeString);
        return new TimelineFeedback(date, eventType, eventDateTime);
    }
}
