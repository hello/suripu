package com.hello.suripu.core.models;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllSensorSampleList {

    private final List<Sample> light = Lists.newArrayList();
    private final List<Sample> sound = Lists.newArrayList();
    private final List<Sample> humidity = Lists.newArrayList();
    private final List<Sample> temperature = Lists.newArrayList();
    private final List<Sample> particulates = Lists.newArrayList();
    private final List<Sample> waveCounts = Lists.newArrayList();

    private final List<Sensor> sensors;

    public AllSensorSampleList() {

        sensors = Lists.newArrayList(
                Sensor.LIGHT,
                Sensor.HUMIDITY,
                Sensor.PARTICULATES,
                Sensor.SOUND,
                Sensor.TEMPERATURE,
                Sensor.WAVE_COUNT);
    }

    public void add(final Sensor sensor, final List<Sample> values) {
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
            case WAVE_COUNT:
                this.waveCounts.addAll(values);
                break;
            default:
                break;
        }

    }

    public List<Sensor> getAvailableSensors() {
        return this.sensors;
    }

    public List<Sample> get(final Sensor sensor) {
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
            case WAVE_COUNT:
                return this.waveCounts;
            default:
                return Collections.EMPTY_LIST;
        }
    }

    public Map<Sensor, List<Sample>> getAllData() {
        final Map<Sensor, List<Sample>> results = new HashMap<>();
        results.put(Sensor.LIGHT, this.light);
        results.put(Sensor.HUMIDITY, this.humidity);
        results.put(Sensor.SOUND, this.sound);
        results.put(Sensor.TEMPERATURE, this.temperature);
        results.put(Sensor.PARTICULATES, this.particulates);
        results.put(Sensor.WAVE_COUNT, this.waveCounts);
        return results;
    }


    public Boolean isEmpty() {
        return light.isEmpty() && humidity.isEmpty() && sound.isEmpty() && temperature.isEmpty()
                && particulates.isEmpty() && waveCounts.isEmpty();
    }
    public static Map<Sensor, List<Sample>> getEmptyData() {
        final Map<Sensor, List<Sample>> results = new HashMap<>();
        results.put(Sensor.LIGHT, Collections.EMPTY_LIST);
        results.put(Sensor.HUMIDITY, Collections.EMPTY_LIST);
        results.put(Sensor.SOUND, Collections.EMPTY_LIST);
        results.put(Sensor.TEMPERATURE, Collections.EMPTY_LIST);
        results.put(Sensor.PARTICULATES, Collections.EMPTY_LIST);
        return results;

    }

}
