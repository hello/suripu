package com.hello.suripu.algorithm.utils;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 6/10/14.
 */
public class NumericalUtils {
    public static ImmutableList<AmplitudeData> roofDataByAverage(final List<AmplitudeData> data){

        double average = mean(data);
        final LinkedList<AmplitudeData> list = new LinkedList<AmplitudeData>();

        for(final AmplitudeData datum:data){
            if(datum.amplitude > average){
                final AmplitudeData newData = new AmplitudeData(datum.timestamp, average, datum.offsetMillis);
                list.add(newData);
            }else{
                list.add(datum);
            }
        }

        return ImmutableList.copyOf(list);

    }

    public static ImmutableList<AmplitudeData> zeroDataUnderAverage(final List<AmplitudeData> data){

        double average = mean(data);
        final LinkedList<AmplitudeData> list = new LinkedList<AmplitudeData>();

        for(final AmplitudeData datum:data){
            if(datum.amplitude <= average){
                AmplitudeData newData = new AmplitudeData(datum.timestamp, 0d, datum.offsetMillis);
                list.add(newData);
            }else{
                AmplitudeData newData = new AmplitudeData(datum.timestamp, average, datum.offsetMillis);
                list.add(newData);
            }
        }

        return ImmutableList.copyOf(list);

    }


    public static double mean(final List<AmplitudeData> data){

        double average = 0.0;
        for(final AmplitudeData datum:data){
            average += datum.amplitude;
        }

        return average / data.size();

    }

    public static double std(final List<AmplitudeData> data, final double mean){

        double std = 0.0;
        for(final AmplitudeData datum:data) {
            final double diff = Math.pow(datum.amplitude - mean, 2);
            std += diff;

        }

        if(data.size() - 1 > 0) {
            return Math.sqrt(std / (double)(data.size() - 1));
        }
        return 0d;

    }

    @Deprecated
    public static double getMaxAmplitude(final List<AmplitudeData> data){
        if(data.size() == 0){
            return -1;
        }
        double max = data.get(0).amplitude;
        for(final AmplitudeData datum:data){
            if(datum.amplitude > max) {
                max = datum.amplitude;
            }
        }

        return max;
    }

}
