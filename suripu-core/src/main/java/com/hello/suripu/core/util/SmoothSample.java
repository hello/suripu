package com.hello.suripu.core.util;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Sample;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class SmoothSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmoothSample.class);
    public static final int DEFAULT_MOVING_AVERAGE_WINDOW_SIZE_FOR_LOW_RESOLUTION = 2;
    public static final int DEFAULT_MOVING_AVERAGE_WINDOW_SIZE_FOR_HIGH_RESOLUTION = 3;
    public static final int DEFAULT_NOISE_LENGTH_TOLERANCE = 2;
    private static final int DEVIATION_STEP = 2;
    private static final int MAXIMUM_NOISE_CORRECTIONS = 5;

    public static List<Sample> convert(final List<Sample> samples) {
        if (samples.size() <= Math.max(DEFAULT_NOISE_LENGTH_TOLERANCE, DEFAULT_MOVING_AVERAGE_WINDOW_SIZE_FOR_HIGH_RESOLUTION)) {
            return samples;
        }

        final double[] values = getSampleValuesArray(samples);
        final double mean = new Mean().evaluate(values);
        final double stdDev = new StandardDeviation().evaluate(values, mean);

        final int movingAvgWindowSize =  new Interval(samples.get(0).dateTime, samples.get(1).dateTime).toDuration().getStandardMinutes() > 5 ?
                DEFAULT_MOVING_AVERAGE_WINDOW_SIZE_FOR_LOW_RESOLUTION : DEFAULT_MOVING_AVERAGE_WINDOW_SIZE_FOR_HIGH_RESOLUTION;
        final double[] noiseFreeValues = smudgeNoise(values, mean, stdDev, DEFAULT_NOISE_LENGTH_TOLERANCE);

        final double[] smoothedValues = smooth(noiseFreeValues, movingAvgWindowSize);

        final List<Sample> convertedSamples = Lists.newArrayList();
        for (int i=0; i < samples.size(); i++) {
            convertedSamples.add(new Sample(samples.get(i).dateTime, (float) smoothedValues[i], samples.get(i).offsetMillis));
        }
        return convertedSamples;
    }


    public static double[] smudgeNoise(final double[] values, final double mean, final double stdDev, final int noiseLengthTolerance) {
        double[] noiseFreeValues = new double[values.length];

        if (isNoise(values[0], mean, stdDev)) {
            noiseFreeValues[0] = mean;
        }
        else {
            noiseFreeValues[0] = values[0];
        }

        // if noise size is greater than tolerance, keep the original values, otherwise try to replace it with average of surroundings
        for (int i = 1; i < values.length - noiseLengthTolerance; i++) {
            noiseFreeValues[i] = values[i];
            if (!isNoise(values[i], mean, stdDev)) {
                noiseFreeValues[i] = values[i];
                continue;
            }
            // replace noise value by average of surrounding good values
            for (int j = 1; j <= noiseLengthTolerance; j++) {
                if (isNoise(values[i + j], mean, stdDev)) {
                    continue;
                }
                double averageSurroundingValues = (noiseFreeValues[i - 1] + values[i + j]) / 2;
                int n = 0;
                while (isNoise(averageSurroundingValues, mean, stdDev) & n <= MAXIMUM_NOISE_CORRECTIONS) {
                    n++;
                    averageSurroundingValues = 0.75 * averageSurroundingValues + 0.25 * mean;
                }
                noiseFreeValues[i] = averageSurroundingValues;
            }
        }

        // replace edge noises with average of past values
        for (int k = values.length - noiseLengthTolerance; k < values.length; k++) {
            if (isNoise(values[k], mean, stdDev)) {
                noiseFreeValues[k] = (noiseFreeValues[k - 2] + noiseFreeValues[k - 1]) / 2;
            }
            else {
                noiseFreeValues[k] = values[k];
            }
        }
        return noiseFreeValues;
    }

    public static double[] smooth(final double[] values, final Integer movingAverageWindowSize) {
        double[] smoothedValues = new double[values.length];
        for (int i = movingAverageWindowSize - 1; i < values.length; i++) {
            final double movingAverage = new Mean().evaluate(Arrays.copyOfRange(values, i - movingAverageWindowSize + 1, i + 1));
            smoothedValues[i] = movingAverage;
        }

        // for first points which do not have enough preceded points for a window
        for (int k = 0; k < movingAverageWindowSize - 1; k++) {
            smoothedValues[k] = 0.5 * (values[k] + smoothedValues[movingAverageWindowSize -1]);
        }
        return smoothedValues;
    }

    public static Boolean isNoise(final double value, final double mean, final double stdDev) {
        return (value  > mean + DEVIATION_STEP * stdDev) || (value < mean - DEVIATION_STEP * stdDev);
    }

    public static double[] getSampleValuesArray(List<Sample> samples) {
        double[] sampleValuesArray = new double[samples.size()];
        int i = 0;

        for (final Sample sample : samples) {
            sampleValuesArray[i++] = sample.value;
        }
        return sampleValuesArray;
    }
}