package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.bayes.ProbabilitySegment;
import com.hello.suripu.algorithm.bayes.ProbabilitySegmenter;
import com.hello.suripu.algorithm.bayes.SensorDataReductionAndInterpretation;
import com.hello.suripu.api.datascience.SleepHmmBayesNetProtos;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 6/24/15.
 */
public class SleepEventProducer implements EventProducer {
    private static final Long NUM_MINUTES_IN_MILLIS = 60000L;

    private final Double PROB_THRESHOLD = 0.90;
    private final Integer MIN_DURATION_OF_SLEEP_IN_MINUTES = 20;
    private final Integer MAX_WAKEUP_PERIOD_IN_MINUTES = 60; //nobody spends more than this time in bed not sleeping when waking up
    private final Double MIN_AVG_MOTION_TO_BE_WAKEFUL = 1.0; //per minute

    private final Double NOMINAL_HOURS_SLEEPING = 8.0;
    private final Double PRE_AND_POST_DURATION_HOURS = 0.5 * (16.0 - NOMINAL_HOURS_SLEEPING);

    @Override
    public List<Event> getEventsFromProbabilitySequence(final Map<String,List<List<Double>>> probsByOutputId,
                                                        final double [][] sensorData,
                                                        final Long t0,
                                                        final Integer numMinutesPerInterval,
                                                        final Integer timezoneOffset) {
        final List<Event> events = Lists.newArrayList();
        final List<List<Double>> probs = probsByOutputId.get(HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_SLEEP);

        if (probs == null) {
            //TODO LOG ERROR
            return events;
        }

        final List<Double> sleepProbForwardsOrBackwards = SensorDataReductionAndInterpretation.getInverseOfNthElement(probs, 1);

        final int numIntervalsPre = ((int)(PRE_AND_POST_DURATION_HOURS * 60.0))  / numMinutesPerInterval;
        final int numIntervalsDuring = ((int)(NOMINAL_HOURS_SLEEPING * 60.0))  / numMinutesPerInterval;
        final int numIntervalsPost = ((int)(PRE_AND_POST_DURATION_HOURS * 60.0))  / numMinutesPerInterval;

        final ProbabilitySegment seg = ProbabilitySegmenter.getBestSegment(numIntervalsPre, numIntervalsDuring, numIntervalsPost, sleepProbForwardsOrBackwards);

        final long sleepTimestamp = getTimestamp(seg.i1,t0,timezoneOffset,numMinutesPerInterval);
        final long wakeTimestamp = getTimestamp(seg.i2,t0,timezoneOffset,numMinutesPerInterval);

        events.add(Event.createFromType(Event.Type.SLEEP, sleepTimestamp, sleepTimestamp + NUM_MINUTES_IN_MILLIS, timezoneOffset,
                Optional.of(English.FALL_ASLEEP_MESSAGE), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent()));

        events.add(Event.createFromType(Event.Type.WAKE_UP, wakeTimestamp, wakeTimestamp + NUM_MINUTES_IN_MILLIS, timezoneOffset,
                Optional.of(English.WAKE_UP_MESSAGE), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent()));

        return events;
    }

    static Long getTimestamp(final int idx, final long t0, final int timezoneOffset, final int measPeriodInMinutes) {
        return t0 + (measPeriodInMinutes*idx*NUM_MINUTES_IN_MILLIS);
    }

    static double getWeightedAverageOfMotionFeatures(final ProbabilitySegment seg, final double[][] sensordata, final int numMinutesPerInterval) {
        final double [] motionDuration = sensordata[SleepHmmBayesNetProtos.MeasType.MOTION_DURATION_VALUE];
        final double [] disturbances = sensordata[SleepHmmBayesNetProtos.MeasType.PILL_MAGNITUDE_DISTURBANCE_VALUE];

        double motion = 0.0;
        int numDisturbances = 0;
        for (int i = seg.i1; i <= seg.i2; i++) {
            motion += motionDuration[i];
            if (disturbances[i] > 0.0) {
                numDisturbances++;
            }
        }

        motion += numDisturbances * 30.0;

        final double duration = (seg.i2 - seg.i1 + 1) * numMinutesPerInterval;
        final double avgmotion = motion / duration;

        return avgmotion;

    }

}
