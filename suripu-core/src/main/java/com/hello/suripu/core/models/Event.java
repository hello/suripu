package com.hello.suripu.core.models;

import com.google.common.base.Objects;

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


    private Type type;

    public final long startTimestamp;

    public final long endTimestamp;

    public final int timezoneOffset;

    private String message = "";

    public Type getType(){
        return type;
    }

    protected void setType(final Type type){
        this.type = type;
    }

    public Event(final Type type,
                 final long startTimestamp,
                 final long endTimestamp,
                 final int timezoneOffset

    ){
        this.type = type;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.timezoneOffset = timezoneOffset;
    }

    public Event(
            final Type type,
            final long startTimestamp,
            final long endTimestamp,
            final String message,
            final int timezoneOffset

    ){
        this.type = type;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.timezoneOffset = timezoneOffset;
        this.message = message;
    }

    public void setDescription(final String message) {
        this.message = message;
    }

    public String getDescription() {
        return this.message;
    }


    @Override
    public String toString(){
        return getDescription();
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
