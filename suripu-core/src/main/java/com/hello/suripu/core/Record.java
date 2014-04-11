package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class Record {

    @JsonProperty("temperature")
    public final float ambientTemperature;

    @JsonProperty("humidity")
    public final float ambientHumidity;

    @JsonProperty("air_quality")
    public final float ambientAirQuality;

    @JsonProperty("light")
    public final float ambientLight;

    @JsonProperty("timestamp_utc")
    public final DateTime dateTime;

    @JsonProperty("offset_millis")
    public final Long offsetMillis;

    public Record(
            final float ambientTemperature,
            float ambientHumidity,
            final float ambientLight,
            float ambientAirQuality,
            final DateTime dateTime,
            final long offsetMillis) {
        this.ambientTemperature = ambientTemperature;
        this.ambientHumidity = ambientHumidity;
        this.ambientAirQuality = ambientAirQuality;
        this.ambientLight = ambientLight;
        this.dateTime = dateTime;
        this.offsetMillis = offsetMillis;
    }
}
