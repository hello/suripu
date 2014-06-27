package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class GroupedRecord {


    private final List<SensorSample> temperature;
    private final List<SensorSample> humidity;
    private final List<SensorSample> sound;
    private final List<SensorSample> light;
    private final List<SensorSample> airQuality;


    public GroupedRecord(
            final List<SensorSample> temperature,
            List<SensorSample> humidity,
            List<SensorSample> sound,
            List<SensorSample> light,
            List<SensorSample> airQuality) {

        checkNotNull(temperature, "temperature list can not be null");
        checkNotNull(humidity, "humidity list can not be null");
        checkNotNull(sound, "sound list can not be null");
        checkNotNull(light, "light list can not be null");
        checkNotNull(airQuality, "airQuality list can not be null");

        this.temperature = ImmutableList.copyOf(temperature);
        this.humidity = ImmutableList.copyOf(humidity);
        this.sound = ImmutableList.copyOf(sound);
        this.light = ImmutableList.copyOf(light);
        this.airQuality = ImmutableList.copyOf(airQuality);
    }


    @JsonProperty("temp")
    public List<SensorSample> getTemperature() {
        return temperature;
    }

    @JsonProperty("humidity")
    public List<SensorSample> getHumidity() {
        return humidity;
    }

    @JsonProperty("sound")
    public List<SensorSample> getSound() {
        return sound;
    }

    @JsonProperty("light")
    public List<SensorSample> getLight() {
        return light;
    }

    @JsonProperty("air_quality")
    public List<SensorSample> getAirQuality() {
        return airQuality;
    }

    public static GroupedRecord fromRecords(final List<DeviceData> deviceDatas) {
        final List<SensorSample> temp = new ArrayList<SensorSample>(deviceDatas.size());
        final List<SensorSample> hum = new ArrayList<SensorSample>(deviceDatas.size());
        final List<SensorSample> snd = new ArrayList<SensorSample>(deviceDatas.size());
        final List<SensorSample> lght = new ArrayList<SensorSample>(deviceDatas.size());
        final List<SensorSample> air = new ArrayList<SensorSample>(deviceDatas.size());


        for(final DeviceData deviceData : deviceDatas) {
            temp.add(new SensorSample(deviceData.dateTimeUTC, deviceData.ambientTemperature, deviceData.offsetMillis));
            hum.add(new SensorSample(deviceData.dateTimeUTC, deviceData.ambientHumidity, deviceData.offsetMillis));
            snd.add(new SensorSample(deviceData.dateTimeUTC, deviceData.ambientHumidity, deviceData.offsetMillis)); // TODO : fix this
            lght.add(new SensorSample(deviceData.dateTimeUTC, deviceData.ambientHumidity, deviceData.offsetMillis)); // TODO : fix this
            air.add(new SensorSample(deviceData.dateTimeUTC, deviceData.ambientAirQuality, deviceData.offsetMillis)); // TODO : fix this
        }

        return new GroupedRecord(temp, hum, snd, lght, air);
    }
}
