package com.hello.suripu.core.models;

public enum Sensor {

    TEMPERATURE("temperature"),
    HUMIDITY("humidity"),
    PARTICULATES("particulates"),
    SOUND("sound"),
    LIGHT("light");


    private String value;

    private Sensor(String value) {
        this.value = value;
    }
}
