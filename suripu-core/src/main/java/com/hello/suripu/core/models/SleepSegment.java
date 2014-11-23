package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.hello.suripu.core.util.EventUtil;
import com.hello.suripu.core.util.TimelineUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.List;

public class SleepSegment implements Comparable {

    public static class SoundInfo {

        public final String url;
        public final Integer durationMillis;

        public SoundInfo(final String url, final Integer durationMillis) {
            this.url = url;
            this.durationMillis = durationMillis;
        }

        public static SoundInfo empty(){
            return new SoundInfo("", 0);
        }

        public boolean isEmpty(){
            return this.url.equals("") && this.durationMillis == 0;
        }
    }

    @JsonProperty
    public final Event event;

    @JsonProperty("id")
    final public Long id;

    @JsonProperty("sleep_depth")
    final public Integer sleepDepth;

    @JsonProperty("sensors")
    final public List<SensorReading> sensors;

    @JsonProperty("sound")
    final public SoundInfo soundInfo;


    @JsonProperty("timestamp")
    public long getTimestamp(){
        return this.event.startTimestamp;
    }

    @JsonProperty("duration")
    public int getDurationInSeconds(){
        return (int)(this.event.endTimestamp - this.event.startTimestamp) / DateTimeConstants.MILLIS_PER_SECOND;
    }

    @JsonProperty("offset_millis")
    public int getOffsetMillis(){
        return this.event.timezoneOffset;
    }

    @JsonProperty("event_type")
    public Event.Type getType(){
        return event.getType();
    }

    @JsonProperty("message")
    public String getMessage(){
        return this.event.getDescription();
    }

    @JsonProperty("message")
    public void setMessage(final String message){
        this.event.setDescription(message);
    }

    public Event getEvent(){
        return this.event;
    }

    public static SleepSegment fromMotionEvent(final MotionEvent event) {
        return new SleepSegment(event.startTimestamp,
                event.startTimestamp,
                event.timezoneOffset,
                EventUtil.getEventDurationInSecond(event),
                Event.Type.MOTION,  // enforce the type, so user input cannot cheat
                TimelineUtils.normalizeSleepDepth(event.amplitude, event.maxAmplitude),
                Collections.EMPTY_LIST,
                SoundInfo.empty());
    }

    public static SleepSegment fromSleepEvent(final Event event) {
        if(event.getType() != Event.Type.SLEEP){
            throw new IllegalArgumentException("event is not a sleep event");

        }

        return new SleepSegment(event.startTimestamp,
                event.startTimestamp,
                event.timezoneOffset,
                EventUtil.getEventDurationInSecond(event),
                event.getType(),
                100,
                Collections.EMPTY_LIST,
                SoundInfo.empty());
    }

    public static SleepSegment fromWakeUpEvent(final Event event) {
        if(event.getType() != Event.Type.WAKE_UP){
            throw new IllegalArgumentException("event is not a wake up event");

        }

        return new SleepSegment(event.startTimestamp,
                event.startTimestamp,
                event.timezoneOffset,
                EventUtil.getEventDurationInSecond(event),
                event.getType(),
                0,
                Collections.EMPTY_LIST,
                SoundInfo.empty());
    }

    public static SleepSegment fromSunRiseEvent(final SunRiseEvent event, final SoundInfo soundInfo) {
        return new SleepSegment(event.startTimestamp,
                event.startTimestamp,
                event.timezoneOffset,
                EventUtil.getEventDurationInSecond(event),
                Event.Type.SUNRISE,
                100,
                Collections.EMPTY_LIST,
                soundInfo);
    }

    public static SleepSegment fromNoneEvent(final Event event) {
        if(event.getType() != Event.Type.NONE){
            throw new IllegalArgumentException("event is not a default event");
        }
        return new SleepSegment(event.startTimestamp,
                event.startTimestamp,
                event.timezoneOffset,
                EventUtil.getEventDurationInSecond(event),
                Event.Type.NONE,
                100,
                Collections.EMPTY_LIST,
                SoundInfo.empty());
    }



    /*
    * This should be the only constructor, to construct different types of sleep object.
    * add fromXXXXEvent method.
     */
    private SleepSegment(final Long id,
                        final Long timestamp, final Integer offsetMillis, final Integer durationInSeconds,
                        final Event.Type eventType,
                        final Integer sleepDepth,
                        final List<SensorReading> sensors,
                        final SoundInfo soundInfo) {
        this.event = new Event(eventType, timestamp, timestamp + durationInSeconds * DateTimeConstants.MILLIS_PER_SECOND, offsetMillis);
        this.id = id;
        this.sleepDepth = sleepDepth;
        this.soundInfo = soundInfo;
        this.sensors = sensors;
    }


    @Override
    public String toString() {
        return Objects.toStringHelper(SleepSegment.class)
                .add("id", this.id)
                .add("timestamp", this.getTimestamp())
                .add("offsetMillis", this.getOffsetMillis())
                .add("durationInSeconds", this.getDurationInSeconds())
                .add("sleepDepth", this.sleepDepth)
                .add("eventType", this.getType())
                .add("message", this.getMessage())
                .add("sensors", this.sensors)
                .add("soundInfo", this.soundInfo)
                .add("from", new DateTime(this.getTimestamp(), DateTimeZone.forOffsetMillis(this.getOffsetMillis())))
                .add("to", new DateTime(this.getTimestamp() + this.getDurationInSeconds() * DateTimeConstants.MILLIS_PER_SECOND,
                        DateTimeZone.forOffsetMillis(this.getOffsetMillis())))
                .add("$minutes", this.getDurationInSeconds() / DateTimeConstants.SECONDS_PER_MINUTE)
                .toString();
    }

    @Override
    public int compareTo(final Object o) {
        final SleepSegment that = (SleepSegment) o;
        return ComparisonChain.start()
                .compare(this.getTimestamp(), that.getTimestamp())
                .result();
    }
}
