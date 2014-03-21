package com.hello.suripu.core;

import org.joda.time.DateTime;

public class Record {

    public final float ambientTemperature;
    public final float ambientHumidity;
    public final float ambientAirQuality;
    public final DateTime dateTime;

    public Record(final float ambientTemperature, float ambientHumidity, float ambientAirQuality, final DateTime dateTime) {
        this.ambientTemperature = ambientTemperature;
        this.ambientHumidity = ambientHumidity;
        this.ambientAirQuality = ambientAirQuality;
        this.dateTime = dateTime;
    }
}
