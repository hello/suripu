package com.hello.suripu.core.models;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllSensorSampleList {

    private final List<Sample> light;
    private final List<Sample> sound;
    private final List<Sample> humidity;
    private final List<Sample> temperature;
    private final List<Sample> particulates;

    private final List<Sensor> sensors;

    public AllSensorSampleList() {
        this.light = new ArrayList<>();
        this.sound = new ArrayList<>();
        this.humidity = new ArrayList<>();
        this.temperature = new ArrayList<>();
        this.particulates = new ArrayList<>();
        sensors = Lists.newArrayList(
                Sensor.LIGHT,
                Sensor.HUMIDITY,
                Sensor.PARTICULATES,
                Sensor.SOUND,
                Sensor.TEMPERATURE);

    }

    public void setData(final Sensor sensor, final List<Sample> values) {
        switch (sensor) {
            case LIGHT:
                this.light.addAll(values);
                break;
            case SOUND:
                this.sound.addAll(values);
                break;
            case HUMIDITY:
                this.humidity.addAll(values);
                break;
            case TEMPERATURE:
                this.temperature.addAll(values);
                break;
            case PARTICULATES:
                this.particulates.addAll(values);
                break;
            default:
                break;
        }

    }

    public List<Sensor> getAvailableSensors() {
        return this.sensors;
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
