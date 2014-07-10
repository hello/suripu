package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

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
}
