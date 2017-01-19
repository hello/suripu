package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.Events.AlarmEvent;
import com.hello.suripu.core.models.Events.InBedEvent;
import com.hello.suripu.core.models.Events.LightEvent;
import com.hello.suripu.core.models.Events.LightsOutEvent;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.NoiseEvent;
import com.hello.suripu.core.models.Events.NullEvent;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import com.hello.suripu.core.models.Events.PartnerMotionEvent;
import com.hello.suripu.core.models.Events.FallingAsleepEvent;
import com.hello.suripu.core.models.Events.SleepDisturbanceEvent;
import com.hello.suripu.core.models.Events.SleepMotionEvent;
import com.hello.suripu.core.models.Events.SleepingEvent;
import com.hello.suripu.core.models.Events.SunRiseEvent;
import com.hello.suripu.core.models.Events.SunSetEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;

/**
 * Created by pangwu on 5/8/14.
 */
public abstract class Event {

    public enum Type { // in order of display priority
        NONE(-1) {
            public String toString() {return "";}
        },
        MOTION(0),
        SLEEPING(1),
        SLEEP_MOTION(2),
        PARTNER_MOTION(3),
        NOISE(4),
        SNORING(5),
        SLEEP_TALK(6),
        LIGHT(7),
        LIGHTS_OUT(8),
        SUNSET(9),
        SUNRISE(10),
        IN_BED(11),
        SLEEP(12),
        OUT_OF_BED(13),
        WAKE_UP(14),
        ALARM(15),
        SLEEP_DISTURBANCE(16);

        private int value;

        private Type(int value) {
            this.value = value;
        }

        public int getValue(){
            return this.value;
        }

        public static Type fromInteger(int value){
            for(final Type type : Type.values()) {
                if(type.value == value) {
                    return type;
                }
            }
            return NONE;
        }

