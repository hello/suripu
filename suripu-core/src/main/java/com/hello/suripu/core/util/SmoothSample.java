package com.hello.suripu.core.util;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Sample;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.util.Arrays;
import java.util.List;

public class SmoothSample {

    public static int DEFAULT_MOVING_AVERAGE_WINDOW_SIZE = 3;
    public static int DEFAULT_FORWARD_ATTEMPTS_TO_REMOVE_NOISE = 2;

    public static List<Sample> convert(final List<Sample> samples) {
        if (samples.size() <= Math.max(DEFAULT_FORWARD_ATTEMPTS_TO_REMOVE_NOISE, DEFAULT_MOVING_AVERAGE_WINDOW_SIZE)) {
            return samples;
        }

        final double[] values = getSampleValuesArray(samples);
        final double mean = new Mean().evaluate(values);
        final double stdDev = new StandardDeviation().evaluate(values, mean);
        
        
        final double[] noiseFreeValues = replaceNoiseWithAverageOfSurroundings(values, mean, stdDev, DEFAULT_FORWARD_ATTEMPTS_TO_REMOVE_NOISE);
        
        final double[] smoothedValues = smooth(noiseFreeValues, DEFAULT_MOVING_AVERAGE_WINDOW_SIZE);

        final List<Sample> convertedSamples = Lists.newArrayList();
        for (int i=0; i < samples.size(); i++) {
            convertedSamples.add(new Sample(samples.get(i).dateTime, (float) smoothedValues[i], samples.get(i).offsetMillis));
        }
        return convertedSamples;
    }


    public static double[] replaceNoiseWithAverageOfSurroundings(final double[] values, final double mean, final double stdDev, final int forwardAttemptsToRemoveNoise) {
        double[] noiseFreeValues = new double[values.length];

        if (isNoise(values[0], mean, stdDev)) {
            noiseFreeValues[0] = mean;
        }
        else {
            noiseFreeValues[0] = values[0];
        }

        for (int i = 1; i < values.length - forwardAttemptsToRemoveNoise; i++) {
            if (!isNoise(values[i], mean, stdDev)) {
                noiseFreeValues[i] = values[i];
                continue;
            }
            // replace noise value by average of surrounding good values
            for (int j = 1; j <= forwardAttemptsToRemoveNoise; j++) {
                if (!isNoise(values[i + j], mean, stdDev)) {
                    noiseFreeValues[i] = 0.5 * (noiseFreeValues[i-1] + values[i + j]);
                    break;
                }
                else {
                    noiseFreeValues[i] = values[i];
                }
            }
        }

        // replace edge noises with average of past values
        for (int k = values.length - forwardAttemptsToRemoveNoise; k < values.length; k++) {
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
            final double movingAverage = new Mean().evaluate(Arrays.copyOfRange(values, i - movingAverageWindowSize + 1, i));
            smoothedValues[i] = movingAverage;
        }

        // for first points which do not have enough precded points for a window
        for (int k = 0; k < movingAverageWindowSize - 1; k++) {
            smoothedValues[k] = 0.5 * (values[k] + smoothedValues[movingAverageWindowSize -1]);
        }
        return smoothedValues;
    }

    public static Boolean isNoise(final double value, final double mean, final double stdDev) {
        return (value  > mean + 2 * stdDev) || (value < mean - 2 * stdDev);
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
