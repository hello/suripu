package com.hello.suripu.core.db.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.AllSensorSampleMap;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.util.DataUtils;
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
    public static Optional<Map<Long, Sample>> populateMap(final List<DeviceData> deviceDataList, final String sensorName) {

        if(deviceDataList == null) {
            LOGGER.error("deviceDataList is null for sensor {}", sensorName);
            return Optional.absent();
        }

        if(deviceDataList.isEmpty()) {
            return Optional.absent();
        }

        final Map<Long, Sample> map = new HashMap<>();

        for(final DeviceData deviceData: deviceDataList) {

            final Long newKey = deviceData.dateTimeUTC.getMillis();

            // TODO: refactor this

            float sensorValue = 0;
            if(sensorName.equals("humidity")) {
                sensorValue = DeviceData.dbIntToFloat(deviceData.ambientHumidity);
            } else if(sensorName.equals("temperature")) {
                sensorValue = DeviceData.dbIntToFloat(deviceData.ambientTemperature);
            } else if (sensorName.equals("particulates")) {
                sensorValue = (float) DataUtils.convertRawDustCountsToAQI(deviceData.ambientDustMax, deviceData.firmwareVersion);
            } else if (sensorName.equals("light")) {
                sensorValue = (float) deviceData.ambientLight;
            } else if (sensorName.equals("sound")) {
                sensorValue = DataUtils.dbIntToFloatAudioDecibels(deviceData.audioPeakBackgroundDB);
            }else if(sensorName.equals("wave_count")) {
                sensorValue = deviceData.waveCount;
            }else if(sensorName.equals("hold_count")) {
                sensorValue = deviceData.holdCount;
            } else {
                LOGGER.warn("Sensor {} is not supported for account_id: {}. Returning early", sensorName);
                return Optional.absent();
            }


            LOGGER.trace("Overriding {}", newKey);

            map.put(newKey, new Sample(newKey, sensorValue, deviceData.offsetMillis));
        }

        return Optional.of(map);
    }

    public static Optional<AllSensorSampleMap> populateMapAll(final List<DeviceData> deviceDataList) {

        if(deviceDataList == null) {
            LOGGER.error("deviceDataList is null for all sensors");
            return Optional.absent();
        }

        if(deviceDataList.isEmpty()) {
            return Optional.absent();
        }

        final AllSensorSampleMap populatedMap = new AllSensorSampleMap();

//        final Map<Sensor, Map<Long, Sample>> map = new HashMap<>();
//        for (Sensor sensor : Sensor.values()) {
//            final Map<Long, Sample> values = new HashMap<>();
//            map.put(sensor, values);
//        }

        for(final DeviceData deviceData: deviceDataList) {

            final Long newKey = deviceData.dateTimeUTC.getMillis();

            final float lightValue = (float) deviceData.ambientLight;
            final float soundValue = DataUtils.dbIntToFloatAudioDecibels(deviceData.audioPeakBackgroundDB);
            final float humidityValue = DeviceData.dbIntToFloat(deviceData.ambientHumidity);
            final float temperatureValue = DeviceData.dbIntToFloat(deviceData.ambientTemperature);
            final float particulatesValue = (float) DataUtils.convertRawDustCountsToAQI(deviceData.ambientDustMax, deviceData.firmwareVersion);

            populatedMap.addSample(newKey, deviceData.offsetMillis,
                    lightValue, soundValue, humidityValue, temperatureValue, particulatesValue);

//            map.get(Sensor.HUMIDITY).put(newKey, new Sample(newKey, humidityValue, deviceData.offsetMillis));
//            map.get(Sensor.TEMPERATURE).put(newKey, new Sample(newKey, temperatureValue, deviceData.offsetMillis));
//            map.get(Sensor.PARTICULATES).put(newKey, new Sample(newKey, particulatesValue, deviceData.offsetMillis));
//            map.get(Sensor.LIGHT).put(newKey, new Sample(newKey, lightValue, deviceData.offsetMillis));
//            map.get(Sensor.SOUND).put(newKey, new Sample(newKey, soundValue, deviceData.offsetMillis));

            LOGGER.trace("Overriding {}", newKey);
        }

        return Optional.of(populatedMap);
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

        LOGGER.debug("Map size = {}", map.size());


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
