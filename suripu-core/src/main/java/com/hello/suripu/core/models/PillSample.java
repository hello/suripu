package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

public class PillSample implements Comparable {

    @JsonProperty("id")
    public final String sampleID;

    @JsonProperty("datetime_utc")
    public final DateTime dateTime;

    @JsonProperty("value")
    public final float val;

    @JsonProperty("timezone_offset")
    public final int timeZoneOffset;

    public PillSample(String id, final DateTime dateTime, final float val, final int timeZoneOffset) {
        this.dateTime = dateTime;
        this.val = val;
        this.timeZoneOffset = timeZoneOffset;
        this.sampleID = id;
    }

    @Override
    public int compareTo(Object o) {
        PillSample sample = (PillSample) o;
        return this.dateTime.compareTo(sample.dateTime);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(PillSample.class)
                .add("id", sampleID)
                .add("date", dateTime)
                .add("tz_offset", timeZoneOffset)
                .add("value", val)
                .toString();
    }
}
