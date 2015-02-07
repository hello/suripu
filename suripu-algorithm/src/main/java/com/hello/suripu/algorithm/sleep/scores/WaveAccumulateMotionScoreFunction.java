package com.hello.suripu.algorithm.sleep.scores;

import com.hello.suripu.algorithm.core.AmplitudeData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pangwu on 2/6/15.
 */
public class WaveAccumulateMotionScoreFunction implements SleepDataScoringFunction<AmplitudeData> {
    private final DateTime firstWaveTime;
    public static final long LOOK_BACK_TIME_MS = 20 * DateTimeConstants.MILLIS_PER_MINUTE;

    public WaveAccumulateMotionScoreFunction(final DateTime lightOutTime){
        this.firstWaveTime = lightOutTime;
    }

    @Override
    public Map<AmplitudeData, EventScores> getPDF(final Collection<AmplitudeData> data) {
        final HashMap<AmplitudeData, EventScores> lightOutPDF = new HashMap<>();
        final long startTimestamp = firstWaveTime.getMillis() - LOOK_BACK_TIME_MS;
        final long endTimestamp = firstWaveTime.getMillis() + LOOK_BACK_TIME_MS;
        for(final AmplitudeData datum:data){
            double wakeUpProbability = 1d;
            if(datum.timestamp >= startTimestamp && datum.timestamp < firstWaveTime.getMillis()){
                wakeUpProbability = 1d + datum.amplitude;  // feature value is out boost weight
            }

            if(datum.timestamp >= firstWaveTime.getMillis() && datum.timestamp <= endTimestamp){
                wakeUpProbability = 1d + datum.amplitude;    // feature value is out boost weight
            }

            if(datum.timestamp < startTimestamp){
                wakeUpProbability = 1d;
            }

            // since all scores are multiplied together and this is just for fall asleep detection
            // the other scores have to be 1.
            lightOutPDF.put(datum, new EventScores(1d, wakeUpProbability, 1d, 1d));
        }
        return lightOutPDF;
    }

    @Override
    public EventScores getScore(final AmplitudeData data, final Map<AmplitudeData, EventScores> pdf) {
        if(pdf.containsKey(data)){
            return pdf.get(data);
        }

        return new EventScores(1d, 1d, 1d, 1d);  // Not found, keep everything as it is.
    }
}
