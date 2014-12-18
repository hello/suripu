package com.hello.suripu.algorithm.sleep.scores;

import com.hello.suripu.algorithm.core.AmplitudeData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pangwu on 12/17/14.
 */
public class LightOutScoringFunction implements SleepDataScoringFunction<AmplitudeData> {
    @Override
    public Map<AmplitudeData, EventScores> getPDF(final Collection<AmplitudeData> data) {
        final HashMap<AmplitudeData, EventScores> lightOutPDF = new HashMap<>();
        for(final AmplitudeData datum:data){
            if(datum.amplitude == 0){
                // since all scores are multiplied together and light out is just for fall asleep detection
                // the wake up score has to be 1.
                lightOutPDF.put(datum, new EventScores(0d, 1d));
            }else{
                lightOutPDF.put(datum, new EventScores(1d, 1d));
            }
        }
        return lightOutPDF;
    }

    @Override
    public EventScores getScore(final AmplitudeData data, final Map<AmplitudeData, EventScores> pdf) {
        if(pdf.containsKey(data)){
            return pdf.get(data);
        }

        return new EventScores(1d, 1d);  // Not found, keep everything as it is.
    }
}
