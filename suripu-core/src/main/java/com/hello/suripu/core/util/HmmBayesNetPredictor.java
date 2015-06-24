package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.hello.suripu.algorithm.bayes.ProbabilitySegment;
import com.hello.suripu.algorithm.bayes.ProbabilitySegmenter;
import com.hello.suripu.algorithm.bayes.SensorDataReductionAndInterpretation;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.TrackerMotion;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 6/24/15.
 */
public class HmmBayesNetPredictor {
    private static final Double PROB_THRESHOLD = 0.95;

    private final HmmBayesNetDeserialization.DeserializedSleepHmmBayesNetWithParams allTheData;
    private final Map<String,EventProducer> eventProducers;

    public HmmBayesNetPredictor(HmmBayesNetDeserialization.DeserializedSleepHmmBayesNetWithParams allTheData) {
        this.allTheData = allTheData;

        //populate factory map
        eventProducers = Maps.newHashMap();
        eventProducers.put(HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_SLEEP,new SleepEventProducer());
        eventProducers.put(HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_BED,new BedEventProducer());

    }

    //returns list of events by output id (the name of the conditional probabilities that produced it)
    public Map<String,List<Event>> makePredictions(final AllSensorSampleList allSensorSampleList, final List<TrackerMotion> pillData, final long startTimeUTC, final long stopTimeUTC, final int timezoneOffset) {
        Map<String, List<Event>> eventsByOutputId = Maps.newHashMap();

        /*  Get the sensor data */
        final Optional<SleepHmmBayesNetSensorDataBinning.BinnedData> binnedDataOptional = SleepHmmBayesNetSensorDataBinning.getBinnedSensorData(allSensorSampleList, pillData, allTheData.params, startTimeUTC, stopTimeUTC, timezoneOffset);

        if (!binnedDataOptional.isPresent()) {
            return eventsByOutputId;
        }

        final SleepHmmBayesNetSensorDataBinning.BinnedData binnedData = binnedDataOptional.get();

        return makePredictions(binnedData.data,binnedData.t0,timezoneOffset);
    }

    public Map<String,List<Event>> makePredictions(final double [][] sensorData,final long t0, final int timezoneOffset) {

        Map<String, List<Event>> eventsByOutputId = Maps.newHashMap();

        /* use models to get probabilities of states */
        final Map<String,List<List<Double>>> probsByOutputId = allTheData.sensorDataReductionAndInterpretation.inferProbabilitiesFromModelAndSensorData(sensorData);

        /* process probabilities by   */
        for (final String key : eventProducers.keySet()) {
            //get prob(true,forwards) OR prob(true,backwards), i.e.  !(!Pf * !Pb)
            final EventProducer producer = eventProducers.get(key);

            if (producer == null) {
                //TODO log error
                continue;
            }

            final List<Event> events = producer.getEventsFromProbabilitySequence(probsByOutputId,sensorData,t0,allTheData.params.numMinutesInMeasPeriod,timezoneOffset);

            eventsByOutputId.put(key,events);
        }

        return eventsByOutputId;

    }
}