        @JsonCreator
        public static Type fromString(String value) {
            for(final Type type : Type.values()) {
                if(type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return NONE;
        }
    }

    @JsonProperty("type")
    private Type type;

    @JsonProperty("startTimestamp")
    private long startTimestamp;

    @JsonProperty("endTimestamp")
    private long endTimestamp;

    @JsonProperty("timezoneOffset")
    private int timezoneOffset;

    public Event(final Type type, final long startTimestamp, final long endTimestamp, final int timezoneOffset){
        setType(type);
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.timezoneOffset = timezoneOffset;
    }

    public final long getStartTimestamp(){
        return this.startTimestamp;
    }


    public final long getEndTimestamp(){
        return this.endTimestamp;
    }


    public final int getTimezoneOffset(){
        return this.timezoneOffset;
    }

    public final Type getType(){
        return type;
    }

    protected final void setType(final Type type){
        this.type = type;
    }

    public static Event extend(final Event event, final long startTimestamp, final long endTimestamp){
        switch (event.getType()){
            case MOTION:
                return new MotionEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getSleepDepth());
            case SLEEPING:
                return new SleepingEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getSleepDepth());
            case SLEEP_MOTION:
                return new SleepMotionEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getSleepDepth());
            case SLEEP:
                return new FallingAsleepEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getDescription());
            case IN_BED:
                return new InBedEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getDescription());
            case OUT_OF_BED:
                return new OutOfBedEvent(startTimestamp, endTimestamp, event.getTimezoneOffset());
            case WAKE_UP:
                return new WakeupEvent(startTimestamp, endTimestamp, event.getTimezoneOffset());
            case NONE:
                return new NullEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getSleepDepth());
            case SUNRISE:
                return new SunRiseEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getSleepDepth(), event.getSoundInfo());
            case SUNSET:
                return new SunSetEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getSleepDepth());
            case PARTNER_MOTION:
                return new PartnerMotionEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getSleepDepth());
            case LIGHT:
                return new LightEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getDescription());
            case LIGHTS_OUT:
                return new LightsOutEvent(startTimestamp, endTimestamp, event.getTimezoneOffset());
            case ALARM:
                return new AlarmEvent(startTimestamp, endTimestamp, event.getTimezoneOffset());
            case NOISE:
                return new NoiseEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getSleepDepth());
            case SLEEP_DISTURBANCE:
                return new SleepDisturbanceEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getSleepDepth());
            default:
                return new NullEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getSleepDepth());

        }
    }

    public static Event extend(final Event event, final long startTimestamp, final long endTimestamp, final int sleepDepth){
        switch (event.getType()){
            case MOTION:
                return new MotionEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), sleepDepth);
            case SLEEP_MOTION:
                return new SleepMotionEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), sleepDepth);
            case SLEEP:
                return new FallingAsleepEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getDescription());
            case WAKE_UP:
                return new WakeupEvent(startTimestamp, endTimestamp, event.getTimezoneOffset());
            case IN_BED:
                return new InBedEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getDescription());
            case OUT_OF_BED:
                return new OutOfBedEvent(startTimestamp, endTimestamp, event.getTimezoneOffset());
            case NONE:
                return new NullEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), sleepDepth);
            case SUNRISE:
                return new SunRiseEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), sleepDepth, event.getSoundInfo());
            case SUNSET:
                return new SunSetEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), sleepDepth);
            case PARTNER_MOTION:
                return new PartnerMotionEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), sleepDepth);
            case LIGHT:
                return new LightEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), event.getDescription());
            case LIGHTS_OUT:
                return new LightsOutEvent(startTimestamp, endTimestamp, event.getTimezoneOffset());
            case ALARM:
                return new AlarmEvent(startTimestamp, endTimestamp, event.getTimezoneOffset());
            case SLEEPING:
                return new SleepingEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), sleepDepth);
            case NOISE:
                return new NoiseEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), sleepDepth);
            case SLEEP_DISTURBANCE:
                return new SleepDisturbanceEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), sleepDepth);
            default:
                return new NullEvent(startTimestamp, endTimestamp, event.getTimezoneOffset(), sleepDepth);

        }
    }

    @JsonCreator
    public static Event createFromType(@JsonProperty("type") final Type type,
                                       @JsonProperty("startTimestamp") final long startTimestamp,
                                       @JsonProperty("endTimestamp") final long endTimestamp,
                                       @JsonProperty("timezoneOffset") final int offsetMillis,
                                       @JsonProperty("description") final String messageOptional,
                                       @JsonProperty("soundInfo") final SleepSegment.SoundInfo soundInfoOptional,
                                       @JsonProperty("sleepDepth") final Integer sleepDepth){
        return createFromType(type,
                startTimestamp,
                endTimestamp,
                offsetMillis,
                Optional.fromNullable(messageOptional),
                Optional.fromNullable(soundInfoOptional),
                Optional.fromNullable(sleepDepth));

    }


    public static Event createFromType(final Type type,
                                       final long startTimestamp,
                                       final long endTimestamp,
                                       final int offsetMillis,
                                       final Optional<String> messageOptional,
                                       final Optional<SleepSegment.SoundInfo> soundInfoOptional,
                                       final Optional<Integer> sleepDepth){
        switch (type){
            case MOTION:
                if(!sleepDepth.isPresent()){
                    throw new IllegalArgumentException("sleepDepth required.");
                }
                return new MotionEvent(startTimestamp, endTimestamp, offsetMillis, sleepDepth.get());
            case SLEEPING:
                if(!sleepDepth.isPresent()) {
                    throw new IllegalArgumentException("sleepDepth required");
                }
                return new SleepingEvent(startTimestamp, endTimestamp, offsetMillis, sleepDepth.get());
            case SLEEP_MOTION:
                if(!sleepDepth.isPresent()){
                    throw new IllegalArgumentException("sleepDepth required.");
                }
                return new SleepMotionEvent(startTimestamp, endTimestamp, offsetMillis, sleepDepth.get());
            case SLEEP:
                if(!messageOptional.isPresent()){
                    throw new IllegalArgumentException("message required.");
                }
                return new FallingAsleepEvent(startTimestamp, endTimestamp, offsetMillis, messageOptional.get());
            case WAKE_UP:
                if (messageOptional.isPresent()) {
                    return new WakeupEvent(startTimestamp, endTimestamp, offsetMillis,messageOptional.get());
                }
                else {
                    return new WakeupEvent(startTimestamp, endTimestamp, offsetMillis);
                }

            case IN_BED:
                if(!messageOptional.isPresent()){
                    throw new IllegalArgumentException("message required.");
                }
                return new InBedEvent(startTimestamp, endTimestamp, offsetMillis, messageOptional.get());
            case OUT_OF_BED:
                return new OutOfBedEvent(startTimestamp, endTimestamp, offsetMillis);
            case NONE:
                if(!sleepDepth.isPresent()){
                    throw new IllegalArgumentException("sleepDepth required.");
                }
                return new NullEvent(startTimestamp, endTimestamp, offsetMillis, sleepDepth.get());
            case SUNRISE:
                if(!sleepDepth.isPresent()){
                    throw new IllegalArgumentException("sleepDepth required.");
                }
                return new SunRiseEvent(startTimestamp, endTimestamp, offsetMillis, sleepDepth.get(),
                        soundInfoOptional.isPresent() ? soundInfoOptional.get(): null);
            case SUNSET:
                if(!sleepDepth.isPresent()){
                    throw new IllegalArgumentException("sleepDepth required.");
                }
                return new SunSetEvent(startTimestamp, endTimestamp, offsetMillis, sleepDepth.get());
            case PARTNER_MOTION:
                if(!sleepDepth.isPresent()){
                    throw new IllegalArgumentException("sleepDepth required.");
                }
                return new PartnerMotionEvent(startTimestamp, endTimestamp, offsetMillis, sleepDepth.get());
            case LIGHT:
                if(!messageOptional.isPresent()){
                    throw new IllegalArgumentException("message required.");
                }
                return new LightEvent(startTimestamp, endTimestamp, offsetMillis, messageOptional.get());
            case LIGHTS_OUT:
                return new LightsOutEvent(startTimestamp, endTimestamp, offsetMillis);
            case ALARM:
                if (!messageOptional.isPresent()) {
                    throw new IllegalArgumentException("message required.");
                }
                return new AlarmEvent(startTimestamp, endTimestamp, offsetMillis, messageOptional.get());
            case NOISE:
                return new NoiseEvent(startTimestamp, endTimestamp, offsetMillis, sleepDepth.get());
            case SLEEP_DISTURBANCE:
                if (!messageOptional.isPresent()) {
                    throw new IllegalArgumentException("message required.");
                }
                return new SleepDisturbanceEvent(startTimestamp, endTimestamp, offsetMillis, sleepDepth.get());

            default:
                if(!sleepDepth.isPresent()){
                    throw new IllegalArgumentException("sleepDepth required.");
                }
                return new NullEvent(startTimestamp, endTimestamp, offsetMillis, sleepDepth.get());

        }
    }

    @JsonProperty("description")
    public abstract String getDescription();

    @JsonProperty("soundInfo")
    public abstract SleepSegment.SoundInfo getSoundInfo();

    @JsonProperty("sleepDepth")
    public abstract int getSleepDepth();



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
