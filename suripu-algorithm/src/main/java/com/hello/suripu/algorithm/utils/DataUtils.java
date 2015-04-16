package com.hello.suripu.algorithm.utils;

import com.google.common.collect.Ordering;
import com.hello.suripu.algorithm.core.AmplitudeData;
import org.joda.time.DateTimeConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by pangwu on 3/23/15.
 */
public class DataUtils {

    public static List<AmplitudeData> dedupe(final List<AmplitudeData> amplitudeData){
        final List<AmplitudeData> result = new ArrayList<>();
        AmplitudeData lastData = null;
        for(final AmplitudeData datum:amplitudeData){
            if(lastData == null){
                lastData = datum;
                result.add(datum);
                continue;
            }

            if(datum.timestamp == lastData.timestamp){
                continue;
            }
            result.add(datum);
            lastData = datum;

        }
        return result;
    }

    public static List<AmplitudeData> getPositive(final List<AmplitudeData> amplitudeData){
        final List<AmplitudeData> result = new ArrayList<>();
        for(final AmplitudeData datum:amplitudeData){
            if(datum.amplitude <= 0){
                continue;
            }
            result.add(datum);
        }
        return result;
    }

    public static List<AmplitudeData> makePositive(final List<AmplitudeData> rawData){
        final List<AmplitudeData> result = new ArrayList<>();
        final double minAmplitude = Ordering.natural().min(rawData).amplitude;
        for(final AmplitudeData motion: rawData) {
            result.add(new AmplitudeData(motion.timestamp,
                    motion.amplitude - minAmplitude,  // DONOT filter out the negative values, they are just off calibration!
                    motion.offsetMillis));
        }

        return result;
    }

    public static List<AmplitudeData> fillMissingValues(final List<AmplitudeData> rawData, final int intervalMillis){

        final List<AmplitudeData> result = new ArrayList<>();
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
                    motion.amplitude,
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

    public static List<AmplitudeData> insertEmptyData(final List<AmplitudeData> data, final int insertCount, final int intervalMinute){
        if(data.size() == 0){
            return Collections.EMPTY_LIST;
        }

        final List<AmplitudeData> result = new ArrayList<>();
        final long firstTimestamp = data.get(0).timestamp;
        final int timeZoneOffsetMillis = data.get(0).offsetMillis;

        for(int i = 0; i < insertCount; i++){
            result.add(0, new AmplitudeData(firstTimestamp - (i + intervalMinute) * DateTimeConstants.MILLIS_PER_MINUTE, 0, timeZoneOffsetMillis));
        }
        result.addAll(data);
        return result;
    }

    public static long findNearestDataTime(final List<AmplitudeData> data, final long targetMillis){
        int minDiff = Integer.MAX_VALUE;
        long time = 0;
        for(int i = 0; i < data.size(); i++){
            if(data.get(i).amplitude == 0){
                continue;
            }

            final int diff = (int) Math.abs(data.get(i).timestamp - targetMillis);
            if(diff < minDiff){
                minDiff = diff;
                time = data.get(i).timestamp;
            }
        }

        if(minDiff == Integer.MAX_VALUE){
            return targetMillis;
        }

        /*if(Math.abs(time - targetMillis) > 15 * DateTimeConstants.MILLIS_PER_MINUTE){
            return targetMillis;
        }*/
        return time;
    }
}
