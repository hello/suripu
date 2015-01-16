package com.hello.suripu.algorithm.sleep.scores;

import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.pdf.LinearRankAscendingScoringFunction;
import com.hello.suripu.algorithm.pdf.LinearRankDescendingScoringFunction;
import com.hello.suripu.algorithm.pdf.RankPowerScoringFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 12/16/14.
 */
public class AmplitudeDataScoringFunction implements SleepDataScoringFunction<AmplitudeData> {
    private final double wakeUpStartPercentage;
    private final double motionMaxPower;

    public AmplitudeDataScoringFunction(final double motionPloyDistributionParam, final double wakeUpPDFParam){
        this.motionMaxPower = motionPloyDistributionParam;
        this.wakeUpStartPercentage = wakeUpPDFParam;
    }

    @Override
    public Map<AmplitudeData, EventScores> getPDF(final Collection<AmplitudeData> data) {
        final List<Long> timestamps = new ArrayList<>(data.size());
        final List<Double> amplitudes = new ArrayList<>(data.size());
        for(final AmplitudeData amplitudeData:data){
            timestamps.add(Long.valueOf(amplitudeData.timestamp));
            amplitudes.add(Double.valueOf(amplitudeData.amplitude));
        }

        final LinearRankAscendingScoringFunction wakeUpTimeScoreFunction = new LinearRankAscendingScoringFunction(this.wakeUpStartPercentage);
        final Map<Long, Double> wakeUpTimePDF = wakeUpTimeScoreFunction.getPDF(timestamps);

        final LinearRankDescendingScoringFunction goToBedTimeScoreFunction = new LinearRankDescendingScoringFunction();  // sleep time should be desc
        final Map<Long, Double> goToBedTimePDF = goToBedTimeScoreFunction.getPDF(timestamps);

        final RankPowerScoringFunction amplitudeScoringFunction = new RankPowerScoringFunction(this.motionMaxPower);
        final Map<Double, Double> amplitudePDF = amplitudeScoringFunction.getPDF(amplitudes);
        final HashMap<AmplitudeData, EventScores> pdf = new HashMap<>();

        for(final AmplitudeData datum:data){
            final double motionScore = amplitudeScoringFunction.getScore(datum.amplitude, amplitudePDF);
            final double goToBedTimeScore = goToBedTimeScoreFunction.getScore(datum.timestamp, goToBedTimePDF);
            final double wakeUpTimeScore = wakeUpTimeScoreFunction.getScore(datum.timestamp, wakeUpTimePDF);

            pdf.put(datum, new EventScores(1d, wakeUpTimeScore * motionScore, goToBedTimeScore * motionScore));
        }
        return pdf;
    }

    @Override
    public EventScores getScore(final AmplitudeData data, final Map<AmplitudeData, EventScores> pdf) {
        if(pdf.containsKey(data)){
            return pdf.get(data);
        }

        // Not found, keep fall asleep score as it is, ground the wake up and go to bed scores.
        return new EventScores(1d, 0d, 0d);
    }
}
