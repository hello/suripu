package com.hello.suripu.core.util;

import com.hello.suripu.core.models.Calibration;
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
            final int calculatedAQI = DataUtils.convertRawDustCountsToAQIWithCalibration(raw_dust[i], Calibration.createDefault("dummy-sense"), 1);
            LOGGER.debug("Under zero calibration, raw_dust {} -> aqi {}", raw_dust[i], calculatedAQI);
            assertThat(calculatedAQI, is(expectedAQI[i]));
        }
    }

    @Test
    public void testDustConversionWithSignificantCalibration() {
        final int raw_dust[] = new int[] {1179, 1570};
        final int expected_aqi[] = new int[] {260, 328};
        for (int i = 0; i < raw_dust.length; i++) {
            final int calculatedAQI = DataUtils.convertRawDustCountsToAQIWithCalibration(raw_dust[i], Calibration.create("dummy-sense", 175, "dummy-metadata"), 1);
            LOGGER.debug("Under calibration of ADC_offset = 175, raw_dust {} -> aqi {}", raw_dust[i], calculatedAQI);
            assertThat(calculatedAQI, is(expected_aqi[i]));
        }
    }
}
