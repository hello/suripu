package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

public class TimelineFeedback {

    @JsonProperty("date_of_night")
    public final DateTime dateOfNight;

    @JsonProperty("sleep_period")
    public final SleepPeriod.Period sleepPeriod;

    @JsonProperty("old_time_event")
    public final String oldTimeOfEvent;

    @JsonProperty("new_time_event")
    public final String newTimeOfEvent;

    @JsonProperty("event_type")
    public final Event.Type eventType;

    @JsonProperty("account_id")
    public final Optional<Long> accountId;

    @JsonProperty("created")
    public final Optional<Long> created;

    @JsonProperty("id")
    public final Optional<Long> id;

    @JsonProperty("is_correct")
    public final Boolean isNewTimeCorrect;

    //public for testing purposes
    private TimelineFeedback(final DateTime dateOfNight, final SleepPeriod.Period period, final String oldTimeOfEvent, final String newTimeOfEvent,
                             final Event.Type eventType, final Optional<Long> accountId, final Optional<Long> created,
                             final Long id, final Boolean isNewTimeCorrect) {
        this.dateOfNight= dateOfNight;
        this.sleepPeriod = period;
        this.oldTimeOfEvent = oldTimeOfEvent;
        this.newTimeOfEvent = newTimeOfEvent;
        this.eventType = eventType;
        this.accountId = accountId;
        //when inserting, this happens automatically (ergo you can make this field absent).
        //When querying, this field will be populated.
        this.created = created;
        this.id = Optional.fromNullable(id);
        this.isNewTimeCorrect = isNewTimeCorrect;
    }

    @JsonCreator
    public static TimelineFeedback create(
            @JsonProperty("date_of_night") final String dateOfNight,
            @JsonProperty("old_time_of_event") final String oldTimeOfEvent,
            @JsonProperty("new_time_of_event") final String newTimeOfEvent,
            @JsonProperty("event_type") final String eventTypeString) {

        final DateTime date = DateTime.parse(dateOfNight);
        final DateTime realDate = new DateTime(date.getMillis(), DateTimeZone.UTC).withTimeAtStartOfDay();
        final Event.Type eventType = Event.Type.fromString(eventTypeString);
        final SleepPeriod.Period defaultPeriod = SleepPeriod.Period.NIGHT;
        return new TimelineFeedback(realDate, defaultPeriod, oldTimeOfEvent, newTimeOfEvent, eventType, Optional.<Long>absent(),Optional.<Long>absent(), null, Boolean.TRUE);
    }

    @JsonCreator
    public static TimelineFeedback create(
            @JsonProperty("date_of_night") final String dateOfNight,
            @JsonProperty("sleep_period") final int sleepPeriodInt,
            @JsonProperty("old_time_of_event") final String oldTimeOfEvent,
            @JsonProperty("new_time_of_event") final String newTimeOfEvent,
            @JsonProperty("event_type") final String eventTypeString) {

        final SleepPeriod.Period period = SleepPeriod.Period.fromInteger(sleepPeriodInt);
        final DateTime date = DateTime.parse(dateOfNight);
        final DateTime realDate = new DateTime(date.getMillis(), DateTimeZone.UTC).withTimeAtStartOfDay();
        final Event.Type eventType = Event.Type.fromString(eventTypeString);
        return new TimelineFeedback(realDate, period, oldTimeOfEvent, newTimeOfEvent, eventType, Optional.<Long>absent(),Optional.<Long>absent(), null, Boolean.TRUE);
    }

    private static TimelineFeedback create(final String dateOfNight, final String oldTimeOfEvent, final String newTimeOfEvent, final Event.Type eventType, final Long accountId, final Boolean isNewTimeCorrect) {
        final DateTime realDate = new DateTime(DateTime.parse(dateOfNight), DateTimeZone.UTC).withTimeAtStartOfDay();
        final SleepPeriod.Period defaultPeriod = SleepPeriod.Period.NIGHT;
        return new TimelineFeedback(realDate,defaultPeriod, oldTimeOfEvent,newTimeOfEvent,eventType,Optional.of(accountId),Optional.<Long>absent(), null, isNewTimeCorrect);
    }

    public static TimelineFeedback create(final DateTime dateOfNight, final String oldTimeOfEvent, final String newTimeOfEvent, final Event.Type eventType, final Long accountId, final Long created, final Long id, final Boolean isNewTimeCorrect) {
        final SleepPeriod.Period defaultPeriod = SleepPeriod.Period.NIGHT;
        return new TimelineFeedback(dateOfNight,defaultPeriod,oldTimeOfEvent,newTimeOfEvent,eventType,Optional.of(accountId),Optional.of(created), id, isNewTimeCorrect);
    }

