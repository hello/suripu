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
    private final static List<Sample> SAMPLES_WITH_ONE_SPIKE = Arrays.asList(
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

    private final static List<Sample> SAMPLES_WITH_THREE_CONSECUTIVE_SPIKES = Arrays.asList(
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 53.958622f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 58.628117f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 60.530506f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 51.758622f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 9.928622f, 0), // this is noise, index = 4
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 10.958621f, 0), // this is noise, index = 5
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 7.958628f, 0), // this is noise, index = 6
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 52.123456f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.987654f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.987654f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.987654f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.987654f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.987654f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.987654f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.987654f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.987654f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.987654f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.987654f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.987654f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.987654f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 58.232322f, 0)
    );

    private final static List<Sample> SAMPLES_WITH_NO_SPIKE = Arrays.asList(
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.958622f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 58.628117f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 60.530506f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 59.758622f, 0)
    );

    private final static List<Sample> SAMPLES_WITH_SHORT_SIZE = Arrays.asList(
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 57.958622f, 0),
            new Sample(DateTime.now(DateTimeZone.UTC).getMillis(), 58.628117f, 0)
    );


    @Test
    public void testRemoveNoiseOnSamplesWithOneSpike() {
        final double[] samplesWithOneSpikeValues = SmoothSample.getSampleValuesArray(SAMPLES_WITH_ONE_SPIKE);
        final double mean = new Mean().evaluate(samplesWithOneSpikeValues);
        final double stdDev = new StandardDeviation().evaluate(samplesWithOneSpikeValues, mean);

        final double[] noiseFreeValues = SmoothSample.replaceNoiseWithAverageOfSurroundings(samplesWithOneSpikeValues, mean, stdDev, SmoothSample.DEFAULT_FORWARD_ATTEMPTS_TO_REMOVE_NOISE);
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
    public void testRemoveNoiseOnSamplesWithNoSpike() {
        final double[] samplesWithOneSpikeValues = SmoothSample.getSampleValuesArray(SAMPLES_WITH_NO_SPIKE);
        final double mean = new Mean().evaluate(samplesWithOneSpikeValues);
        final double stdDev = new StandardDeviation().evaluate(samplesWithOneSpikeValues, mean);

        final double[] noiseFreeValues = SmoothSample.replaceNoiseWithAverageOfSurroundings(samplesWithOneSpikeValues, mean, stdDev, SmoothSample.DEFAULT_FORWARD_ATTEMPTS_TO_REMOVE_NOISE);
        for (int i = 0; i < noiseFreeValues.length; i++) {
            assertThat(noiseFreeValues[i], equalTo(samplesWithOneSpikeValues[i]));
        }
    }

    @Test
    public void testRemoveNoiseOnSamplesWithThreeConsecutiveSpikes() {
        final double[] samplesWithOneSpikeValues = SmoothSample.getSampleValuesArray(SAMPLES_WITH_THREE_CONSECUTIVE_SPIKES);
        final double mean = new Mean().evaluate(samplesWithOneSpikeValues);
        final double stdDev = new StandardDeviation().evaluate(samplesWithOneSpikeValues, mean);

        System.out.println(mean);
        System.out.println(stdDev);

        final double[] noiseFreeValues = SmoothSample.replaceNoiseWithAverageOfSurroundings(samplesWithOneSpikeValues, mean, stdDev, SmoothSample.DEFAULT_FORWARD_ATTEMPTS_TO_REMOVE_NOISE);
        for (int i = 0; i < noiseFreeValues.length; i++) {
            if (SmoothSample.isNoise(samplesWithOneSpikeValues[i], mean, stdDev)) {
                if (i == 4) {
                    assertThat(noiseFreeValues[i] == samplesWithOneSpikeValues[i], equalTo(true)); // First spike cannot be smudged because its next 2 values are also spiky
                }
                else {
                    assertThat(noiseFreeValues[i] == samplesWithOneSpikeValues[i], equalTo(false));
                }
            }
            else {
                assertThat(noiseFreeValues[i], equalTo(samplesWithOneSpikeValues[i]));
            }
        }
    }

    @Test
    public void testSmoothOnSamplesWithOneSpike() {
        final double[] expectedSmoothValues = {
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


        final double[] samplesWithOneSpikeValues = SmoothSample.getSampleValuesArray(SAMPLES_WITH_ONE_SPIKE);
        final double mean = new Mean().evaluate(samplesWithOneSpikeValues);
        final double stdDev = new StandardDeviation().evaluate(samplesWithOneSpikeValues, mean);

        final double[] noiseFreeValues = SmoothSample.replaceNoiseWithAverageOfSurroundings(samplesWithOneSpikeValues, mean, stdDev, SmoothSample.DEFAULT_FORWARD_ATTEMPTS_TO_REMOVE_NOISE);
        final double[] smoothedvalues = SmoothSample.smooth(noiseFreeValues, SmoothSample.DEFAULT_MOVING_AVERAGE_WINDOW_SIZE);
        for (int i = 0; i < smoothedvalues.length; i++) {
            assertThat(smoothedvalues[i], equalTo(expectedSmoothValues[i]));
        }
    }

    @Test
    public void testSamplesWithLessThanMovingAvgWindowSize() {
        final List<Sample> results = SmoothSample.convert(SAMPLES_WITH_SHORT_SIZE);
        for (int i = 0; i < results.size() ; i++) {
            assertThat(results.get(i).value, equalTo(SAMPLES_WITH_SHORT_SIZE.get(i).value));
        }
    }


}
