package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class SensorSample {

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
}
