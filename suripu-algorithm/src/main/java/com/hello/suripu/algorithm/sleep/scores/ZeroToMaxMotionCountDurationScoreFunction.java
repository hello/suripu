package com.hello.suripu.algorithm.sleep.scores;

import com.hello.suripu.algorithm.core.AmplitudeData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pangwu on 2/9/15.
 */
public class ZeroToMaxMotionCountDurationScoreFunction implements SleepDataScoringFunction<AmplitudeData> {

    @Override
    public Map<AmplitudeData, EventScores> getPDF(final Collection<AmplitudeData> data) {
        final Map<AmplitudeData, EventScores> pdf = new HashMap<>();
        for(final AmplitudeData datum:data){
            pdf.put(datum, new EventScores(1d, datum.amplitude, 1d, datum.amplitude));
        }
        return pdf;
    }
}
