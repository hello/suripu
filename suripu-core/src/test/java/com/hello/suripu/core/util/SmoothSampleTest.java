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

        final double[] noiseFreeValues = SmoothSample.smudgeNoise(samplesWithOneSpikeValues, mean, stdDev, SmoothSample.DEFAULT_NOISE_LENGTH_TOLERANCE);
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

        final double[] noiseFreeValues = SmoothSample.smudgeNoise(samplesWithOneSpikeValues, mean, stdDev, SmoothSample.DEFAULT_NOISE_LENGTH_TOLERANCE);
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

        final double[] noiseFreeValues = SmoothSample.smudgeNoise(samplesWithOneSpikeValues, mean, stdDev, SmoothSample.DEFAULT_NOISE_LENGTH_TOLERANCE);
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
                54.97733116149902,
                55.49233436584473,
                54.54104232788086,
                55.05555534362793
        };


        final double[] samplesWithOneSpikeValues = SmoothSample.getSampleValuesArray(SAMPLES_WITH_ONE_SPIKE);
        final double mean = new Mean().evaluate(samplesWithOneSpikeValues);
        final double stdDev = new StandardDeviation().evaluate(samplesWithOneSpikeValues, mean);

        final double[] noiseFreeValues = SmoothSample.smudgeNoise(samplesWithOneSpikeValues, mean, stdDev, SmoothSample.DEFAULT_NOISE_LENGTH_TOLERANCE);
        final double[] smoothedvalues = SmoothSample.smooth(noiseFreeValues, SmoothSample.DEFAULT_MOVING_AVERAGE_WINDOW_SIZE_FOR_HIGH_RESOLUTION);
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


    @Test
    public void testMinuteResolutionDustValues() {
        final double[] values = {40.468964,40.123077,41.506634,39.604244,38.047745,40.64191,41.160744,44.44669,38.39363,37.52891,38.566578,38.220688,38.39363,40.29602,36.837135,41.333687,40.64191,42.544296,36.491245,39.4313,51.018566,39.258354,39.77719,39.77719,40.814854,41.333687,38.566578,39.08541,38.73952,39.08541,38.912464,41.85252,38.39363,43.581963,38.39363,39.4313,42.19841,38.220688,37.183025,39.4313,39.950134,36.664192,38.220688,36.664192,34.58886,40.9878,42.371353,37.874798,39.4313,40.468964,43.06313,37.874798,45.13846,39.604244,41.506634,39.950134,41.333687,41.160744,40.9878,40.814854,37.701855,43.06313,35.972412,40.468964,38.566578,43.92785,43.581963,39.604244,42.71724,65.02706,83.01327,106.87958,92.5252,86.99098,77.65199,82.66737,67.10239,65.200005,59.319893,59.838726,54.82334,68.14005,53.785675,49.635014,47.90557,47.04085,45.83024,43.409016,48.25146,43.409016,39.950134,46.176132,56.033955,43.92785,39.08541,43.409016,43.236076,41.160744,40.468964,39.950134,42.544296,44.273743,41.85252,42.71724,41.160744,42.19841,39.950134,40.468964,36.837135,41.160744,39.604244,54.13157,39.77719,39.604244,44.61963,38.047745,40.9878,40.9878,39.4313,40.814854,41.160744,36.491245,40.9878,40.123077,40.9878,39.4313,45.6573,41.506634,38.912464,40.123077,39.258354,37.35597,39.950134,42.71724,43.581963,36.837135,36.14536,50.499733,41.506634,40.29602,41.506634,40.64191,39.950134,37.01008,40.468964,40.123077,42.890182,37.874798,39.08541,36.491245,37.35597,40.9878,38.566578,41.85252,38.220688,40.64191,39.4313,38.912464,42.71724,39.4313,37.01008,40.9878,41.333687,40.814854,46.003185,40.123077,38.047745,39.604244,39.950134,40.64191,42.19841,41.160744,38.39363,37.874798,55.51512,39.77719,40.29602,39.604244,37.183025,37.35597,36.318302,40.814854,39.950134,45.83024,37.52891,44.61963,39.77719,38.566578,43.581963,38.047745,39.258354,40.123077,38.047745,42.890182,38.566578,38.566578,37.52891,39.950134,38.566578,41.506634,42.19841,38.912464,39.258354,38.566578,37.35597,38.73952,39.258354,39.258354,42.71724,39.950134,41.506634,34.242973,37.874798,40.9878,54.477455,36.837135,40.814854,52.056236,41.333687,39.08541,36.664192,37.35597,38.73952,39.258354,40.123077,37.52891,39.4313,38.912464,42.890182,39.08541,37.52891,40.814854,38.73952,40.123077,38.220688,37.52891,40.123077,37.35597,38.73952,37.01008,34.761806,36.664192,40.9878,38.912464,37.701855,37.701855,40.468964,43.06313,39.08541,39.258354,37.874798,38.047745,38.566578,40.468964,34.761806,34.58886,41.506634,43.06313,40.123077,41.160744,39.77719,38.047745,39.08541,38.566578,39.08541,40.123077,38.73952,39.950134,37.52891,38.73952,38.73952,37.874798,37.52891,40.9878,41.679577,37.183025,40.29602,37.874798,41.679577,39.604244,40.123077,40.9878,37.183025,38.73952,41.333687,43.236076,38.912464,40.814854,38.047745};
        final double[] expectedSmoothedValues = {40.38249225,40.209548749999996,40.2960205,40.8148555,40.555439,38.8259945,39.3448275,40.901327,42.803717,41.420159999999996,37.96127,38.047744,38.393633,38.307159,39.344825,38.5665775,39.085411,40.9877985,41.593103,39.5177705,37.9612725,45.224933,45.138459999999995,39.517771999999994,39.77719,40.296021999999994,41.0742705,39.950132499999995,38.825994,38.912465,38.912465,38.998937,40.382492,40.123075,40.9877965,40.9877965,38.912465,40.814855,40.209549,37.701856500000005,38.307162500000004,39.690717,38.307163,37.442440000000005,37.442440000000005,35.626526,37.78833,41.679576499999996,40.1230755,38.653048999999996,39.950131999999996,41.766047,40.468964,41.506629000000004,42.371352,40.555439,40.728384,40.641910499999994,41.247215499999996,41.074272,40.901326999999995,39.258354499999996,40.3824925,39.517770999999996,38.220687999999996,39.517770999999996,41.247214,43.754906500000004,41.5931035,41.160742,53.872150000000005,74.020165,94.946425,99.70239000000001,89.75809,82.321485,80.15968000000001,74.88488000000001,66.1511975,60.342901330233566,55.32018324535035,54.98895441511678,53.5262585,53.007425999999995,51.7103445,48.770292,47.473209999999995,46.435545000000005,44.619628000000006,45.830238,45.830238,41.679575,43.063133,51.1050435,49.9809025,41.50663,41.247213,43.322546,42.198409999999996,40.814854,40.209548999999996,41.247215,43.4090195,43.0631315,42.28488,41.938992,41.679577,41.074272,40.209548999999996,38.6530495,38.998939500000006,40.382494,46.867907,46.95438,39.690717,42.111937,41.333687499999996,39.5177725,40.9878,40.20955,40.123076999999995,40.987798999999995,38.8259945,38.7395225,40.5554385,40.5554385,40.20955,42.5443,43.581967,40.209548999999996,39.5177705,39.690715499999996,38.307162,38.653052,41.333687,43.1496015,40.209549,36.4912475,43.3225465,46.0031835,40.901326999999995,40.901326999999995,41.074272,40.296022,38.480107000000004,38.739522,40.2960205,41.5066295,40.382490000000004,38.480104,37.7883275,36.9236075,39.171885,39.777189,40.209548999999996,40.036604,39.431299,40.036605,39.171882,40.814852,41.07427,38.220690000000005,38.998940000000005,41.160743499999995,41.0742705,43.4090195,43.063131,39.085411,38.8259945,39.777189,40.296022,41.42016,41.679577,39.777187,38.134214,46.694959,47.646155,40.036604999999994,39.950131999999996,38.393634500000005,37.2694975,36.837136,38.566578,40.382493999999994,42.890187,41.679575,41.07427,42.198409999999996,39.171884,41.0742705,40.814854,38.653049499999995,39.690715499999996,39.085411,40.4689635,40.72838,38.566578,38.047744,38.739522,39.258356,40.036606,41.852522,40.555437,39.085409,38.912465999999995,37.961274,38.047745,38.998937,39.258354,40.987797,41.333687,40.728384,37.8748035,36.0588855,39.431298999999996,47.7326275,45.657295000000005,38.8259945,46.435545,46.6949615,40.2095485,37.874801000000005,37.010081,38.047745,38.998937,39.690715499999996,38.8259935,38.480105,39.171882,40.901323000000005,40.987796,38.30716,39.171882,39.777187,39.4312985,39.1718825,37.874799,38.8259935,38.739523500000004,38.047745,37.8748,35.885943,35.712998999999996,38.825996,39.950131999999996,38.3071595,37.701855,39.0854095,41.766047,41.07427,39.171882,38.566576,37.961271499999995,38.3071615,39.517770999999996,37.615385,34.675332999999995,38.047747,42.284881999999996,41.5931035,40.6419105,40.468967,38.9124675,38.5665775,38.825994,38.825994,39.6042435,39.4312985,39.344826999999995,38.739522,38.134215,38.73952,38.307159,37.701854,39.258355,41.3336885,39.431301000000005,38.7395225,39.085409,39.7771875,40.6419105,39.8636605,40.5554385,39.085412500000004,37.9612725,40.0366035,42.2848815,41.07427,39.863659};
        final double mean = new Mean().evaluate(values);
        final double stdDev = new StandardDeviation().evaluate(values, mean);
        final double[] noiseFreeValues = SmoothSample.smudgeNoise(values, mean, stdDev, SmoothSample.DEFAULT_NOISE_LENGTH_TOLERANCE);
        final double[] smoothedValues = SmoothSample.smooth(noiseFreeValues, SmoothSample.DEFAULT_MOVING_AVERAGE_WINDOW_SIZE_FOR_HIGH_RESOLUTION);
        assertThat(smoothedValues, equalTo(expectedSmoothedValues));
    }
}
