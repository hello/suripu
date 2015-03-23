package com.hello.suripu.algorithm.sleep;

import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.ClusterAmplitudeData;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.algorithm.utils.NumericalUtils;
import org.joda.time.DateTimeConstants;
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

    private final List<AmplitudeData> rawMotion;
    private List<ClusterAmplitudeData> clusters;
    private double threshold;
    private Segment sleepPeriod;

    public static List<ClusterAmplitudeData> getClusters(final List<AmplitudeData> amplitudeData, final double threshold){
        final List<ClusterAmplitudeData> result = new ArrayList<>();
        for(final AmplitudeData datum:amplitudeData) {
            if(datum.amplitude > threshold){
                result.add(ClusterAmplitudeData.create(datum, true));
            }else{
                result.add(ClusterAmplitudeData.create(datum, false));
            }
        }

        return result;
    }



    private static List<ClusterAmplitudeData> copyRange(final List<ClusterAmplitudeData> origin, final int start, final int end){
        final List<ClusterAmplitudeData> maxCluster = origin.subList(start, end + 1);
        final List<ClusterAmplitudeData> maxClusterCopy = new ArrayList<>();
        for (final ClusterAmplitudeData clusterAmplitudeData:maxCluster){
            maxClusterCopy.add(clusterAmplitudeData.copy());
        }

        return maxClusterCopy;
    }

    public static List<AmplitudeData> getInputFeatureFromMotions(final List<AmplitudeData> rawData){
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> allMotionFeatures =
                MotionFeatures.generateTimestampAlignedFeatures(rawData,
                        MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                        MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                        false);
        if(!allMotionFeatures.containsKey(MotionFeatures.FeatureType.MOTION_COUNT_20MIN)){
            return Collections.EMPTY_LIST;
        }

        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures = MotionFeatures.aggregateData(allMotionFeatures, MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES);
        final List<AmplitudeData> awakeDetectionFeatures = aggregatedFeatures.get(MotionFeatures.FeatureType.MOTION_COUNT_20MIN);
        return awakeDetectionFeatures;

    }

    private MotionCluster(final List<AmplitudeData> alignedMotionWithGapFilled){
        this.rawMotion = alignedMotionWithGapFilled;

        final List<AmplitudeData> inputFeature = MotionCluster.getInputFeatureFromMotions(this.rawMotion);
        final double threshold = NumericalUtils.mean(inputFeature);
        this.threshold = threshold;
        this.sleepPeriod = MotionCluster.getSleepPeriod(inputFeature, threshold);
        final List<ClusterAmplitudeData> rawClusters = MotionCluster.getClusters(inputFeature, threshold);
        this.clusters = rawClusters;
    }

    public double getThreshold(){
        return this.threshold;
    }

    public static MotionCluster create(final List<AmplitudeData> rawMotion){
        final MotionCluster cluster = new MotionCluster(rawMotion);
        return cluster;
    }

    public List<ClusterAmplitudeData> getSignificantCluster(final long targetTimestamp){
        long clusterStartTimestamp = 0;
        long clusterEndTimestamp = 0;
        int clusterStartIndex = 0;
        int clusterEndIndex = 0;

        if(clusters.size() == 0){
            return Collections.EMPTY_LIST;
        }

        int i = 0;
        for(final ClusterAmplitudeData clusterAmplitudeData:this.clusters){
            if(clusterStartTimestamp == 0 && clusterAmplitudeData.isInCluster()){
                clusterStartTimestamp = clusterAmplitudeData.timestamp;
                clusterStartIndex = clusters.indexOf(clusterAmplitudeData);
            }

            if(clusterStartTimestamp > 0 && clusterAmplitudeData.isInCluster()){
                clusterEndTimestamp = clusterAmplitudeData.timestamp;
                clusterEndIndex = clusters.indexOf(clusterAmplitudeData);
            }

            // when the last item is not in cluster
            if(clusterEndTimestamp > 0 && !clusterAmplitudeData.isInCluster()){
                if(targetTimestamp >= clusterStartTimestamp && targetTimestamp <= clusterEndTimestamp + 2 * 10 * DateTimeConstants.MILLIS_PER_MINUTE){
                    return copyRange(clusters, clusterStartIndex, clusterEndIndex);
                }

                clusterStartTimestamp = 0;
                clusterEndTimestamp = 0;
            }

            // dangling case, when the last item is in cluster
            if(clusterEndTimestamp > 0 && clusterAmplitudeData.isInCluster() && i == this.clusters.size() - 1){
                if(targetTimestamp >= clusterStartTimestamp && targetTimestamp <= clusterEndTimestamp){
                    return copyRange(clusters, clusterStartIndex, clusterEndIndex);
                }
            }

            i++;
        }

        return Collections.EMPTY_LIST;
    }

    public List<ClusterAmplitudeData> getClusters(){
        return this.clusters;
    }

    public Segment getSleepTimeSpan(){
        return new Segment(this.sleepPeriod.getStartTimestamp(), this.sleepPeriod.getEndTimestamp(), this.sleepPeriod.getOffsetMillis());
    }

    public static List<Segment> itemsToSegments(final List<ClusterAmplitudeData> items){
        final List<Segment> segments = new ArrayList<>();
        long startTimestamp = 0;
        long endTimestamp = 0;
        for(final ClusterAmplitudeData item:items){
            if(item.isInCluster() && startTimestamp == 0){
                startTimestamp = item.timestamp;
            }

            if(item.isInCluster() && startTimestamp > 0){
                endTimestamp = item.timestamp;
            }

            if(!item.isInCluster() && startTimestamp > 0 && endTimestamp > 0){
                segments.add(new Segment(startTimestamp, endTimestamp, item.offsetMillis));
            }
        }

        // dangling case
        final ClusterAmplitudeData lastItem = items.get(items.size() - 1);
        if(lastItem.isInCluster() && startTimestamp > 0){
            segments.add(new Segment(startTimestamp, lastItem.timestamp, lastItem.offsetMillis));
        }

        return segments;
    }

    public static Segment getSleepPeriod(final List<AmplitudeData> features, final double threshold){
        final long firstTimestamp = features.get(0).timestamp;
        final long lastTimestamp  = features.get(features.size() - 1).timestamp;

        for(int i = 0; i < features.size(); i++) {
            final AmplitudeData item = features.get(i);

            if(item.amplitude > threshold) {
                final long peakTimestamp = item.timestamp;
                LOGGER.debug("++++++++++++++++++++ peak {}, first {}", peakTimestamp, firstTimestamp);

                if(peakTimestamp - firstTimestamp > 2 * DateTimeConstants.MILLIS_PER_HOUR) {
                    return new Segment(peakTimestamp, lastTimestamp, item.offsetMillis);
                }
            }
        }
        return new Segment(firstTimestamp, lastTimestamp, features.get(features.size() - 1).offsetMillis);
    }

}
