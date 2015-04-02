package com.hello.suripu.core.models;

import com.hello.suripu.core.models.Insights.TrendGraph;
import org.junit.Test;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jimmy on 4/2/15.
 */
public class TrendGraphTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(TrendGraphTest.class);

    @Test
    public void testGetTimeSeriesOptions() {
        LOGGER.debug("getTimeSeriesOptions should return incrementally larger options");
        final List<String> options = TrendGraph.TimePeriodType.getTimeSeriesOptions(90);
        int previousTime = 0;
        for (final String option : options) {
            final TrendGraph.TimePeriodType type = TrendGraph.TimePeriodType.fromString(option);
            final int time = TrendGraph.PERIOD_TYPE_DAYS.get(type);
            Assert.assertTrue(previousTime < time);
            previousTime = time;
        }
    }

}