    //with SleepPeriod
    private static TimelineFeedback create(final String dateOfNight, final SleepPeriod.Period sleepPeriod, final String oldTimeOfEvent, final String newTimeOfEvent, final Event.Type eventType, final Long accountId, final Boolean isNewTimeCorrect) {
        final DateTime realDate = new DateTime(DateTime.parse(dateOfNight), DateTimeZone.UTC).withTimeAtStartOfDay();
        return new TimelineFeedback(realDate,sleepPeriod, oldTimeOfEvent,newTimeOfEvent,eventType,Optional.of(accountId),Optional.<Long>absent(), null, isNewTimeCorrect);
    }

    public static TimelineFeedback create(final DateTime dateOfNight,final SleepPeriod.Period sleepPeriod, final String oldTimeOfEvent, final String newTimeOfEvent, final Event.Type eventType, final Long accountId, final Long created, final Long id, final Boolean isNewTimeCorrect) {
        return new TimelineFeedback(dateOfNight,sleepPeriod,oldTimeOfEvent,newTimeOfEvent,eventType,Optional.of(accountId),Optional.of(created), id, isNewTimeCorrect);
    }


    public static TimelineFeedback createTimeAmendedFeedback(final String dateOfNight, final String oldTimeOfEvent, final String newTimeOfEvent, final Event.Type eventType, final Long accountId) {
        return create(dateOfNight, oldTimeOfEvent, newTimeOfEvent, eventType, accountId, Boolean.TRUE);
    }

    public static TimelineFeedback createMarkedIncorrect(final String dateOfNight, final String oldTimeOfEvent, final Event.Type eventType, final Long accountId) {
        return create(dateOfNight, oldTimeOfEvent, oldTimeOfEvent, eventType, accountId, Boolean.FALSE);
    }

    public static TimelineFeedback createMarkedCorrect(final String dateOfNight, final String oldTimeOfEvent, final Event.Type eventType, final Long accountId) {
        return create(dateOfNight, oldTimeOfEvent, oldTimeOfEvent, eventType, accountId, Boolean.TRUE);
    }

    public static TimelineFeedback createTimeAmendedFeedback(final String dateOfNight,SleepPeriod.Period sleepPeriod, final String oldTimeOfEvent, final String newTimeOfEvent, final Event.Type eventType, final Long accountId) {
        return create(dateOfNight,sleepPeriod, oldTimeOfEvent, newTimeOfEvent, eventType, accountId, Boolean.TRUE);
    }

    public static TimelineFeedback createMarkedIncorrect(final String dateOfNight, SleepPeriod.Period sleepPeriod,final String oldTimeOfEvent, final Event.Type eventType, final Long accountId) {
        return create(dateOfNight, sleepPeriod,oldTimeOfEvent, oldTimeOfEvent, eventType, accountId, Boolean.FALSE);
    }

    public static TimelineFeedback createMarkedCorrect(final String dateOfNight,SleepPeriod.Period sleepPeriod, final String oldTimeOfEvent, final Event.Type eventType, final Long accountId) {
        return create(dateOfNight, sleepPeriod,oldTimeOfEvent, oldTimeOfEvent, eventType, accountId, Boolean.TRUE);
    }


    public int getDeltaInMinutes() {
        final DateTime oldDateTime  = DateTime.parse(oldTimeOfEvent, DateTimeFormat.forPattern("HH:mm"));
        final DateTime newDateTime  = DateTime.parse(newTimeOfEvent, DateTimeFormat.forPattern("HH:mm"));

        int delta = (int) (newDateTime.getMillis() - oldDateTime.getMillis());

        // t1          t2
        // 00:00 ---> 23:59 == 23:59 - 00:00 should be -1 min
        if (delta > DateTimeConstants.MILLIS_PER_DAY / 2) {
            delta = delta - DateTimeConstants.MILLIS_PER_DAY;
        }

        //  t1         t2
        //23:59 ---> 00:00 == 00:00 - 23:59 should be +1 min
        if (delta < -DateTimeConstants.MILLIS_PER_DAY / 2) {
            delta = DateTimeConstants.MILLIS_PER_DAY + delta;
        }

        return delta / DateTimeConstants.MILLIS_PER_MINUTE;
    }
}
