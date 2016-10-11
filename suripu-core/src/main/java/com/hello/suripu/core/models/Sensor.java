package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Sensor {

    TEMPERATURE("temperature"),
    HUMIDITY("humidity"),
    PARTICULATES("particulates"),
    SOUND("sound"),
    LIGHT("light"),
    WAVE_COUNT("wave_count"),
    HOLD_COUNT("hold_count"),
    SOUND_NUM_DISTURBANCES("num_disturbances"),
    SOUND_PEAK_DISTURBANCE("peak_disturbance"),
    SOUND_PEAK_ENERGY("peak_energy"),
    PRESSURE("pressure"),
    CO2("co2"),
    TVOC("tvoc"),
    RGB("rgb"),
    IR("ir"),
    CLEAR("clear"),
    LUX("lux"),
    UV("uv");


    private String value;

    private Sensor(String value) {
        this.value = value;
    }

    public static Sensor fromString(String sensorName) {
        for(Sensor sensor: Sensor.values()) {
            if(sensor.toString().equalsIgnoreCase(sensorName)) {
                return sensor;
            }
        }
        throw new IllegalArgumentException(String.format("Invalid sensor name: %s", sensorName));
    }

    @JsonValue
    public String toString() {
        return value;
    }
}
