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
 * Created by pangwu on 12/17/14.
 */
public class LightOutScoringFunction implements SleepDataScoringFunction<AmplitudeData> {
    private final List<DateTime> lightOutTime;
    private final double modalityWeight;
    public static final long LOOK_BACK_TIME_MS = 30 * DateTimeConstants.MILLIS_PER_MINUTE;
    public LightOutScoringFunction(final List<DateTime> lightOutTime, final double modalityWeight){
        this.lightOutTime = lightOutTime;
        this.modalityWeight = modalityWeight;
    }

    @Override
    public Map<AmplitudeData, EventScores> getPDF(final Collection<AmplitudeData> data) {
        final HashMap<AmplitudeData, EventScores> lightOutPDF = new HashMap<>();
        final long[] startTimestamps = new long[this.lightOutTime.size()];
        final long[] endTimestamps = new long[this.lightOutTime.size()];

        for(int i = 0; i < this.lightOutTime.size(); i++) {
            startTimestamps[i] = lightOutTime.get(i).getMillis() - LOOK_BACK_TIME_MS;
            endTimestamps[i] = lightOutTime.get(i).getMillis() + LOOK_BACK_TIME_MS;
        }

        final ArrayList<Double> lightScores = new ArrayList<>();
        for(final AmplitudeData datum:data){
            double sleepProbability = 1d;

            for(int i = 0; i < this.lightOutTime.size(); i++) {
                final long startTimestamp = startTimestamps[i];
                final long endTimestamp = endTimestamps[i];

                if (datum.timestamp >= startTimestamp && datum.timestamp < lightOutTime.get(i).getMillis()) {
                    sleepProbability = 1d + this.modalityWeight;
                }

                if (datum.timestamp >= lightOutTime.get(i).getMillis() && datum.timestamp <= endTimestamp) {
                    sleepProbability = 1d + this.modalityWeight;
                }

                if (datum.timestamp < startTimestamp) {
                    sleepProbability = 0.001;
                }

                lightScores.add(sleepProbability);
            }

            // since all scores are multiplied together and light out is just for go to bed detection
            // the other scores have to be 1.
            final double maxScore = Ordering.natural().max(lightScores);
            lightOutPDF.put(datum, new EventScores(1d, 1d, maxScore, 1d));
        }
        return lightOutPDF;
    }
}
