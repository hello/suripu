package com.hello.suripu.algorithm.utils;

import com.hello.suripu.algorithm.core.AmplitudeData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 1/15/15.
 */
public class MotionFeatures {
    private final static Logger LOGGER = LoggerFactory.getLogger(MotionFeatures.class);
    public enum FeatureType{
        MAX_AMPLITUDE,
        DENSITY_DECADE_BACKTRACK_MAX_AMPLITUDE
    }
    public static Map<FeatureType, List<AmplitudeData>> generateTimestampAlignedFeatures(final List<AmplitudeData> rawData, final int windowSizeInMinute){
        final LinkedList<AmplitudeData> densityWindow = new LinkedList<>();
        final LinkedList<AmplitudeData> ampWindow = new LinkedList<>();

        LinkedList<AmplitudeData> densityBuffer1 = new LinkedList<>();
        LinkedList<AmplitudeData> densityBuffer2 = new LinkedList<>();

        final HashMap<FeatureType, List<AmplitudeData>> features = new HashMap<>();

        int densityMax1 = 0;
        int densityMax2 = 0;

        int densityCount = 0;

        int i = 0;
        for(final AmplitudeData datum:rawData){
            densityWindow.add(datum);
            if(datum.amplitude > 0){
                densityCount++;
            }

            if(densityWindow.size() > windowSizeInMinute){
                if(densityWindow.getFirst().amplitude > 0){
                    densityCount--;
                }
                ampWindow.add(densityWindow.removeFirst());
                if(ampWindow.size() > windowSizeInMinute){
                    ampWindow.removeFirst();
                }
            }


            if(densityBuffer1.size() < windowSizeInMinute){
                densityBuffer1.add(new AmplitudeData(densityWindow.getLast().timestamp, densityCount, densityWindow.getLast().offsetMillis));
                if(densityMax1 < densityCount){
                    densityMax1 = densityCount;
                }
                i++;
                continue;
            }

            if(densityBuffer2.size() < windowSizeInMinute){
                densityBuffer2.add(new AmplitudeData(densityWindow.getLast().timestamp, densityCount, densityWindow.getLast().offsetMillis));
                if(densityMax2 < densityCount){
                    densityMax2 = densityCount;
                }

                i++;
                continue;
            }

            // Compute density decade feature
            final double densityDecade = densityMax1 - densityMax2;

            // compute aggregated max motion backtrack amplitude feature.
            final double maxBackTrackAmplitude = NumericalUtils.getMaxAmplitude(ampWindow);

            final long timestamp = ampWindow.getLast().timestamp;
            final int offsetMillis = ampWindow.getLast().offsetMillis;

            LOGGER.debug("{}, delta: {}, max_amp: {}",
                    new DateTime(timestamp, DateTimeZone.forOffsetMillis(offsetMillis)),
                    densityDecade,
                    maxBackTrackAmplitude);

            if(!features.containsKey(FeatureType.MAX_AMPLITUDE)){
                features.put(FeatureType.MAX_AMPLITUDE, new LinkedList<AmplitudeData>());
            }

            if(!features.containsKey(FeatureType.DENSITY_DECADE_BACKTRACK_MAX_AMPLITUDE)){
                features.put(FeatureType.DENSITY_DECADE_BACKTRACK_MAX_AMPLITUDE, new LinkedList<AmplitudeData>());
            }

            features.get(FeatureType.MAX_AMPLITUDE).add(new AmplitudeData(timestamp, maxBackTrackAmplitude, offsetMillis));
            features.get(FeatureType.DENSITY_DECADE_BACKTRACK_MAX_AMPLITUDE).add(new AmplitudeData(timestamp, maxBackTrackAmplitude * densityDecade, offsetMillis));

            densityBuffer1 = densityBuffer2;
            densityBuffer2 = new LinkedList<>();
            densityMax1 = densityMax2;
            densityMax2 = 0;

            i++;
        }

        return features;
    }

}
