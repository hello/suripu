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
    public void testDustConversion() {
        final int values[] = new int[] {1179, 1570};
        final int correct[] = new int[] {254, 322};
        for (int i = 0; i < values.length; i++) {
            final int AQI = DataUtils.convertRawDustCountsToAQIWithCalibration(values[i], Calibration.createDefault("dummy-sense"), 1);
            LOGGER.debug("value {} -> {}", values[i], AQI);
            assertThat(AQI, is(correct[i]));
        }
    }
}
