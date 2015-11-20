package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.common.base.Objects;

public class Sample {

    @JsonProperty("datetime")
    public final long dateTime;

    @JsonProperty("value")
    public final float value;

    @JsonProperty("offset_millis")
    public Integer offsetMillis; // Warning mutable value

    public Sample(final long dateTime, final float value, final Integer offsetMillis) {
        this.dateTime = dateTime;
        this.value = value;
        this.offsetMillis = offsetMillis;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) return false;
        if (getClass() != object.getClass()) return false;
        final Sample other = (Sample) object;
        return Objects.equal(dateTime, other.dateTime)
                && Objects.equal(value, other.value)
                && Objects.equal(offsetMillis, other.offsetMillis);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(Sample.class)
                .add("datetime", dateTime)
                .add("value", value)
                .add("offset", offsetMillis).toString();

    }
}
