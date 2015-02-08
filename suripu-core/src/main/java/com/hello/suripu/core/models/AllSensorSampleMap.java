package com.hello.suripu.core.models;

import com.google.common.collect.Maps;

import java.util.Map;

public class AllSensorSampleMap {

    private final Map<Long, Sample> light = Maps.newHashMap();
    private final Map<Long, Sample> sound = Maps.newHashMap();
    private final Map<Long, Sample> humidity = Maps.newHashMap();
    private final Map<Long, Sample> temperature = Maps.newHashMap();
    private final Map<Long, Sample> particulates = Maps.newHashMap();
    private final Map<Long, Sample> waveCounts = Maps.newHashMap();

    public AllSensorSampleMap() {
    }

    public void addSample(final Long dateTime, final int offsetMillis,
                          final float light,
                          final float sound,
                          final float humidity,
                          final float temperature,
                          final float particulates,
                          final int waveCount) {

        this.light.put(dateTime, new Sample(dateTime, light, offsetMillis));
        this.sound.put(dateTime, new Sample(dateTime, sound, offsetMillis));
        this.humidity.put(dateTime, new Sample(dateTime, humidity, offsetMillis));
        this.temperature.put(dateTime, new Sample(dateTime, temperature, offsetMillis));
        this.particulates.put(dateTime, new Sample(dateTime, particulates, offsetMillis));
        this.waveCounts.put(dateTime, new Sample(dateTime, waveCount, offsetMillis));
    }

    public void setSampleMap(final Sensor sensor, final Map<Long, Sample> sampleMap) {
        switch (sensor) {
            case LIGHT:
                this.light.putAll(sampleMap);
                break;
            case SOUND:
                this.sound.putAll(sampleMap);
                break;
            case HUMIDITY:
                this.humidity.putAll(sampleMap);
                break;
            case TEMPERATURE:
                this.temperature.putAll(sampleMap);
                break;
            case PARTICULATES:
                this.particulates.putAll(sampleMap);
                break;
            case WAVE_COUNT:
                this.waveCounts.putAll(sampleMap);
                break;
            default:
                break;
        }
    }

    public Map<Long, Sample> get(final Sensor sensor) {
        switch (sensor) {
            case LIGHT:
                return light;
            case SOUND:
                return sound;
            case HUMIDITY:
                return humidity;
            case TEMPERATURE:
                return temperature;
            case PARTICULATES:
                return particulates;
            case WAVE_COUNT:
                return waveCounts;
        }
        return Maps.newHashMap();
    }

    public Boolean isEmpty() {
        return light.isEmpty() && sound.isEmpty() && humidity.isEmpty() && temperature.isEmpty()
                && particulates.isEmpty() && waveCounts.isEmpty();
    }

}
