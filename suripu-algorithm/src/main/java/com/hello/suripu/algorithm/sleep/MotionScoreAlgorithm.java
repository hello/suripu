package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by pangwu on 12/14/14.
 */
public class MotionScoreAlgorithm {
    private final static Logger LOGGER = LoggerFactory.getLogger(MotionScoreAlgorithm.class);
    private final List<SleepDataScoringFunction> scoringFunctions = new ArrayList<>();
    private final Map<Long, List<AmplitudeData>> dimensions;
    private final int dimensionCount;
    private final int rowCount;
    public static boolean debugMode = false;

    public MotionScoreAlgorithm(final Map<Long, List<AmplitudeData>> dataMatrix, final int dimensionCount, final int rowCount,
                                final List<SleepDataScoringFunction> scoringFunctions){

        checkNotNull(dataMatrix, "dataMatrix cannot be null");
        if(scoringFunctions.size() != dimensionCount){
            throw new IllegalArgumentException("Number of scoring function must be the same with data dimension.");
        }
        this.scoringFunctions.addAll(scoringFunctions);
        this.dimensions = dataMatrix;
        this.dimensionCount = dimensionCount;
        this.rowCount = rowCount;
    }


    public static Optional<InternalScore> getHighestScore(final List<InternalScore> scores){
        final List<InternalScore> copy = Ordering.natural().reverse().immutableSortedCopy(scores);

        if(scores.size() == 0){
            return Optional.absent();
        }

        return Optional.of(copy.get(0));
    }

