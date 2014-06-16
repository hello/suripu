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

        double average = selectAverage(data);
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

        double average = selectAverage(data);
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


    public static double selectAverage(final List<AmplitudeData> data){

        double average = 0.0;
        for(final AmplitudeData datum:data){
            average += datum.amplitude;
        }

        return average / data.size();

    }


    public static double getMaxAmplitude(final List<AmplitudeData> data){
        double max = -1;
        for(final AmplitudeData datum:data){
            if(max == -1){
                max = datum.amplitude;
            }else{
                if(datum.amplitude > max){
                    max = datum.amplitude;
                }
            }
        }

        return max;
    }

}
