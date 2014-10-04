package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.List;

public class SensorReading {

    @JsonProperty("name")
    public final String name;

    @JsonProperty("value")
    public final Integer value;

    @JsonProperty("unit")
    public final CurrentRoomState.State.Unit unit;


    public SensorReading(final String name, final Integer value, final CurrentRoomState.State.Unit unit) {
        this.name = name;
        this.value = value;
        this.unit = unit;
    }

    public static List<SensorReading> fromDeviceData(final DeviceData deviceData) {
        final List<SensorReading> readings = new ArrayList<>();
        readings.add(new SensorReading("temperature", Math.round(DeviceData.dbIntToFloat(deviceData.ambientTemperature)), CurrentRoomState.State.Unit.CELCIUS));
        readings.add(new SensorReading("particulates", Math.round(DeviceData.dbIntToFloat(deviceData.ambientAirQuality)), CurrentRoomState.State.Unit.PPM));
        readings.add(new SensorReading("humidity", Math.round(DeviceData.dbIntToFloat(deviceData.ambientHumidity)), CurrentRoomState.State.Unit.PERCENT));

        return readings;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(SensorReading.class)
                .add("name", name)
                .add("value", value)
                .add("unit", unit)
                .toString();
    }
}
