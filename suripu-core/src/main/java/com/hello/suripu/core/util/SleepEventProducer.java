package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.bayes.ProbabilitySegment;
import com.hello.suripu.algorithm.bayes.ProbabilitySegmenter;
import com.hello.suripu.algorithm.bayes.SensorDataReductionAndInterpretation;
import com.hello.suripu.api.datascience.SleepHmmBayesNetProtos;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.translations.English;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 6/24/15.
 */
public class SleepEventProducer implements EventProducer {

    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(SleepEventProducer.class);
    private final Logger LOGGER;

    private static final Long NUM_MINUTES_IN_MILLIS = 60000L;

    private final Double NOMINAL_HOURS_SLEEPING = 8.0;
    private final Double PRE_AND_POST_DURATION_HOURS = 0.5 * (16.0 - NOMINAL_HOURS_SLEEPING);

    public SleepEventProducer(Optional<UUID> uuidOptional) {
        this.LOGGER = new LoggerWithSessionId(STATIC_LOGGER, uuidOptional);
    }


    @Override
    public List<Event> getEventsFromProbabilitySequence(final Map<String,List<List<Double>>> probsByOutputId,
                                                        final double [][] sensorData,
                                                        final Long t0,
                                                        final Integer numMinutesPerInterval,
                                                        final Integer timezoneOffset) {
        final List<Event> events = Lists.newArrayList();
        final List<List<Double>> probs = probsByOutputId.get(HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_SLEEP);

        if (probs == null) {
            LOGGER.error("cond null probabilities from {}",HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_SLEEP);
            return events;
        }

        final List<Double> sleepProbForwardsOrBackwards = SensorDataReductionAndInterpretation.getInverseOfNthElement(probs, 1);

        final int numIntervalsPre = ((int)(PRE_AND_POST_DURATION_HOURS * 60.0))  / numMinutesPerInterval;
        final int numIntervalsDuring = ((int)(NOMINAL_HOURS_SLEEPING * 60.0))  / numMinutesPerInterval;
        final int numIntervalsPost = ((int)(PRE_AND_POST_DURATION_HOURS * 60.0))  / numMinutesPerInterval;

        final Optional<ProbabilitySegment> segmentOptional = ProbabilitySegmenter.getBestSegment(numIntervalsPre, numIntervalsDuring, numIntervalsPost, sleepProbForwardsOrBackwards);

        if (!segmentOptional.isPresent()) {
            LOGGER.info("no segments found, therefore not returning any events");
            return events;
        }

        final ProbabilitySegment seg = segmentOptional.get();

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


}
