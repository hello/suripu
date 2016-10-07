package com.hello.suripu.core.db.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.AllSensorSampleMap;
import com.hello.suripu.core.models.CalibratedDeviceData;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bucketing {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bucketing.class);

    /**
     * Transform map of Samples to sorted list of Samples
     * @param map
     * @param currentOffsetMillis
     * @return
     */
    public static List<Sample> sortResults(final Map<Long, Sample> map, final Integer currentOffsetMillis) {

        final Sample[] samples = map.values().toArray(new Sample[0]);
        Arrays.sort(samples, new Comparator<Sample>() {
            @Override
            public int compare(Sample o1, Sample o2) {
                return Long.compare(o1.dateTime, o2.dateTime);
            }
        });

        int lastOffsetMillis = -1;
        for(final Sample sample : samples) {
            if(sample.offsetMillis == null) {
                if(lastOffsetMillis == -1) {
                    sample.offsetMillis = currentOffsetMillis;
                } else {
                    sample.offsetMillis = lastOffsetMillis;
                }
            }

            lastOffsetMillis = sample.offsetMillis;
        }
        return Lists.newArrayList(samples);
    }

    /**
     * Overrides map values with data from deviceDataList
     * @param deviceDataList
     * @param sensorName
     * @return
     */
    public static Optional<Map<Long, Sample>> populateMap(final List<DeviceData> deviceDataList, final Sensor sensorName,
                                                          final Optional<Device.Color> optionalColor, final Optional<Calibration> calibrationOptional,
                                                          final Boolean useAudioPeakEnergy) {

        if(deviceDataList == null) {
            LOGGER.error("deviceDataList is null for sensor {}", sensorName);
            return Optional.absent();
        }

        if(deviceDataList.isEmpty()) {
            return Optional.absent();
        }

        final Device.Color color = optionalColor.or(Device.DEFAULT_COLOR);

        final Map<Long, Sample> map = new HashMap<>();

        for(final DeviceData deviceData: deviceDataList) {

            final Long newKey = deviceData.dateTimeUTC.getMillis();

            // TODO: refactor this
            final CalibratedDeviceData calibratedDeviceData = new CalibratedDeviceData(deviceData, color, calibrationOptional);

            float sensorValue = 0;
            if(sensorName.equals(Sensor.HUMIDITY)) {
                sensorValue = calibratedDeviceData.humidity();
            } else if(sensorName.equals(Sensor.TEMPERATURE)) {
                sensorValue = calibratedDeviceData.temperature();
            } else if (sensorName.equals(Sensor.PARTICULATES) && calibrationOptional.isPresent()) {
                sensorValue = calibratedDeviceData.particulates();
            } else if (sensorName.equals(Sensor.LIGHT)) {
                sensorValue = calibratedDeviceData.lux();
            } else if (sensorName.equals(Sensor.SOUND)) {
                sensorValue = calibratedDeviceData.sound(useAudioPeakEnergy);
            } else if(sensorName.equals(Sensor.WAVE_COUNT)) {
                sensorValue = deviceData.waveCount;
            } else if(sensorName.equals(Sensor.HOLD_COUNT)) {
                sensorValue = deviceData.holdCount;
            } else if(sensorName.equals(Sensor.SOUND_NUM_DISTURBANCES)) {
                sensorValue = deviceData.audioNumDisturbances;
            } else if(sensorName.equals(Sensor.SOUND_PEAK_ENERGY)) {
                sensorValue = calibratedDeviceData.soundPeakEnergy();
            } else if(sensorName.equals(Sensor.SOUND_PEAK_DISTURBANCE)) {
                sensorValue = calibratedDeviceData.soundPeakDisturbance();
            } else {
                LOGGER.warn("Sensor {} is not supported. Returning early", sensorName);
                return Optional.absent();
            }

            map.put(newKey, new Sample(newKey, sensorValue, deviceData.offsetMillis));
        }

        return Optional.of(map);
    }

    public static AllSensorSampleMap populateMapAll(@NotNull final List<DeviceData> deviceDataList, final Optional<Device.Color> optionalColor,
                                                    final Optional<Calibration> calibrationOptional, final Boolean useAudioPeakEnergy) {

        final AllSensorSampleMap populatedMap = new AllSensorSampleMap();

        if(deviceDataList.isEmpty()) {
            return populatedMap;
        }

        final Device.Color color = optionalColor.or(Device.DEFAULT_COLOR);

        for(final DeviceData deviceData: deviceDataList) {

            final Long newKey = deviceData.dateTimeUTC.getMillis();

            final CalibratedDeviceData calibratedDeviceData = new CalibratedDeviceData(deviceData, color, calibrationOptional);
            final float lightValue = calibratedDeviceData.lux();

            final float soundValue =  calibratedDeviceData.sound(useAudioPeakEnergy);

            final float humidityValue = calibratedDeviceData.humidity();
            final float temperatureValue = calibratedDeviceData.temperature();

            final float particulatesValue = calibratedDeviceData.particulates();
            final int waveCount = deviceData.waveCount;
            final int holdCount = deviceData.holdCount;
            final float soundNumDisturbances = calibratedDeviceData.audioNumDisturbances();
            final float soundPeakDisturbance = calibratedDeviceData.soundPeakDisturbance();
            final float soundPeakEnergy = calibratedDeviceData.soundPeakEnergy();

            populatedMap.addSample(newKey, deviceData.offsetMillis,
                    lightValue, soundValue, humidityValue, temperatureValue, particulatesValue, waveCount, holdCount,
                    soundNumDisturbances, soundPeakDisturbance, soundPeakEnergy);

            if(deviceData.hasExtra()) {
                final float pressure = calibratedDeviceData.pressure();
                final float tvoc = calibratedDeviceData.tvoc();
                final float co2 = calibratedDeviceData.co2();
                final float ir = deviceData.extra().ir();
                final float clear = deviceData.extra().clear();
                final int uv = deviceData.extra().uvCount();
                populatedMap.addExtraSample(newKey, deviceData.offsetMillis, pressure, tvoc, co2, ir, clear, uv);
            }
        }

        return populatedMap;
    }

    /**
     * Generates a map with every bucket containing empty sample
     * @param numberOfBuckets
     * @param startDate
     * @param slotDurationInMinutes
     * @return
     */
    public static Map<Long, Sample>  generateEmptyMap(final int numberOfBuckets, final DateTime startDate, final int slotDurationInMinutes, final int missingSampleDefaultValue) {

        final Map<Long, Sample> map = Maps.newHashMap();

        for(int i = 0; i < numberOfBuckets; i++) {
            final Long key = startDate.minusMinutes(i * slotDurationInMinutes).getMillis();
            LOGGER.trace("Inserting {}", key);

            map.put(key, new Sample(key, missingSampleDefaultValue, null));
        }

        LOGGER.trace("Map size = {}", map.size());


        return map;
    }

    /**
     * Merge both result sets and returns an immutable copy
     * @param generated
     * @param data
     * @return
     */
    public static ImmutableMap<Long, Sample> mergeResults(final Map<Long, Sample> generated, final Map<Long, Sample> data) {
        final Map<Long, Sample> all = new HashMap<>();
        all.putAll(generated);
        all.putAll(data);
        return ImmutableMap.copyOf(all);
    }
}
