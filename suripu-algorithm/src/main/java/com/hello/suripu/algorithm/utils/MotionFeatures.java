package com.hello.suripu.algorithm.utils;

import com.hello.suripu.algorithm.core.AmplitudeData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
        DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE,
        DENSITY_INCREASE_FORWARD_MAX_AMPLITUDE,
        MAX_NO_MOTION_PERIOD,
        MAX_MOTION_PERIOD
    }

    public static Map<FeatureType, List<AmplitudeData>> aggregateData(final Map<FeatureType, List<AmplitudeData>> rawFeatures, final int windowSize){
        final LinkedList<AmplitudeData> window = new LinkedList<>();
        final LinkedHashMap<FeatureType, List<AmplitudeData>> aggregatedData = new LinkedHashMap<>();

        for(final FeatureType featureType:rawFeatures.keySet()){
            final List<AmplitudeData> alignedData = rawFeatures.get(featureType);
            window.clear();
            final LinkedList<AmplitudeData> aggregatedDimension = new LinkedList<>();
            aggregatedData.put(featureType, aggregatedDimension);

            for(final AmplitudeData amplitudeData:alignedData){
                window.add(amplitudeData);

                if(window.getLast().timestamp - window.getFirst().timestamp < windowSize * DateTimeConstants.MILLIS_PER_MINUTE){
                    continue;
                }

                final long timestamp = window.getLast().timestamp;
                final int offsetMillis = window.getLast().offsetMillis;
                switch (featureType){
                    case MAX_AMPLITUDE:
                    case DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE:
                    case DENSITY_INCREASE_FORWARD_MAX_AMPLITUDE:
                    case MAX_NO_MOTION_PERIOD:
                    case MAX_MOTION_PERIOD:
                        final double aggregatedMaxAmplitude = NumericalUtils.getMaxAmplitude(window);
                        aggregatedDimension.add(new AmplitudeData(timestamp, aggregatedMaxAmplitude, offsetMillis));
                        break;
                }
                window.clear();
            }

            if(window.size() > 0){
                final long timestamp = window.getLast().timestamp;
                final int offsetMillis = window.getLast().offsetMillis;
                switch (featureType){
                    case MAX_AMPLITUDE:
                    case DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE:
                    case DENSITY_INCREASE_FORWARD_MAX_AMPLITUDE:
                    case MAX_NO_MOTION_PERIOD:
                    case MAX_MOTION_PERIOD:
                        final double aggregatedMaxAmplitude = NumericalUtils.getMaxAmplitude(window);
                        aggregatedDimension.add(new AmplitudeData(timestamp, aggregatedMaxAmplitude, offsetMillis));
                        break;
                }
            }
        }

        return aggregatedData;
    }

    public static Map<FeatureType, List<AmplitudeData>> generateTimestampAlignedFeatures(final List<AmplitudeData> rawData, final int windowSizeInMinute){
        final LinkedList<AmplitudeData> densityWindow = new LinkedList<>();
        final LinkedList<AmplitudeData> backTrackAmpWindow = new LinkedList<>();
        final LinkedList<AmplitudeData> forwardAmpWindow = new LinkedList<>();

        LinkedList<AmplitudeData> densityBuffer1 = new LinkedList<>();
        LinkedList<AmplitudeData> densityBuffer2 = new LinkedList<>();

        final HashMap<FeatureType, List<AmplitudeData>> features = new HashMap<>();

        int densityCount = 0;
        int maxNoMotionPeriodCount = 0;
        int maxMotionPeriodCount = 0;
        
        for(final AmplitudeData datum:rawData){
            densityWindow.add(datum);
            if(datum.amplitude > 0){
                densityCount++;
            }

            if(densityWindow.size() > windowSizeInMinute){
                if(densityWindow.getFirst().amplitude > 0){
                    densityCount--;
                }
                backTrackAmpWindow.add(densityWindow.removeFirst());
                if(backTrackAmpWindow.size() > windowSizeInMinute){
                    backTrackAmpWindow.removeFirst();
                }
            }


            if(densityBuffer1.size() < windowSizeInMinute){
                densityBuffer1.add(new AmplitudeData(densityWindow.getLast().timestamp, densityCount, densityWindow.getLast().offsetMillis));
                continue;
            }

            densityBuffer2.add(new AmplitudeData(densityWindow.getLast().timestamp, densityCount, densityWindow.getLast().offsetMillis));

            forwardAmpWindow.add(datum);
            if(forwardAmpWindow.size() > windowSizeInMinute){
                forwardAmpWindow.removeFirst();
            }

            if(densityBuffer2.size() == windowSizeInMinute){
                // Compute density decade feature
                final double densityMax1 = NumericalUtils.getMaxAmplitude(densityBuffer1);
                final double densityMax2 = NumericalUtils.getMaxAmplitude(densityBuffer2);

                final double densityDrop = densityMax1 - densityMax2;
                final double densityIncrease = densityMax2 - densityMax1;

                // compute aggregated max motion backtrack amplitude feature.
                final double maxBackTrackAmplitude = NumericalUtils.getMaxAmplitude(backTrackAmpWindow);
                final double maxForwardAmplitude = NumericalUtils.getMaxAmplitude(forwardAmpWindow);

                final long timestamp = backTrackAmpWindow.getLast().timestamp;
                final int offsetMillis = backTrackAmpWindow.getLast().offsetMillis;

                LOGGER.debug("{}, delta: {}, max_amp: {}",
                        new DateTime(timestamp, DateTimeZone.forOffsetMillis(offsetMillis)),
                        densityDrop,
                        maxBackTrackAmplitude);

                if(!features.containsKey(FeatureType.MAX_AMPLITUDE)){
                    features.put(FeatureType.MAX_AMPLITUDE, new LinkedList<AmplitudeData>());
                }

                if(!features.containsKey(FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE)){
                    features.put(FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE, new LinkedList<AmplitudeData>());
                }

                if(!features.containsKey(FeatureType.DENSITY_INCREASE_FORWARD_MAX_AMPLITUDE)){
                    features.put(FeatureType.DENSITY_INCREASE_FORWARD_MAX_AMPLITUDE, new LinkedList<AmplitudeData>());
                }

                if(!features.containsKey(FeatureType.MAX_MOTION_PERIOD)){
                    features.put(FeatureType.MAX_MOTION_PERIOD, new LinkedList<AmplitudeData>());
                }

                if(!features.containsKey(FeatureType.MAX_NO_MOTION_PERIOD)){
                    features.put(FeatureType.MAX_NO_MOTION_PERIOD, new LinkedList<AmplitudeData>());
                }

                features.get(FeatureType.MAX_AMPLITUDE).add(new AmplitudeData(timestamp, maxBackTrackAmplitude, offsetMillis));

                final double combinedBackward = maxBackTrackAmplitude * densityDrop * (1d + maxMotionPeriodCount);
                features.get(FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE).add(new AmplitudeData(timestamp, combinedBackward, offsetMillis));

                final double combinedForward = maxForwardAmplitude * densityIncrease * 1d / (1d + maxNoMotionPeriodCount);
                features.get(FeatureType.DENSITY_INCREASE_FORWARD_MAX_AMPLITUDE).add(new AmplitudeData(timestamp, combinedForward, offsetMillis));

                features.get(FeatureType.MAX_MOTION_PERIOD).add(new AmplitudeData(timestamp, maxMotionPeriodCount, offsetMillis));
                features.get(FeatureType.MAX_NO_MOTION_PERIOD).add(new AmplitudeData(timestamp, maxNoMotionPeriodCount, offsetMillis));

                if(densityMax1 == 0){
                    maxMotionPeriodCount = 0;
                    maxNoMotionPeriodCount++;
                }else{
                    maxMotionPeriodCount++;
                    maxNoMotionPeriodCount = 0;
                }

                densityBuffer1.add(densityBuffer2.removeFirst());
                densityBuffer1.removeFirst();


            }

        }

        return features;
    }

}
