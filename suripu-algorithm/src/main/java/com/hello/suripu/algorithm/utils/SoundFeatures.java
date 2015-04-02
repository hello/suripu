package com.hello.suripu.algorithm.utils;

import com.hello.suripu.algorithm.core.AmplitudeData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 3/30/15.
 */
public class SoundFeatures {
    private static final int NOISE_THRESHOLD = 40;

    public static List<AmplitudeData> getDensityFeature(final List<AmplitudeData> alignedSoundPerMinute, final int aggregateWindowInMinute){
        final LinkedList<AmplitudeData> aggregateWindow = new LinkedList<>();
        final List<AmplitudeData> result = new ArrayList<>();

        int count = 0;
        for(int i = 0; i < alignedSoundPerMinute.size(); i++){
            final AmplitudeData datum = alignedSoundPerMinute.get(i);
            aggregateWindow.add(datum);
            if(datum.amplitude >= NOISE_THRESHOLD){
                count++;
            }

            if(aggregateWindow.size() > aggregateWindowInMinute){
                if(aggregateWindow.getFirst().amplitude >= NOISE_THRESHOLD){
                    count--;
                }
                aggregateWindow.removeFirst();
            }
            result.add(new AmplitudeData(datum.timestamp, count, datum.offsetMillis));
        }
        return result;
    }
}
