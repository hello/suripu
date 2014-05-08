package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class Record {

    @JsonProperty("ambient_temperature")
    public final float ambientTemperature;

    @JsonProperty("ambient_humidity")
    public final float ambientHumidity;

    @JsonProperty("ambient_air_quality")
    public final float ambientAirQuality;

    @JsonProperty("ambient_light")
    public final float ambientLight;

    @JsonProperty("timestamp_utc")
    public final DateTime dateTime;

    @JsonProperty("offset_millis")
    public final int offsetMillis;

    public Record(
            final float ambientTemperature,
            float ambientHumidity,
            float ambientAirQuality,
            final float ambientLight,
            final DateTime dateTime,
            final int offsetMillis) {
        this.ambientTemperature = ambientTemperature;
        this.ambientHumidity = ambientHumidity;
        this.ambientAirQuality = ambientAirQuality;
        this.ambientLight = ambientLight;
        this.dateTime = dateTime;
        this.offsetMillis = offsetMillis;
    }
}
