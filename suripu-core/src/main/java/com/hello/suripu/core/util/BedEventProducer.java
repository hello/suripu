package com.hello.suripu.core.util;

import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.bayes.SensorDataReductionAndInterpretation;
import com.hello.suripu.core.models.Event;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 6/24/15.
 */
public class BedEventProducer implements  EventProducer{
    private final Logger LOGGER;

    //we pass in logger directly so we have the UUID
    public BedEventProducer(Logger logger) {
        LOGGER = logger;
    }

    @Override
    public List<Event> getEventsFromProbabilitySequence(Map<String,List<List<Double>>> probsByOutputId, double [][] sensorData,Long t0, Integer numMinutesPerInterval, Integer timezoneOffset) {
        final List<Event> events = Lists.newArrayList();
        final List<List<Double>> probsOfBed = probsByOutputId.get(HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_BED);
        final List<List<Double>> probsOfSleep = probsByOutputId.get(HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_SLEEP);

        if (probsOfBed == null) {
            LOGGER.error("BedEventProducer - could not find {}",HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_BED);
            return events;
        }

        if (probsOfSleep == null) {
            LOGGER.error("BedEventProducer - could not find {}",HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_SLEEP);
            return events;
        }

        final List<Double> sleepProbForwardsOrBackwards = SensorDataReductionAndInterpretation.getInverseOfNthElement(probsOfSleep, 1);

        return events;
    }
}
