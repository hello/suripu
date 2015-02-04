package com.hello.suripu.algorithm.sleep.scores;

import com.hello.suripu.algorithm.core.AmplitudeData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pangwu on 2/1/15.
 */
public class LightOutCumulatedMotionMixScoringFunction implements SleepDataScoringFunction<AmplitudeData> {
    private final DateTime lightOutTime;
    public static final long LOOK_BACK_TIME_MS = 30 * DateTimeConstants.MILLIS_PER_MINUTE;

    public LightOutCumulatedMotionMixScoringFunction(final DateTime lightOutTime){
        this.lightOutTime = lightOutTime;
    }

    @Override
    public Map<AmplitudeData, EventScores> getPDF(final Collection<AmplitudeData> data) {
        final HashMap<AmplitudeData, EventScores> lightOutPDF = new HashMap<>();
        final long startTimestamp = lightOutTime.getMillis() - LOOK_BACK_TIME_MS;
        final long endTimestamp = lightOutTime.getMillis() + LOOK_BACK_TIME_MS;
        for(final AmplitudeData datum:data){
            double sleepProbability = 1d;
            if(datum.timestamp >= startTimestamp && datum.timestamp < lightOutTime.getMillis()){
                sleepProbability = 1d + datum.amplitude;  // feature value is out boost weight
            }

            if(datum.timestamp >= lightOutTime.getMillis() && datum.timestamp <= endTimestamp){
                sleepProbability = 1d + datum.amplitude;    // feature value is out boost weight
            }

            if(datum.timestamp < startTimestamp){
                sleepProbability = 0.001;
            }

            // since all scores are multiplied together and this is just for fall asleep detection
            // the other scores have to be 1.
            lightOutPDF.put(datum, new EventScores(sleepProbability, 1d, 1d, 1d));
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
