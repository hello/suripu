package com.hello.suripu.core.models.timeline.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepSegment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TimelineEvent {

    @JsonProperty("timestamp")
    public final Long timestamp;

    @JsonProperty("timezone_offset")
    public final Integer timezoneOffset;

    @JsonProperty("duration_millis")
    public final Long duration;

    @JsonProperty("message")
    public final String message;

    @JsonProperty("sleep_depth")
    public final Integer sleepDepth;

    @JsonProperty("sleep_state")
    public final SleepState sleepState;

    @JsonProperty("event_type")
    public final EventType eventType;

    @JsonProperty("sleep_period")
    public final SleepPeriod.Period sleepPeriod;

    @JsonProperty("valid_actions")
    public final List<ValidAction> validActions;

    private TimelineEvent(final Long timestamp,
                          final Integer timezoneOffset,
                          final Integer duration,
                          final String message,
                          final Integer sleepDepth,
                          final SleepState sleepState,
                          final EventType eventType,
                          final SleepPeriod.Period sleepPeriod,
                          final List<ValidAction> validActions) {
        this.timestamp = timestamp;
        this.timezoneOffset = timezoneOffset;
        this.duration = duration * 1000L;
        this.message = message;
        this.sleepDepth = sleepDepth;
        this.sleepState = sleepState;
        this.eventType = eventType;
        this.sleepPeriod = sleepPeriod;
        this.validActions = validActions;
    }

    public static TimelineEvent create(final Long timestamp,
                                       final Integer timezoneOffset,
                                       final Integer durationInSeconds,
                                       final String message,
                                       final Integer sleepDepth,
                                       final SleepState sleepState,
                                       final EventType eventType,
                                       final SleepPeriod.Period sleepPeriod) {
        return new TimelineEvent(timestamp, timezoneOffset, durationInSeconds, message, sleepDepth, sleepState, eventType, sleepPeriod, Collections.EMPTY_LIST);
    }




    public static TimelineEvent fromV1(final SleepSegment segment) {
        SleepState sleepState;
        EventType eventType;
        SleepPeriod.Period sleepPeriod = segment.getSleepPeriod();
        if (segment.getType() == Event.Type.NONE) {
            sleepState = SleepState.AWAKE;
            eventType = EventType.IN_BED;
        } else {
            sleepState = SleepState.from(segment.getSleepDepth());
            eventType = from(segment.getType());
        }

        return new TimelineEvent(
                segment.getTimestamp(),
                segment.getOffsetMillis(),
                segment.getDurationInSeconds(),
                segment.getMessage(),
                segment.getSleepDepth(),
                sleepState,
                eventType,
                sleepPeriod,
                ValidAction.from(segment.getType())
        );
    }

    public static List<TimelineEvent>  fromV1(final List<SleepSegment> segments) {
        final List<TimelineEvent> eventList = Lists.newArrayList();
        for(final SleepSegment segment : segments) {
            eventList.add(fromV1(segment));
        }
        return eventList;
    }


    private static final ImmutableMap<Event.Type, EventType> typesMapping;
    static {
        final Map<Event.Type, EventType> temp = Maps.newHashMap();

        temp.put(Event.Type.SLEEPING, EventType.IN_BED);

        temp.put(Event.Type.IN_BED, EventType.GOT_IN_BED);
        temp.put(Event.Type.OUT_OF_BED, EventType.GOT_OUT_OF_BED);
        temp.put(Event.Type.SLEEP, EventType.FELL_ASLEEP);
        temp.put(Event.Type.WAKE_UP, EventType.WOKE_UP);


        temp.put(Event.Type.MOTION, EventType.GENERIC_MOTION);
        temp.put(Event.Type.PARTNER_MOTION, EventType.PARTNER_MOTION);

        temp.put(Event.Type.NOISE, EventType.GENERIC_SOUND);
        temp.put(Event.Type.ALARM, EventType.ALARM_RANG);

        temp.put(Event.Type.SNORING, EventType.SNORED);
        temp.put(Event.Type.SLEEP_TALK, EventType.SLEEP_TALKED);

        temp.put(Event.Type.LIGHT, EventType.LIGHT);
        temp.put(Event.Type.LIGHTS_OUT, EventType.LIGHTS_OUT);

        temp.put(Event.Type.SUNSET, EventType.SUNSET);
        temp.put(Event.Type.SUNRISE, EventType.SUNRISE);

        typesMapping = ImmutableMap.copyOf(temp);
    }

    public static EventType from(Event.Type eventType) {
        if(typesMapping.containsKey(eventType)) {
            return typesMapping.get(eventType);
        }

        return EventType.UNKNOWN;
    }


    public static class TimeAmendment {
        public final String newEventTime;
        public final Integer timezoneOffset;


        private TimeAmendment(final String newEventTime, final Integer timezoneOffset) {
            this.newEventTime = newEventTime;
            this.timezoneOffset = timezoneOffset;
        }

        public static TimeAmendment create(@JsonProperty("new_event_time") final String newEventTime) {
            return new TimeAmendment(newEventTime, 0);
        }

        @JsonCreator
        public static TimeAmendment create(@JsonProperty("new_event_time") final String newEventTime,
                                           @JsonProperty("timezone_offset") final Integer timezoneOffset) {
            final Integer offset = (timezoneOffset == null) ? 0 : timezoneOffset;
            return new TimeAmendment(newEventTime, offset);
        }

        public static TimeAmendment withOffset(final TimeAmendment timeAmendment, final Integer timezoneOffset) {
            return new TimeAmendment(timeAmendment.newEventTime, timezoneOffset);
        }
    }
}
