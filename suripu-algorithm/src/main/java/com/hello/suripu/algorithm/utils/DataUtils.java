package com.hello.suripu.algorithm.utils;

import com.google.common.collect.Ordering;
import com.hello.suripu.algorithm.core.AmplitudeData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 3/23/15.
 */
public class DataUtils {
    public static List<AmplitudeData> fillMissingValuesAndMakePositive(final List<AmplitudeData> rawData, final int intervalMillis){

        final List<AmplitudeData> result = new ArrayList<>();
        final double minAmplitude = Ordering.natural().min(rawData).amplitude;
        for(final AmplitudeData motion: rawData) {
            if(result.size() > 0) {
                final AmplitudeData lastData = result.get(result.size() - 1);
                if (motion.timestamp - lastData.timestamp > intervalMillis) {
                    final List<AmplitudeData> gapData = fillMissingData(lastData.timestamp,
                            motion.timestamp,
                            intervalMillis,
                            0,
                            lastData.offsetMillis);
                    result.addAll(gapData);
                }
            }
            result.add(new AmplitudeData(motion.timestamp,
                    motion.amplitude - minAmplitude,  // DONOT filter out the negative values, they are just off calibration!
                    motion.offsetMillis));

        }

        return result;
    }

    /*
    * Insert gap with empty data.
     */
    private static List<AmplitudeData> fillMissingData(final long gapStartTimestamp, final long gapEndTimestamp,
                                                       final int dataIntervalMillis, final double defaultValue,
                                                       final int timezoneOffset) {
        final long gapInterval = gapEndTimestamp - gapStartTimestamp;
        int insertCount = (int)(gapInterval / dataIntervalMillis);
        if(gapInterval % dataIntervalMillis == 0){
            insertCount--;
        }

        final ArrayList<AmplitudeData> insertData = new ArrayList<>();
        for(int i = 0; i < insertCount; i++){
            insertData.add(new AmplitudeData(gapStartTimestamp + (i + 1) * dataIntervalMillis, defaultValue, timezoneOffset));
        }

        return insertData;

    }
}
