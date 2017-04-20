package com.hello.suripu.core.models;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllSensorSampleList {

    private final Map<Sensor, List<Sample>> sensorMap;

    public AllSensorSampleList() {

        sensorMap = Maps.newHashMap();
        for (final Sensor sensor: Sensor.values()) {
            sensorMap.put(sensor, Lists.<Sample>newArrayList());
        }

    }

    public void add(final Sensor sensor, final List<Sample> values) {
        sensorMap.get(sensor).addAll(values);
    }

    public void update(final Sensor sensor, final List<Sample> values) {
        final List<Sample> samples = sensorMap.get(sensor);
        samples.clear();
        samples.addAll(values);
    }

    public List<Sensor> getAvailableSensors() {
        return Arrays.asList(Sensor.values());
    }

    public List<Sample> get(final Sensor sensor) {
        return sensorMap.get(sensor);
    }

    public Map<Sensor, List<Sample>> getAllData() {
        return sensorMap;
    }


    public Boolean isEmpty() {
        for (final List<Sample> samples : sensorMap.values()) {
            if (!samples.isEmpty()) {
                return false;
            }
        }
        return true;
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

    public AllSensorSampleList getSensorDataForTimeWindow(final long startTime, final long endTime){
        final List<Sensor> sensorList = getAvailableSensors();
        final AllSensorSampleList currentPeriodSensorSampleList = new AllSensorSampleList();

        for (final Sensor sensor : sensorList){
            final List<Sample> currentPeriodSampleList = new ArrayList<>();
            final List<Sample> sampleList = this.get(sensor);
            for (final Sample sample: sampleList){
                if (sample.dateTime >=startTime && sample.dateTime  < endTime){
                    currentPeriodSampleList.add(sample);
                }
            }
            currentPeriodSensorSampleList.add(sensor, currentPeriodSampleList);
        }
        return currentPeriodSensorSampleList;
    }

}
