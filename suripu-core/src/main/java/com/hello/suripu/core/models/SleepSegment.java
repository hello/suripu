package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.util.List;

public class SleepSegment implements Comparable {

    public static class SoundInfo {

        public final String url;
        public final Integer durationMillis;

        public SoundInfo(final String url, final Integer durationMillis) {
            this.url = url;
            this.durationMillis = durationMillis;
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




    public SleepSegment(final Long id,
                        final Event event,
                        final Integer sleepDepth,
                        final List<SensorReading> sensors,
                        final SoundInfo soundInfo) {
        this.event = event;
        this.id = id;
        this.sleepDepth = sleepDepth;
        this.soundInfo = soundInfo;
        this.sensors = sensors;
    }

    public SleepSegment(final Long id,
                        final Long timestamp, final Integer offsetMillis, final Integer durationInSeconds,
                        final Integer sleepDepth, final Event.Type eventType,
                        final List<SensorReading> sensors,
                        final SoundInfo soundInfo) {
        this.event = new Event(eventType, timestamp, timestamp + durationInSeconds * DateTimeConstants.MILLIS_PER_SECOND, offsetMillis);
        this.id = id;
        this.sleepDepth = sleepDepth;
        this.soundInfo = soundInfo;
        this.sensors = sensors;
    }

    public static SleepSegment withSleepDepth(final SleepSegment segment, final Integer sleepDepth) {
        return new SleepSegment(segment.id,
                segment.event,
                sleepDepth,
                segment.sensors, segment.soundInfo);
    }

    public static SleepSegment withSleepDepthAndDuration(final SleepSegment segment, final Integer sleepDepth, final Integer durationInSeconds) {
        return new SleepSegment(segment.id,
                segment.event,
                sleepDepth,
                segment.sensors, segment.soundInfo);
    }

    public static SleepSegment withEventType(final SleepSegment segment, final Event.Type eventType) {
        return new SleepSegment(
                segment.id,
                segment.event,
                segment.sleepDepth,
                segment.sensors, segment.soundInfo);
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
