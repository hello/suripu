package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by pangwu on 5/8/14.
 */
public class Event {
    public enum Type {
        MOTION(0),
        NOISE(1),
        SNORING(2),
        SLEEP_TALK(3);

        private int value;

        private Type(int value) {
            this.value = value;
        }

        public int getValue(){
            return this.value;
        }

        public static Type fromInteger(int value){
            switch (value){
                case 0:
                    return MOTION;
                case 1:
                    return NOISE;
                case 2:
                    return SNORING;
                default:
                    return SLEEP_TALK;
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
}
