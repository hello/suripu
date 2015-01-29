package com.hello.suripu.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllSensorSampleList {

    private final List<Sample> light;
    private final List<Sample> sound;
    private final List<Sample> humidity;
    private final List<Sample>temperature;
    private final List<Sample> particulates;

    public AllSensorSampleList() {
        this.light = new ArrayList<>();
        this.sound = new ArrayList<>();
        this.humidity = new ArrayList<>();
        this.temperature = new ArrayList<>();
        this.particulates = new ArrayList<>();
    }

    public void setData(final Sensor sensor, final List<Sample> values) {
        switch (sensor) {
            case LIGHT:
                this.light.addAll(values);
            case SOUND:
                this.sound.addAll(values);
            case HUMIDITY:
                this.humidity.addAll(values);
            case TEMPERATURE:
                this.temperature.addAll(values);
            case PARTICULATES:
                this.particulates.addAll(values);
            default:
                break;
        }

    }

    public List<Sensor> getAvailableSensors() {
        final List<Sensor> sensors = new ArrayList<>();
        sensors.add(Sensor.LIGHT);
        sensors.add(Sensor.HUMIDITY);
        sensors.add(Sensor.PARTICULATES);
        sensors.add(Sensor.SOUND);
        sensors.add(Sensor.TEMPERATURE);
        return sensors;
    }

    public void setLight(final List<Sample> values) {
        this.light.addAll(values);
    }

    public void setSound(final List<Sample> values) {
        this.sound.addAll(values);
    }

    public void setHumidity(final List<Sample> values) {
        this.humidity.addAll(values);
    }

    public void setTemperature(final List<Sample> values) {
        this.temperature.addAll(values);
    }

    public void setParticulates(final List<Sample> values) {
        this.particulates.addAll(values);
    }

    public List<Sample> getData(final Sensor sensor) {
        switch (sensor) {
            case LIGHT:
                    return this.light;
            case SOUND:
                    return this.sound;
            case HUMIDITY:
                    return this.humidity;
            case TEMPERATURE:
                    return this.temperature;
            case PARTICULATES:
                    return this.particulates;
            default:
                return Collections.EMPTY_LIST;
        }
    }

    public Map<Sensor, List<Sample>> getData() {
        final Map<Sensor, List<Sample>> results = new HashMap<>();
        results.put(Sensor.LIGHT, this.light);
        results.put(Sensor.HUMIDITY, this.humidity);
        results.put(Sensor.SOUND, this.sound);
        results.put(Sensor.TEMPERATURE, this.temperature);
        results.put(Sensor.PARTICULATES, this.particulates);
        return results;
    }
}
