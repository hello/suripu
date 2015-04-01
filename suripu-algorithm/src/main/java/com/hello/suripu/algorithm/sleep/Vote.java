package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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
import java.util.HashMap;
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

    private final SleepPeriod sleepPeriod;

    private final boolean insertEmpty = true;
    private final boolean ampFilter = false;
    private final boolean tailBias = false;
    private final boolean smoothCluster = false;
    private final boolean removeNoise = true;
    private final boolean defaultSearch = false;
    private final boolean multiSleep = true;
    private final boolean capScoreOutOfPeriod = false;
    private final boolean defaultOverride = false;

    public Vote(final List<AmplitudeData> rawData,
                final List<AmplitudeData> kickOffCounts,
                final List<AmplitudeData> rawSound,
                final List<DateTime> lightOutTimes,
                final Optional<DateTime> firstWaveTimeOptional){
        this.motionScoreAlgorithmDefault = MotionScoreAlgorithm.createDefault(rawData,
                lightOutTimes,
                firstWaveTimeOptional);

        final List<AmplitudeData> noDuplicates = DataUtils.makePositive(DataUtils.dedupe(rawData));
        this.rawAmpMean = NumericalUtils.mean(noDuplicates);
        final List<AmplitudeData> noDuplicateKickOffCounts = DataUtils.dedupe(kickOffCounts);
        this.rawKickOffMean = NumericalUtils.mean(noDuplicateKickOffCounts);

        List<AmplitudeData> dataWithGapFilled = DataUtils.fillMissingValues(noDuplicates, DateTimeConstants.MILLIS_PER_MINUTE);
        List<AmplitudeData> alignedKickOffs = DataUtils.fillMissingValues(noDuplicateKickOffCounts, DateTimeConstants.MILLIS_PER_MINUTE);
        if(insertEmpty) {
            final int insertLengthMin = 20;
            dataWithGapFilled = DataUtils.insertEmptyData(dataWithGapFilled, insertLengthMin, 1);
            alignedKickOffs = DataUtils.insertEmptyData(alignedKickOffs, insertLengthMin, 1);
        }

        this.motionCluster = MotionCluster.create(dataWithGapFilled, rawAmpMean, alignedKickOffs, rawKickOffMean, removeNoise);
        final List<Segment> motionSegments = MotionCluster.toSegments(this.motionCluster.getCopyOfClusters());
        final Optional<Segment> inBedSegment = this.multiSleep == false ?
                Optional.of(this.motionCluster.getSleepTimeSpan()) :
                SleepPeriod.getSleepPeriod(dataWithGapFilled, motionSegments);

        this.sleepPeriod = SleepPeriod.createFromSegment(inBedSegment.get());
        this.sleepPeriod.addVotingSegments(motionSegments);
        this.sleepPeriod.addVotingSegments(SoundCluster.getClusters(rawSound));

        LOGGER.debug("data start from {} to {}", new DateTime(this.sleepPeriod.getStartTimestamp(), DateTimeZone.forOffsetMillis(this.sleepPeriod.getOffsetMillis())),
                new DateTime(this.sleepPeriod.getEndTimestamp(), DateTimeZone.forOffsetMillis(this.sleepPeriod.getOffsetMillis())));

        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> motionFeatures = MotionFeatures.generateTimestampAlignedFeatures(dataWithGapFilled,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                false);
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures = MotionFeatures.aggregateData(motionFeatures, MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES);
        LOGGER.debug("smoothed data size {}", aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE).size());

        if(this.capScoreOutOfPeriod) {
            final Map<MotionFeatures.FeatureType, List<AmplitudeData>> capFeatures = capFeaturesBySleepPeriod(aggregatedFeatures, sleepPeriod);
            this.aggregatedFeatures = capFeatures;
        }else{
            this.aggregatedFeatures = aggregatedFeatures;
            final long preserveTimeMillis = 15 * DateTimeConstants.MILLIS_PER_MINUTE;
            final Set<MotionFeatures.FeatureType> featureTypes = aggregatedFeatures.keySet();
            for(final MotionFeatures.FeatureType featureType:featureTypes){
                final List<AmplitudeData> originalFeature = aggregatedFeatures.get(featureType);
                if(this.removeNoise) {
                    if(sleepPeriod.getStartTimestamp() - originalFeature.get(0).timestamp > 2 * DateTimeConstants.MILLIS_PER_HOUR) {
                        final List<AmplitudeData> trimmed = MotionCluster.trim(originalFeature,
                                sleepPeriod.getStartTimestamp() - preserveTimeMillis,
                                sleepPeriod.getEndTimestamp());
                        aggregatedFeatures.put(featureType, trimmed);
                    }
                    continue;
                }

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
        sleepDetectionAlgorithm.addFeature(this.aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE), new AmplitudeDataScoringFunction());
        sleepDetectionAlgorithm.addFeature(this.aggregatedFeatures.get(MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE), new MotionDensityScoringFunction(MotionDensityScoringFunction.ScoreType.SLEEP));
        sleepDetectionAlgorithm.addFeature(this.aggregatedFeatures.get(MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE), new MotionDensityScoringFunction(MotionDensityScoringFunction.ScoreType.WAKE_UP));
        sleepDetectionAlgorithm.addFeature(this.aggregatedFeatures.get(MotionFeatures.FeatureType.ZERO_TO_MAX_MOTION_COUNT_DURATION), new ZeroToMaxMotionCountDurationScoreFunction());

        if(!lightOutTimes.isEmpty()) {
            final LinkedList<AmplitudeData> lightFeature = new LinkedList<>();
            for (final AmplitudeData amplitudeData : this.aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE)) {
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
            for (final AmplitudeData amplitudeData : this.aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_MOTION_PERIOD)) {
                // this is the magical light feature that can keep both magic and fix broken things.
                lightAndCumulatedMotionFeature.add(new AmplitudeData(amplitudeData.timestamp,
                        1d / (amplitudeData.amplitude + 0.3),  // Max can go 3 times as much as the original score
                        amplitudeData.offsetMillis));

            }
            sleepDetectionAlgorithm.addFeature(lightAndCumulatedMotionFeature, new LightOutCumulatedMotionMixScoringFunction(lightOutTimes));
        }

        if(firstWaveTimeOptional.isPresent()) {

            final LinkedList<AmplitudeData> waveAndCumulateMotionFeature = new LinkedList<>();
            for (final AmplitudeData amplitudeData : this.aggregatedFeatures.get(MotionFeatures.FeatureType.AWAKE_BACKWARD_DENSITY)) {
                waveAndCumulateMotionFeature.add(new AmplitudeData(amplitudeData.timestamp,
                        amplitudeData.amplitude,
                        amplitudeData.offsetMillis));

            }
            sleepDetectionAlgorithm.addFeature(waveAndCumulateMotionFeature, new WaveAccumulateMotionScoreFunction(firstWaveTimeOptional.get()));
        }

        this.motionScoreAlgorithmInternal = sleepDetectionAlgorithm;
    }

    public final List<Segment> getAwakes(final boolean debug){
        return this.sleepPeriod.getAwakePeriods(debug);
    }

    @Deprecated
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

    private Map<MotionFeatures.FeatureType, List<AmplitudeData>> capFeaturesBySleepPeriod(final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                                                                                          final Segment sleepPeriod){
        final Set<MotionFeatures.FeatureType> featureTypes = features.keySet();
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> result = new HashMap<>();
        for(final MotionFeatures.FeatureType featureType:featureTypes){
            result.put(featureType, new ArrayList<AmplitudeData>());

            if(featureType != MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE &&
                    featureType != MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE){
                result.put(featureType, Lists.newArrayList(features.get(featureType)));
            }

            final List<AmplitudeData> originalFeature = features.get(featureType);

            for(int i = 0; i < originalFeature.size(); i++){
                final AmplitudeData item = originalFeature.get(i);
                final long timestamp = item.timestamp;
                if(timestamp >= sleepPeriod.getStartTimestamp() && timestamp <= sleepPeriod.getEndTimestamp()){
                    result.get(featureType).add(item);
                    continue;
                }

                result.get(featureType).add(new AmplitudeData(timestamp, 0d, item.offsetMillis));
            }
        }
        return features;
    }

    public SleepEvents<Segment> getResult(final boolean debug){
        if(debug){
            LOGGER.debug("+++++++++++++ amp mean {}, kickoff mean {}", this.rawAmpMean, this.rawKickOffMean);
            MotionCluster.printClusters(this.motionCluster.getCopyOfClusters());
        }

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

        //final long wakeUpSearchTime = Math.min(defaultEvents.wakeUp.getStartTimestamp(), sleepEvents.fallAsleep.getStartTimestamp());

        final List<ClusterAmplitudeData> clusterCopy = this.motionCluster.getCopyOfClusters();

        Segment inBed = sleepEvents.goToBed;
        Segment sleep = sleepEvents.fallAsleep;

        final Pair<Long, Long> sleepTimesMillis = pickSleep(this.sleepPeriod,
                this.getAggregatedFeatures(),
                sleep.getStartTimestamp());

//        if(!this.multiSleep) {
            final Pair<Integer, Integer> sleepBounds = pickSleepClusterIndexOld(clusterCopy, this.aggregatedFeatures, sleep.getStartTimestamp());
            if (!isEmptyBounds(sleepBounds)) {
                final ClusterAmplitudeData clusterStart = clusterCopy.get(sleepBounds.fst);
                inBed = new Segment(clusterStart.timestamp, clusterStart.timestamp + DateTimeConstants.MILLIS_PER_MINUTE, clusterStart.offsetMillis);
            }
//        }


        //if(this.multiSleep) {
//            inBed = new Segment(sleepTimesMillis.fst,
//                    sleepTimesMillis.fst + DateTimeConstants.MILLIS_PER_MINUTE,
//                    sleepPeriod.getOffsetMillis());
        //}


        if(!defaultOverride) {
            sleep = new Segment(sleepTimesMillis.snd,
                    sleepTimesMillis.snd + DateTimeConstants.MILLIS_PER_MINUTE,
                    defaultEvents.fallAsleep.getOffsetMillis());
        } else {
            sleep = new Segment(defaultEvents.fallAsleep.getStartTimestamp(),
                    defaultEvents.fallAsleep.getEndTimestamp(),
                    defaultEvents.fallAsleep.getOffsetMillis());
            if(defaultEvents.goToBed.getStartTimestamp() > defaultEvents.fallAsleep.getStartTimestamp()){
                inBed = new Segment(defaultEvents.fallAsleep.getStartTimestamp() - 10 * DateTimeConstants.MILLIS_PER_MINUTE,
                        defaultEvents.fallAsleep.getEndTimestamp() - 10 * DateTimeConstants.MILLIS_PER_MINUTE,
                        defaultEvents.fallAsleep.getOffsetMillis());
            }
        }

        Segment wakeUp = sleepEvents.wakeUp;
        Segment outBed = sleepEvents.outOfBed;

        final long wakeUpSearchTime = Math.min(defaultEvents.wakeUp.getStartTimestamp(), sleepEvents.fallAsleep.getStartTimestamp());
        final Pair<Integer, Integer> wakeUpBounds = pickWakeUpClusterIndex(clusterCopy,
                this.getAggregatedFeatures(),
                this.sleepPeriod,
                wakeUpSearchTime);
        
        if(!isEmptyBounds(wakeUpBounds)){
            final ClusterAmplitudeData clusterStart = clusterCopy.get(wakeUpBounds.fst);
            final ClusterAmplitudeData clusterEnd = clusterCopy.get(wakeUpBounds.snd);

            LOGGER.debug("Wake up cluster start at {}, end at {}",
                    new DateTime(clusterStart.timestamp, DateTimeZone.forOffsetMillis(clusterStart.offsetMillis)),
                    new DateTime(clusterEnd.timestamp, DateTimeZone.forOffsetMillis(clusterEnd.offsetMillis)));

            outBed = new Segment(clusterEnd.timestamp,
                    clusterEnd.timestamp + DateTimeConstants.MILLIS_PER_MINUTE,
                    clusterEnd.offsetMillis);

            final long wakeUpTimestamp = pickWakeUp(clusterCopy,
                    MotionCluster.copyRange(clusterCopy, wakeUpBounds.fst, wakeUpBounds.snd),
                    this.sleepPeriod,
                    this.getAggregatedFeatures(),
                    sleepEvents.wakeUp.getStartTimestamp());

            if(!defaultOverride) {
                wakeUp = new Segment(wakeUpTimestamp,
                        wakeUpTimestamp + DateTimeConstants.MILLIS_PER_MINUTE,
                        defaultEvents.wakeUp.getOffsetMillis());
                if(outBed.getStartTimestamp() < wakeUpTimestamp){
                    outBed = new Segment(wakeUp.getStartTimestamp() + 10 * DateTimeConstants.MILLIS_PER_MINUTE,
                            wakeUp.getEndTimestamp() + 10 * DateTimeConstants.MILLIS_PER_MINUTE,
                            wakeUp.getOffsetMillis());
                }
            }
        }else{
            wakeUp = new Segment(defaultEvents.wakeUp.getStartTimestamp(),
                    defaultEvents.wakeUp.getEndTimestamp(),
                    defaultEvents.wakeUp.getOffsetMillis());
            if(outBed.getStartTimestamp() < wakeUp.getStartTimestamp()){
                outBed = new Segment(wakeUp.getStartTimestamp() + 10 * DateTimeConstants.MILLIS_PER_MINUTE,
                        wakeUp.getEndTimestamp() + 10 * DateTimeConstants.MILLIS_PER_MINUTE,
                        wakeUp.getOffsetMillis());
            }
        }

        /*
        final long wakeUpMillis = pickWakeUp(sleepPeriod, getAggregatedFeatures(), wakeUp.getStartTimestamp());
        final Pair<Integer, Integer> wakeUpBounds = MotionCluster.getClusterByTime(clusterCopy, wakeUpMillis);
        if(isEmptyBounds(wakeUpBounds)){
            outBed = new Segment(wakeUpMillis + 10 * DateTimeConstants.MILLIS_PER_MINUTE,
                    wakeUpMillis + 11 * DateTimeConstants.MILLIS_PER_MINUTE,
                    sleepPeriod.getOffsetMillis());
        }else{
            final ClusterAmplitudeData clusterEnd = clusterCopy.get(wakeUpBounds.snd);
            outBed = new Segment(clusterEnd.timestamp, clusterEnd.timestamp + DateTimeConstants.MILLIS_PER_MINUTE, clusterEnd.offsetMillis);
        }

        if(!defaultOverride){
            wakeUp = new Segment(wakeUpMillis,
                    wakeUpMillis + DateTimeConstants.MILLIS_PER_MINUTE,
                    defaultEvents.wakeUp.getOffsetMillis());
        } else {
            wakeUp = new Segment(defaultEvents.wakeUp.getStartTimestamp(),
                    defaultEvents.wakeUp.getEndTimestamp(),
                    defaultEvents.wakeUp.getOffsetMillis());
        }
        */

        return SleepEvents.create(inBed, sleep, wakeUp, outBed);
    }

    private static long pickMaxScoreWakeUp(final SleepPeriod sleepPeriod,
                                           final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                                           final long originalWakeUpMillis){

        final Optional<AmplitudeData> maxScoreItem = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                originalWakeUpMillis,
                sleepPeriod.getEndTimestamp());
        if(!maxScoreItem.isPresent()){
            return originalWakeUpMillis;
        }

        return maxScoreItem.get().timestamp;
    }

    protected static long pickWakeUp(final List<ClusterAmplitudeData> clusters,
                                final List<ClusterAmplitudeData> wakeUpMotionCluster,
                                final SleepPeriod sleepPeriod,
                                final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                                final long wakeUpMillisSleepPeriod){
        if(wakeUpMotionCluster.size() == 0){
            return wakeUpMillisSleepPeriod;
        }

        final Pair<Integer, Integer> originalBounds = MotionCluster.getClusterByTime(clusters, wakeUpMillisSleepPeriod);

        if(isEmptyBounds(originalBounds)){
            return pickMaxScoreWakeUp(sleepPeriod, features, wakeUpMillisSleepPeriod);
        }

        if(clusters.get(originalBounds.fst).timestamp == wakeUpMotionCluster.get(0).timestamp &&
                clusters.get(originalBounds.snd).timestamp == wakeUpMotionCluster.get(wakeUpMotionCluster.size() - 1).timestamp){
            return wakeUpMillisSleepPeriod;
        }

        return pickMaxScoreWakeUp(sleepPeriod, features, wakeUpMillisSleepPeriod);

    }

    /*
    protected static long pickWakeUp(final SleepPeriod sleepPeriod,
                                     final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                                     final long wakeUpMillisSleepPeriod){
        final Optional<AmplitudeData> maxScoreOptional = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                sleepPeriod.getEndTimestamp() - DateTimeConstants.MILLIS_PER_HOUR,
                sleepPeriod.getEndTimestamp());
        if(!maxScoreOptional.isPresent()){
            return wakeUpMillisSleepPeriod;
        }

        if(maxScoreOptional.get().timestamp > wakeUpMillisSleepPeriod + DateTimeConstants.MILLIS_PER_HOUR){
            return maxScoreOptional.get().timestamp;
        }

        return wakeUpMillisSleepPeriod;

    }*/

    private static ClusterAmplitudeData getItem(final List<ClusterAmplitudeData> clusters, final int i){
        return clusters.get(i);
    }

    protected static Pair<Integer, Integer> pickSleepClusterIndexOld(final List<ClusterAmplitudeData> clusters,
                                                                  final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures,
                                                                  final long originalSleepMillis) {
        final Pair<Integer, Integer> originalBounds = MotionCluster.getClusterByTime(clusters, originalSleepMillis);
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
        final Pair<Integer, Integer> wakeBounds = MotionCluster.getClusterByTime(clusters, maxWakeMillis);
        final Pair<Integer, Integer> sleepBounds = MotionCluster.getClusterByTime(clusters, maxSleepMillis);
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

    protected static Pair<Integer, Integer> pickSleepClusterIndex(final List<ClusterAmplitudeData> clusters,
                                                           final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures,
                                                           final Segment sleepPeriod,
                                                           final long originalSleepMillis){
        final Pair<Integer, Integer> originalBounds = MotionCluster.getClusterByTime(clusters, originalSleepMillis);
        final long endSearchTimeMillis = sleepPeriod.getStartTimestamp() + 3 * DateTimeConstants.MILLIS_PER_HOUR;
        final Optional<AmplitudeData> maxWakeScoreItem = getMaxScore(aggregatedFeatures,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                sleepPeriod.getStartTimestamp(),
                endSearchTimeMillis);
        final Optional<AmplitudeData> maxSleepScoreItem = getMaxScore(aggregatedFeatures,
                MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE,
                sleepPeriod.getStartTimestamp(),
                endSearchTimeMillis);

        if(!maxSleepScoreItem.isPresent() && !maxWakeScoreItem.isPresent()){
            LOGGER.debug("No score peak found.");
            return new Pair<>(-1, -1);
        }

        final Pair<Integer, Integer> maxWakeBounds = MotionCluster.getClusterByTime(clusters, maxWakeScoreItem.get().timestamp);
        final Pair<Integer, Integer> maxSleepBounds = MotionCluster.getClusterByTime(clusters, maxSleepScoreItem.get().timestamp);

        if (maxWakeBounds.fst == maxSleepBounds.fst && maxWakeBounds.fst == originalBounds.fst) {
            if(!isEmptyBounds(originalBounds)) {
                LOGGER.debug("All agree! sleep cluster start {} end {}",
                        new DateTime(getItem(clusters, originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.snd).offsetMillis)));
            }
            return new Pair<>(originalBounds.fst, originalBounds.snd);
        }

        if (maxWakeBounds.fst == maxSleepBounds.fst && maxWakeBounds.fst != originalBounds.fst) {

            if(!isEmptyBounds(originalBounds) && !isEmptyBounds(maxWakeBounds)) {
                LOGGER.debug("Two agree, false detection impacted by other source (time/light). " +
                                "sleep cluster start {} end {}, corrected {}, {}",
                        new DateTime(getItem(clusters, originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.snd).offsetMillis)),
                        new DateTime(getItem(clusters, maxWakeBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, maxWakeBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, maxWakeBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, maxWakeBounds.snd).offsetMillis)));
            }
            return maxWakeBounds;
        }

        if (maxWakeBounds.fst != maxSleepBounds.fst) {
            // Need to further investigate this case
            if(!isEmptyBounds(originalBounds) && !isEmptyBounds(maxWakeBounds) && !isEmptyBounds(maxSleepBounds)) {
                LOGGER.debug("None agree, use wake, wake {} - {}, sleep {} - {}, detect {} - {}",
                        new DateTime(getItem(clusters, maxWakeBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, maxWakeBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, maxWakeBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, maxWakeBounds.snd).offsetMillis)),
                        new DateTime(getItem(clusters, maxSleepBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, maxSleepBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, maxSleepBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, maxSleepBounds.snd).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.snd).offsetMillis)));
            }
            return maxWakeBounds;
        }

        if(isEmptyBounds(originalBounds)){
            return originalBounds;
        }

        return new Pair<>(originalBounds.fst, originalBounds.snd);
    }


    protected static Pair<Integer, Integer> pickWakeUpClusterIndex(final List<ClusterAmplitudeData> clusters,
                                                                   final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                                                                   final Segment sleepPeriod,
                                                                   final long originalWakeUpMillis){
        final Pair<Integer, Integer> originalBounds = MotionCluster.getClusterByTime(clusters, originalWakeUpMillis);
        if(isEmptyBounds(originalBounds)){
            return new Pair<>(-1, -1);
        }

        final List<Segment> clustersInSleepPeriod = new ArrayList<>();
        final List<Segment> allClusters = MotionCluster.toSegments(clusters);
        for(final Segment cluster:allClusters){
            if(cluster.getStartTimestamp() >= sleepPeriod.getStartTimestamp() && cluster.getEndTimestamp() <= sleepPeriod.getEndTimestamp()){
                clustersInSleepPeriod.add(cluster);
            }
        }

        if(clustersInSleepPeriod.size() == 0){
            return new Pair<>(-1, -1);
        }
        final Segment lastClusterInSleepPeriod = clustersInSleepPeriod.get(clustersInSleepPeriod.size() - 1);
        if(lastClusterInSleepPeriod.getStartTimestamp() == clusters.get(originalBounds.fst).timestamp &&
                lastClusterInSleepPeriod.getEndTimestamp() == clusters.get(originalBounds.snd).timestamp){
            LOGGER.debug("Wake up all agree! wake up cluster {} - {}",
                    new DateTime(lastClusterInSleepPeriod.getStartTimestamp(), DateTimeZone.forOffsetMillis(lastClusterInSleepPeriod.getOffsetMillis())),
                    new DateTime(lastClusterInSleepPeriod.getEndTimestamp(), DateTimeZone.forOffsetMillis(lastClusterInSleepPeriod.getOffsetMillis())));
            return originalBounds;
        }

        if(lastClusterInSleepPeriod.getStartTimestamp() - clusters.get(originalBounds.snd).timestamp < DateTimeConstants.MILLIS_PER_HOUR &&
                lastClusterInSleepPeriod.getDuration() < 20 * DateTimeConstants.MILLIS_PER_MINUTE){
            return originalBounds;
        }

        LOGGER.debug("Detected cluster too far form end of sleep, detected cluster {} - {}, last cluster {} - {}",
                new DateTime(clusters.get(originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(clusters.get(originalBounds.fst).offsetMillis)),
                new DateTime(clusters.get(originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(clusters.get(originalBounds.snd).offsetMillis)),
                new DateTime(lastClusterInSleepPeriod.getStartTimestamp(), DateTimeZone.forOffsetMillis(lastClusterInSleepPeriod.getOffsetMillis())),
                new DateTime(lastClusterInSleepPeriod.getEndTimestamp(), DateTimeZone.forOffsetMillis(lastClusterInSleepPeriod.getOffsetMillis())));

        final long startSearchMillis = clusters.get(originalBounds.snd).timestamp + 5 * DateTimeConstants.MILLIS_PER_MINUTE;
        final long endSearchMillis = sleepPeriod.getEndTimestamp();
        final Optional<AmplitudeData> maxScoreItem = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                startSearchMillis,
                endSearchMillis);
        if(!maxScoreItem.isPresent()){
            LOGGER.debug("Cannot find max score in {} - {}",
                    new DateTime(startSearchMillis, DateTimeZone.forOffsetMillis(sleepPeriod.getOffsetMillis())),
                    new DateTime(endSearchMillis, DateTimeZone.forOffsetMillis(sleepPeriod.getOffsetMillis())));
            return new Pair<>(-1, -1);
        }
        final Pair<Integer, Integer> newBounds = MotionCluster.getClusterByTime(clusters, maxScoreItem.get().timestamp);
        if(isEmptyBounds(newBounds)){
            LOGGER.debug("Max score time {} doesn't has a cluster.",
                    new DateTime(maxScoreItem.get().timestamp, DateTimeZone.forOffsetMillis(maxScoreItem.get().offsetMillis)));
            final Pair<Integer, Integer> lastBounds = MotionCluster.getLast(clusters);
            return lastBounds;
        }
        return newBounds;

    }

    protected static Pair<Long, Long> pickSleepOld(final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                             final long originalSleepMillis){

        final List<AmplitudeData> wakeFeature = features.get(MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE);
        final List<AmplitudeData> sleepFeature = features.get(MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE);
        final long endTimestamp = wakeFeature.get(wakeFeature.size() - 1).timestamp;
        long searchEndMillis = endTimestamp;

        for(int i = 0; i < wakeFeature.size(); i++) {
            final long timestamp = wakeFeature.get(i).timestamp;
            final double combinedForward = wakeFeature.get(i).amplitude;
            final double combinedBackWard = sleepFeature.get(i).amplitude;
            if ((combinedForward > 0 || combinedBackWard > 0) && searchEndMillis == endTimestamp) {
                searchEndMillis = timestamp + 3 * DateTimeConstants.MILLIS_PER_HOUR;
                break;
            }
        }

        long inBedMillis = originalSleepMillis - 10 * DateTimeConstants.MILLIS_PER_MINUTE;
        final Optional<AmplitudeData> firstMaxScoreItemOptional = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                wakeFeature.get(0).timestamp,
                searchEndMillis);
        if(firstMaxScoreItemOptional.isPresent()){
            inBedMillis = firstMaxScoreItemOptional.get().timestamp;
        }

        final Optional<AmplitudeData> predictedMaxScoreItemOptional = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                originalSleepMillis - 20 * DateTimeConstants.MILLIS_PER_MINUTE,
                originalSleepMillis + 20 * DateTimeConstants.MILLIS_PER_MINUTE);

        LOGGER.debug("first {}, max {}, predicted {}",
                new DateTime(firstMaxScoreItemOptional.get().timestamp, DateTimeZone.forOffsetMillis(firstMaxScoreItemOptional.get().offsetMillis)),
                new DateTime(predictedMaxScoreItemOptional.get().timestamp, DateTimeZone.forOffsetMillis(predictedMaxScoreItemOptional.get().offsetMillis)),
                new DateTime(originalSleepMillis, DateTimeZone.forOffsetMillis(firstMaxScoreItemOptional.get().offsetMillis)));

        if(firstMaxScoreItemOptional.get().timestamp < originalSleepMillis - 2 * DateTimeConstants.MILLIS_PER_HOUR){
            if(firstMaxScoreItemOptional.get().amplitude * 5 < predictedMaxScoreItemOptional.get().amplitude){
                return new Pair<>(inBedMillis, originalSleepMillis);
            }
            final Optional<AmplitudeData> maxDrop = getMaxScore(features,
                    MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE,
                    sleepFeature.get(0).timestamp,
                    searchEndMillis);
            if(!maxDrop.isPresent()){
                return new Pair<>(inBedMillis, inBedMillis + 10 * DateTimeConstants.MILLIS_PER_MINUTE);
            }
            return new Pair<>(inBedMillis, maxDrop.get().timestamp);
        }
        return new Pair<>(inBedMillis, originalSleepMillis);
    }


    protected static Pair<Long, Long> pickSleep(final SleepPeriod sleepPeriod,
                                                final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                                                final long originalSleepMillis){


        long inBedMillis = originalSleepMillis - 10 * DateTimeConstants.MILLIS_PER_MINUTE;
        final Optional<AmplitudeData> firstMaxScoreItemOptional = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                sleepPeriod.getStartTimestamp(),
                sleepPeriod.getEndTimestamp() + 2 * DateTimeConstants.MILLIS_PER_HOUR);

        final Optional<AmplitudeData> predictedMaxScoreItemOptional = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                originalSleepMillis - 20 * DateTimeConstants.MILLIS_PER_MINUTE,
                originalSleepMillis + 20 * DateTimeConstants.MILLIS_PER_MINUTE);

        if(firstMaxScoreItemOptional.isPresent()){
            inBedMillis = firstMaxScoreItemOptional.get().timestamp;
         }

        LOGGER.debug("first {}, max {}, predicted {}",
                new DateTime(firstMaxScoreItemOptional.get().timestamp, DateTimeZone.forOffsetMillis(firstMaxScoreItemOptional.get().offsetMillis)),
                new DateTime(predictedMaxScoreItemOptional.get().timestamp, DateTimeZone.forOffsetMillis(predictedMaxScoreItemOptional.get().offsetMillis)),
                new DateTime(originalSleepMillis, DateTimeZone.forOffsetMillis(firstMaxScoreItemOptional.get().offsetMillis)));

        if(firstMaxScoreItemOptional.get().timestamp < originalSleepMillis - 2 * DateTimeConstants.MILLIS_PER_HOUR){
            if(firstMaxScoreItemOptional.get().amplitude * 5 < predictedMaxScoreItemOptional.get().amplitude){
                return new Pair<>(inBedMillis, originalSleepMillis);
            }
            final Optional<AmplitudeData> maxDrop = getMaxScore(features,
                    MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE,
                    sleepPeriod.getStartTimestamp(),
                    sleepPeriod.getEndTimestamp() + 2 * DateTimeConstants.MILLIS_PER_HOUR);
            if(!maxDrop.isPresent()){
                return new Pair<>(inBedMillis, inBedMillis + 10 * DateTimeConstants.MILLIS_PER_MINUTE);
            }
            return new Pair<>(inBedMillis, maxDrop.get().timestamp);
        }
        return new Pair<>(inBedMillis, originalSleepMillis);
    }

    public Map<MotionFeatures.FeatureType, List<AmplitudeData>> getAggregatedFeatures(){
        return ImmutableMap.copyOf(this.aggregatedFeatures);
    }

    public static Optional<AmplitudeData> getMaxScore(final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                                                        final MotionFeatures.FeatureType featureType,
                                                      final long startSearchMillis, final long endSearchMillis){
        final List<AmplitudeData> feature = features.get(featureType);
        if(feature == null || feature.size() == 0){
            return Optional.absent();
        }

        Optional<AmplitudeData> maxScore = Optional.absent();
        for(final AmplitudeData datum:feature){
            if(datum.timestamp >= startSearchMillis && datum.timestamp <= endSearchMillis){
                if(!maxScore.isPresent()){
                    maxScore = Optional.of(datum);
                    continue;
                }

                if(maxScore.get().amplitude <= datum.amplitude){
                    maxScore = Optional.of(datum);
                }
            }
        }

        return maxScore;
    }

}
