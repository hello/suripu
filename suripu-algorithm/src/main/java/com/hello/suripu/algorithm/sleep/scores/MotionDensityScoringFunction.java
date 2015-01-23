package com.hello.suripu.algorithm.sleep.scores;

import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.pdf.LinearRankAscendingScoringFunction;
import com.hello.suripu.algorithm.pdf.LinearRankDescendingScoringFunction;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 1/15/15.
 */
public class MotionDensityScoringFunction implements SleepDataScoringFunction<AmplitudeData> {

    public static enum ScoreType {
        SLEEP,
        WAKE_UP
    }
    private final static Logger LOGGER = LoggerFactory.getLogger(MotionDensityScoringFunction.class);
    private final double motionMaxPower;
    private final ScoreType type;

    public MotionDensityScoringFunction(final ScoreType type){
        this.motionMaxPower = 10;
        this.type = type;
    }

    @Override
    public Map<AmplitudeData, EventScores> getPDF(Collection<AmplitudeData> data) {
        final List<Long> timestamps = new ArrayList<>(data.size());
        final List<Long> amplitudes = new ArrayList<>(data.size());
        for(final AmplitudeData amplitudeData:data){
            timestamps.add(amplitudeData.timestamp);
            amplitudes.add((long)amplitudeData.amplitude);
        }

        final LinearRankDescendingScoringFunction sleepTimeScoreFunction =
                new LinearRankDescendingScoringFunction(1d, 1d, new double[]{0d, 0.5d});  // sleep time should be desc

        final LinearRankAscendingScoringFunction wakeUpTimeScoreFunction =
                new LinearRankAscendingScoringFunction(0d, 1d, new double[]{0.5d, 1.0d});

        final Map<Long, Double> sleepTimePDF = sleepTimeScoreFunction.getPDF(timestamps);
        final Map<Long, Double> wakeUpTimePDF = wakeUpTimeScoreFunction.getPDF(timestamps);

        final LinearRankAscendingScoringFunction sleepMotionScoringFunction =
                new LinearRankAscendingScoringFunction(0d, 1d, new double[]{0d, 1d});

        final LinearRankAscendingScoringFunction wakeUpMotionScoringFunction =
                new LinearRankAscendingScoringFunction(0d, 1d, new double[]{0d, 1d});

        final Map<Long, Double> sleepMotionDensityRankPDF = sleepMotionScoringFunction.getPDF(amplitudes);
        final Map<Long, Double> wakeUpMotionDensityRankPDF = wakeUpMotionScoringFunction.getPDF(amplitudes);

        final HashMap<AmplitudeData, EventScores> pdf = new HashMap<>();

        for(final AmplitudeData datum:data){
            if(type == ScoreType.SLEEP) {
                final double sleepMotionDensityScore = datum.amplitude == 0 ? 0 : sleepMotionScoringFunction.getScore((long) datum.amplitude,
                        sleepMotionDensityRankPDF);
                final double sleepTimeScore = sleepTimeScoreFunction.getScore(datum.timestamp, sleepTimePDF);
                pdf.put(datum, new EventScores(Math.pow(sleepMotionDensityScore, this.motionMaxPower) * sleepTimeScore,
                        1d, 1d, 1d));

                LOGGER.debug("    density {}: t {}, sl_r {}, wup 0, val {}",
                        new DateTime(datum.timestamp, DateTimeZone.forOffsetMillis(datum.offsetMillis)),
                        sleepTimeScore,
                        sleepMotionDensityScore,
                        datum.amplitude);
            }

            if(this.type == ScoreType.WAKE_UP) {
                final double wakeUpMotionDensityScore = datum.amplitude == 0 ? 0 : wakeUpMotionScoringFunction.getScore((long) datum.amplitude,
                        wakeUpMotionDensityRankPDF);
                final double wakeUpTimeScore = wakeUpTimeScoreFunction.getScore(datum.timestamp, wakeUpTimePDF);
                pdf.put(datum, new EventScores(1d,
                        Math.pow(wakeUpMotionDensityScore, this.motionMaxPower) * wakeUpTimeScore, 1d, 1d));
                LOGGER.debug("    density {}: t {}, sl_r 0, wup {}, val {}",
                        new DateTime(datum.timestamp, DateTimeZone.forOffsetMillis(datum.offsetMillis)),
                        wakeUpTimeScore,
                        wakeUpMotionDensityScore,
                        datum.amplitude);
            }

        }
        return pdf;
    }

    @Override
    public EventScores getScore(AmplitudeData data, Map<AmplitudeData, EventScores> pdf) {
        if(pdf.containsKey(data)){
            return pdf.get(data);
        }

        // Not found, keep fall asleep score as it is, ground the wake up and go to bed scores.
        if(this.type == ScoreType.SLEEP) {
            return new EventScores(0d, 1d, 1d, 1d);
        }

        return new EventScores(1d, 0d, 1d, 1d);
    }
}
