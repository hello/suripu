package com.hello.suripu.algorithm.sleep;

import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 3/19/15.
 */
public class MotionCluster {

    private static final Logger LOGGER = LoggerFactory.getLogger(MotionCluster.class);

    private final List<AmplitudeData> motionNoMissingValues;
    private List<ClusterAmplitudeData> clusters;

    public static List<ClusterAmplitudeData> getClusters(final List<AmplitudeData> densityFeature,
                                                         final double densityThreshold,
                                                         final List<AmplitudeData> amplitudeFeature,
                                                         final double amplitudeThreshold,
                                                         final List<AmplitudeData> kickOffCounts,
                                                         final double kickOffCountThreshold){
        final List<ClusterAmplitudeData> result = new ArrayList<>();
        for(int i = 0; i < densityFeature.size(); i++) {
            final AmplitudeData density = densityFeature.get(i);
            final AmplitudeData amplitude = amplitudeFeature.get(i);
            final AmplitudeData kickOff = kickOffCounts.get(i);
            if(density.amplitude >= densityThreshold ||
                    amplitude.amplitude >= amplitudeThreshold ||
                    kickOff.amplitude >= kickOffCountThreshold){
                result.add(ClusterAmplitudeData.create(density, true));
            }else{
                result.add(ClusterAmplitudeData.create(density, false));
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

    public static Map<MotionFeatures.FeatureType, List<AmplitudeData>> getInputFeatureFromMotions(final List<AmplitudeData> dataWithoutMissingValues,
                                                                                                  final List<AmplitudeData> alignedKickOffCounts){
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> allMotionFeatures =
                MotionFeatures.generateTimestampAlignedFeatures(dataWithoutMissingValues,
                        MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                        MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                        false);
        allMotionFeatures.put(MotionFeatures.FeatureType.MAX_KICKOFF_COUNT,
                MotionFeatures.generateTimestampAlignedKickOffFeatures(alignedKickOffCounts,
                        MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES)
                        .get(MotionFeatures.FeatureType.MAX_KICKOFF_COUNT));

        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures = MotionFeatures.aggregateData(allMotionFeatures, MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES);
        return aggregatedFeatures;

    }

    private MotionCluster(final List<AmplitudeData> alignedMotionWithGapFilled,
                          final double originalAmplitudeMean,
                          final List<AmplitudeData> alignedKickOffCounts,
                          final double kickOffMean){
        this.motionNoMissingValues = alignedMotionWithGapFilled;
        //printData(alignedMotionWithGapFilled);

        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features = MotionCluster.getInputFeatureFromMotions(
                alignedMotionWithGapFilled,
                alignedKickOffCounts);
        final List<AmplitudeData> densityFeature = features.get(MotionFeatures.FeatureType.MOTION_COUNT_20MIN);
        final double densityThreshold = NumericalUtils.mean(densityFeature);
        LOGGER.debug("+++++++++++++++++++ density mean {}", densityThreshold);

        final List<AmplitudeData> amplitudeFeature = features.get(MotionFeatures.FeatureType.MAX_AMPLITUDE);
        final List<AmplitudeData> kickOffFeature = features.get(MotionFeatures.FeatureType.MAX_KICKOFF_COUNT);
        final List<ClusterAmplitudeData> rawClusters = MotionCluster.getClusters(densityFeature, densityThreshold,
                amplitudeFeature, originalAmplitudeMean,
                kickOffFeature, kickOffMean);
        this.clusters = rawClusters;
    }

    public static void printClusters(final List<ClusterAmplitudeData> clusters){
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


    public static MotionCluster create(final List<AmplitudeData> alignedAmplitudeData,
                                       final double originalMean,
                                       final List<AmplitudeData> alignedKickOffCounts,
                                       final double kickOffMean){
        if(alignedAmplitudeData.size() != alignedKickOffCounts.size()){
            LOGGER.error("Amp data size {}, kick off data size {}", alignedAmplitudeData.size(), alignedKickOffCounts.size());
            throw new AlgorithmException("Data size not equal");
        }

        for(int i = 0; i < alignedAmplitudeData.size(); i++){
            if(alignedAmplitudeData.get(i).timestamp != alignedKickOffCounts.get(i).timestamp){
                final long ampTimeMillis  = alignedAmplitudeData.get(i).timestamp;
                final long kickOffTimeMillis = alignedKickOffCounts.get(i).timestamp;
                final int offsetMillis = alignedAmplitudeData.get(i).offsetMillis;

                LOGGER.error("Amp data {} not aligned with kick off data {}",
                        new DateTime(ampTimeMillis, DateTimeZone.forOffsetMillis(offsetMillis)),
                        new DateTime(kickOffTimeMillis, DateTimeZone.forOffsetMillis(offsetMillis)));
                throw new AlgorithmException("Data not aligned!");
            }
        }

        final MotionCluster cluster = new MotionCluster(alignedAmplitudeData, originalMean,
                alignedKickOffCounts, kickOffMean);
        return cluster;
    }

    public static Pair<Integer, Integer> getClusterByTime(final List<ClusterAmplitudeData> clusters, final long targetTimestamp){
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

    public static List<Segment> toSegments(final List<ClusterAmplitudeData> list){
        if(list.size() == 0){
            return Collections.EMPTY_LIST;
        }
        long startMillis = 0;
        long endMillis = 0;

        final List<Segment> segments = new ArrayList<>();

        for(final ClusterAmplitudeData clusterAmplitudeData:list){
            if(clusterAmplitudeData.isInCluster()){
                if(startMillis == 0){
                    startMillis = clusterAmplitudeData.timestamp;
                }
                endMillis = clusterAmplitudeData.timestamp;
            }else{
                if(startMillis > 0){
                    segments.add(new Segment(startMillis, endMillis, clusterAmplitudeData.offsetMillis));
                }
                startMillis = 0;
                endMillis = 0;
            }
        }

        if(startMillis > 0){
            segments.add(new Segment(startMillis, endMillis, list.get(list.size() - 1).offsetMillis));
        }
        return segments;
    }

    public List<ClusterAmplitudeData> getCopyOfClusters(){
        final List<ClusterAmplitudeData> clusterCopy = new ArrayList<>();
        for(final ClusterAmplitudeData datum:this.clusters){
            clusterCopy.add(datum.copy());
        }
        return clusterCopy;
    }

    public static Pair<Integer, Integer> getLast(final List<ClusterAmplitudeData> clusters){
        int startIndex = -1;
        int endIndex = -1;
        final List<Segment> segments = toSegments(clusters);
        final Segment lastSegment = segments.get(segments.size() - 1);
        int i = 0;
        for(final ClusterAmplitudeData clusterAmplitudeData:clusters){
            if(clusterAmplitudeData.timestamp < lastSegment.getStartTimestamp() ||
                    clusterAmplitudeData.timestamp > lastSegment.getEndTimestamp()){
                i++;
                continue;
            }

            if(startIndex == -1){
                startIndex = i;
            }
            endIndex = i;
            i++;
        }

        return new Pair<>(startIndex, endIndex);
    }

}
