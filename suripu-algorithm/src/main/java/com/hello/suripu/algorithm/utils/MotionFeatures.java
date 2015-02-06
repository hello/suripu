package com.hello.suripu.algorithm.utils;

import com.google.common.collect.Ordering;
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
    public static final int MOTION_AGGREGATE_WINDOW_IN_MINUTES = 10;
    public static final int WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES = 15;

    public enum FeatureType{
        MAX_AMPLITUDE,
        DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE,
        @Deprecated
        DENSITY_INCREASE_FORWARD_MAX_AMPLITUDE,
        @Deprecated
        MAX_NO_MOTION_PERIOD,
        MAX_MOTION_PERIOD,
        DENSITY_BACKWARD_AVERAGE_AMPLITUDE
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

                final long timestamp = (window.getLast().timestamp  - window.getFirst().timestamp) / 2 + window.getFirst().timestamp;
                final int offsetMillis = window.getLast().offsetMillis;
                switch (featureType){
                    case MAX_AMPLITUDE:
                    case DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE:
                    case DENSITY_INCREASE_FORWARD_MAX_AMPLITUDE:
                    case MAX_NO_MOTION_PERIOD:
                    case MAX_MOTION_PERIOD:
                    case DENSITY_BACKWARD_AVERAGE_AMPLITUDE:
                        final double aggregatedMaxAmplitude = Ordering.natural().max(window).amplitude;
                        aggregatedDimension.add(new AmplitudeData(timestamp, aggregatedMaxAmplitude, offsetMillis));
                        break;
                }
                window.clear();
            }

            if(window.size() > 0){
                final long timestamp = (window.getLast().timestamp  - window.getFirst().timestamp) / 2 + window.getFirst().timestamp;
                final int offsetMillis = window.getLast().offsetMillis;
                switch (featureType){
                    case MAX_AMPLITUDE:
                    case DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE:
                    case DENSITY_INCREASE_FORWARD_MAX_AMPLITUDE:
                    case MAX_NO_MOTION_PERIOD:
                    case MAX_MOTION_PERIOD:
                    case DENSITY_BACKWARD_AVERAGE_AMPLITUDE:
                        final double aggregatedMaxAmplitude = Ordering.natural().max(window).amplitude;
                        aggregatedDimension.add(new AmplitudeData(timestamp, aggregatedMaxAmplitude, offsetMillis));
                        break;
                }
            }
        }

        return aggregatedData;
    }

    public static Map<FeatureType, List<AmplitudeData>> generateTimestampAlignedFeatures(final List<AmplitudeData> rawData,
                                                                                         final int sleepDetectionWindowSizeInMinute,
                                                                                         final int awakeDetectionWindowSizeInMinute,
                                                                                         final boolean debugMode){
        final LinkedList<AmplitudeData> densityWindow = new LinkedList<>();
        final LinkedList<AmplitudeData> backTrackAmpWindow = new LinkedList<>();
        final LinkedList<AmplitudeData> wakeUpAggregateWindow = new LinkedList<>();
        final LinkedList<AmplitudeData> forwardAmpWindow = new LinkedList<>();

        LinkedList<AmplitudeData> densityBuffer1 = new LinkedList<>();
        LinkedList<AmplitudeData> densityBuffer2 = new LinkedList<>();

        final HashMap<FeatureType, List<AmplitudeData>> features = new HashMap<>();

        int densityCount = 0;
        int wakeUpAggregateDensity = 0;
        int maxNoMotionPeriodCount = 0;
        int maxMotionPeriodCount = 0;
        
        for(final AmplitudeData datum:rawData){
            densityWindow.add(datum);
            wakeUpAggregateWindow.add(datum);

            if(datum.amplitude > 0){
                densityCount++;
                wakeUpAggregateDensity++;
            }

            if(densityWindow.size() > sleepDetectionWindowSizeInMinute){
                if(densityWindow.getFirst().amplitude > 0){
                    densityCount--;
                }
                backTrackAmpWindow.add(densityWindow.removeFirst());
                if(backTrackAmpWindow.size() > sleepDetectionWindowSizeInMinute){
                    backTrackAmpWindow.removeFirst();
                }
            }

            if(wakeUpAggregateWindow.size() > awakeDetectionWindowSizeInMinute){
                if(wakeUpAggregateWindow.getFirst().amplitude > 0){
                    wakeUpAggregateDensity--;
                }
                wakeUpAggregateWindow.removeFirst();
            }


            if(densityBuffer1.size() < sleepDetectionWindowSizeInMinute){
                densityBuffer1.add(new AmplitudeData(densityWindow.getLast().timestamp, densityCount, densityWindow.getLast().offsetMillis));
                continue;
            }

            densityBuffer2.add(new AmplitudeData(densityWindow.getLast().timestamp, densityCount, densityWindow.getLast().offsetMillis));

            forwardAmpWindow.add(datum);
            if(forwardAmpWindow.size() > sleepDetectionWindowSizeInMinute){
                forwardAmpWindow.removeFirst();
            }

            if(densityBuffer2.size() == sleepDetectionWindowSizeInMinute){
                // Compute density decade feature
                final double densityMax1 = Ordering.natural().max(densityBuffer1).amplitude;
                final double densityMax2 = Ordering.natural().max(densityBuffer2).amplitude;

                final double densityDrop = densityMax1 - densityMax2;
                final double densityIncrease = densityMax2 - densityMax1;

                // compute aggregated max motion backtrack amplitude feature.
                final double maxBackTrackAmplitude = Ordering.natural().max(backTrackAmpWindow).amplitude;
                final double maxForwardAmplitude = Ordering.natural().max(forwardAmpWindow).amplitude;

                final long timestamp = backTrackAmpWindow.getLast().timestamp;
                final int offsetMillis = backTrackAmpWindow.getLast().offsetMillis;

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

                if(!features.containsKey(FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE)){
                    features.put(FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE, new LinkedList<AmplitudeData>());
                }

                features.get(FeatureType.MAX_AMPLITUDE).add(new AmplitudeData(timestamp, maxBackTrackAmplitude, offsetMillis));

                final double combinedBackward = maxBackTrackAmplitude * densityDrop * Math.pow((1d + maxMotionPeriodCount), 3);
                features.get(FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE).add(new AmplitudeData(timestamp, combinedBackward, offsetMillis));

                final double combinedForward = maxForwardAmplitude * densityIncrease * 1d / Math.pow((1d + maxNoMotionPeriodCount), 3);
                features.get(FeatureType.DENSITY_INCREASE_FORWARD_MAX_AMPLITUDE).add(new AmplitudeData(timestamp, combinedForward, offsetMillis));

                final double wakeUpFeature = wakeUpAggregateDensity * NumericalUtils.mean(wakeUpAggregateWindow);
                features.get(FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE).add(new AmplitudeData(timestamp, wakeUpFeature, offsetMillis));

                features.get(FeatureType.MAX_MOTION_PERIOD).add(new AmplitudeData(timestamp, maxMotionPeriodCount, offsetMillis));
                features.get(FeatureType.MAX_NO_MOTION_PERIOD).add(new AmplitudeData(timestamp, maxNoMotionPeriodCount, offsetMillis));

                if(debugMode) {
                    LOGGER.debug("{}, max_amp: {}, drop: {}, increase: {}, comb_bkwd: {}, comb_frwd: {}, last: {}, z_last {}, max1: {}, max2: {}",
                            new DateTime(timestamp, DateTimeZone.forOffsetMillis(offsetMillis)),
                            maxBackTrackAmplitude,
                            densityDrop,
                            densityIncrease,
                            combinedBackward,
                            combinedForward,
                            maxMotionPeriodCount,
                            maxNoMotionPeriodCount,
                            densityMax1,
                            densityMax2);
                }

                if(densityMax1 < sleepDetectionWindowSizeInMinute / 3){
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
