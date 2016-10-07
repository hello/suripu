package com.hello.suripu.core.db.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.AllSensorSampleMap;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.util.DataUtils;
import com.hello.suripu.core.util.calibration.SenseOneFiveDataConversion;
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

        Device.Color color = Device.DEFAULT_COLOR;

        if (optionalColor.isPresent()) {
            color = optionalColor.get();
        }

        final Map<Long, Sample> map = new HashMap<>();

        for(final DeviceData deviceData: deviceDataList) {

            final Long newKey = deviceData.dateTimeUTC.getMillis();

            // TODO: refactor this

            float sensorValue = 0;
            if(sensorName.equals(Sensor.HUMIDITY)) {
                sensorValue = DataUtils.calibrateHumidity(deviceData.ambientTemperature, deviceData.ambientHumidity);
            } else if(sensorName.equals(Sensor.TEMPERATURE)) {
                sensorValue = DataUtils.calibrateTemperature(deviceData.ambientTemperature);
            } else if (sensorName.equals(Sensor.PARTICULATES) && calibrationOptional.isPresent()) {
                sensorValue = DataUtils.convertRawDustCountsToDensity(deviceData.ambientAirQualityRaw, calibrationOptional);
            } else if (sensorName.equals(Sensor.LIGHT)) {
                sensorValue = DataUtils.calibrateLight(deviceData.ambientLightFloat,color);
            } else if (sensorName.equals(Sensor.SOUND)) {
                final Integer audioPeakDB;
                if (useAudioPeakEnergy && deviceData.audioPeakEnergyDB != 0) {
                    audioPeakDB = deviceData.audioPeakEnergyDB;
                } else {
                    audioPeakDB = deviceData.audioPeakDisturbancesDB;
                }
                sensorValue = DataUtils.calibrateAudio(DataUtils.dbIntToFloatAudioDecibels(deviceData.audioPeakBackgroundDB), DataUtils.dbIntToFloatAudioDecibels(audioPeakDB), deviceData.firmwareVersion);
            } else if(sensorName.equals(Sensor.WAVE_COUNT)) {
                sensorValue = deviceData.waveCount;
            } else if(sensorName.equals(Sensor.HOLD_COUNT)) {
                sensorValue = deviceData.holdCount;
            } else if(sensorName.equals(Sensor.SOUND_NUM_DISTURBANCES)) {
                sensorValue = deviceData.audioNumDisturbances;
            } else if(sensorName.equals(Sensor.SOUND_PEAK_ENERGY)) {
                sensorValue = DataUtils.dbIntToFloatAudioDecibels(deviceData.audioPeakBackgroundDB);
            } else if(sensorName.equals(Sensor.SOUND_PEAK_DISTURBANCE)) {
                sensorValue = DataUtils.dbIntToFloatAudioDecibels(deviceData.audioPeakDisturbancesDB);
            } else {
                LOGGER.warn("Sensor {} is not supported. Returning early", sensorName);
                return Optional.absent();
            }


            LOGGER.trace("Overriding {}", newKey);

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

        Device.Color color = Device.DEFAULT_COLOR;

        if (optionalColor.isPresent()) {
            color = optionalColor.get();
        }

        for(final DeviceData deviceData: deviceDataList) {

            final Long newKey = deviceData.dateTimeUTC.getMillis();

            final float lightValue = getLux(deviceData, color);

            final Integer audioPeakDB;
            if (useAudioPeakEnergy && deviceData.audioPeakEnergyDB != 0) {
                audioPeakDB = deviceData.audioPeakEnergyDB;
            } else {
                audioPeakDB = deviceData.audioPeakDisturbancesDB;
            }

            final float soundValue = DataUtils.calibrateAudio(DataUtils.dbIntToFloatAudioDecibels(deviceData.audioPeakBackgroundDB), DataUtils.dbIntToFloatAudioDecibels(audioPeakDB), deviceData.firmwareVersion);

            final float humidityValue = DataUtils.calibrateHumidity(deviceData.ambientTemperature, deviceData.ambientHumidity);
            final float temperatureValue = DataUtils.calibrateTemperature(deviceData.ambientTemperature);
            final float particulatesValue = DataUtils.convertRawDustCountsToDensity(deviceData.ambientAirQualityRaw, calibrationOptional);
            final int waveCount = deviceData.waveCount;
            final int holdCount = deviceData.holdCount;
            final float soundNumDisturbances = (float) deviceData.audioNumDisturbances;
            final float soundPeakDisturbance = DataUtils.dbIntToFloatAudioDecibels(deviceData.audioPeakDisturbancesDB);
            final float soundPeakEnergy = DataUtils.dbIntToFloatAudioDecibels(deviceData.audioPeakEnergyDB);

            populatedMap.addSample(newKey, deviceData.offsetMillis,
                    lightValue, soundValue, humidityValue, temperatureValue, particulatesValue, waveCount, holdCount,
                    soundNumDisturbances, soundPeakDisturbance, soundPeakEnergy);
            if(deviceData.hasExtra()) {
                final float pressure = SenseOneFiveDataConversion.convertRawToMilliBar(deviceData.extra().pressure());
                final float tvoc = SenseOneFiveDataConversion.convertRawToVOC(deviceData.extra().tvoc());
                final float co2 = SenseOneFiveDataConversion.convertRawToCO2(deviceData.extra().co2());
                final float ir = deviceData.extra().ir();
                final float clear = deviceData.extra().clear();
                final float lux = deviceData.extra().luxCount();

                final int uv = deviceData.extra().uvCount();
                populatedMap.addExtraSample(newKey, deviceData.offsetMillis, pressure, tvoc, co2, ir, clear, lux, uv);
            }
        }

        return populatedMap;
    }


    static float getLux(final DeviceData deviceData, final Device.Color color) {
        // Sense one
        if(!deviceData.hasExtra()) {
            return DataUtils.calibrateLight(deviceData.ambientLightFloat,color);
        }
        return (float) deviceData.extra().luxCount();
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
