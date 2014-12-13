package com.hello.suripu.algorithm.utils;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.AmplitudeDataPreprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by kingshy on 12/10/14.
 */
public class GaussianSmoother implements AmplitudeDataPreprocessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GaussianSmoother.class);

    private final int windowSize;
    private final double [] weights;
    private final double weightsTotal;

    public GaussianSmoother(final int degree) {
        this.windowSize = degree * 2 - 1;
        this.weights = new double[this.windowSize];
        double total = 0.0;
        for (int i = 0; i < this.windowSize; i++) {
            final double index = (double) (i - degree + 1);
            final double gauss = Math.pow((4.0 * (index / (double) this.windowSize)), 2);
            final double gaussianWeight = 1.0 / Math.exp(gauss);
            weights[i] = gaussianWeight;
            total += gaussianWeight;
        }
        this.weightsTotal = total;
    }

    @Override
    public ImmutableList<AmplitudeData> process(final List<AmplitudeData> rawData) {
        final LinkedList<AmplitudeData> smoothedData = new LinkedList<>();

        // pad beginning with 0.0 values
        int startIndex = 0;
        for (int i = 0; i < this.windowSize/2; i++) {
            final AmplitudeData datum = rawData.get(i);
            smoothedData.add(new AmplitudeData(datum.timestamp, 0.0, datum.offsetMillis));
            startIndex++;
        }

        final int smoothedSize = rawData.size() - this.windowSize;
        for (int i = 0; i < smoothedSize; i++) {
            double totalValues = 0.0;
            for (int j = 0; j < this.windowSize; j++) {
                final AmplitudeData datum = rawData.get(i + j);
                final double smoothedValue = datum.amplitude * this.weights[j];
                totalValues += smoothedValue;
            }

            final AmplitudeData datum = rawData.get(i + startIndex);
            smoothedData.add(new AmplitudeData(datum.timestamp, totalValues/this.weightsTotal, datum.offsetMillis));
        }

        // pad endings
        for (int i = 0; i < (this.windowSize - this.windowSize/2); i++) {
            final int index = smoothedSize + i;
            final AmplitudeData datum = rawData.get(index);
            smoothedData.add(new AmplitudeData(datum.timestamp, 0.0, datum.offsetMillis));
        }
        return ImmutableList.copyOf(smoothedData);
    }
}
