package com.hello.suripu.algorithm.sleep.scores;

import com.google.common.collect.Ordering;
import com.hello.suripu.algorithm.core.AmplitudeData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 2/1/15.
 */
public class LightOutCumulatedMotionMixScoringFunction implements SleepDataScoringFunction<AmplitudeData> {
    private final List<DateTime> lightOutTimes;
    public static final long LOOK_BACK_TIME_MS = 30 * DateTimeConstants.MILLIS_PER_MINUTE;

    public LightOutCumulatedMotionMixScoringFunction(final List<DateTime> lightOutTimes){
        this.lightOutTimes = lightOutTimes;
    }

    @Override
    public Map<AmplitudeData, EventScores> getPDF(final Collection<AmplitudeData> data) {
        final HashMap<AmplitudeData, EventScores> lightOutPDF = new HashMap<>();
        final long[] startTimestamps = new long[this.lightOutTimes.size()];
        final long[] endTimestamps = new long[this.lightOutTimes.size()];

        for(int i = 0; i < this.lightOutTimes.size(); i++) {
            startTimestamps[i] = lightOutTimes.get(i).getMillis() - LOOK_BACK_TIME_MS;
            endTimestamps[i] = lightOutTimes.get(i).getMillis() + LOOK_BACK_TIME_MS;
        }
        for(final AmplitudeData datum:data){

            final ArrayList<Double> lightScores = new ArrayList<>();
            for(int i = 0; i < this.lightOutTimes.size(); i++) {
                final long startTimestamp = startTimestamps[i];
                final long endTimestamp = endTimestamps[i];
                double sleepProbability = 1d;
                if (datum.timestamp >= startTimestamp && datum.timestamp < lightOutTimes.get(i).getMillis()) {
                    sleepProbability = 1d + datum.amplitude;  // feature value is out boost weight
                }

                if (datum.timestamp >= lightOutTimes.get(i).getMillis() && datum.timestamp <= endTimestamp) {
                    sleepProbability = 1d + datum.amplitude;    // feature value is out boost weight
                }

                if (datum.timestamp < startTimestamp) {
                    sleepProbability = 0.001;
                }

                lightScores.add(sleepProbability);
            }

            if(lightScores.size() == 0){
                lightScores.add(1d);
            }
            // since all scores are multiplied together and this is just for fall asleep detection
            // the other scores have to be 1.
            final double maxScore = Ordering.natural().max(lightScores);
            lightOutPDF.put(datum, new EventScores(maxScore, 1d, 1d, 1d));
        }
        return lightOutPDF;
    }
}
