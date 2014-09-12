package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import java.util.Comparator;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SensorSample implements Comparable {

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
    }

    @Override
    public int compareTo(Object o) {
        SensorSample sample = (SensorSample) o;
        return this.dateTime.compareTo(sample.dateTime);
    }
}