    public List<Segment> getSleepEvents() throws AlgorithmException {
        final List<List<AmplitudeData>> rawData = new ArrayList<>();
        long timestampOfLastData = 0;

        for(int i = 0; i < this.dimensionCount; i++){
            rawData.add(new ArrayList<AmplitudeData>());
        }

        for(final Long timestamp:this.dimensions.keySet()){
            final List<AmplitudeData> dataVector = this.dimensions.get(timestamp);
            for(int k = 0; k < this.dimensionCount; k++){
                final AmplitudeData sensorData = dataVector.get(k);
                if(sensorData.timestamp != timestamp){
                    throw new AlgorithmException("Data not aligned, expected timestamp :" + timestamp + ","
                     + " actual timestamp: " + sensorData.timestamp);
                }
                checkNotNull(sensorData, "Unaligned data");
                rawData.get(k).add(sensorData);
            }
            if(timestamp > timestampOfLastData){
                timestampOfLastData = timestamp;
            }
        }
        // Step 2: generate ranking position
        final List<Map<AmplitudeData, EventScores>> sleepScorePDFs = new ArrayList<>();

        final ArrayList<InternalScore> fallAsleepScores = new ArrayList<>();
        final ArrayList<InternalScore> wakeUpScores = new ArrayList<>();
        final ArrayList<InternalScore> goToBedScores = new ArrayList<>();
        final ArrayList<InternalScore> outOfBedScores = new ArrayList<>();

        for(int i = 0; i < this.dimensionCount; i++){
            sleepScorePDFs.add(this.scoringFunctions.get(i).getPDF(rawData.get(i)));
        }

        for(int r = 0; r < this.rowCount; r++){
            double sleepScore = 1d;
            double wakeUpScore = 1d;
            double goToBedScore = 1d;
            double outOfBedScore = 1d;

            long timestamp = 0;
            boolean printedTime = false;

            for(int d = 0; d < this.dimensionCount; d++){

                final AmplitudeData datum = rawData.get(d).get(r);
                timestamp = datum.timestamp;

                if(!printedTime && debugMode){
                    LOGGER.debug("time {}: ", new DateTime(timestamp, DateTimeZone.forOffsetMillis(rawData.get(0).get(r).offsetMillis)));
                    printedTime = true;
                }


                final Map<AmplitudeData, EventScores> pdf = sleepScorePDFs.get(d);
                final SleepDataScoringFunction<AmplitudeData> scoringFunction = this.scoringFunctions.get(d);
                sleepScore *= scoringFunction.getScore(datum, pdf).sleepEventScore;
                wakeUpScore *= scoringFunction.getScore(datum, pdf).wakeUpEventScore;
                goToBedScore *= scoringFunction.getScore(datum, pdf).goToBedEventScore;
                outOfBedScore *= scoringFunction.getScore(datum, pdf).outOfBedEventScore;

                if(debugMode) {
                    LOGGER.debug("    {}, sleep: {}, wakeup: {}, in_bed: {}, out_bed: {}, val: {}",
                            d,
                            scoringFunction.getScore(datum, pdf).sleepEventScore,
                            scoringFunction.getScore(datum, pdf).wakeUpEventScore,
                            scoringFunction.getScore(datum, pdf).goToBedEventScore,
                            scoringFunction.getScore(datum, pdf).outOfBedEventScore,
                            datum.amplitude);
                }
            }
            fallAsleepScores.add(new InternalScore(timestamp, sleepScore));
            wakeUpScores.add(new InternalScore(timestamp, wakeUpScore));
            goToBedScores.add(new InternalScore(timestamp, goToBedScore));
            outOfBedScores.add(new InternalScore(timestamp, outOfBedScore));

            if(debugMode) {
                LOGGER.debug("goto_bed_prob {}, sleep_prob {}, wake up prob {}, out_bed_prob {}",
                        goToBedScore,
                        sleepScore,
                        wakeUpScore,
                        outOfBedScore);
            }
        }

        // Step 4: Pick the highest sleep and wake up scores, sleep and wake up detected.
        final Optional<InternalScore> wakeUpScore = getHighestScore(wakeUpScores);
        final Optional<InternalScore> fallAsleepScore = getHighestScore(fallAsleepScores);
        final Optional<InternalScore> goToBedScore = getHighestScore(goToBedScores);
        final Optional<InternalScore> outOfBedScore = getHighestScore(outOfBedScores);

        // We always have data, no need to check score.isPresent()
        final AmplitudeData fallAsleepData = this.dimensions.get(fallAsleepScore.get().timestamp).get(1);
        final AmplitudeData wakeUpData = this.dimensions.get(wakeUpScore.get().timestamp).get(1);
        final AmplitudeData goToBedData = this.dimensions.get(goToBedScore.get().timestamp).get(0);
        final AmplitudeData outOfBedData = this.dimensions.get(outOfBedScore.get().timestamp).get(0);


        LOGGER.info("Prob go to bed time: {}, score {}, amp {}", new DateTime(goToBedData.timestamp,
                        DateTimeZone.forOffsetMillis(goToBedData.offsetMillis)),
                goToBedScore.get().score,
                goToBedData.amplitude);
        LOGGER.info("Prob fall asleep time: {}, score {}, amp {}", new DateTime(fallAsleepData.timestamp,
                    DateTimeZone.forOffsetMillis(fallAsleepData.offsetMillis)),
                fallAsleepScore.get().score,
                fallAsleepData.amplitude);
        LOGGER.info("Prob wake up time: {}, score {}, amp {}", new DateTime(wakeUpData.timestamp,
                    DateTimeZone.forOffsetMillis(wakeUpData.offsetMillis)),
                wakeUpScore.get().score,
                wakeUpData.amplitude);
        LOGGER.info("Prob out of bed time: {}, score {}, amp {}", new DateTime(outOfBedData.timestamp,
                    DateTimeZone.forOffsetMillis(outOfBedData.offsetMillis)),
                outOfBedScore.get().score,
                outOfBedData.amplitude);

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

    public static Map<Long, List<AmplitudeData>> createFeatureMatrix(final List<AmplitudeData> firstFeatureDimension){
        final Map<Long, List<AmplitudeData>> matrix = new LinkedHashMap<>();
        for(final AmplitudeData motion:firstFeatureDimension){
            if(!matrix.containsKey(motion.timestamp)){
                matrix.put(motion.timestamp, new ArrayList<AmplitudeData>());
            }
            matrix.get(motion.timestamp).add(motion);
        }

        return matrix;
    }

    public static int addToFeatureMatrix(final Map<Long, List<AmplitudeData>> featureMatrix, final List<AmplitudeData> additionalFeature){

        int dimension = 0;
        for(final AmplitudeData feature:additionalFeature){
            if(!featureMatrix.containsKey(feature.timestamp)){
                LOGGER.error("feature not aligned on {}", feature.timestamp);
                throw new AlgorithmException("feature not aligned on " + new DateTime(feature.timestamp, DateTimeZone.forOffsetMillis(feature.offsetMillis)));
            }
            featureMatrix.get(feature.timestamp).add(feature);

            if(dimension == 0) {
                dimension = featureMatrix.get(feature.timestamp).size();
            }
        }

        return dimension;
    }
}
