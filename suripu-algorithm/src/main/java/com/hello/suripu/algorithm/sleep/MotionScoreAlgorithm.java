package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.google.common.collect.Ordering;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.AmplitudeDataPreprocessor;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.MaxAmplitudeAggregator;
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
public class MotionScoreAlgorithm extends SleepDetectionAlgorithm {
    private final static Logger LOGGER = LoggerFactory.getLogger(MotionScoreAlgorithm.class);

    public MotionScoreAlgorithm(final DataSource<AmplitudeData> dataSource, final int smoothWindowMillis){
        super(dataSource, smoothWindowMillis);
    }

    public static Map<Comparable, Double> getLinearRankingPositionPDF(final List<Comparable> unsortedArray, final double cutPercentage, final boolean orderByDescending){

        List<Comparable> sortedCopy = Ordering.natural().immutableSortedCopy(unsortedArray);
        if(orderByDescending){
            sortedCopy = Ordering.natural().reverse().immutableSortedCopy(unsortedArray);
        }

        final LinkedHashMap<Comparable, Double> rankingPositions = new LinkedHashMap<>();
        final double cutBound = unsortedArray.size() * cutPercentage;
        for(int i = 0; i < sortedCopy.size(); i++){
            rankingPositions.put(sortedCopy.get(i), Double.valueOf(i - cutBound) / (unsortedArray.size() - cutBound));
        }
        return rankingPositions;
    }

    public static Map<Comparable, Double> getPloyRankingPositionPDF(final List<Comparable> unsortedArray, final double maxPower){

        final List<Comparable> sortedCopy = Ordering.natural().immutableSortedCopy(unsortedArray);

        final LinkedHashMap<Comparable, Double> rankingPositions = new LinkedHashMap<>();
        for(int i = 0; i < sortedCopy.size(); i++){
            rankingPositions.put(sortedCopy.get(i), Math.pow(Double.valueOf(i) / unsortedArray.size(), maxPower));
        }
        return rankingPositions;
    }

    public static double getScoreFromPDF(final Comparable value, final Map<Comparable, Double> pdf){
        if(pdf.containsKey(value)){
            return pdf.get(value);  // It is linear distribution
        }

        return 0f;
    }


    public static Optional<InternalScore> getHighestScore(final List<InternalScore> scores){
        final List<InternalScore> copy = Ordering.natural().reverse().immutableSortedCopy(scores);

        if(scores.size() == 0){
            return Optional.absent();
        }

        return Optional.of(copy.get(0));
    }

    @Override
    public Segment getSleepPeriod(final DateTime dateOfTheNightLocalUTC) throws AlgorithmException {
        final List<AmplitudeData> rawData = getDataSource().getDataForDate(dateOfTheNightLocalUTC);
        if(rawData.size() == 0){
            throw new AlgorithmException("No data available for date: " + dateOfTheNightLocalUTC);
        }
        LOGGER.info("Raw data size {}", rawData.size());

        // Step 1: Aggregate the data based on a 10 minute interval.
        final AmplitudeDataPreprocessor smoother = new MaxAmplitudeAggregator(getSmoothWindow());
        final List<AmplitudeData> smoothedData = smoother.process(rawData);
        LOGGER.info("smoothed data size {}", smoothedData.size());


        // Step 2: generate ranking position
        final List<Comparable> timestamps = new ArrayList<>();
        final List<Comparable> amplitudes = new ArrayList<>();
        for(final AmplitudeData amplitudeData:smoothedData){
            timestamps.add(Long.valueOf(amplitudeData.timestamp));
            amplitudes.add(Double.valueOf(amplitudeData.amplitude));
        }

        final Map<Comparable, Double> wakeUpTimePDF = getLinearRankingPositionPDF(timestamps, 0.5, false);
        final Map<Comparable, Double> sleepTimePDF = getLinearRankingPositionPDF(timestamps, 0, true);  // sleep time should be desc
        final Map<Comparable, Double> amplitudePDF = getPloyRankingPositionPDF(amplitudes, 10);

        // Step 3: Get scores from ranking PDF function
        final ArrayList<InternalScore> fallAsleepScores = new ArrayList<>();
        final ArrayList<InternalScore> wakeUpScores = new ArrayList<>();
        for(int i = 0; i < smoothedData.size(); i++){
            final AmplitudeData data = smoothedData.get(i);
            final double sleepScore = getScoreFromPDF(data.timestamp, sleepTimePDF) * getScoreFromPDF(data.amplitude, amplitudePDF);
            fallAsleepScores.add(new InternalScore(data, sleepScore));

            final double wakeUpScore = getScoreFromPDF(data.timestamp, wakeUpTimePDF) * getScoreFromPDF(data.amplitude, amplitudePDF);
            wakeUpScores.add(new InternalScore(data, wakeUpScore));
            LOGGER.info("time {}, sleep_prob {}, wake up prob {}, amp {}",
                    new DateTime(data.timestamp, DateTimeZone.forOffsetMillis(data.offsetMillis)),
                    sleepScore,
                    wakeUpScore,
                    data.amplitude);

        }

        // Step 4: Pick the highest sleep and wake up scores, sleep and wake up detected.
        final Optional<InternalScore> wakeUpScore = getHighestScore(wakeUpScores);
        final Optional<InternalScore> fallAsleepScore = getHighestScore(fallAsleepScores);

        // We always have data, no need to check score.isPresent()
        LOGGER.info("Prob fall asleep time: {}, score {}, amp {}", new DateTime(fallAsleepScore.get().data.timestamp,
                DateTimeZone.forOffsetMillis(fallAsleepScore.get().data.offsetMillis)),
                fallAsleepScore.get().score,
                fallAsleepScore.get().data.amplitude);
        LOGGER.info("Prob wake up time: {}, score {}, amp {}", new DateTime(wakeUpScore.get().data.timestamp,
                DateTimeZone.forOffsetMillis(wakeUpScore.get().data.offsetMillis)),
                wakeUpScore.get().score,
                wakeUpScore.get().data.amplitude);

        return new Segment(fallAsleepScore.get().data.timestamp, wakeUpScore.get().data.timestamp, fallAsleepScore.get().data.offsetMillis);
    }
}
