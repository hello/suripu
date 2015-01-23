package com.hello.suripu.algorithm.sleep.scores;

import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.pdf.LinearRankAscendingScoringFunction;
import com.hello.suripu.algorithm.pdf.LinearRankDescendingScoringFunction;

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

        final LinearRankAscendingScoringFunction wakeUpTimeScoreFunction =
                new LinearRankAscendingScoringFunction(0d, 1d, new double[]{0.5d, 1d});
        final Map<Long, Double> outOfBedTimePDF = wakeUpTimeScoreFunction.getPDF(timestamps);

        final LinearRankDescendingScoringFunction goToBedTimeScoreFunction =
                new LinearRankDescendingScoringFunction(1d, 0d, new double[]{0d, 1d});  // sleep time should be desc
        final Map<Long, Double> goToBedTimePDF = goToBedTimeScoreFunction.getPDF(timestamps);

        final LinearRankAscendingScoringFunction amplitudeScoringFunction =
                new LinearRankAscendingScoringFunction(0d, 1d, new double[]{0d, 1d});
        final Map<Long, Double> amplitudePDF = amplitudeScoringFunction.getPDF(amplitudes);
        final HashMap<AmplitudeData, EventScores> pdf = new HashMap<>();

        for(final AmplitudeData datum:data){
            final double motionScore = amplitudeScoringFunction.getScore((long)datum.amplitude, amplitudePDF);
            final double goToBedTimeScore = goToBedTimeScoreFunction.getScore(datum.timestamp, goToBedTimePDF);
            final double outOfBedTimeScore = wakeUpTimeScoreFunction.getScore(datum.timestamp, outOfBedTimePDF);

            pdf.put(datum, new EventScores(1d, 1d, goToBedTimeScore * Math.pow(motionScore, this.motionMaxPower),
                    outOfBedTimeScore * Math.pow(motionScore, this.motionMaxPower)));
        }
        return pdf;
    }

    @Override
    public EventScores getScore(final AmplitudeData data, final Map<AmplitudeData, EventScores> pdf) {
        if(pdf.containsKey(data)){
            return pdf.get(data);
        }

        // Not found, keep fall asleep score as it is, ground the wake up and go to bed scores.
        return new EventScores(1d, 1d, 0d, 0d);
    }
}
