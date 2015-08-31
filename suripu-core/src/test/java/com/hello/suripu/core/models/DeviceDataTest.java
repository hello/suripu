package com.hello.suripu.core.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kingshy on 10/20/14.
 */
public class DeviceDataTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceDataTest.class);

    @Test
    public void testBuilder() {
        final long timestamp = 1413862008 * 1000L;
        final int temp = 2448;
        final int humid = 3384;
        final int dust = 1000;
        final int light = 156;
        final Long accountId = 3L;

        final DateTime roundedDateTime = new DateTime(timestamp, DateTimeZone.UTC).withSecondOfMinute(0);
        final DeviceData.Builder builder = new DeviceData.Builder()
                .withAccountId(accountId)
                .withDeviceId(251278L)
                .withAmbientTemperature(temp)
                .withAmbientAirQualityRaw(dust)
                .withAmbientDustVariance(0)
                .withAmbientDustMin(0)
                .withAmbientDustMax(100)
                .withAmbientHumidity(humid)
                .withAmbientLight(light)
                .withAmbientLightVariance(14)
                .withAmbientLightPeakiness(27)
                .withOffsetMillis(-25200000)
                .withDateTimeUTC(roundedDateTime);

        final DeviceData deviceData = builder.build();

        assertThat(deviceData.ambientAirQuality, is(0));
        assertThat(deviceData.ambientAirQualityRaw, is(dust));
    }
}


