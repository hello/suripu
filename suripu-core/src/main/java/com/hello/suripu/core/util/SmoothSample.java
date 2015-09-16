package com.hello.suripu.core.util;

import com.hello.suripu.core.models.Sample;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.util.List;

public class SmoothSample {

    private static Integer DEFAULT_MOVING_AVERAGE_WINDOW_SIZE = 3;
    private static Integer DEFAULT_FORWARD_ATTEMPTS_TO_KILL_NOISE = 2;

    public static List<Sample> manipulateSamples(final List<Sample> samples) {
        if (samples.size() < Math.max(DEFAULT_FORWARD_ATTEMPTS_TO_KILL_NOISE, DEFAULT_MOVING_AVERAGE_WINDOW_SIZE)) {
            return samples;
        }
        final Float mean = getSamplesMean(samples);
        final Float stdDev = getSamplesStdDev(samples, mean);

        final List<Sample> noiseFreeSample = replaceNoiseWithAvergeOfSurroundings(samples, mean, stdDev, DEFAULT_FORWARD_ATTEMPTS_TO_KILL_NOISE);
        return replaceByMovingAverage(noiseFreeSample, DEFAULT_MOVING_AVERAGE_WINDOW_SIZE);
    }

    public static Float getSamplesMean(final List<Sample> samples) {
        final Mean meanGen = new Mean();
        return (float) meanGen.evaluate(getSampleValuesArray(samples)) ;
    }


    public static Float getSamplesStdDev(final List<Sample> samples, final Float mean) {
        final StandardDeviation standardDeviationGen = new StandardDeviation();
        return (float) standardDeviationGen.evaluate(getSampleValuesArray(samples), mean);
    }


    public static List<Sample> replaceNoiseWithAvergeOfSurroundings(final List<Sample> samples, final Float mean, final Float stdDev, final Integer forwardAttemptsToKillNoise) {
        List<Sample> noiseFreeSample = samples;
        for (int i = 1; i < samples.size() - forwardAttemptsToKillNoise; i++) {
            if (hasReasonableValue(samples.get(i), mean, stdDev)) {
                continue;
            }
            for (int j = 1; j <= forwardAttemptsToKillNoise; j++) {
                if (hasReasonableValue(samples.get(i + j), mean, stdDev)) {
                    final Sample updatedSample = new Sample(
                            samples.get(i).dateTime,
                            (noiseFreeSample.get(i-1).value + samples.get(i + j).value)/2,
                            samples.get(i).offsetMillis
                    );
                    noiseFreeSample.set(i, updatedSample);
                    break;
                }
            }
        }

        // replace edge noises with nearest non-noise value
        for (int k = samples.size() - forwardAttemptsToKillNoise; k < samples.size(); k++) {
            if (!hasReasonableValue(samples.get(k), mean, stdDev)) {
                final Sample updatedSample = new Sample(
                        samples.get(k).dateTime,
                        (noiseFreeSample.get(k-2).value + samples.get(k-1).value)/2,
                        samples.get(k).offsetMillis
                );
                noiseFreeSample.set(k, updatedSample);
                break;
            }
        }
        return noiseFreeSample;
    }

    public static List<Sample> replaceByMovingAverage(final List<Sample> samples, final Integer movingAverageWindowSize) {
        List<Sample> smoothedsamples = samples;
        for (int i = movingAverageWindowSize - 1; i < samples.size(); i++) {
            final Float movingAverage = getSamplesMean(samples.subList(i - movingAverageWindowSize + 1, i));
            final Sample updatedSample = new Sample(
                samples.get(i).dateTime,
                movingAverage,
                samples.get(i).offsetMillis
            );
            smoothedsamples.set(i, updatedSample);
        }

        for (int k = 0; k < movingAverageWindowSize - 1; k++) {
            final Sample updatedSample = new Sample(
                    samples.get(k).dateTime,
                    (samples.get(k).value + smoothedsamples.get(movingAverageWindowSize -1).value)/2,
                    samples.get(k).offsetMillis
            );
            smoothedsamples.set(k, updatedSample);
        }
        return samples;
    }

    public static Boolean hasReasonableValue(final Sample sample, final Float mean, final Float stdDev) {
        return (sample.value  <= mean + 2 * stdDev) && (sample.value >= mean - 2 * stdDev);
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
