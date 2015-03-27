package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.scores.AmplitudeDataScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.EventScores;
import com.hello.suripu.algorithm.sleep.scores.LightOutCumulatedMotionMixScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.LightOutScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.MotionDensityScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.SleepDataScoringFunction;
import com.hello.suripu.algorithm.sleep.scores.WaveAccumulateMotionScoreFunction;
import com.hello.suripu.algorithm.sleep.scores.ZeroToMaxMotionCountDurationScoreFunction;
import com.hello.suripu.algorithm.utils.DataUtils;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 12/14/14.
 */
public class MotionScoreAlgorithm {
    private final static Logger LOGGER = LoggerFactory.getLogger(MotionScoreAlgorithm.class);
    private final Map<SleepDataScoringFunction, List<AmplitudeData>> features = new LinkedHashMap<>();

    public static Optional<InternalScore> getHighestScore(final List<InternalScore> scores){
        if(scores.size() == 0){
            return Optional.absent();
        }

        return Optional.of(Ordering.natural().max(scores));
    }

    public SleepEvents<Segment> getSleepEvents(final boolean debugMode) throws AlgorithmException {

        final ArrayList<InternalScore> fallAsleepScores = new ArrayList<>();
        final ArrayList<InternalScore> wakeUpScores = new ArrayList<>();
        final ArrayList<InternalScore> goToBedScores = new ArrayList<>();
        final ArrayList<InternalScore> outOfBedScores = new ArrayList<>();

        List<AmplitudeData> firstFeature = new ArrayList<>();
        for(final SleepDataScoringFunction sleepDataScoringFunction:this.features.keySet()){
            final List<AmplitudeData> feature = this.features.get(sleepDataScoringFunction);
            if(firstFeature.size() == 0){
                firstFeature = Lists.newArrayList(feature);
            }
            final Map<AmplitudeData, EventScores> pdf = sleepDataScoringFunction.getPDF(feature);

            for(int i = 0; i < feature.size(); i++) {
                final AmplitudeData datum = feature.get(i);
                final EventScores eventScoresForFeatureValue = pdf.get(datum);

                if(fallAsleepScores.size() < feature.size()){
                    fallAsleepScores.add(new InternalScore(datum.timestamp, eventScoresForFeatureValue.sleepEventScore));
                }else{
                    final InternalScore previousScore = fallAsleepScores.get(i);
                    if(datum.timestamp != previousScore.timestamp){
                        throw new AlgorithmException("Feature timestamp not aligned!");
                    }
                    fallAsleepScores.set(i, new InternalScore(datum.timestamp, eventScoresForFeatureValue.sleepEventScore * previousScore.score));
                }

                if(wakeUpScores.size() < feature.size()){
                    wakeUpScores.add(new InternalScore(datum.timestamp, eventScoresForFeatureValue.wakeUpEventScore));
                }else{
                    final InternalScore previousScore = wakeUpScores.get(i);
                    if(datum.timestamp != previousScore.timestamp){
                        throw new AlgorithmException("Feature timestamp not aligned!");
                    }
                    wakeUpScores.set(i, new InternalScore(datum.timestamp, eventScoresForFeatureValue.wakeUpEventScore * previousScore.score));
                }

                if(goToBedScores.size() < feature.size()){
                    goToBedScores.add(new InternalScore(datum.timestamp, eventScoresForFeatureValue.goToBedEventScore));
                }else{
                    final InternalScore previousScore = goToBedScores.get(i);
                    if(datum.timestamp != previousScore.timestamp){
                        throw new AlgorithmException("Feature timestamp not aligned!");
                    }
                    goToBedScores.set(i, new InternalScore(datum.timestamp, eventScoresForFeatureValue.goToBedEventScore * previousScore.score));
                }

                if(outOfBedScores.size() < feature.size()){
                    outOfBedScores.add(new InternalScore(datum.timestamp, eventScoresForFeatureValue.outOfBedEventScore));
                }else{
                    final InternalScore previousScore = outOfBedScores.get(i);
                    if(datum.timestamp != previousScore.timestamp){
                        throw new AlgorithmException("Feature timestamp not aligned!");
                    }
                    outOfBedScores.set(i, new InternalScore(datum.timestamp, eventScoresForFeatureValue.outOfBedEventScore * previousScore.score));
                }

            }
        }

        if(debugMode) {
            for(int i = 0; i < fallAsleepScores.size(); i++) {
                LOGGER.trace("goto_bed_prob {}, sleep_prob {}, wake up prob {}, out_bed_prob {}",
                        goToBedScores.get(i).score,
                        fallAsleepScores.get(i).score,
                        wakeUpScores.get(i).score,
                        outOfBedScores.get(i).score);
            }
        }

        // Step 4: Pick the highest sleep and wake up scores, sleep and wake up detected.
        final Optional<InternalScore> wakeUpScore = getHighestScore(wakeUpScores);
        final Optional<InternalScore> fallAsleepScore = getHighestScore(fallAsleepScores);
        final Optional<InternalScore> goToBedScore = getHighestScore(goToBedScores);
        final Optional<InternalScore> outOfBedScore = getHighestScore(outOfBedScores);

        // We always have data, no need to check score.isPresent()
        final AmplitudeData fallAsleepData = getDataByTimestamp(fallAsleepScore.get().timestamp, firstFeature).get();
        final AmplitudeData wakeUpData = getDataByTimestamp(wakeUpScore.get().timestamp, firstFeature).get();
        final AmplitudeData goToBedData = getDataByTimestamp(goToBedScore.get().timestamp, firstFeature).get();
        final AmplitudeData outOfBedData = getDataByTimestamp(outOfBedScore.get().timestamp, firstFeature).get();


        LOGGER.info("Prob go to bed time: {}, score {}", new DateTime(goToBedData.timestamp,
                        DateTimeZone.forOffsetMillis(goToBedData.offsetMillis)),
                goToBedScore.get().score);
        LOGGER.info("Prob fall asleep time: {}, score {}", new DateTime(fallAsleepData.timestamp,
                    DateTimeZone.forOffsetMillis(fallAsleepData.offsetMillis)),
                fallAsleepScore.get().score);
        LOGGER.info("Prob wake up time: {}, score {}", new DateTime(wakeUpData.timestamp,
                    DateTimeZone.forOffsetMillis(wakeUpData.offsetMillis)),
                wakeUpScore.get().score);
        LOGGER.info("Prob out of bed time: {}, score {}", new DateTime(outOfBedData.timestamp,
                    DateTimeZone.forOffsetMillis(outOfBedData.offsetMillis)),
                outOfBedScore.get().score);

        Segment goToBed = new Segment(goToBedScore.get().timestamp, goToBedScore.get().timestamp, goToBedData.offsetMillis);
        Segment sleep = new Segment(fallAsleepScore.get().timestamp, fallAsleepScore.get().timestamp, fallAsleepData.offsetMillis);
        Segment wakeUp = new Segment(wakeUpScore.get().timestamp, wakeUpScore.get().timestamp, wakeUpData.offsetMillis);
        Segment outOfBed = new Segment(outOfBedScore.get().timestamp, outOfBedScore.get().timestamp, outOfBedData.offsetMillis);

        return SleepEvents.create(goToBed, sleep, wakeUp, outOfBed);
    }

