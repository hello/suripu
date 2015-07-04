package com.hello.suripu.core.util;

import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.bayes.SensorDataReductionAndInterpretation;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.core.models.Event;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 6/24/15.
 */
public class BedEventProducer implements  EventProducer{


    @Override
    public List<Event> getEventsFromProbabilitySequence(Map<String,List<List<Double>>> probsByOutputId, double [][] sensorData,Long t0, Integer numMinutesPerInterval, Integer timezoneOffset) throws AlgorithmException {
        final List<Event> events = Lists.newArrayList();
        final List<List<Double>> probsOfBed = probsByOutputId.get(HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_BED);
        final List<List<Double>> probsOfSleep = probsByOutputId.get(HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_SLEEP);

        if (probsOfBed == null) {
            throw  new AlgorithmException(String.format("BedEventProducer - could not find %s",HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_BED));
        }

        if (probsOfSleep == null) {
            throw  new AlgorithmException(String.format("BedEventProducer - could not find %s",HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_SLEEP));
        }


        final List<Double> sleepProbForwardsOrBackwards = SensorDataReductionAndInterpretation.getInverseOfNthElement(probsOfSleep, 1);

        return events;
    }
}
