package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.scores.AmplitudeDataScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.LightOutCumulatedMotionMixScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.LightOutScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.MotionDensityScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.WaveAccumulateMotionScoreFunction;
import com.hello.suripu.algorithm.sleep.scores.ZeroToMaxMotionCountDurationScoreFunction;
import com.hello.suripu.algorithm.utils.DataUtils;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.algorithm.utils.NumericalUtils;
import com.sun.tools.javac.util.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by pangwu on 3/19/15.
 */
public class Vote {
    private final static Logger LOGGER = LoggerFactory.getLogger(Vote.class);
    private final MotionScoreAlgorithm motionScoreAlgorithmInternal;
    private final MotionScoreAlgorithm motionScoreAlgorithmDefault;

    private final MotionCluster motionCluster;
    private final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures;
    private final double rawAmpMean;
    private final double rawKickOffMean;

    private final boolean insertEmpty = true;
    private final boolean ampFilter = false;
    private final boolean tailBias = false;
    private final boolean smoothCluster = false;
    private final boolean removeNoise = true;

    public Vote(final List<AmplitudeData> rawData,
                final List<AmplitudeData> kickOffCounts,
                final List<DateTime> lightOutTimes,
                final Optional<DateTime> firstWaveTimeOptional){
        this.motionScoreAlgorithmDefault = MotionScoreAlgorithm.createDefault(rawData,
                lightOutTimes,
                firstWaveTimeOptional);

        final List<AmplitudeData> noDuplicates = DataUtils.dedupe(rawData);
        this.rawAmpMean = NumericalUtils.mean(noDuplicates);
        final List<AmplitudeData> noDuplicateKickOffCounts = DataUtils.dedupe(kickOffCounts);
        this.rawKickOffMean = NumericalUtils.mean(noDuplicateKickOffCounts);

        if(ampFilter){
            final List<AmplitudeData> preprocessed = preprocessNoiseFilter(noDuplicates, noDuplicateKickOffCounts);
            noDuplicates.clear();
            noDuplicates.addAll(preprocessed);
        }

        List<AmplitudeData> dataWithGapFilled = DataUtils.fillMissingValuesAndMakePositive(noDuplicates, DateTimeConstants.MILLIS_PER_MINUTE);
        List<AmplitudeData> alignedKickOffs = DataUtils.fillMissingValuesAndMakePositive(noDuplicateKickOffCounts, DateTimeConstants.MILLIS_PER_MINUTE);
        if(insertEmpty) {
            final int insertLengthMin = 20;
            dataWithGapFilled = DataUtils.insertEmptyData(dataWithGapFilled, insertLengthMin, 1);
            alignedKickOffs = DataUtils.insertEmptyData(alignedKickOffs, insertLengthMin, 1);
        }

        this.motionCluster = MotionCluster.create(dataWithGapFilled, alignedKickOffs, rawAmpMean, smoothCluster, removeNoise);
        final Segment sleepPeriod  = this.motionCluster.getSleepTimeSpan();
        LOGGER.debug("data start from {} to {}", new DateTime(sleepPeriod.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepPeriod.getOffsetMillis())),
                new DateTime(sleepPeriod.getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepPeriod.getOffsetMillis())));

        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> motionFeatures = MotionFeatures.generateTimestampAlignedFeatures(dataWithGapFilled,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                false);
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures = MotionFeatures.aggregateData(motionFeatures, MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES);
        LOGGER.debug("smoothed data size {}", aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE).size());
        this.aggregatedFeatures = aggregatedFeatures;


        final long preserveTimeMillis = 15 * DateTimeConstants.MILLIS_PER_MINUTE;
        final Set<MotionFeatures.FeatureType> featureTypes = aggregatedFeatures.keySet();
        for(final MotionFeatures.FeatureType featureType:featureTypes){
            final List<AmplitudeData> originalFeature = aggregatedFeatures.get(featureType);
            if(removeNoise) {
                if(sleepPeriod.getStartTimestamp() - originalFeature.get(0).timestamp > 2 * DateTimeConstants.MILLIS_PER_HOUR) {
                    final List<AmplitudeData> trimmed = MotionCluster.trim(originalFeature,
                            sleepPeriod.getStartTimestamp() - preserveTimeMillis,
                            sleepPeriod.getEndTimestamp());
                    aggregatedFeatures.put(featureType, trimmed);
                }
            }else{
                for(int i = 0; i < originalFeature.size(); i++){
                    final AmplitudeData item = originalFeature.get(i);
                    final long timestamp = item.timestamp;
                    final int offsetMillis = item.offsetMillis;
                    if(timestamp > sleepPeriod.getStartTimestamp() - preserveTimeMillis){
                        continue;
                    }
                    if(featureType == MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE ||
                            featureType == MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE){
                        originalFeature.set(i, new AmplitudeData(timestamp, 0d, offsetMillis));
                    }
                }
            }
        }

        final MotionScoreAlgorithm sleepDetectionAlgorithm = new MotionScoreAlgorithm();
        sleepDetectionAlgorithm.addFeature(aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE), new AmplitudeDataScoringFunction());
        sleepDetectionAlgorithm.addFeature(aggregatedFeatures.get(MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE), new MotionDensityScoringFunction(MotionDensityScoringFunction.ScoreType.SLEEP));
        sleepDetectionAlgorithm.addFeature(aggregatedFeatures.get(MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE), new MotionDensityScoringFunction(MotionDensityScoringFunction.ScoreType.WAKE_UP));
        sleepDetectionAlgorithm.addFeature(aggregatedFeatures.get(MotionFeatures.FeatureType.ZERO_TO_MAX_MOTION_COUNT_DURATION), new ZeroToMaxMotionCountDurationScoreFunction());

        if(!lightOutTimes.isEmpty()) {
            final LinkedList<AmplitudeData> lightFeature = new LinkedList<>();
            for (final AmplitudeData amplitudeData : aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE)) {
                // Pad the light data
                lightFeature.add(new AmplitudeData(amplitudeData.timestamp, 0, amplitudeData.offsetMillis));

            }
            if(dataWithGapFilled.size() > 0) {
                for(final DateTime lightOutTime:lightOutTimes) {
                    LOGGER.info("Light out time {}", lightOutTime
                            .withZone(DateTimeZone.forOffsetMillis(dataWithGapFilled.get(0).offsetMillis)));
                }
            }
            sleepDetectionAlgorithm.addFeature(lightFeature, new LightOutScoringFunction(lightOutTimes, 3d));

            final LinkedList<AmplitudeData> lightAndCumulatedMotionFeature = new LinkedList<>();
            for (final AmplitudeData amplitudeData : aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_MOTION_PERIOD)) {
                // this is the magical light feature that can keep both magic and fix broken things.
                lightAndCumulatedMotionFeature.add(new AmplitudeData(amplitudeData.timestamp,
                        1d / (amplitudeData.amplitude + 0.3),  // Max can go 3 times as much as the original score
                        amplitudeData.offsetMillis));

            }
            sleepDetectionAlgorithm.addFeature(lightAndCumulatedMotionFeature, new LightOutCumulatedMotionMixScoringFunction(lightOutTimes));
        }

        if(firstWaveTimeOptional.isPresent()) {

            final LinkedList<AmplitudeData> waveAndCumulateMotionFeature = new LinkedList<>();
            for (final AmplitudeData amplitudeData : aggregatedFeatures.get(MotionFeatures.FeatureType.AWAKE_BACKWARD_DENSITY)) {
                waveAndCumulateMotionFeature.add(new AmplitudeData(amplitudeData.timestamp,
                        amplitudeData.amplitude,
                        amplitudeData.offsetMillis));

            }
            sleepDetectionAlgorithm.addFeature(waveAndCumulateMotionFeature, new WaveAccumulateMotionScoreFunction(firstWaveTimeOptional.get()));
        }

        this.motionScoreAlgorithmInternal = sleepDetectionAlgorithm;
    }

    private List<AmplitudeData> preprocessNoiseFilter(final List<AmplitudeData> rawData, final List<AmplitudeData> kickOffCounts){
        final double amplitudeMean = NumericalUtils.mean(rawData);
        final double kickOffMean = NumericalUtils.mean(kickOffCounts);

        final List<AmplitudeData> filtered = new ArrayList<>();
        for(int i = 0; i < rawData.size(); i++){
            final AmplitudeData amplitude = rawData.get(i);
            final AmplitudeData kickOff = kickOffCounts.get(i);

            if(amplitude.amplitude < amplitudeMean && kickOff.amplitude <= kickOffMean){
                filtered.add(new AmplitudeData(amplitude.timestamp, 0d, amplitude.offsetMillis));
                continue;
            }
            filtered.add(new AmplitudeData(amplitude.timestamp, amplitude.amplitude, amplitude.offsetMillis));
        }

        return filtered;
    }

    public SleepEvents<Segment> getResult(final boolean debug){
        final SleepEvents<Segment> sleepEvents = motionScoreAlgorithmInternal.getSleepEvents(debug);
        final SleepEvents<Segment> defaultEvents = this.motionScoreAlgorithmDefault.getSleepEvents(debug);

        final SleepEvents<Segment> events = aggregate(sleepEvents, defaultEvents);
        if(debug){
            LOGGER.debug("IN_BED: {}",
                    new DateTime(events.goToBed.getStartTimestamp(),
                            DateTimeZone.forOffsetMillis(events.goToBed.getOffsetMillis())));
            LOGGER.debug("SLEEP: {}",
                    new DateTime(events.fallAsleep.getStartTimestamp(),
                            DateTimeZone.forOffsetMillis(events.fallAsleep.getOffsetMillis())));
            LOGGER.debug("WAKE_UP: {}",
                    new DateTime(events.wakeUp.getStartTimestamp(),
                            DateTimeZone.forOffsetMillis(events.wakeUp.getOffsetMillis())));
            LOGGER.debug("OUT_BED: {}",
                    new DateTime(events.outOfBed.getStartTimestamp(),
                            DateTimeZone.forOffsetMillis(events.outOfBed.getOffsetMillis())));
        }

        return events;
    }

    private static boolean isEmptyBounds(final Pair<Integer, Integer> bounds){
        if(bounds.fst > -1 && bounds.snd > -1){
            return false;
        }
        return true;
    }

    private SleepEvents<Segment> aggregate(final SleepEvents<Segment> sleepEvents,
                                           final SleepEvents<Segment> defaultEvents){
        final long sleepTime = sleepEvents.fallAsleep.getStartTimestamp();
        final long wakeUpTime = sleepEvents.wakeUp.getStartTimestamp();
        final List<ClusterAmplitudeData> clusterCopy = this.motionCluster.getCopyOfClusters();
        final Pair<Integer, Integer> sleepBounds = pickSleepClusterIndex(clusterCopy, this.aggregatedFeatures, sleepTime);
        final Pair<Integer, Integer> wakeUpBounds = pickWakeUpClusterIndex(clusterCopy, wakeUpTime, this.tailBias);

        Segment inBed = sleepEvents.goToBed;
        Segment sleep = sleepEvents.fallAsleep;
        if(!isEmptyBounds(sleepBounds)){
            final ClusterAmplitudeData clusterStart = clusterCopy.get(sleepBounds.fst);
            final ClusterAmplitudeData clusterEnd = clusterCopy.get(sleepBounds.snd);
            LOGGER.debug("Sleep cluster start at {}, end at {}",
                    new DateTime(clusterStart.timestamp, DateTimeZone.forOffsetMillis(clusterStart.offsetMillis)),
                    new DateTime(clusterEnd.timestamp, DateTimeZone.forOffsetMillis(clusterEnd.offsetMillis)));

            //final long inBedTimestamp = pickInBed(sleepMotionCluster, inBed.getStartTimestamp());
            inBed = new Segment(clusterStart.timestamp, clusterStart.timestamp + DateTimeConstants.MILLIS_PER_MINUTE, clusterStart.offsetMillis);
            final long sleepTimestamp = pickSleep(MotionCluster.copyRange(clusterCopy, sleepBounds.fst, sleepBounds.snd),
                    this.aggregatedFeatures,
                    sleep.getStartTimestamp());
            sleep = new Segment(defaultEvents.fallAsleep.getStartTimestamp(),
                    defaultEvents.fallAsleep.getEndTimestamp(),
                    defaultEvents.fallAsleep.getOffsetMillis());
        }

        Segment wakeUp = sleepEvents.wakeUp;
        Segment outBed = sleepEvents.outOfBed;
        if(!isEmptyBounds(wakeUpBounds)){
            final ClusterAmplitudeData clusterStart = clusterCopy.get(wakeUpBounds.fst);
            final ClusterAmplitudeData clusterEnd = clusterCopy.get(wakeUpBounds.snd);

            LOGGER.debug("Wake up cluster start at {}, end at {}",
                    new DateTime(clusterStart.timestamp, DateTimeZone.forOffsetMillis(clusterStart.offsetMillis)),
                    new DateTime(clusterEnd.timestamp, DateTimeZone.forOffsetMillis(clusterEnd.offsetMillis)));

            outBed = new Segment(clusterEnd.timestamp, clusterEnd.timestamp + DateTimeConstants.MILLIS_PER_MINUTE, clusterEnd.offsetMillis);

            final long wakeUpTimestamp = pickWakeUp(MotionCluster.copyRange(clusterCopy, wakeUpBounds.fst, wakeUpBounds.snd),
                    this.aggregatedFeatures,
                    wakeUp.getStartTimestamp());
            wakeUp = new Segment(defaultEvents.wakeUp.getStartTimestamp(),
                    defaultEvents.wakeUp.getEndTimestamp(),
                    defaultEvents.wakeUp.getOffsetMillis());
        }

        return SleepEvents.create(inBed, sleep, wakeUp, outBed);
    }

    protected long pickWakeUp(final List<ClusterAmplitudeData> wakeUpMotionCluster,
                              final Map<MotionFeatures.FeatureType, List<AmplitudeData>> motionFeatures,
                              final long originalWakeUpMillis){
        if(wakeUpMotionCluster.size() == 0){
            return originalWakeUpMillis;
        }

        long newWakeUpMillis = 0;
        final long clusterStartMillis = wakeUpMotionCluster.get(0).timestamp;
        if(originalWakeUpMillis < clusterStartMillis){
            if(clusterStartMillis - originalWakeUpMillis < 30 * DateTimeConstants.MILLIS_PER_MINUTE){
                return originalWakeUpMillis;
            }
            return clusterStartMillis;
        }
        for(final ClusterAmplitudeData clusterAmplitudeData:wakeUpMotionCluster) {
            if(clusterAmplitudeData.timestamp < originalWakeUpMillis) {
                newWakeUpMillis = clusterAmplitudeData.timestamp;
                break;
            }
        }

        if(originalWakeUpMillis - newWakeUpMillis < 60 * DateTimeConstants.MILLIS_PER_MINUTE){
            return newWakeUpMillis;
        }

        return originalWakeUpMillis;
    }

    private static ClusterAmplitudeData getItem(final List<ClusterAmplitudeData> clusters, final int i){
        return clusters.get(i);
    }

    protected static Pair<Integer, Integer> pickSleepClusterIndex(final List<ClusterAmplitudeData> clusters,
                                                           final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures,
                                                            final long originalSleepMillis){
        final Pair<Integer, Integer> originalBounds = MotionCluster.getSignificantClusterIndex(clusters, originalSleepMillis);

        final List<AmplitudeData> wakeFeature = aggregatedFeatures.get(MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE);
        double maxWakeScore = 0d;
        long maxWakeMillis = 0;

        final List<AmplitudeData> sleepFeature = aggregatedFeatures.get(MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE);
        double maxSleepScore = 0d;
        long maxSleepMillis = 0;

        final long endTimestamp = wakeFeature.get(wakeFeature.size() - 1).timestamp;
        long searchEndMillis = endTimestamp;


        for(int i = 0; i < wakeFeature.size(); i++) {
            final long timestamp = wakeFeature.get(i).timestamp;
            final double combinedForward = wakeFeature.get(i).amplitude;
            final double combinedBackWard = sleepFeature.get(i).amplitude;
            if((combinedForward > 0 || combinedBackWard > 0) && searchEndMillis == endTimestamp){
                searchEndMillis = timestamp + 3 * DateTimeConstants.MILLIS_PER_HOUR;
            }

            if (timestamp > searchEndMillis) {
                break;
            }

            if (combinedForward > maxWakeScore) {
                maxWakeScore = combinedForward;
                maxWakeMillis = timestamp;
            }
            if (combinedBackWard > maxSleepScore) {
                maxSleepScore = combinedBackWard;
                maxSleepMillis = timestamp;
            }
        }


        final Pair<Integer, Integer> wakeBounds = MotionCluster.getSignificantClusterIndex(clusters, maxWakeMillis);
        final Pair<Integer, Integer> sleepBounds = MotionCluster.getSignificantClusterIndex(clusters, maxSleepMillis);

        if (wakeBounds.fst == sleepBounds.fst && wakeBounds.fst == originalBounds.fst) {
            if(!isEmptyBounds(originalBounds)) {
                LOGGER.debug("All agree! sleep cluster start {} end {}",
                        new DateTime(getItem(clusters, originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.snd).offsetMillis)));
            }
            return new Pair<>(originalBounds.fst + 1, originalBounds.snd);
        }

        if (wakeBounds.fst == sleepBounds.fst && wakeBounds.fst != originalBounds.fst) {

            if(!isEmptyBounds(originalBounds) && !isEmptyBounds(wakeBounds)) {
                LOGGER.debug("Two agree, false detection impacted by other source (time/light). " +
                                "sleep cluster start {} end {}, corrected {}, {}",
                        new DateTime(getItem(clusters, originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.snd).offsetMillis)),
                        new DateTime(getItem(clusters, wakeBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, wakeBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, wakeBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, wakeBounds.snd).offsetMillis)));
            }
            return wakeBounds;
        }

        if (wakeBounds.fst != sleepBounds.fst) {
            // Need to further investigate this case
            if(!isEmptyBounds(originalBounds) && !isEmptyBounds(wakeBounds) && !isEmptyBounds(sleepBounds)) {
                LOGGER.debug("None agree, use wake, wake {} - {}, sleep {} - {}, detect {} - {}",
                        new DateTime(getItem(clusters, wakeBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, wakeBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, wakeBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, wakeBounds.snd).offsetMillis)),
                        new DateTime(getItem(clusters, sleepBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, sleepBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, sleepBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, sleepBounds.snd).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.snd).offsetMillis)));
            }
            return wakeBounds;
        }

        if(isEmptyBounds(originalBounds)){
            return originalBounds;
        }

        return new Pair<>(originalBounds.fst + 1, originalBounds.snd);
    }


    protected static Pair<Integer, Integer> pickWakeUpClusterIndex(final List<ClusterAmplitudeData> clusters,
                                                            final long originalWakeUpMillis,
                                                            final boolean tailBias){
        if(!tailBias){
            return MotionCluster.getSignificantClusterIndex(clusters, originalWakeUpMillis);
        }


        int gapCount = 0;
        int lastGapStartIndex = 0;
        final Pair<Integer, Integer> originalBounds = MotionCluster.getSignificantClusterIndex(clusters, originalWakeUpMillis);
        if(isEmptyBounds(originalBounds)){
            return originalBounds;
        }

        int startIndex = originalBounds.fst;
        int endIndex = 0;


        for(int i = 0; i < clusters.size(); i++) {
            final ClusterAmplitudeData current = clusters.get(i);
            if(current.timestamp < clusters.get(originalBounds.fst).timestamp){
                continue;
            }

            if(!current.isInCluster()) {
                if(gapCount == 0) {
                    lastGapStartIndex = i;
                }
                gapCount++;
                continue;
            }

            if(gapCount > 4) {
                break;
            }
            if(!clusters.get(i - 1).isInCluster()) {
                startIndex = i;
                LOGGER.debug("start index set to {}:{}",
                        startIndex,
                        new DateTime(clusters.get(startIndex).timestamp, DateTimeZone.forOffsetMillis(clusters.get(startIndex).offsetMillis)));
            }
            endIndex = i;
            gapCount = 0;
        }

        return new Pair<>(startIndex, endIndex);
    }

    protected long pickSleep(final List<ClusterAmplitudeData> sleepMotionCluster,
                             final Map<MotionFeatures.FeatureType, List<AmplitudeData>> motionFeatures,
                             final long originalSleepMillis){

        if(sleepMotionCluster.size() == 0){
            return originalSleepMillis;
        }

        final long startMillis = sleepMotionCluster.get(0).timestamp;
        final long endMillis = sleepMotionCluster.get(sleepMotionCluster.size() - 1).timestamp;
        double maxValue = 0;
        long maxTimeMillis = 0;

        final List<AmplitudeData> sleepFeature = motionFeatures.get(MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE);
        for(final AmplitudeData feature:sleepFeature) {
            if(feature.timestamp >= startMillis && feature.timestamp <= endMillis && feature.amplitude >= maxValue) {
                maxValue = feature.amplitude;
                maxTimeMillis = feature.timestamp;
            }
        }

        if(maxTimeMillis > 0 && endMillis - maxTimeMillis > 15 * DateTimeConstants.MILLIS_PER_MINUTE){
            return maxTimeMillis;
        }

        return endMillis;
    }

    public Map<MotionFeatures.FeatureType, List<AmplitudeData>> getAggregatedFeatures(){
        return ImmutableMap.copyOf(this.aggregatedFeatures);
    }

    public MotionCluster getMotionClusterAlgorithm(){
        return this.motionCluster;
    }

    public MotionScoreAlgorithm getMultiScoreAlgorithm(){
        return this.motionScoreAlgorithmInternal;
    }
}