    private Optional<AmplitudeData> getDataByTimestamp(final long timestamp, final List<AmplitudeData> feature){
        for(final AmplitudeData datum:feature){
            if(datum.timestamp == timestamp){
                return Optional.of(datum);
            }
        }

        return Optional.absent();
    }

    public int addFeature(final List<AmplitudeData> feature, final SleepDataScoringFunction scoringFunction){

        this.features.put(scoringFunction, feature);
        return this.features.size();
    }

    public static MotionScoreAlgorithm createDefault(final List<AmplitudeData> rawAmplitude,
                                                 final List<DateTime> lightOutTimes,
                                                 final Optional<DateTime> firstWaveTimeOptional){
        final List<AmplitudeData> noDuplicates = DataUtils.dedupe(rawAmplitude);
        List<AmplitudeData> dataWithGapFilled = DataUtils.fillMissingValuesAndMakePositive(noDuplicates, DateTimeConstants.MILLIS_PER_MINUTE);
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> motionFeatures = MotionFeatures.generateTimestampAlignedFeatures(dataWithGapFilled,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                false);
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures = MotionFeatures.aggregateData(motionFeatures, MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES);
        LOGGER.debug("smoothed data size {}", aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE).size());

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
        return sleepDetectionAlgorithm;
    }
}
