package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.Set;

/**
 * Created by pangwu on 5/8/14.
 */
public class Event {
    public enum Type { // in order of display priority
        NONE(-1),
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
    public final Type type;

    @JsonProperty("start_timestamp")
    public final long startTimestamp;


    @JsonProperty("end_timestamp")
    public final long endTimestamp;

    @JsonProperty("timezone_offset")
    public final int timezoneOffset;

    @JsonCreator
    public Event(

            @JsonProperty("event_type") final Type type,
            @JsonProperty("start_timestamp") final long startTimestamp,
            @JsonProperty("end_timestamp") final long endTimestamp,
            @JsonProperty("timezone_offset") final int timezoneOffset

    ){
        this.type = type;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.timezoneOffset = timezoneOffset;
    }

    public static String getHighPriorityEvents(final Set<String> eventTypes) {
        String winner = "";
        Integer winnerScore = -100;

        for (final String eventType : eventTypes) {
            final Integer eventScore = Type.valueOf(eventType).getValue();
            if (eventScore > winnerScore) {
                winner = eventType;
                winnerScore = eventScore;
            }
        }
        return winner;
    }

    public static String getMessage(final Type eventType, final DateTime dateTime) {
        // TODO: words words words
        final String eventMessage;
        switch (eventType) {
            case MOTION:
                eventMessage = "We detected lots of movement";
                break;
            case SLEEP_MOTION:
                eventMessage = "Movement detected";
                break;
            case PARTNER_MOTION:
                eventMessage = "Your partner kicked you";
                break;
            case NOISE:
                eventMessage = "Unusual sound detected";
                break;
            case SNORING:
                eventMessage = "Snoring detected";
                break;
            case SLEEP_TALK:
                eventMessage = "Sleep talking detected";
                break;
            case LIGHT:
                eventMessage = "Unusual brightness detected";
                break;
            case SUNSET:
                eventMessage = "The sun set";
                break;
            case SUNRISE:
                eventMessage = "The sun rose";
                break;
            default:
                return "";
        }
        return String.format("%s at %s", eventMessage, dateTime.toString(DateTimeFormat.forPattern("HH:mma")));
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
