package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.scores.EventScores;
import com.hello.suripu.algorithm.sleep.scores.SleepDataScoringFunction;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    public List<Segment> getSleepEvents(final boolean debugMode) throws AlgorithmException {

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

        final ArrayList<Segment> sleepEvents = new ArrayList<>();
        Segment goToBed = new Segment(goToBedScore.get().timestamp, goToBedScore.get().timestamp, goToBedData.offsetMillis);
        Segment sleep = new Segment(fallAsleepScore.get().timestamp, fallAsleepScore.get().timestamp, fallAsleepData.offsetMillis);
        Segment wakeUp = new Segment(wakeUpScore.get().timestamp, wakeUpScore.get().timestamp, wakeUpData.offsetMillis);
        Segment outOfBed = new Segment(outOfBedScore.get().timestamp, outOfBedScore.get().timestamp, outOfBedData.offsetMillis);

        sleepEvents.add(goToBed);
        sleepEvents.add(sleep);
        sleepEvents.add(wakeUp);
        sleepEvents.add(outOfBed);

        return sleepEvents;
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
}
