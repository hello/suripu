package com.hello.suripu.workers.sense;

import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.models.DeviceData;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jakepiccolo on 11/24/15.
 */
public class SenseProcessorUtilsTest {

    @Test
    public void testPeriodicDataToDeviceDataBuilder() {
        final DataInputProtos.periodic_data periodicData = DataInputProtos.periodic_data.newBuilder()
                .setAudioNumDisturbances(1)
                .setAudioPeakBackgroundEnergyDb(1)
                .setAudioPeakDisturbanceEnergyDb(1)
                .setDust(1)
                .setDustMax(2)
                .setDustMin(3)
                .setDustVariability(4)
                .setHoldCount(5)
                .setHumidity(6)
                .setLight(7)
                .setLightTonality(8)
                .setLightVariability(9)
                .setTemperature(10)
                .setWaveCount(11)
                .build();
        final DeviceData deviceData = SenseProcessorUtils.periodicDataToDeviceDataBuilder(periodicData)
                .withAccountId(1L)
                .withExternalDeviceId("lol")
                .withOffsetMillis(0)
                .withDateTimeUTC(new DateTime())
                .build();
        assertThat(deviceData.ambientTemperature, is(periodicData.getTemperature()));
        assertThat(deviceData.ambientAirQualityRaw, is(periodicData.getDust()));
        assertThat(deviceData.ambientDustVariance, is(periodicData.getDustVariability()));
        assertThat(deviceData.ambientDustMin, is(periodicData.getDustMin()));
        assertThat(deviceData.ambientDustMax, is(periodicData.getDustMax()));
        assertThat(deviceData.ambientHumidity, is(periodicData.getHumidity()));
        assertThat(deviceData.ambientLight, is(periodicData.getLight()));
        assertThat(deviceData.ambientLightVariance, is(periodicData.getLightVariability()));
        assertThat(deviceData.ambientLightPeakiness, is(periodicData.getLightTonality()));
        assertThat(deviceData.waveCount, is(periodicData.getWaveCount()));
        assertThat(deviceData.holdCount, is(periodicData.getHoldCount()));
        assertThat(deviceData.audioNumDisturbances, is(periodicData.getAudioNumDisturbances()));
    }
}