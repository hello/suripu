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

    public MotionScoreAlgorithm(final Map<Long, List<AmplitudeData>> dataMatrix, final int dimensionCount, final int rowCount, final List<SleepDataScoringFunction> scoringFunctions){

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
        }
        // Step 2: generate ranking position
        final List<Map<AmplitudeData, EventScores>> sleepScorePDFs = new ArrayList<>();

        final ArrayList<InternalScore> fallAsleepScores = new ArrayList<>();
        final ArrayList<InternalScore> wakeUpScores = new ArrayList<>();
        final ArrayList<InternalScore> goToBedScores = new ArrayList<>();

        for(int i = 0; i < this.dimensionCount; i++){
            sleepScorePDFs.add(this.scoringFunctions.get(i).getPDF(rawData.get(i)));
        }

        for(int r = 0; r < this.rowCount; r++){
            double sleepScore = 1d;
            double wakeUpScore = 1d;
            double goToBedScore = 1d;
            long timestamp = 0;
            for(int d = 0; d < this.dimensionCount; d++){

                final AmplitudeData datum = rawData.get(d).get(r);
                timestamp = datum.timestamp;
                final Map<AmplitudeData, EventScores> pdf = sleepScorePDFs.get(d);
                final SleepDataScoringFunction<AmplitudeData> scoringFunction = this.scoringFunctions.get(d);
                sleepScore *= scoringFunction.getScore(datum, pdf).sleepEventScore;
                wakeUpScore *= scoringFunction.getScore(datum, pdf).wakeUpEventScore;
                goToBedScore *= scoringFunction.getScore(datum, pdf).goToBedEventScore;
                //LOGGER.info("ds: {}, dw: {}", scoringFunction.getScore(datum, pdf).sleepEventScore, scoringFunction.getScore(datum, pdf).wakeUpEventScore);
            }
            fallAsleepScores.add(new InternalScore(timestamp, sleepScore));
            wakeUpScores.add(new InternalScore(timestamp, wakeUpScore));
            goToBedScores.add(new InternalScore(timestamp, goToBedScore));
            LOGGER.info("time {}, goto_bed_prob {}, sleep_prob {}, wake up prob {}, amp {}",
                    new DateTime(timestamp, DateTimeZone.forOffsetMillis(rawData.get(0).get(r).offsetMillis)),
                    goToBedScore,
                    sleepScore,
                    wakeUpScore,
                    rawData.get(0).get(r).amplitude);
        }

        // Step 4: Pick the highest sleep and wake up scores, sleep and wake up detected.
        final Optional<InternalScore> wakeUpScore = getHighestScore(wakeUpScores);
        final Optional<InternalScore> fallAsleepScore = getHighestScore(fallAsleepScores);
        final Optional<InternalScore> goToBedScore = getHighestScore(goToBedScores);

        // We always have data, no need to check score.isPresent()
        final AmplitudeData fallAsleepData = this.dimensions.get(fallAsleepScore.get().timestamp).get(0);
        final AmplitudeData wakeUpData = this.dimensions.get(wakeUpScore.get().timestamp).get(0);
        AmplitudeData goToBedData = this.dimensions.get(goToBedScore.get().timestamp).get(0);

        if(goToBedData.timestamp > fallAsleepData.timestamp){
            LOGGER.warn("Go to bed later the fall asleep");
            goToBedData = fallAsleepData;
        }

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

        final ArrayList<Segment> sleepEvents = new ArrayList<>();
        sleepEvents.add(new Segment(goToBedScore.get().timestamp, goToBedScore.get().timestamp, goToBedData.offsetMillis));
        sleepEvents.add(new Segment(fallAsleepScore.get().timestamp, fallAsleepScore.get().timestamp, fallAsleepData.offsetMillis));
        sleepEvents.add(new Segment(wakeUpScore.get().timestamp, wakeUpScore.get().timestamp, wakeUpData.offsetMillis));

        return sleepEvents;
    }

    public static Map<Long, List<AmplitudeData>> getMatrix(final List<AmplitudeData> smoothedMotion){
        final Map<Long, List<AmplitudeData>> matrix = new LinkedHashMap<>();
        for(final AmplitudeData motion:smoothedMotion){
            if(!matrix.containsKey(motion.timestamp)){
                matrix.put(motion.timestamp, new ArrayList<AmplitudeData>());
            }
            matrix.get(motion.timestamp).add(motion);
        }

        return matrix;
    }
}
