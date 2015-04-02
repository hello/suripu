package com.hello.suripu.algorithm.utils;

import com.google.common.collect.Ordering;
import com.hello.suripu.algorithm.core.AmplitudeData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
    public static final int ROOM_MAID_MOTION_DENSITY_THRESHOLD = 3;
    public static final int ROOM_MAID_AGGREGATION_WINDOW_IN_MINUTES = 60;

    public enum FeatureType{
        MAX_AMPLITUDE,
        DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE,
        @Deprecated
        DENSITY_INCREASE_FORWARD_MAX_AMPLITUDE,
        @Deprecated
        MAX_NO_MOTION_PERIOD,
        MAX_MOTION_PERIOD,
        MAX_KICKOFF_COUNT,
        DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
        HOURLY_MOTION_COUNT,
        MOTION_COUNT_20MIN,
        ZERO_TO_MAX_MOTION_COUNT_DURATION,
        AWAKE_BACKWARD_DENSITY,
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
                    case MAX_KICKOFF_COUNT:
                    case DENSITY_BACKWARD_AVERAGE_AMPLITUDE:
                    case ZERO_TO_MAX_MOTION_COUNT_DURATION:
                    case MOTION_COUNT_20MIN:
                        final double aggregatedMaxAmplitude = Ordering.natural().max(window).amplitude;
                        aggregatedDimension.add(new AmplitudeData(timestamp, aggregatedMaxAmplitude, offsetMillis));
                        break;
                    case HOURLY_MOTION_COUNT:
                    case AWAKE_BACKWARD_DENSITY:
                        aggregatedDimension.add(new AmplitudeData(timestamp, amplitudeData.amplitude, offsetMillis));
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
                    case MAX_KICKOFF_COUNT:
                    case DENSITY_BACKWARD_AVERAGE_AMPLITUDE:
                    case ZERO_TO_MAX_MOTION_COUNT_DURATION:
                    case MOTION_COUNT_20MIN:
                        final double aggregatedMaxAmplitude = Ordering.natural().max(window).amplitude;
                        aggregatedDimension.add(new AmplitudeData(timestamp, aggregatedMaxAmplitude, offsetMillis));
                        break;
                    case HOURLY_MOTION_COUNT:
                    case AWAKE_BACKWARD_DENSITY:
                        aggregatedDimension.add(new AmplitudeData(timestamp, window.getLast().amplitude, offsetMillis));
                        break;
                }
            }
        }

        return aggregatedData;
    }

    private static HashMap<FeatureType, List<AmplitudeData>> addAllFeatureKey(final HashMap<FeatureType, List<AmplitudeData>> features){
        if(!features.containsKey(FeatureType.MAX_AMPLITUDE)){
            features.put(FeatureType.MAX_AMPLITUDE, new LinkedList<AmplitudeData>());
        }

        if(!features.containsKey(FeatureType.AWAKE_BACKWARD_DENSITY)){
            features.put(FeatureType.AWAKE_BACKWARD_DENSITY, new LinkedList<AmplitudeData>());
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

        if(!features.containsKey(FeatureType.ZERO_TO_MAX_MOTION_COUNT_DURATION)){
            features.put(FeatureType.ZERO_TO_MAX_MOTION_COUNT_DURATION, new LinkedList<AmplitudeData>());
        }

        if(!features.containsKey(FeatureType.HOURLY_MOTION_COUNT)){
            features.put(FeatureType.HOURLY_MOTION_COUNT, new LinkedList<AmplitudeData>());
        }

        if(!features.containsKey(FeatureType.MOTION_COUNT_20MIN)){
            features.put(FeatureType.MOTION_COUNT_20MIN, new LinkedList<AmplitudeData>());
        }

        return features;
    }

    public static Map<FeatureType, List<AmplitudeData>> generateTimestampAlignedFeatures(final List<AmplitudeData> rawData,
                                                                                         final int sleepDetectionWindowSizeInMinute,
                                                                                         final int awakeDetectionWindowSizeInMinute,
                                                                                         final boolean debugMode){
        final LinkedList<AmplitudeData> densityWindow = new LinkedList<>();
        final LinkedList<AmplitudeData> backTrackAmpWindow = new LinkedList<>();
        final LinkedList<AmplitudeData> wakeUpAggregateWindow = new LinkedList<>();
        final LinkedList<AmplitudeData> forwardAmpWindow = new LinkedList<>();
        final LinkedList<AmplitudeData> hourlyAmpWindow = new LinkedList<>();
        final LinkedList<AmplitudeData> ampWindow20Min = new LinkedList<>();

        LinkedList<AmplitudeData> densityBuffer1 = new LinkedList<>();
        LinkedList<AmplitudeData> densityBuffer2 = new LinkedList<>();

        final HashMap<FeatureType, List<AmplitudeData>> features = new HashMap<>();
        addAllFeatureKey(features);

        int densityCount = 0;
        int wakeUpAggregateDensity = 0;
        int maxNoMotionPeriodCount = 0;
        int maxMotionPeriodCount = 0;

        int hourlyMotionCount = 0;
        int previousHourlyMotionCount = 0;
        int zeroToMaxDurationInMinute = 0;
        int stepCountInMinute = 0;
        int countIn20Minute = 0;
        
        for(final AmplitudeData datum:rawData){
            densityWindow.add(datum);
            wakeUpAggregateWindow.add(datum);
            hourlyAmpWindow.add(datum);
            ampWindow20Min.add(datum);

            if(datum.amplitude > 0){
                densityCount++;
                wakeUpAggregateDensity++;
                hourlyMotionCount++;
                countIn20Minute++;
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

            if(ampWindow20Min.size() > 20){
                if(ampWindow20Min.getFirst().amplitude > 0){
                    countIn20Minute--;
                }
                ampWindow20Min.removeFirst();
            }

            if(hourlyAmpWindow.size() > ROOM_MAID_AGGREGATION_WINDOW_IN_MINUTES){
                if(hourlyAmpWindow.getFirst().amplitude > 0){
                    hourlyMotionCount--;
                }
                hourlyAmpWindow.removeFirst();
                if(previousHourlyMotionCount > 0 && hourlyMotionCount == 0){
                    zeroToMaxDurationInMinute = 0;
                    stepCountInMinute = 0;
                }

                if(previousHourlyMotionCount == 0 && hourlyMotionCount > 0){
                    zeroToMaxDurationInMinute = 0;
                    stepCountInMinute = 0;
                }

                stepCountInMinute++;
                if(hourlyMotionCount > previousHourlyMotionCount){
                    zeroToMaxDurationInMinute = stepCountInMinute;
                }
            }
            previousHourlyMotionCount = hourlyMotionCount;


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

                features.get(FeatureType.MAX_AMPLITUDE).add(new AmplitudeData(timestamp, maxBackTrackAmplitude, offsetMillis));

                final double combinedBackward = maxBackTrackAmplitude * densityDrop * Math.pow((1d + maxMotionPeriodCount), 3);
                features.get(FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE).add(new AmplitudeData(timestamp, combinedBackward, offsetMillis));

                final double combinedForward = maxForwardAmplitude * densityIncrease * 1d / Math.pow((1d + maxNoMotionPeriodCount), 3);
                features.get(FeatureType.DENSITY_INCREASE_FORWARD_MAX_AMPLITUDE).add(new AmplitudeData(timestamp, combinedForward, offsetMillis));

                final double wakeUpFeature = wakeUpAggregateDensity * NumericalUtils.mean(wakeUpAggregateWindow);
                features.get(FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE).add(new AmplitudeData(timestamp, wakeUpFeature, offsetMillis));
                features.get(FeatureType.AWAKE_BACKWARD_DENSITY).add(new AmplitudeData(timestamp, wakeUpAggregateDensity, offsetMillis));

                features.get(FeatureType.MAX_MOTION_PERIOD).add(new AmplitudeData(timestamp, maxMotionPeriodCount, offsetMillis));
                features.get(FeatureType.MAX_NO_MOTION_PERIOD).add(new AmplitudeData(timestamp, maxNoMotionPeriodCount, offsetMillis));
                features.get(FeatureType.ZERO_TO_MAX_MOTION_COUNT_DURATION).add(new AmplitudeData(timestamp, zeroToMaxDurationInMinute, offsetMillis));
                features.get(FeatureType.HOURLY_MOTION_COUNT).add(new AmplitudeData(timestamp, hourlyMotionCount, offsetMillis));
                features.get(FeatureType.MOTION_COUNT_20MIN).add(new AmplitudeData(timestamp, countIn20Minute, offsetMillis));

                if(debugMode) {
                    final StringBuilder builder = new StringBuilder();
                    builder.append("[");
                    for(final AmplitudeData amplitudeData:backTrackAmpWindow){
                        builder.append(amplitudeData.amplitude).append(",");
                    }
                    builder.append("]");

                    LOGGER.trace("{}: {}",
                            new DateTime(timestamp, DateTimeZone.forOffsetMillis(offsetMillis)),
                            builder.toString());
                    /*
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
                            */

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


    public static Map<FeatureType, List<AmplitudeData>> generateTimestampAlignedKickOffFeatures(final List<AmplitudeData> rawData,
                                                                                         final int sleepDetectionWindowSizeInMinute){
        final LinkedList<AmplitudeData> backTrackAmpWindow = new LinkedList<>();

        final HashMap<FeatureType, List<AmplitudeData>> features = new HashMap<>();
        features.put(FeatureType.MAX_KICKOFF_COUNT, new ArrayList<AmplitudeData>());
        int i = 0;


        for(final AmplitudeData datum:rawData){
            if(i >= sleepDetectionWindowSizeInMinute * 2 - 1){
                final long timestamp = datum.timestamp;
                final int offsetMillis = datum.offsetMillis;
                features.get(FeatureType.MAX_KICKOFF_COUNT).add(new AmplitudeData(timestamp, datum.amplitude, offsetMillis));
            }
            i++;
        }

        return features;
    }

}
