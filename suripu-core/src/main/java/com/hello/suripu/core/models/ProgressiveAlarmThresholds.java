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

    private final static float KICKOFF_COUNT_DECAY_RATE = 0.20f; //kickoff count decays to 2 with 15 minutes left - so starting with motion events 22 minutes before alarm time.
    private final static int AMPLITUDE_DECAY_RATE =  267; // amplitude threshold decays to 500 with 15 minutes left
    private final static float AMPLITUDE_COUNT_DECAY_RATE =  0.07f; // required count decays to 1 with 15 minutes left
    private final static float ON_DURATION_DECAY_RATE = 0.45f; //od threshold decays to 2 with 15 minutes left.
    private final static int AMPLITUDE_MIN_THRESHOLD = 0;
    private final static int ON_DURATION_MIN_THRESHOLD = 2;
    private final static int KICKOFF_COUNT_MIN_THREHSOLD = 2;
    private final static int AMPLITUDE_MIN_COUNT = 1;


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

        if (!useDecayingThresholds || elapsedMinutes <= 5){
            return new ProgressiveAlarmThresholds();
        }

        final int kickOffCountDecay = (int) (KICKOFF_COUNT_DECAY_RATE * elapsedMinutes);
        final int amplitudeDecay =  AMPLITUDE_DECAY_RATE* elapsedMinutes;
        final int amplitudeCountDecay = (int) (AMPLITUDE_COUNT_DECAY_RATE* elapsedMinutes);
        final int onDurationDecay = (int) (ON_DURATION_DECAY_RATE* elapsedMinutes);

        final int kickoffCountThreshold =  Math.max(SleepCycleAlgorithm.AWAKE_KICKOFF_THRESHOLD - kickOffCountDecay, KICKOFF_COUNT_MIN_THREHSOLD);
        final int amplitudeThreshold = Math.max(SleepCycleAlgorithm.AWAKE_AMPLITUDE_THRESHOLD_MILLIG - amplitudeDecay, AMPLITUDE_MIN_THRESHOLD);
        final int amplitudeThresholdCountLimit = Math.max(SleepCycleAlgorithm.AWAKE_AMPLITUDE_THRESHOLD_COUNT_LIMIT - amplitudeCountDecay, AMPLITUDE_MIN_COUNT) ;
        final int onDurationThreshold = Math.max(SleepCycleAlgorithm.AWAKE_ON_DURATION_THRESHOLD - onDurationDecay, ON_DURATION_MIN_THRESHOLD);

        return new ProgressiveAlarmThresholds(amplitudeThreshold, amplitudeThresholdCountLimit, kickoffCountThreshold, onDurationThreshold);
    }


}
