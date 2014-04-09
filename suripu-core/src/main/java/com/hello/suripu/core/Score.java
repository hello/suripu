package com.hello.suripu.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class Score {

    @JsonProperty("temperature")
    public final int temperature;

    @JsonProperty("humidity")
    public final int humidity;

    @JsonProperty("sound")
    public final int sound;

    @JsonProperty("air_quality")
    public final int airQuality;

    @JsonProperty("light")
    public final int light;

    @JsonProperty("date")
    public final DateTime dateTime;

    public Score(
            final int temperature,
            final int humidity,
            final int sound,
            final int airQuality,
            final int light,
            final DateTime dateTime) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.sound = sound;
        this.airQuality = airQuality;
        this.light = light;
        this.dateTime = dateTime;
    }
}
