package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class SensorSample {

    @JsonProperty("datetime")
    public final DateTime dateTime;

    @JsonProperty("value")
    public final float val;

    public SensorSample(final DateTime dateTime, final float val) {

        this.dateTime = dateTime;
        this.val = val;
    }
}
