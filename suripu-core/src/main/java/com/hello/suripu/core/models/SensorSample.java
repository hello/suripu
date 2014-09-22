package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

public class SensorSample implements Comparable {

    @JsonProperty("id")
    public String id;

    @JsonProperty("datetime_utc")
    public final DateTime dateTime;

    @JsonProperty("value")
    public final float val;

    @JsonProperty("timezone_offset")
    public final int timeZoneOffset;

    public SensorSample(final DateTime dateTime, final float val, final int timeZoneOffset) {
        this.dateTime = dateTime;
        this.val = val;
        this.timeZoneOffset = timeZoneOffset;
        this.id = null;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getID() {
        return this.id;
    }

    @Override
    public int compareTo(Object o) {
        SensorSample sample = (SensorSample) o;
        return this.dateTime.compareTo(sample.dateTime);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(SensorSample.class)
                .add("id", id)
                .add("date", dateTime)
                .add("tz_offset", timeZoneOffset)
                .add("value", val)
                .toString();
    }
}
