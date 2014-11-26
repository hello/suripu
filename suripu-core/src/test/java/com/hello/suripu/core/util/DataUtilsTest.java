package com.hello.suripu.core.util;

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
        final int values[] = new int[] {399, 453}; // {1179, 1570};
        final int correct[] = new int[] {171, 211};
        for (int i = 0; i < values.length; i++) {
            float dustDensity = DataUtils.convertDustDataFromCountsToDensity(values[i], 1);
            final int AQI = DataUtils.convertDustDensityToAQI(dustDensity);
            LOGGER.debug("value {} -> {} -> {}", values[i], dustDensity, AQI);
            assertThat(AQI, is(correct[i]));
        }
    }
}
