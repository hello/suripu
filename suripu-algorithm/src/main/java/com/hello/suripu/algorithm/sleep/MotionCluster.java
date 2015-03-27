package com.hello.suripu.algorithm.sleep;

import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.ClusterAmplitudeData;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.algorithm.utils.NumericalUtils;
import com.sun.tools.javac.util.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HEAD;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 3/19/15.
 */
public class MotionCluster {

    private static final Logger LOGGER = LoggerFactory.getLogger(MotionCluster.class);

    private final List<AmplitudeData> motionNoMissingValues;
    private List<ClusterAmplitudeData> clusters;
    private double densityMean;
    private double std;
    private Segment sleepPeriod;

    public static List<ClusterAmplitudeData> getClusters(final List<AmplitudeData> amplitudeData, final double threshold){
        final List<ClusterAmplitudeData> result = new ArrayList<>();
        for(final AmplitudeData datum:amplitudeData) {
            if(datum.amplitude >= threshold){
                result.add(ClusterAmplitudeData.create(datum, true));
            }else{
                result.add(ClusterAmplitudeData.create(datum, false));
            }
        }

        return result;
    }



    public static List<ClusterAmplitudeData> copyRange(final List<ClusterAmplitudeData> origin, final int start, final int end){
        final List<ClusterAmplitudeData> maxCluster = origin.subList(start, end + 1);
        final List<ClusterAmplitudeData> maxClusterCopy = new ArrayList<>();
        for (final ClusterAmplitudeData clusterAmplitudeData:maxCluster){
            maxClusterCopy.add(clusterAmplitudeData.copy());
        }

        return maxClusterCopy;
    }

    public static Map<MotionFeatures.FeatureType, List<AmplitudeData>> getInputFeatureFromMotions(final List<AmplitudeData> dataWithoutMissingValues){
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> allMotionFeatures =
                MotionFeatures.generateTimestampAlignedFeatures(dataWithoutMissingValues,
                        MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                        MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                        false);

        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures = MotionFeatures.aggregateData(allMotionFeatures, MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES);
        return aggregatedFeatures;

    }

    private MotionCluster(final List<AmplitudeData> alignedMotionWithGapFilled,
                          final double originalAmplitudeMean,
                          final boolean smoothCluster,
                          final boolean removeNoise){
        this.motionNoMissingValues = alignedMotionWithGapFilled;
        //printData(alignedMotionWithGapFilled);

        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features = MotionCluster.getInputFeatureFromMotions(this.motionNoMissingValues);
        final List<AmplitudeData> densityFeature = features.get(MotionFeatures.FeatureType.MOTION_COUNT_20MIN);
        final double densityThreshold = NumericalUtils.mean(densityFeature);
        this.densityMean = densityThreshold;
        this.std = NumericalUtils.std(densityFeature, this.densityMean);


        final List<AmplitudeData> amplitudeFeature = features.get(MotionFeatures.FeatureType.MAX_AMPLITUDE);
        final List<ClusterAmplitudeData> rawClusters = MotionCluster.getClusters(densityFeature, densityThreshold);

        this.sleepPeriod = MotionCluster.getSleepPeriod(densityFeature, amplitudeFeature, densityThreshold, originalAmplitudeMean);

        if(smoothCluster){
            this.clusters = smooth(rawClusters);
            printClusters(rawClusters);
        }else {
            printClusters(rawClusters);
            this.clusters = rawClusters;
        }

        final List<ClusterAmplitudeData> noiseCutCopy = new ArrayList<>();
        if(removeNoise){
            for(final ClusterAmplitudeData datum:rawClusters){
                if(datum.timestamp > this.sleepPeriod.getStartTimestamp() - 15 * DateTimeConstants.MILLIS_PER_MINUTE){
                    noiseCutCopy.add(datum.copy());
                }
            }
            this.clusters.clear();
            this.clusters.addAll(noiseCutCopy);
        }
    }

    private void printClusters(final List<ClusterAmplitudeData> clusters){
        for (final ClusterAmplitudeData clusterAmplitudeData:clusters){
            LOGGER.debug("{}, {}, {}",
                    new DateTime(clusterAmplitudeData.timestamp, DateTimeZone.forOffsetMillis(clusterAmplitudeData.offsetMillis)),
                    clusterAmplitudeData.isInCluster(),
                    clusterAmplitudeData.amplitude);
        }
    }

    private void printData(final List<AmplitudeData> clusters){
        for (final AmplitudeData amplitudeData:clusters){
            LOGGER.debug("{}, {}",
                    new DateTime(amplitudeData.timestamp, DateTimeZone.forOffsetMillis(amplitudeData.offsetMillis)),
                    amplitudeData.amplitude);
        }
    }

    public double getDensityMean(){
        return this.densityMean;
    }

