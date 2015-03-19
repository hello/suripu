package com.hello.suripu.algorithm.sleep;

import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.utils.ClusterAmplitudeData;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.algorithm.utils.NumericalUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 3/19/15.
 */
public class MotionCluster {

    private static final Logger LOGGER = LoggerFactory.getLogger(MotionCluster.class);
    public static final double DEFAULT_STD_COUNT = 5d;

    public static List<ClusterAmplitudeData> getClusters(final List<AmplitudeData> amplitudeData, final double stdCount, final int windowSizeInDataCount){
        final List<AmplitudeData> window = new ArrayList<>();
        final List<ClusterAmplitudeData> result = new ArrayList<>();
        double mean = 0d;
        double std = 0d;
        for(final AmplitudeData datum:amplitudeData) {
            window.add(datum);
            if(window.size() < windowSizeInDataCount) {
                mean = NumericalUtils.mean(window);
                std = NumericalUtils.std(window, mean);
                result.add(ClusterAmplitudeData.create(datum, false));
                continue;
            }
               final double value = datum.amplitude;
                if(value > mean + stdCount * std) {
                    LOGGER.debug("* val: {} , mean_t: {}, std_t: {}, date: {}",
                            value,
                            mean,
                            std,
                            new DateTime(datum.timestamp, DateTimeZone.forOffsetMillis(datum.offsetMillis)));
                    result.add(ClusterAmplitudeData.create(datum, true));
                }else {
                    LOGGER.debug("val: {}, mean_t: {}, std_t: {}, date: {}",
                            value,
                            mean,
                            std,
                            new DateTime(datum.timestamp, DateTimeZone.forOffsetMillis(datum.offsetMillis)));
                    std = (std + Math.sqrt(Math.pow(value - mean, 2))) / 2d;
                    mean = (mean + value) / 2d;
                    result.add(ClusterAmplitudeData.create(datum, false));
                }
        }

        return result;
    }

    private static void setAsMotionCluster(final List<ClusterAmplitudeData> clusters, final int start, final int end){
        for(int i = start; i < end; i++){
            clusters.get(i).setInCluster(true);
        }
    }

    public static List<ClusterAmplitudeData> smoothCluster(final List<ClusterAmplitudeData> rawClusters){
        int gapCount = 0;
        int clusterLength = 0;
        int lastGapStartIndex = 0;

        int dynamicThreshold = 1;
        final List<ClusterAmplitudeData> smoothed = new ArrayList<>();

        for(int i = 0; i < rawClusters.size(); i++) {
            final ClusterAmplitudeData clusterAmplitudeData = rawClusters.get(i);
            if(!clusterAmplitudeData.isInCluster()) {
                if(gapCount == 0) {
                    lastGapStartIndex = i;
                }
                gapCount++;
                if(clusterLength > 0) {
                    dynamicThreshold = Math.max(clusterLength / 3, 1);
                    clusterLength = 0;
                }
            }else {
                if(gapCount <= dynamicThreshold && gapCount > 0) {
                    setAsMotionCluster(smoothed, lastGapStartIndex, i);
                }
                gapCount = 0;
                clusterLength++;
            }

            smoothed.add(clusterAmplitudeData.copy());
        }
        return smoothed;
    }

    public static List<ClusterAmplitudeData> getLargestCluster(final List<ClusterAmplitudeData> clusters){
        int maxLength = 0;
        int currentLength = 0;
        int startIndex = -1;
        int endIndex = startIndex;

        int maxStartIndex = 0;
        int maxEndIndex = 0;

        for(int i = 0; i < clusters.size(); i++) {
            final ClusterAmplitudeData clusterAmplitudeData = clusters.get(i);
            if (clusterAmplitudeData.isInCluster()) {
                currentLength++;
                if(startIndex == -1) {
                    startIndex = i;
                }
                endIndex = i;
            } else {
                if(currentLength > maxLength){
                    maxStartIndex = startIndex;
                    maxEndIndex = endIndex;
                }

                currentLength = 0;
                startIndex = -1;
                endIndex = startIndex;
            }
        }

        if(currentLength > maxLength){  // deal with dangling case.
            maxStartIndex = startIndex;
            maxEndIndex = endIndex;
        }

        if(maxEndIndex > maxStartIndex){
            final List<ClusterAmplitudeData> maxCluster = clusters.subList(maxStartIndex, maxEndIndex + 1);
            final List<ClusterAmplitudeData> maxClusterCopy = new ArrayList<>();
            for (final ClusterAmplitudeData clusterAmplitudeData:maxCluster){
                maxClusterCopy.add(clusterAmplitudeData.copy());
            }

            return maxClusterCopy;
        }

        return Collections.EMPTY_LIST;
    }

    public static List<AmplitudeData> getInputFeatureFromMotions(final List<AmplitudeData> rawData){
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> allMotionFeatures =
                MotionFeatures.generateTimestampAlignedFeatures(rawData,
                        MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                        MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                        false);
        if(!allMotionFeatures.containsKey(MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE)){
            return Collections.EMPTY_LIST;
        }
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedAwakeDetectionFeatures = MotionFeatures.aggregateData(allMotionFeatures,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES);
        final List<AmplitudeData> awakeDetectionFeatures = aggregatedAwakeDetectionFeatures.get(MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE);
        return awakeDetectionFeatures;

    }
}
