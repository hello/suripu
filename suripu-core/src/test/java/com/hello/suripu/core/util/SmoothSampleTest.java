package com.hello.suripu.core.util;

import com.hello.suripu.core.models.Sample;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


public class SmoothSampleTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmoothSampleTest.class);
    private final static List<Sample> samplesWithOneSpike = Arrays.asList(
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 53.958622f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 58.628117f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 60.530506f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 51.758622f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 55.928622f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 10.958621f, 0), // this is noise, index = 5
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 56.958628f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 52.123456f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.987654f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 58.232322f, 0)
    );

    private final static double[] samplesWithOneSpikeValues = SmoothSample.getSampleValuesArray(samplesWithOneSpike);
    private final static double[] expectedSmoothValues = {
            55.12599563598633,
            57.46074295043945,
            56.29336929321289,
            59.57931137084961,
            56.14456367492676,
            53.8436222076416,
            41.94646739959717,
            42.46147060394287,
            54.54104232788086,
            55.05555534362793
    };




    @Test
    public void testReplaceNoiseWithAvergeOfSurroundings() {
        final double mean = new Mean().evaluate(samplesWithOneSpikeValues);
        final double stdDev = new StandardDeviation().evaluate(samplesWithOneSpikeValues);

        final double[] noiseFreeValues = SmoothSample.replaceNoiseWithAvergeOfSurroundings(samplesWithOneSpikeValues, mean, stdDev, SmoothSample.DEFAULT_FORWARD_ATTEMPTS_TO_REMOVE_NOISE);
        for (int i = 0; i < noiseFreeValues.length; i++) {
            if (SmoothSample.isNoise(samplesWithOneSpikeValues[i], mean, stdDev)) {
                assertThat(noiseFreeValues[i] == samplesWithOneSpikeValues[i], equalTo(false));
            }
            else {
                assertThat(noiseFreeValues[i], equalTo(samplesWithOneSpikeValues[i]));
            }
        }
    }

    @Test
    public void testSmooth() {
        final double mean = new Mean().evaluate(samplesWithOneSpikeValues);
        final double stdDev = new StandardDeviation().evaluate(samplesWithOneSpikeValues);

        final double[] noiseFreeValues = SmoothSample.replaceNoiseWithAvergeOfSurroundings(samplesWithOneSpikeValues, mean, stdDev, SmoothSample.DEFAULT_FORWARD_ATTEMPTS_TO_REMOVE_NOISE);
        final double[] smoothedvalues = SmoothSample.smooth(noiseFreeValues, SmoothSample.DEFAULT_MOVING_AVERAGE_WINDOW_SIZE);
        for (int i = 0; i < smoothedvalues.length; i++) {
            assertThat(smoothedvalues[i], equalTo(expectedSmoothValues[i]));
        }
    }
}
