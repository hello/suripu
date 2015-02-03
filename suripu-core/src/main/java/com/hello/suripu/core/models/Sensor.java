package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Sensor {

    TEMPERATURE("temperature"),
    HUMIDITY("humidity"),
    PARTICULATES("particulates"),
    SOUND("sound"),
    LIGHT("light"),
    WAVE_COUNT("wave_count"),
    HOLD_COUNT("hold_count");


    private String value;

    private Sensor(String value) {
        this.value = value;
    }


    @JsonValue
    public String toString() {
        return value;
    }
}
