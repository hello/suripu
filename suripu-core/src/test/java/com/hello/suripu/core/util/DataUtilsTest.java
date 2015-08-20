package com.hello.suripu.core.util;

import com.hello.suripu.core.models.Calibration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kingshy on 11/25/14.
 */
public class DataUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataUtilsTest.class);

    @Test
    public void testDustConversionWithZeroCalibration() {
        final int raw_dust[] = new int[] {1179, 1570};
        final int expectedAQI[] = new int[] {254, 322};
        for (int i = 0; i < raw_dust.length; i++) {
            final int calculatedAQI = DataUtils.convertRawDustCountsToAQI(raw_dust[i], Calibration.createDefault("dummy-sense"), 1);
            LOGGER.trace("Under zero calibration, raw_dust {} -> aqi {}", raw_dust[i], calculatedAQI);
            assertThat(calculatedAQI, is(expectedAQI[i]));
        }
    }

    @Test
    public void testDustConversionWithSignificantCalibration() {
        final int raw_dust[] = new int[] {1179, 1570};
        final int expectedAQI[] = new int[] {260, 328};
        for (int i = 0; i < raw_dust.length; i++) {
            final int calculatedAQI = DataUtils.convertRawDustCountsToAQI(raw_dust[i], Calibration.create("dummy-sense", 175, DateTime.now(DateTimeZone.UTC).getMillis()), 1);
            LOGGER.trace("Under calibration of ADC_offset = 175, raw_dust {} -> aqi {}", raw_dust[i], calculatedAQI);
            assertThat(calculatedAQI, is(expectedAQI[i]));
        }
    }

    @Test
    public void testDustCountToDensityWithZeroCalibration() {
        final int raw_dust[] = new int[] {1179, 1570};
        final float expectedDensity[] = new float[] {203.90134f, 271.52255f};
        for (int i = 0; i < raw_dust.length; i++) {
            final float calculatedDensity = DataUtils.convertRawDustCountsToDensity(raw_dust[i], Calibration.createDefault("dummy-sense"), 1);
            LOGGER.trace("Under calibration of ADC_offset = 175, raw_dust {} -> aqi {}", raw_dust[i], calculatedDensity);
            assertThat(calculatedDensity, is(expectedDensity[i]));
        }
    }

    @Test
    public void testDustCountToDensityWithSignificantCalibration() {
        final int raw_dust[] = new int[] {1179, 1570};
        final float expectedDensity[] = new float[] {210.4732f, 278.09445f};
        for (int i = 0; i < raw_dust.length; i++) {
            final float calculatedDensity = DataUtils.convertRawDustCountsToDensity(raw_dust[i], Calibration.create("dummy-sense", 175, DateTime.now(DateTimeZone.UTC).getMillis()), 1);
            LOGGER.trace("Under calibration of ADC_offset = 175, raw_dust {} -> aqi {}", raw_dust[i], calculatedDensity);
            assertThat(calculatedDensity, is(expectedDensity[i]));
        }
    }
}
