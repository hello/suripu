package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

import java.util.List;

public class SleepSegment {

    @JsonProperty("id")
    final public Long id;

    @JsonProperty("timestamp")
    final public Long timestamp;

    @JsonProperty("duration")
    final public Integer durationInSeconds;

    @JsonProperty("sleep_depth")
    final public Integer sleepDepth;

    @JsonProperty("event_type")
    final public String eventType;

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
                        final Integer sleepDepth, final String eventType, final String message, final List<SensorReading> sensors) {
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
}
