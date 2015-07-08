package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.datascience.SleepHmmBayesNetProtos;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.TrackerMotion;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 6/24/15.
 */
public class HmmBayesNetPredictor {


    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(TimelineUtils.class);
    private final Logger LOGGER;

    private final DeserializedSleepHmmBayesNetWithParams allTheData;
    private final Map<String,EventProducer> eventProducers;

    public HmmBayesNetPredictor(final DeserializedSleepHmmBayesNetWithParams data, Optional<UUID> uuid) {
        //setup logger
        if (uuid.isPresent()) {
            LOGGER = new LoggerWithSessionId(STATIC_LOGGER, uuid.get());
        }
        else {
            LOGGER = new LoggerWithSessionId(STATIC_LOGGER);
        }

        //populate factory map
        eventProducers = Maps.newHashMap();
        eventProducers.put(HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_SLEEP,new SleepEventProducer());

        //later, later
        //eventProducers.put(HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_BED,new BedEventProducer());

        allTheData = data;
    }

    public ImmutableList<Event> getBayesNetHmmEvents(final DateTime targetDate, final DateTime endDate,final long  currentTimeMillis,final long accountId,
                                                      final AllSensorSampleList allSensorSampleList, final List<TrackerMotion> myMotion,final int timezoneOffset) {

        final Long startTimeUTC = targetDate.minusMillis(timezoneOffset).getMillis();
        final Long endTimeUTC = startTimeUTC + 60000L * 60 * 16;

        final List<Event> outputEvents = Lists.newArrayList();

        final Map<String, List<Event>> eventsByOutputId = makePredictions(allSensorSampleList, myMotion, startTimeUTC, endTimeUTC, timezoneOffset);

        final List<Event> events = eventsByOutputId.get(HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_SLEEP);

        //TODO add events for on/off bed

        if (events == null) {
            return ImmutableList.copyOf(Collections.EMPTY_LIST);
        }

        outputEvents.addAll(events);



        return ImmutableList.copyOf(outputEvents);


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
                LOGGER.warn("could not find event producer for conditional probabilities of {}", key);
                continue;
            }

            final List<Event> events = producer.getEventsFromProbabilitySequence(probsByOutputId,sensorData,t0,allTheData.params.numMinutesInMeasPeriod,timezoneOffset);

            eventsByOutputId.put(key,events);
        }

        return eventsByOutputId;

    }
}
