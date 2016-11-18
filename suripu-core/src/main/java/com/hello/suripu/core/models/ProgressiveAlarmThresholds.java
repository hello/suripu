package com.hello.suripu.core.models;

import com.hello.suripu.algorithm.event.SleepCycleAlgorithm;
import org.joda.time.DateTimeConstants;

/**
 * Created by jarredheinrich on 11/17/16.
 */
public class ProgressiveAlarmThresholds {
    public final int amplitudeThreshold;
    public final int amplitudeThresholdCountLimit;
    public final int kickoffCountThreshold;
    public final int onDurationThreshold;

    private final static float KICKOFF_COUNT_DECAY_RATE = 0.10f; //kickoff count decays to 3 with 10 minutes left
    private final static int AMPLITUDE_DECAY_RATE=  140; // amplitude threshold decays to 3000 with 10 minutes left
    private final static float AMPLITUDE_COUNT_DECAY_RATE=  0.04f; // required count decays to 1 with 5 minutes left
    private final static float ON_DURATION_DECAY_RATE= 0.2f; //od threshold decays to 5 with 10 minutes left.


    public ProgressiveAlarmThresholds(final int amplitudeThreshold, final int amplitudeThresholdCountLimit, final int kickoffCountThreshold, final int onDurationThreshold) {
        this.amplitudeThreshold = amplitudeThreshold;
        this.amplitudeThresholdCountLimit = amplitudeThresholdCountLimit;
        this.kickoffCountThreshold = kickoffCountThreshold;
        this.onDurationThreshold = onDurationThreshold;
    }

    public ProgressiveAlarmThresholds(){
        this.amplitudeThreshold = SleepCycleAlgorithm.AWAKE_AMPLITUDE_THRESHOLD_MILLIG;
        this.amplitudeThresholdCountLimit = SleepCycleAlgorithm.AWAKE_AMPLITUDE_THRESHOLD_COUNT_LIMIT;
        this.kickoffCountThreshold = SleepCycleAlgorithm.AWAKE_KICKOFF_THRESHOLD;
        this.onDurationThreshold = SleepCycleAlgorithm.AWAKE_ON_DURATION_THRESHOLD;
    }

    public static ProgressiveAlarmThresholds getDecayingThreshold(final long currentTime, final long nextRingTime, final boolean useDecayingThresholds){

        final int elapsedMinutes = 30 - (int) (nextRingTime - currentTime)/ DateTimeConstants.MILLIS_PER_MINUTE;

        if (!useDecayingThresholds || elapsedMinutes <= 20){
            return new ProgressiveAlarmThresholds();
        }

        final int kickOffCountDecay = (int) (KICKOFF_COUNT_DECAY_RATE * elapsedMinutes);
        final int amplitudeDecay =  AMPLITUDE_DECAY_RATE* elapsedMinutes;
        final int amplitudeCountDecay = (int) (AMPLITUDE_COUNT_DECAY_RATE* elapsedMinutes);
        final int onDurationDecay = (int) (ON_DURATION_DECAY_RATE* elapsedMinutes);

        final int kickoffCountThreshold =  SleepCycleAlgorithm.AWAKE_KICKOFF_THRESHOLD - kickOffCountDecay;
        final int amplitudeThreshold = SleepCycleAlgorithm.AWAKE_AMPLITUDE_THRESHOLD_MILLIG - amplitudeDecay;
        final int amplitudeThresholdCountLimit = SleepCycleAlgorithm.AWAKE_AMPLITUDE_THRESHOLD_COUNT_LIMIT - amplitudeCountDecay ;
        final int onDurationThreshold = SleepCycleAlgorithm.AWAKE_ON_DURATION_THRESHOLD - onDurationDecay;

        return new ProgressiveAlarmThresholds(amplitudeThreshold, amplitudeThresholdCountLimit, kickoffCountThreshold, onDurationThreshold);
    }


}
