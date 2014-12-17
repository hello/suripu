package com.hello.suripu.algorithm.sleep.scores;

import com.hello.suripu.algorithm.core.AmplitudeData;

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
        final List<Long> timestamps = new ArrayList<>();
        final List<Double> amplitudes = new ArrayList<>();
        for(final AmplitudeData amplitudeData:data){
            timestamps.add(Long.valueOf(amplitudeData.timestamp));
            amplitudes.add(Double.valueOf(amplitudeData.amplitude));
        }

        final WakeUpTimeScoringFunction wakeUpTimeScoreFunction = new WakeUpTimeScoringFunction(this.wakeUpStartPercentage);
        final Map<Long, Double> wakeUpTimePDF = wakeUpTimeScoreFunction.getPDF(timestamps);

        final SleepTimeScoringFunction sleepTimeScoreFunction = new SleepTimeScoringFunction();  // sleep time should be desc
        final Map<Long, Double> sleepTimePDF = sleepTimeScoreFunction.getPDF(timestamps);

        final MotionScoringFunction amplitudeScoringFunction = new MotionScoringFunction(this.motionMaxPower);
        final Map<Double, Double> amplitudePDF = amplitudeScoringFunction.getPDF(amplitudes);
        final HashMap<AmplitudeData, EventScores> pdf = new HashMap<>();

        for(final AmplitudeData datum:data){
            final double sleepScore = sleepTimeScoreFunction.getScore(datum.timestamp, sleepTimePDF) * amplitudeScoringFunction.getScore(datum.amplitude, amplitudePDF);
            final double wakeUpScore = wakeUpTimeScoreFunction.getScore(datum.timestamp, wakeUpTimePDF) * amplitudeScoringFunction.getScore(datum.amplitude, amplitudePDF);

            pdf.put(datum, new EventScores(sleepScore, wakeUpScore));
        }
        return pdf;
    }

    @Override
    public EventScores getScore(final AmplitudeData data, final Map<AmplitudeData, EventScores> pdf) {
        if(pdf.containsKey(data)){
            return pdf.get(data);
        }

        return new EventScores(0d, 0d);
    }
}