    public double getStd(){
        return this.std;
    }

    public static MotionCluster create(final List<AmplitudeData> dataWithoutMissingValues,
                                       final double originalMean,
                                       final boolean smoothCluster,
                                       final boolean removeNoise){
        final MotionCluster cluster = new MotionCluster(dataWithoutMissingValues, originalMean, smoothCluster, removeNoise);
        return cluster;
    }

    public static Pair<Integer, Integer> getSignificantClusterIndex(final List<ClusterAmplitudeData> clusters, final long targetTimestamp){
        long clusterStartTimestamp = 0;
        long clusterEndTimestamp = 0;
        int clusterStartIndex = 0;
        int clusterEndIndex = 0;

        if(clusters.size() == 0){
            return new Pair<>(-1, -1);
        }

        int i = 0;
        for(final ClusterAmplitudeData clusterAmplitudeData:clusters){
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
                if(targetTimestamp >= clusterStartTimestamp && targetTimestamp <= clusterEndTimestamp + 15 * DateTimeConstants.MILLIS_PER_MINUTE){
                    return new Pair<>(clusterStartIndex, clusterEndIndex);
                }

                clusterStartTimestamp = 0;
                clusterEndTimestamp = 0;
            }

            // dangling case, when the last item is in cluster
            if(clusterEndTimestamp > 0 && clusterAmplitudeData.isInCluster() && i == clusters.size() - 1){
                if(targetTimestamp >= clusterStartTimestamp && targetTimestamp <= clusterEndTimestamp + 15 * DateTimeConstants.MILLIS_PER_MINUTE){
                    return new Pair<>(clusterStartIndex, clusterEndIndex);
                }
            }

            i++;
        }

        return new Pair<>(-1, -1);
    }

    public Segment getSleepTimeSpan(){
        return new Segment(this.sleepPeriod.getStartTimestamp(), this.sleepPeriod.getEndTimestamp(), this.sleepPeriod.getOffsetMillis());
    }

    public static List<AmplitudeData> trim(final List<AmplitudeData> alignedData, final long startTimestamp, final long endTimestamp){
        final List<AmplitudeData> trimmed = new ArrayList<>();
        for(final AmplitudeData datum:alignedData){
            if(datum.timestamp >= startTimestamp && datum.timestamp <= endTimestamp){
                trimmed.add(datum);
            }
        }
        return trimmed;

    }

    public static Segment getSleepPeriod(final List<AmplitudeData> densityFeatures, final List<AmplitudeData> amplitudeFeatures,
                                         final double threshold, final double originalAmpMean){
        long firstTimestamp = densityFeatures.get(0).timestamp;
        final long lastTimestamp  = densityFeatures.get(densityFeatures.size() - 1).timestamp;
        long startTimestamp = firstTimestamp;

        for(int i = 0; i < densityFeatures.size(); i++) {
            final AmplitudeData item = densityFeatures.get(i);
            final AmplitudeData amplitudeData = amplitudeFeatures.get(i);

            if(item.amplitude > threshold || amplitudeData.amplitude > originalAmpMean) {
                final long peakTimestamp = item.timestamp;
                LOGGER.debug("++++++++++++++++++++ peak {}, date {}",
                        peakTimestamp,
                        new DateTime(firstTimestamp, DateTimeZone.forOffsetMillis(amplitudeData.offsetMillis)));
                startTimestamp = peakTimestamp;
                break;
            }
        }

        return new Segment(startTimestamp, lastTimestamp, densityFeatures.get(densityFeatures.size() - 1).offsetMillis);
    }

    private static List<ClusterAmplitudeData> setInCluster(final List<ClusterAmplitudeData> clusters, final int start, final int end){
        for(int i = 0; i < clusters.size(); i++){
            if(i >= start && i < end){
                clusters.get(i).setInCluster(true);
            }
        }

        return clusters;
    }

    public static List<ClusterAmplitudeData> smooth(final List<ClusterAmplitudeData> clusters){
        int gapCount = 0;
        int gapStartIndex = 0;

        for(int i = 0; i < clusters.size(); i++) {
            final ClusterAmplitudeData datum = clusters.get(i);
            if(!datum.isInCluster()) {
                if(gapCount == 0) {
                    gapStartIndex = i;
                }
                gapCount++;
            }else {
                if(gapCount <= 1 && gapCount > 0) {
                    setInCluster(clusters, gapStartIndex, i);
                }
                gapCount = 0;
            }

        }

        return clusters;
    }

    public List<ClusterAmplitudeData> getCopyOfClusters(){
        final List<ClusterAmplitudeData> clusterCopy = new ArrayList<>();
        for(final ClusterAmplitudeData datum:this.clusters){
            clusterCopy.add(datum.copy());
        }
        return clusterCopy;
    }

}
