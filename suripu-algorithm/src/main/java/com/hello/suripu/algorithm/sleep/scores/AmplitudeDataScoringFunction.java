package com.hello.suripu.algorithm.sleep.scores;

import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.pdf.LinearRankAscendingScoringFunction;
import com.hello.suripu.algorithm.pdf.LinearRankDescendingScoringFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 12/16/14.
 */
public class AmplitudeDataScoringFunction implements SleepDataScoringFunction<AmplitudeData> {
    private final double motionMaxPower;
    private final static Logger LOGGER = LoggerFactory.getLogger(MotionDensityScoringFunction.class);
    public AmplitudeDataScoringFunction(){
        this.motionMaxPower = 10;
    }

    @Override
    public Map<AmplitudeData, EventScores> getPDF(final Collection<AmplitudeData> data) {
        final List<Long> timestamps = new ArrayList<>(data.size());
        final List<Long> amplitudes = new ArrayList<>(data.size());
        for(final AmplitudeData amplitudeData:data){
            timestamps.add(Long.valueOf(amplitudeData.timestamp));
            amplitudes.add((long)amplitudeData.amplitude);
        }

        final LinearRankAscendingScoringFunction<Long> wakeUpTimeScoreFunction =
                new LinearRankAscendingScoringFunction<>(0d, 1d, new double[]{0.5d, 1d});
        final Map<Long, Double> outOfBedTimePDF = wakeUpTimeScoreFunction.getPDF(timestamps);

        final LinearRankDescendingScoringFunction<Long> goToBedTimeScoreFunction =
                new LinearRankDescendingScoringFunction<>(1d, 0d, new double[]{0d, 0.5d});  // sleep time should be desc
        final Map<Long, Double> goToBedTimePDF = goToBedTimeScoreFunction.getPDF(timestamps);

        final LinearRankAscendingScoringFunction<Long> amplitudeScoringFunction =
                new LinearRankAscendingScoringFunction<>(0d, 1d, new double[]{0d, 1d});
        final Map<Long, Double> amplitudePDF = amplitudeScoringFunction.getPDF(amplitudes);
        final HashMap<AmplitudeData, EventScores> pdf = new HashMap<>();

        for(final AmplitudeData datum:data){
            final double motionScore = amplitudePDF.get((long) datum.amplitude);
            final double goToBedTimeScore = goToBedTimePDF.get(datum.timestamp);
            final double outOfBedTimeScore = outOfBedTimePDF.get(datum.timestamp);

            pdf.put(datum, new EventScores(1d, 1d, goToBedTimeScore * Math.pow(motionScore, this.motionMaxPower),
                    outOfBedTimeScore * Math.pow(motionScore, this.motionMaxPower)));
        }
        return pdf;
    }
}
