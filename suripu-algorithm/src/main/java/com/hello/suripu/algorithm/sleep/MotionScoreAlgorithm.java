package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
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
import java.util.Arrays;
import java.util.Comparator;
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

    public static Map<Comparable, Double> getRankingPositionMap(final List<Comparable> unsortedArray, final boolean orderByDescending){
        final Comparable[] sorted = unsortedArray.toArray(new Comparable[0]);
        Arrays.sort(sorted, new Comparator<Comparable>() {
            @Override
            public int compare(Comparable o1, Comparable o2) {
                if(!orderByDescending){
                    return o1.compareTo(o2);
                }else{
                    return -o1.compareTo(o2);
                }
            }
        });
        final LinkedHashMap<Comparable, Double> rankingPositions = new LinkedHashMap<>();
        for(int i = 0; i < sorted.length; i++){
            rankingPositions.put(sorted[i], Double.valueOf(i) / unsortedArray.size());
        }
        return rankingPositions;
    }

    public static double getScoreFromTimeLinearPDF(final Long timestamp, final Map<Comparable, Double> timestampRank){
        if(timestampRank.containsKey(timestamp)){
            return timestampRank.get(timestamp);  // It is linear distribution
        }

        return 0f;
    }

    public static double getScoreFromMotionPolyPDF(final Double amplitude, final Map<Comparable, Double> amplitudeRank){
        if(amplitudeRank.containsKey(amplitude)){
            return Math.pow(amplitudeRank.get(amplitude), 10);  // polynominals distribution with max power of 10  // TODO: Research is this personalizable?
        }

        return 0f;
    }

    public static Optional<InternalScore> getHighestScore(final InternalScore[] scores){
        final InternalScore[] copy = Arrays.copyOf(scores, scores.length);
        Arrays.sort(copy, new Comparator<InternalScore>() {
            @Override
            public int compare(InternalScore o1, InternalScore o2) {
                return -Double.compare(o1.score, o2.score);
            }
        });

        if(scores.length == 0){
            return Optional.absent();
        }

        return Optional.of(copy[0]);
    }

    @Override
    public Segment getSleepPeriod(final DateTime dateOfTheNightLocalUTC, final Optional<DateTime> sleepTimeThreshold) throws AlgorithmException {
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

        final Map<Comparable, Double> wakeUpTimestampRank = getRankingPositionMap(timestamps, false);
        final Map<Comparable, Double> sleepTimestampRank = getRankingPositionMap(timestamps, true);  // sleep time should be desc
        final Map<Comparable, Double> amplitudeRank = getRankingPositionMap(amplitudes, false);

        // Step 3: Get scores from ranking PDF function
        final InternalScore[] fallAsleepScores = new InternalScore[smoothedData.size()];
        final InternalScore[] wakeUpScores = new InternalScore[smoothedData.size()];
        for(int i = 0; i < smoothedData.size(); i++){
            final AmplitudeData data = smoothedData.get(i);
            final double sleepScore = getScoreFromTimeLinearPDF(data.timestamp, sleepTimestampRank) * getScoreFromMotionPolyPDF(data.amplitude, amplitudeRank);
            fallAsleepScores[i] = new InternalScore(data, sleepScore);

            final double wakeUpScore = getScoreFromTimeLinearPDF(data.timestamp, wakeUpTimestampRank) * getScoreFromMotionPolyPDF(data.amplitude, amplitudeRank);
            wakeUpScores[i] = new InternalScore(data, wakeUpScore);
            LOGGER.info("time {}, sleep_prob {}, wake up prob {}, amp {}",
                    new DateTime(data.timestamp, DateTimeZone.forOffsetMillis(data.offsetMillis)),
                    sleepScore,
                    wakeUpScore,
                    data.amplitude);

        }

        // Step 4: Pick the highest sleep and wake up scores, sleep and wake up detected.
        final Optional<InternalScore> wakeUpScore = getHighestScore(wakeUpScores);
        final Optional<InternalScore> fallAsleepScore = getHighestScore(fallAsleepScores);

        // We always has data, no need to check score.isPresent()
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
