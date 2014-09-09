package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class SleepSegment {

    @JsonProperty("id")
    final public Long id;

    @JsonProperty("timestamp")
    final public Long timestamp;

    @JsonProperty("duration")
    final public Integer duration;

    @JsonProperty("sleep_depth")
    final public Integer sleepDepth;

    @JsonProperty("event_type")
    final public String eventType;

    @JsonProperty("message")
    final public String message;

    @JsonProperty("sensors")
    final public List<SensorSample> sensors;

    @JsonProperty("offset_millis")
    final public Integer offsetMillis;


    /**
     *
     * @param id
     * @param timestamp
     * @param duration
     * @param sleepDepth
     * @param eventType
     * @param message
     * @param sensors
     */
    public SleepSegment(final Long id, final Long timestamp, final Integer offsetMillis, final Integer duration,
                        final Integer sleepDepth, final String eventType, final String message, final List<SensorSample> sensors) {
        this.id = id;
        this.timestamp = timestamp;
        this.offsetMillis = offsetMillis;
        this.duration = duration;
        this.sleepDepth = sleepDepth;
        this.eventType = eventType;
        this.message = message;
        this.sensors = new ArrayList<>();
    }
}
