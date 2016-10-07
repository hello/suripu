package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.util.Bucketing;
import com.hello.suripu.core.models.AllSensorSampleMap;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;

public class BucketingTest {

    private final int numberOfBuckets = 5;
    private final int slotDurationInMinutes = 5;
    private final DateTime startDate = new DateTime(2014,1,1, 1,0,0, DateTimeZone.UTC);

    private final Optional<Calibration> calibration = Optional.of(Calibration.createDefault("dummy-sense"));

    private Map<Long, Sample> generateMap(final DateTime startDate) {

        final DeviceData deviceData = DeviceData.senseOne(999L, 111L, "", 222,333,444,555, 0, 0, 0, 666, 777,777, 888, startDate, 0, 0, 0, 0, 0, 0, 0, 0);
        final List<DeviceData> deviceDataList = new ArrayList<>();
        deviceDataList.add(deviceData);

        final Optional<Map<Long, Sample>> populatedMap = Bucketing.populateMap(deviceDataList, Sensor.TEMPERATURE, Optional.<Device.Color>absent(), calibration, false);
        return populatedMap.get();
    }

    @Test
    public void testEmptyMap() {

        final Map<Long, Sample> map = Bucketing.generateEmptyMap(numberOfBuckets, startDate, slotDurationInMinutes, 0);

        final Set<Long> keys = map.keySet();
        final Set<Long> timestamps = new HashSet<>();

        for(int i = 0; i < numberOfBuckets; i ++) {
            timestamps.add(startDate.minusMinutes(i * slotDurationInMinutes).getMillis());
        }

        assertThat(map.size(), is(numberOfBuckets));
        assertThat(keys, hasItem(startDate.getMillis()));
        assertThat(Sets.difference(keys, timestamps).size(), is(0));
    }

    @Test
    public void testPopulateMap() {
        final Map<Long, Sample> map = Bucketing.generateEmptyMap(numberOfBuckets, startDate, slotDurationInMinutes, 0);

        final DeviceData deviceData = DeviceData.senseOne(999L, 111L, "", 222, 333, 444, 555, 0, 0, 0, 666, 777,777, 888, startDate, 0, 0, 0, 0, 0, 0, 0, 0);
        final List<DeviceData> deviceDataList = new ArrayList<>();
        deviceDataList.add(deviceData);

        final Optional<Map<Long, Sample>> populatedMap = Bucketing.populateMap(deviceDataList, Sensor.TEMPERATURE, Optional.<Device.Color>absent(), calibration, false);
        assertThat(populatedMap.isPresent(), is(true));

        assertThat(populatedMap.get().size(), is(1));
        assertThat(populatedMap.get().get(startDate.getMillis()).value, is(-1.67F)); // 2.22 - 3.89 (current temp offset)

    }


    @Test
    public void testPopulateMapNoData() {

        final List<DeviceData> deviceDataList = new ArrayList<>();

        Optional<Map<Long, Sample>> populatedMap = Bucketing.populateMap(new ArrayList<DeviceData>(), Sensor.TEMPERATURE ,Optional.<Device.Color>absent(), calibration, false);
        assertThat(populatedMap.isPresent(), is(false));

        populatedMap = Bucketing.populateMap(null, Sensor.TEMPERATURE,Optional.<Device.Color>absent(), calibration, false);
        assertThat(populatedMap.isPresent(), is(false));
    }

    @Test
    public void testPopulateMapMissingSensor() {

        final Optional<Map<Long, Sample>> populatedMap = Bucketing.populateMap(Collections.EMPTY_LIST, Sensor.CO2,Optional.<Device.Color>absent(), calibration, false);
        assertThat(populatedMap.isPresent(), is(false));
    }

    @Test
    public void testMergeSubset() {
        Map<Long, Sample> generated = Bucketing.generateEmptyMap(numberOfBuckets, startDate, slotDurationInMinutes, 0);
        Map<Long, Sample> data = generateMap(startDate);
        Map<Long, Sample> merged = Bucketing.mergeResults(generated, data);
        assertThat(merged.size(), is(generated.size()));

        // Data is out of range of the generate map
        data = generateMap(startDate.plusDays(1));
        merged = Bucketing.mergeResults(generated, data);
        assertThat(merged.size(), is(generated.size() + data.size()));
    }

    @Test
    public void testPopulateMapAllEmpty() {
        final List<DeviceData> deviceDataList = Lists.newArrayList();
        final AllSensorSampleMap map = Bucketing.populateMapAll(deviceDataList, Optional.<Device.Color>absent(), Optional.<Calibration>absent(), false);
        assertThat(map.isEmpty(), is(true));
    }

    @Test
    public void testPopulateMapNoSound() {
        final List<DeviceData> deviceDataList = ImmutableList.of(
                new DeviceData.Builder()
                        .withAmbientLight(0)
                        .withAmbientAirQualityRaw(0)
                        .withFirmwareVersion(1)
                        .withAccountId(1L)
                        .withAlreadyCalibratedAudioPeakBackgroundDB(0)
                        .withAlreadyCalibratedAudioPeakDisturbancesDB(0)
                        .withAlreadyCalibratedPeakEnergyDB(0)
                        .withAmbientDustMax(0)
                        .withAmbientDustMin(0)
                        .withAmbientDustVariance(0)
                        .withAmbientHumidity(0)
                        .withAmbientTemperature(0)
                        .withAudioNumDisturbances(0)
                        .withDateTimeUTC(new DateTime(0))
                        .withOffsetMillis(0)
                        .withWaveCount(0)
                        .withHoldCount(0)
                        .build(),
                new DeviceData.Builder()
                        .withAmbientLight(0)
                        .withAmbientAirQualityRaw(0)
                        .withFirmwareVersion(1)
                        .withAccountId(1L)
                        .withAlreadyCalibratedAudioPeakBackgroundDB(0)
                        .withAlreadyCalibratedAudioPeakDisturbancesDB(50000)
                        .withAlreadyCalibratedPeakEnergyDB(10000)
                        .withAmbientDustMax(0)
                        .withAmbientDustMin(0)
                        .withAmbientDustVariance(0)
                        .withAmbientHumidity(0)
                        .withAmbientTemperature(0)
                        .withAudioNumDisturbances(1)
                        .withDateTimeUTC(new DateTime(1))
                        .withOffsetMillis(0)
                        .withWaveCount(0)
                        .withHoldCount(0)
                        .build()
        );
        final AllSensorSampleMap map = Bucketing.populateMapAll(deviceDataList, Optional.<Device.Color>absent(), Optional.<Calibration>absent(), false);
        assertThat(map.get(Sensor.SOUND).get(0L).value, is((float) 0));
        assertThat(map.get(Sensor.SOUND).get(1L).value, greaterThan((float) 25.0));
    }


}
