package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Objects;
import com.hello.suripu.core.util.EventTypeSerializer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.util.Set;

/**
 * Created by pangwu on 5/8/14.
 */
public class Event {
    public enum Type { // in order of display priority
        NONE(-1) {
            public String toString() {return "";}
        },
        MOTION(0),
        SLEEP_MOTION(1),
        PARTNER_MOTION(2),
        NOISE(3),
        SNORING(4),
        SLEEP_TALK(5),
        LIGHT(6),
        SUNSET(7),
        SUNRISE(8),
        SLEEP(9),
        WAKE_UP(10);

        private int value;

        private Type(int value) {
            this.value = value;
        }

        public int getValue(){
            return this.value;
        }

        public static Type fromInteger(int value){
            switch (value){
                case -1:
                    return NONE;
                case 0:
                    return MOTION;
                case 1:
                    return SLEEP_MOTION;
                case 2:
                    return PARTNER_MOTION;
                case 3:
                    return NOISE;
                case 4:
                    return SNORING;
                case 5:
                    return SLEEP_TALK;
                case 6:
                    return LIGHT;
                case 7:
                    return SUNSET;
                case 8:
                    return SUNRISE;
                case 9:
                    return SLEEP;
                case 10:
                    return WAKE_UP;
                default:
                    return NONE;
            }
        }
    }

    @JsonProperty("event_type")
    @JsonSerialize(using = EventTypeSerializer.class)
    public final Type type;

    @JsonProperty("start_timestamp")
    public final long startTimestamp;


    @JsonProperty("end_timestamp")
    public final long endTimestamp;

    @JsonProperty("offset_millis")
    public final int timezoneOffset;

    private String message = "";

    @JsonCreator
    public Event(
            @JsonProperty("event_type") final Type type,
            @JsonProperty("start_timestamp") final long startTimestamp,
            @JsonProperty("end_timestamp") final long endTimestamp,
            @JsonProperty("offset_millis") final int timezoneOffset

    ){
        this.type = type;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.timezoneOffset = timezoneOffset;
        initDefaultMessage();
    }


    @JsonCreator
    public Event(
            @JsonProperty("event_type") final Type type,
            @JsonProperty("start_timestamp") final long startTimestamp,
            @JsonProperty("end_timestamp") final long endTimestamp,
            @JsonProperty("message") final String message,
            @JsonProperty("offset_millis") final int timezoneOffset

    ){
        this.type = type;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.timezoneOffset = timezoneOffset;
        this.message = message;
    }

    public static Type getHighPriorityEvents(final Set<Type> eventTypes) {
        Type winner = Type.NONE;
        Integer winnerScore = -100;

        for (final Type eventType : eventTypes) {
            Integer eventScore = -1;
            if (eventType != eventType.NONE) {
                eventScore = eventType.getValue();
            }
            if (eventScore > winnerScore) {
                winner = eventType;
                winnerScore = eventScore;
            }
        }
        return winner;
    }

    @JsonProperty("message")
    public void setMessage(final String message) {
        this.message = message;
    }

    @JsonProperty("message")
    public String getMessage() {
        return this.message;
    }

    private void initDefaultMessage() {
        // TODO: words words words

        switch (this.type) {
            case MOTION:
                this.message = "We detected lots of movement";
                break;
            case SLEEP_MOTION:
                this.message = "Movement detected";
                break;
            case PARTNER_MOTION:
                this.message = String.format("Your partner kicked you at %s",
                        new DateTime(this.startTimestamp, DateTimeZone.forOffsetMillis(this.timezoneOffset))
                                .toString(DateTimeFormat.forPattern("HH:mma")));
                break;
            case NOISE:
                this.message = "Unusual sound detected";
                break;
            case SNORING:
                this.message = "Snoring detected";
                break;
            case SLEEP_TALK:
                this.message = "Sleep talking detected";
                break;
            case LIGHT:
                this.message = "Unusual brightness detected";
                break;
            case SUNSET:
                this.message = String.format("The sun set at %s",
                        new DateTime(this.startTimestamp, DateTimeZone.forOffsetMillis(this.timezoneOffset))
                        .toString(DateTimeFormat.forPattern("HH:mma"))
                );
                break;
            case SUNRISE:
                this.message = String.format("The sun rose at %s",
                        new DateTime(this.startTimestamp, DateTimeZone.forOffsetMillis(this.timezoneOffset))
                                .toString(DateTimeFormat.forPattern("HH:mma"))
                );
                break;
            case SLEEP:
                this.message = String.format("Fell asleep at %s",
                        new DateTime(this.startTimestamp, DateTimeZone.forOffsetMillis(this.timezoneOffset))
                                .toString(DateTimeFormat.forPattern("HH:mma"))
                );
                break;
            case WAKE_UP:
                this.message = "You woke up";
                break;
            case NONE:
                this.message = "";
                break;
            default:
                this.message = String.format("%s at %s", this.type.toString(),
                        new DateTime(this.startTimestamp, DateTimeZone.forOffsetMillis(this.timezoneOffset))
                                .toString(DateTimeFormat.forPattern("HH:mma")));
                break;
        }

    }

    @Override
    public String toString(){
        return getMessage();
    }


    @Override
    public boolean equals(Object other){
        if (other == null){
            return false;
        }

        if (getClass() != other.getClass()){
            return false;
        }

        final Event convertedObject = (Event) other;

        return  Objects.equal(this.type, convertedObject.type)
                && Objects.equal(this.startTimestamp, convertedObject.startTimestamp)
                && Objects.equal(this.endTimestamp, convertedObject.endTimestamp)
                && Objects.equal(this.timezoneOffset, convertedObject.timezoneOffset);
    }
}
