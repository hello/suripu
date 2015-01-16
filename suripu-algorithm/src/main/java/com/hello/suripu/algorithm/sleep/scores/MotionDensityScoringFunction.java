package com.hello.suripu.algorithm.sleep.scores;

import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.pdf.LinearRankDescendingScoringFunction;
import com.hello.suripu.algorithm.pdf.RankPowerScoringFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 1/15/15.
 */
public class MotionDensityScoringFunction implements SleepDataScoringFunction<AmplitudeData> {

    private final double motionMaxPower;

    public MotionDensityScoringFunction(final double motionPloyDistributionParam){
        this.motionMaxPower = motionPloyDistributionParam;
    }

    @Override
    public Map<AmplitudeData, EventScores> getPDF(Collection<AmplitudeData> data) {
        final List<Long> timestamps = new ArrayList<>(data.size());
        final List<Double> amplitudes = new ArrayList<>(data.size());
        for(final AmplitudeData amplitudeData:data){
            timestamps.add(Long.valueOf(amplitudeData.timestamp));
            amplitudes.add(Double.valueOf(amplitudeData.amplitude));
        }

        final LinearRankDescendingScoringFunction sleepTimeScoreFunction = new LinearRankDescendingScoringFunction();  // sleep time should be desc
        final Map<Long, Double> sleepTimePDF = sleepTimeScoreFunction.getPDF(timestamps);

        final RankPowerScoringFunction amplitudeScoringFunction = new RankPowerScoringFunction(this.motionMaxPower);
        final Map<Double, Double> motionDensityRankPDF = amplitudeScoringFunction.getPDF(amplitudes);
        final HashMap<AmplitudeData, EventScores> pdf = new HashMap<>();

        for(final AmplitudeData datum:data){
            final double motionDensityScore = amplitudeScoringFunction.getScore(datum.amplitude, motionDensityRankPDF);
            final double sleepTimeScore = sleepTimeScoreFunction.getScore(datum.timestamp, sleepTimePDF);

            pdf.put(datum, new EventScores(motionDensityScore * sleepTimeScore, 1d, 1d));
        }
        return pdf;
    }

    @Override
    public EventScores getScore(AmplitudeData data, Map<AmplitudeData, EventScores> pdf) {
        if(pdf.containsKey(data)){
            return pdf.get(data);
        }

        // Not found, keep fall asleep score as it is, ground the wake up and go to bed scores.
        return new EventScores(0d, 1d, 1d);
    }
}
