package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class TimelineFeedback {

    public final DateTime dateOfNight;
    public final String oldTimeOfEvent;
    public final String newTimeOfEvent;
    public final Event.Type eventType;

    private TimelineFeedback(final DateTime dateOfNight, final String oldTimeOfEvent, final String newTimeOfEvent, final Event.Type eventType) {
        this.dateOfNight= dateOfNight;
        this.oldTimeOfEvent = oldTimeOfEvent;
        this.newTimeOfEvent = newTimeOfEvent;
        this.eventType = eventType;
    }

    @JsonCreator
    public static TimelineFeedback create(
            @JsonProperty("date_of_night") final String dateOfNight,
            @JsonProperty("old_time_of_event") final String oldTimeOfEvent,
            @JsonProperty("new_time_of_event") final String newTimeOfEvent,
            @JsonProperty("event_type") final String eventTypeString) {

        final DateTime date = DateTime.parse(dateOfNight).withZone(DateTimeZone.UTC);
        final Event.Type eventType = Event.Type.fromString(eventTypeString);
        return new TimelineFeedback(date, oldTimeOfEvent, newTimeOfEvent, eventType);
    }
}
