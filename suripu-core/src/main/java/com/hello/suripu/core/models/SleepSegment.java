package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.hello.suripu.core.util.EventTypeSerializer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

public class SleepSegment implements Comparable {

    @JsonProperty("id")
    final public Long id;

    @JsonProperty("timestamp")
    final public Long timestamp;

    @JsonProperty("duration")
    final public Integer durationInSeconds;

    @JsonProperty("sleep_depth")
    final public Integer sleepDepth;

    @JsonProperty("event_type")
    @JsonSerialize(using = EventTypeSerializer.class)
    final public Event.Type eventType;

    @JsonProperty("message")
    final public String message;

    @JsonProperty("sensors")
    final public List<SensorReading> sensors;

    @JsonProperty("offset_millis")
    final public Integer offsetMillis;


    /**
     *
     * @param id
     * @param timestamp
     * @param durationInSeconds
     * @param sleepDepth
     * @param eventType
     * @param message
     * @param sensors
     */
    public SleepSegment(final Long id, final Long timestamp, final Integer offsetMillis, final Integer durationInSeconds,
                        final Integer sleepDepth, final Event.Type eventType, final String message, final List<SensorReading> sensors) {
        this.id = id;
        this.timestamp = timestamp;
        this.offsetMillis = offsetMillis;
        this.durationInSeconds = durationInSeconds;
        this.sleepDepth = sleepDepth;
        this.eventType = eventType;
        this.message = message;
        this.sensors = sensors;
    }


    public static SleepSegment withSleepDepth(final SleepSegment segment, final Integer sleepDepth) {
        return new SleepSegment(segment.id, segment.timestamp, segment.offsetMillis, segment.durationInSeconds, sleepDepth,
                segment.eventType, segment.message, segment.sensors);
    }

    public static SleepSegment withSleepDepthAndDuration(final SleepSegment segment, final Integer sleepDepth, final Integer durationInSeconds) {
        return new SleepSegment(segment.id, segment.timestamp, segment.offsetMillis, durationInSeconds, sleepDepth,
                segment.eventType, segment.message, segment.sensors);
    }

    public static SleepSegment withEventType(final SleepSegment segment, final Event.Type eventType) {
        return new SleepSegment(
                segment.id, segment.timestamp,
                segment.offsetMillis, segment.durationInSeconds, segment.sleepDepth,
                eventType,
                Event.getMessage(eventType, new DateTime(segment.timestamp, DateTimeZone.UTC).plusMillis(segment.offsetMillis)),
                segment.sensors);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(SleepSegment.class)
                .add("id", id)
                .add("timestamp", timestamp)
                .add("offsetMillis", offsetMillis)
                .add("durationInSeconds", durationInSeconds)
                .add("sleepDepth", sleepDepth)
                .add("eventType", eventType)
                .add("message", message)
                .add("sensors", sensors)
                .add("$when", new DateTime(timestamp))
                .add("$minutes", durationInSeconds/60)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        final SleepSegment that = (SleepSegment) o;
        return ComparisonChain.start()
                .compare(timestamp, that.timestamp)
                .result();
    }
}
