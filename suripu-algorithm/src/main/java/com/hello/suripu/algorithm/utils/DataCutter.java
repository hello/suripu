package com.hello.suripu.algorithm.utils;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.AmplitudeDataPreprocessor;
import org.joda.time.DateTime;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 6/11/14.
 */
public class DataCutter implements AmplitudeDataPreprocessor {
    private final DateTime startTime;
    private final DateTime endTime;

    public DataCutter(final DateTime startTime, final DateTime endTime){
        this.startTime = startTime;
        this.endTime = endTime;

    }

    @Override
    public ImmutableList<AmplitudeData> process(final List<AmplitudeData> rawData) {
        final LinkedList<AmplitudeData> processedData = new LinkedList<AmplitudeData>();
        final long startLocalTimestamp = this.startTime.getMillis();
        final long endLocalTimestamp = this.endTime.getMillis();

        for(final AmplitudeData datum:rawData){
            if(datum.timestamp >= startLocalTimestamp && datum.timestamp <= endLocalTimestamp){
                processedData.add(datum);
            }
        }

        return ImmutableList.copyOf(processedData);
    }
}
