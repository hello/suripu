package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.util.List;


@JsonIgnoreProperties({
        "start_timestamp",
        "end_timestamp"
})
public class SleepSegment extends Event implements Comparable {

    public static class SoundInfo {

        public final String url;
        public final Integer durationMillis;

        public SoundInfo(final String url, final Integer durationMillis) {
            this.url = url;
            this.durationMillis = durationMillis;
        }
    }

    @JsonProperty("id")
    final public Long id;

    @JsonProperty("timestamp")
    public long getTimestamp(){
        return this.startTimestamp;
    }

    @JsonProperty("duration")
    public int getDurationInSeconds(){
        return (int)(this.endTimestamp - this.startTimestamp) / DateTimeConstants.MILLIS_PER_SECOND;
    }

    @JsonProperty("sleep_depth")
    final public Integer sleepDepth;

    @JsonProperty("sensors")
    final public List<SensorReading> sensors;

    @JsonProperty("sound")
    final public SoundInfo soundInfo;

    /**
     *
     * @param id
     * @param timestamp
     * @param durationInSeconds
     * @param sleepDepth
     * @param eventType
     * @param sensors
     */
    public SleepSegment(final Long id,
                        final Long timestamp, final Integer offsetMillis, final Integer durationInSeconds,
                        final Integer sleepDepth, final Event.Type eventType,
                        final List<SensorReading> sensors,
                        final SoundInfo soundInfo) {
        super(eventType, timestamp, timestamp + durationInSeconds * DateTimeConstants.MILLIS_PER_SECOND, offsetMillis);
        this.id = id;
        this.sleepDepth = sleepDepth;
        this.soundInfo = soundInfo;
        this.sensors = sensors;
    }


    public static SleepSegment withSleepDepth(final SleepSegment segment, final Integer sleepDepth) {
        return new SleepSegment(segment.id,
                segment.getTimestamp(), segment.timezoneOffset, segment.getDurationInSeconds(),
                sleepDepth,
                segment.type, segment.sensors, segment.soundInfo);
    }

    public static SleepSegment withSleepDepthAndDuration(final SleepSegment segment, final Integer sleepDepth, final Integer durationInSeconds) {
        return new SleepSegment(segment.id,
                segment.getTimestamp(), segment.timezoneOffset, durationInSeconds,
                sleepDepth,
                segment.type, segment.sensors, segment.soundInfo);
    }

    public static SleepSegment withEventType(final SleepSegment segment, final Event.Type eventType) {
        return new SleepSegment(
                segment.id,
                segment.getTimestamp(), segment.timezoneOffset, segment.getDurationInSeconds(),
                segment.sleepDepth,
                eventType,
                segment.sensors, segment.soundInfo);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(SleepSegment.class)
                .add("id", id)
                .add("timestamp", this.getTimestamp())
                .add("offsetMillis", this.timezoneOffset)
                .add("durationInSeconds", this.getDurationInSeconds())
                .add("sleepDepth", this.sleepDepth)
                .add("eventType", this.type)
                .add("message", this.getMessage())
                .add("sensors", this.sensors)
                .add("soundInfo", this.soundInfo)
                .add("from", new DateTime(this.startTimestamp, DateTimeZone.forOffsetMillis(this.timezoneOffset)))
                .add("to", new DateTime(this.endTimestamp, DateTimeZone.forOffsetMillis(this.timezoneOffset)))
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
