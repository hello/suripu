package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class Record {

    @JsonProperty("temp")
    public final float ambientTemperature;

    @JsonProperty("humidity")
    public final float ambientHumidity;

    @JsonProperty("air_quality")
    public final float ambientAirQuality;

    @JsonProperty("ts")
    public final DateTime dateTime;

    public Record(final float ambientTemperature, float ambientHumidity, float ambientAirQuality, final DateTime dateTime) {
        this.ambientTemperature = ambientTemperature;
        this.ambientHumidity = ambientHumidity;
        this.ambientAirQuality = ambientAirQuality;
        this.dateTime = dateTime;
    }
}
