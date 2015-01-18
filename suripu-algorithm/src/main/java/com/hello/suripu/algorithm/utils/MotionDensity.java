package com.hello.suripu.algorithm.utils;

import com.hello.suripu.algorithm.core.AmplitudeData;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 1/15/15.
 */
public class MotionDensity {
    public static List<AmplitudeData> generateDensity(final List<AmplitudeData> data, final int windowSizeInMillis){
        final LinkedList<AmplitudeData> window = new LinkedList<>();
        final LinkedList<AmplitudeData> densities = new LinkedList<>();
        int densityCount = 0;
        for(final AmplitudeData datum:data){
            window.add(datum);
            if(datum.amplitude > 0){
                densityCount++;
            }

            if(window.getLast().timestamp - window.getFirst().timestamp > windowSizeInMillis){
                if(window.getFirst().amplitude > 0){
                    densityCount--;
                }
                window.removeFirst();
            }

            densities.add(new AmplitudeData(datum.timestamp, densityCount, datum.offsetMillis));
        }

        return densities;
    }

    public static List<AmplitudeData> densityDelta(final List<AmplitudeData> density){
        final LinkedList<AmplitudeData> deltas = new LinkedList<>();
        AmplitudeData previous  = null;
        for(final AmplitudeData datum:density){
            if(previous == null){
                previous = datum;
                continue;
            }

            deltas.add(new AmplitudeData(previous.timestamp, previous.amplitude - datum.amplitude, previous.offsetMillis));
            previous = datum;
        }
        deltas.add(new AmplitudeData(previous.timestamp, 0d, previous.offsetMillis));
        return deltas;
    }
}
