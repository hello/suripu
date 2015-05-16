package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class TimelineFeedback {

    public final DateTime dateOfNight;
    public final String oldTimeOfEvent;
    public final String newTimeOfEvent;
    public final Event.Type eventType;
    public final Optional<Long> accountId;

    private TimelineFeedback(final DateTime dateOfNight, final String oldTimeOfEvent, final String newTimeOfEvent, final Event.Type eventType, final Optional<Long> accountId) {
        this.dateOfNight= dateOfNight;
        this.oldTimeOfEvent = oldTimeOfEvent;
        this.newTimeOfEvent = newTimeOfEvent;
        this.eventType = eventType;
        this.accountId = accountId;
    }

    @JsonCreator
    public static TimelineFeedback create(
            @JsonProperty("date_of_night") final String dateOfNight,
            @JsonProperty("old_time_of_event") final String oldTimeOfEvent,
            @JsonProperty("new_time_of_event") final String newTimeOfEvent,
            @JsonProperty("event_type") final String eventTypeString) {

        final DateTime date = DateTime.parse(dateOfNight).withZone(DateTimeZone.UTC);
        final Event.Type eventType = Event.Type.fromString(eventTypeString);
        return new TimelineFeedback(date, oldTimeOfEvent, newTimeOfEvent, eventType, Optional.<Long>absent());
    }

    public static TimelineFeedback create(final DateTime dateOfNight, final String oldTimeOfEvent, final String newTimeOfEvent, final Event.Type eventType) {
        return new TimelineFeedback(dateOfNight,oldTimeOfEvent,newTimeOfEvent,eventType,Optional.<Long>absent());
    }

    public static TimelineFeedback create(final DateTime dateOfNight, final String oldTimeOfEvent, final String newTimeOfEvent, final Event.Type eventType, final Long accountId) {
        return new TimelineFeedback(dateOfNight,oldTimeOfEvent,newTimeOfEvent,eventType,Optional.of(accountId));
    }
}
