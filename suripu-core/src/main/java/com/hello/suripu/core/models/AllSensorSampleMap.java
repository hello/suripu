package com.hello.suripu.core.models;

import com.google.common.collect.Maps;

import java.util.Map;

public class AllSensorSampleMap {

    private final Map<Sensor, Map<Long, Sample>> sensorMap;
    
    public AllSensorSampleMap() {
        sensorMap = Maps.newHashMap();
        for (final Sensor sensor: Sensor.values()) {
            sensorMap.put(sensor, Maps.<Long, Sample>newHashMap());
        }
    }

    private void put(final Sensor sensor, final Long dateTime, final int offsetMillis, final int value) {
        sensorMap.get(sensor).put(dateTime, new Sample(dateTime, value, offsetMillis));
    }

    private void put(final Sensor sensor, final Long dateTime, final int offsetMillis, final float value) {
        sensorMap.get(sensor).put(dateTime, new Sample(dateTime, value, offsetMillis));
    }

    public void addSample(final Long dateTime, final int offsetMillis,
                          final float light,
                          final float sound,
                          final float humidity,
                          final float temperature,
                          final float particulates,
                          final int waveCount,
                          final int holdCount,
                          final float soundNumDisturbance,
                          final float soundPeakDisturbance,
                          final float soundPeakEnergy) {

        put(Sensor.LIGHT, dateTime, offsetMillis, light);
        put(Sensor.SOUND, dateTime, offsetMillis, sound);
        put(Sensor.HUMIDITY, dateTime, offsetMillis, humidity);
        put(Sensor.TEMPERATURE, dateTime, offsetMillis, temperature);
        put(Sensor.PARTICULATES, dateTime, offsetMillis, particulates);
        put(Sensor.WAVE_COUNT, dateTime, offsetMillis, waveCount);
        put(Sensor.HOLD_COUNT, dateTime, offsetMillis, holdCount);
        put(Sensor.SOUND_NUM_DISTURBANCES, dateTime, offsetMillis, soundNumDisturbance);
        put(Sensor.SOUND_PEAK_DISTURBANCE, dateTime, offsetMillis, soundPeakDisturbance);
        put(Sensor.SOUND_PEAK_ENERGY, dateTime, offsetMillis, soundPeakEnergy);
    }

    public void addExtraSample(final Long dateTime, final int offsetMillis,
                          final float pressure,
                          final float tvoc,
                          final float co2,
                          final float ir,
                          final float clear,
                          final int uvCount) {

        put(Sensor.PRESSURE, dateTime, offsetMillis, pressure);
        put(Sensor.TVOC, dateTime, offsetMillis, tvoc);
        put(Sensor.CO2, dateTime, offsetMillis, co2);
        put(Sensor.IR, dateTime, offsetMillis, ir);
        put(Sensor.CLEAR, dateTime, offsetMillis, clear);
        put(Sensor.UV, dateTime, offsetMillis, uvCount);
    }


    public void setSampleMap(final Sensor sensor, final Map<Long, Sample> sampleMap) {
        if (this.sensorMap.containsKey(sensor)) {
            this.sensorMap.get(sensor).putAll(sampleMap);
        }
    }

    public Map<Long, Sample> get(final Sensor sensor) {
        if (sensorMap.containsKey(sensor)) {
            return sensorMap.get(sensor);
        }

        return Maps.newHashMap();
    }

    public Boolean isEmpty() {
        for (final Map<Long, Sample> samples : sensorMap.values()) {
            if (!samples.isEmpty()) {
                return false;
            }
        }
        return true;
    }

}
