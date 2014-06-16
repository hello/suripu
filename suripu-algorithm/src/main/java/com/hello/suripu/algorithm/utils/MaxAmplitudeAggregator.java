package com.hello.suripu.algorithm.utils;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.AmplitudeDataPreprocessor;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 6/11/14.
 */
public class MaxAmplitudeAggregator implements AmplitudeDataPreprocessor {

    private final int smoothWindowMillis;

    public MaxAmplitudeAggregator(final int smoothWindowMillis){
        this.smoothWindowMillis = smoothWindowMillis;
    }

    @Override
    public ImmutableList<AmplitudeData> process(final List<AmplitudeData> rawData) {
        final LinkedList<AmplitudeData> buffer = new LinkedList<AmplitudeData>();
        final LinkedList<AmplitudeData> smoothedData = new LinkedList<AmplitudeData>();

        for (final AmplitudeData datum:rawData) {
            final AmplitudeData amplitudeData = new AmplitudeData(datum.timestamp, datum.amplitude, datum.offsetMillis);
            if (buffer.size() > 0) {
                if (datum.timestamp - buffer.getFirst().timestamp >= this.smoothWindowMillis) {
                    final AmplitudeData data = new AmplitudeData(buffer.getFirst().timestamp, NumericalUtils.getMaxAmplitude(buffer), buffer.getFirst().offsetMillis);
                    buffer.clear();

                    smoothedData.add(data);
                }
            }

            buffer.add(amplitudeData);

        }

        return ImmutableList.copyOf(smoothedData);
    }
}
