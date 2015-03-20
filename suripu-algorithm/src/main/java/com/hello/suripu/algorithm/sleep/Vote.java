package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.scores.AmplitudeDataScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.LightOutCumulatedMotionMixScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.LightOutScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.MotionDensityScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.WaveAccumulateMotionScoreFunction;
import com.hello.suripu.algorithm.sleep.scores.ZeroToMaxMotionCountDurationScoreFunction;
import com.hello.suripu.algorithm.utils.ClusterAmplitudeData;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 3/19/15.
 */
public class Vote {
    private final static Logger LOGGER = LoggerFactory.getLogger(Vote.class);
    private final MotionScoreAlgorithm motionScoreAlgorithm;
    private final MotionCluster motionCluster;

    public Vote(final List<AmplitudeData> dataWithGapFilled,
                final List<DateTime> lightOutTimes,
                final Optional<DateTime> firstWaveTimeOptional){

        final List<AmplitudeData> insertedData = insertBefore(dataWithGapFilled, 60);
        this.motionCluster = MotionCluster.create(insertedData);
        if(this.motionCluster.isBizarreOrPillowTooooHard()){
            final List<ClusterAmplitudeData> largestCluster = this.motionCluster.getLargestCluster();
            insertedData.clear();
            final List<AmplitudeData> trimData = trim(dataWithGapFilled, largestCluster.get(0).timestamp, largestCluster.get(largestCluster.size() - 1).timestamp);
            insertedData.addAll(insertBefore(trimData, 60));
        }


        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> motionFeatures = MotionFeatures.generateTimestampAlignedFeatures(insertedData,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                false);
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures = MotionFeatures.aggregateData(motionFeatures, MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES);
        LOGGER.info("smoothed data size {}", aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE).size());

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

    public SleepEvents<Segment> getResult(){
        final SleepEvents<Segment> sleepEvents = motionScoreAlgorithm.getSleepEvents(false);
        final long sleepTime = sleepEvents.fallAsleep.getStartTimestamp();
        final long wakeUpTime = sleepEvents.wakeUp.getStartTimestamp();

        final List<ClusterAmplitudeData> sleepMotionCluster = this.motionCluster.getSignificantCluster(sleepTime);
        final List<ClusterAmplitudeData> wakeUpMotionCluster = this.motionCluster.getSignificantCluster(wakeUpTime);

        Segment inBed = sleepEvents.goToBed;
        if(!sleepMotionCluster.isEmpty()){
            final ClusterAmplitudeData clusterAmplitudeData = sleepMotionCluster.get(0);
            inBed = new Segment(clusterAmplitudeData.timestamp, clusterAmplitudeData.timestamp + DateTimeConstants.MILLIS_PER_MINUTE, clusterAmplitudeData.offsetMillis);
        }

        Segment outBed = sleepEvents.outOfBed;
        if(!wakeUpMotionCluster.isEmpty()){
            final ClusterAmplitudeData clusterAmplitudeData = wakeUpMotionCluster.get(wakeUpMotionCluster.size() - 1);
            outBed = new Segment(clusterAmplitudeData.timestamp, clusterAmplitudeData.timestamp + DateTimeConstants.MILLIS_PER_MINUTE, clusterAmplitudeData.offsetMillis);
        }

        return SleepEvents.create(inBed, sleepEvents.fallAsleep, sleepEvents.goToBed, outBed);
    }

    public static List<AmplitudeData> insertBefore(final List<AmplitudeData> alignedData, final int numInsert){
        final List<AmplitudeData> inserted = new ArrayList<>();
        if(alignedData.size() == 0){
            return Collections.EMPTY_LIST;
        }

        final AmplitudeData firstData = alignedData.get(0);
        for(int i = 0; i < numInsert; i++){
            inserted.add(0, new AmplitudeData(firstData.timestamp - (i + 1) * DateTimeConstants.MILLIS_PER_MINUTE, 0d, firstData.offsetMillis));
        }

        inserted.addAll(alignedData);
        return inserted;
    }

    public static List<AmplitudeData> trim(final List<AmplitudeData> alignedData, final long startTimestamp, final long endTimestamp){
        final List<AmplitudeData> trimed = new ArrayList<>();
        for(final AmplitudeData datum:alignedData){
            if(datum.timestamp >= startTimestamp && datum.timestamp <= endTimestamp){
                trimed.add(datum);
            }
        }
        return trimed;

    }
}
