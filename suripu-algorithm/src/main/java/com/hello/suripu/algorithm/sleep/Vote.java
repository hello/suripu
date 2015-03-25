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
import com.hello.suripu.algorithm.utils.ClusterAmplitudeData;
import com.hello.suripu.algorithm.utils.DataUtils;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by pangwu on 3/19/15.
 */
public class Vote {
    private final static Logger LOGGER = LoggerFactory.getLogger(Vote.class);
    private final MotionScoreAlgorithm motionScoreAlgorithm;
    private final MotionCluster motionCluster;
    private final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures;

    public Vote(final List<AmplitudeData> raw,
                final List<DateTime> lightOutTimes,
                final Optional<DateTime> firstWaveTimeOptional){
        final List<AmplitudeData> noDuplicates = DataUtils.dedupe(raw);
        final List<AmplitudeData> dataWithGapFilled = DataUtils.fillMissingValuesAndMakePositive(noDuplicates, DateTimeConstants.MILLIS_PER_MINUTE);
        this.motionCluster = MotionCluster.create(dataWithGapFilled);
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

        final Set<MotionFeatures.FeatureType> featureTypes = aggregatedFeatures.keySet();
        for(final MotionFeatures.FeatureType featureType:featureTypes){
            final List<AmplitudeData> originalFeature = aggregatedFeatures.get(featureType);
            final List<AmplitudeData> trimmed = MotionCluster.trim(originalFeature, sleepPeriod.getStartTimestamp(), sleepPeriod.getEndTimestamp());
            aggregatedFeatures.put(featureType, trimmed);
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

        this.motionScoreAlgorithm = sleepDetectionAlgorithm;
    }

    public SleepEvents<Segment> getResult(final boolean debug){
        final SleepEvents<Segment> sleepEvents = motionScoreAlgorithm.getSleepEvents(debug);
        return aggregate(sleepEvents);
    }

    private SleepEvents<Segment> aggregate(final SleepEvents<Segment> sleepEvents){
        final long sleepTime = sleepEvents.fallAsleep.getStartTimestamp();
        final long wakeUpTime = sleepEvents.wakeUp.getStartTimestamp();

        final List<ClusterAmplitudeData> sleepMotionCluster = this.motionCluster.getSignificantCluster(sleepTime);
        final List<ClusterAmplitudeData> wakeUpMotionCluster = this.motionCluster.getSignificantCluster(wakeUpTime);

        Segment inBed = sleepEvents.goToBed;
        Segment sleep = sleepEvents.fallAsleep;
        if(!sleepMotionCluster.isEmpty()){
            final ClusterAmplitudeData clusterStart = sleepMotionCluster.get(0);
            final ClusterAmplitudeData clusterEnd = sleepMotionCluster.get(sleepMotionCluster.size() - 1);
            inBed = new Segment(clusterStart.timestamp, clusterStart.timestamp + DateTimeConstants.MILLIS_PER_MINUTE, clusterStart.offsetMillis);
            sleep = new Segment(clusterEnd.timestamp, clusterEnd.timestamp + DateTimeConstants.MILLIS_PER_MINUTE, clusterEnd.offsetMillis);
        }

        Segment wakeUp = sleepEvents.wakeUp;
        Segment outBed = sleepEvents.outOfBed;
        if(!wakeUpMotionCluster.isEmpty()){
            final ClusterAmplitudeData clusterStart = wakeUpMotionCluster.get(0);
            final ClusterAmplitudeData clusterEnd = wakeUpMotionCluster.get(wakeUpMotionCluster.size() - 1);

            LOGGER.debug("Wake up cluster start {} end {}",
                    new DateTime(clusterStart.timestamp, DateTimeZone.forOffsetMillis(clusterStart.offsetMillis)),
                    new DateTime(clusterEnd.timestamp, DateTimeZone.forOffsetMillis(clusterEnd.offsetMillis)));
            outBed = new Segment(clusterEnd.timestamp, clusterEnd.timestamp + DateTimeConstants.MILLIS_PER_MINUTE, clusterEnd.offsetMillis);

            final long wakeUpTimestamp = pickWakeUp(wakeUpMotionCluster, wakeUp.getStartTimestamp());
            wakeUp = new Segment(wakeUpTimestamp, wakeUpTimestamp + DateTimeConstants.MILLIS_PER_MINUTE, wakeUp.getOffsetMillis());
        }

        return SleepEvents.create(inBed, sleep, wakeUp, outBed);
    }

    protected long pickWakeUp(final List<ClusterAmplitudeData> wakeUpMotionCluster, final long originalWakeUp){
        for(final ClusterAmplitudeData clusterAmplitudeData:wakeUpMotionCluster) {
            if(clusterAmplitudeData.amplitude > this.motionCluster.getDensityMean() + this.motionCluster.getStd() &&
                    clusterAmplitudeData.amplitude < originalWakeUp) {
                return clusterAmplitudeData.timestamp;
            }
        }

        return originalWakeUp;
    }

    public Map<MotionFeatures.FeatureType, List<AmplitudeData>> getAggregatedFeatures(){
        return ImmutableMap.copyOf(this.aggregatedFeatures);
    }

    public MotionCluster getMotionClusterAlgorithm(){
        return this.motionCluster;
    }

    public MotionScoreAlgorithm getMultiScoreAlgorithm(){
        return this.motionScoreAlgorithm;
    }
}
