package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.algorithm.bayes.ProbabilitySegment;
import com.hello.suripu.algorithm.bayes.ProbabilitySegmenter;
import com.hello.suripu.algorithm.bayes.SensorDataReductionAndInterpretation;
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

    private static final String DEFAULT_PROTOBUF = "CiEIBRAAGQAAAAAAADBAIQAAAAAAABBAOAFBAAAAAABMzUASfAoMZGlzdHVyYmFuY2VzEhIuL2Rpc3R1cmJhbmNlLmpzb24aGAgAEAJKEgmuR+F6FK7vPwl7FK5H4XqEPxoYCAEQAkoSCXsUrkfheoQ/Ca5H4XoUru8/IQAAAAAAAOA/IQAAAAAAAOA/IQAAAAAAAOA/IQAAAAAAAOA/KAISuy8KBm1vdGlvbhINLi9tb3Rpb24uanNvbhoPCAAQAUIJCZqZmZmZmbk/Gg8IARABQgkJpFTCE3p9/D8aDwgCEAFCCQmHinH+JihLQBoPCAMQAUIJCSb+KOrMvSpAGg8IBBABQgkJSIeHMH66EEAaDwgFEAFCCQkjvD0IAdkdQBoPCAYQAUIJCUEqxY7Gobo/Gg8IBxABQgkJzCVV202wD0AaDwgIEAFCCQkMWkjA6MNXQBoPCAkQAUIJCXVat0HtszxAGg8IChABQgkJ/tXjvtWgQEAaDwgLEAFCCQlBvK5fsOJkQBoPCAwQAUIJCVEtIorJm/g/Gg8IDRABQgkJBwq8k0/fGEAaDwgOEAFCCQniqx3FOfo6QBoPCA8QAUIJCajg8IKIVBdAGg8IEBABQgkJZ0P+mUGcHUAaDwgREAFCCQlnnlxTIAsuQBoPCBIQAUIJCUEtBg/TBixAGg8IExABQgkJ3gIJih9jIkAaDwgUEAFCCQmlaybfbKMWQBoPCBUQAUIJCfaWcr7YOzRAGg8IFhABQgkJHt5zYDnaIkAaDwgXEAFCCQkmGTkLe6oVQBoPCBgQAUIJCaj8a3nlajZAIbFppRDIpe8/IVVKdDugEWE/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IbWv8ro0NGQ/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IfcGX5hMFdQ/IUhuTbotkdQ/IQte9BWkGYs/Ifyp8dJNYlA/Ifyp8dJNYlA/ISob1lQWhc0/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IWco7niT37I/Ickh4uZUMqg/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IZBBOLBmro0/ISm0rPvHQtY/Ifyp8dJNYlA/ITV7oBUYssA/IeGaO/pfrp0/ISm0rPvHQrY/IeGaO/pfrq0/ISm0rPvHQsY/IeGaO/pfrr0/IZBBOLBmro0/Ifyp8dJNYlA/IeGaO/pfrp0/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IYhJuJBHcL8/IYBSYSoF9oM/IbBW7ZqQ1sI/Ifyp8dJNYlA/IQ0dO6jEdaQ/IdsTJLa7h+A/Ifyp8dJNYlA/Ifyp8dJNYlA/IQolOJrIcaw/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Iayql99pMrs/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IYDVkSOdgdM/Ifyp8dJNYlA/Ifyp8dJNYlA/IUAVN24xP+Y/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ic2pCUc1kY4/Ie3T8ZiByuE/Ifyp8dJNYlA/IfD6zFmfcsY/Ifyp8dJNYlA/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Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IQn84ee/B58/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IcrFGFjHcew/IRzyKsZUVbU/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IQolOJrIcZw/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IcrFGFjHcew/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IdU3ekZswbY/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/ISkO+5hqwZY/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IfwBDwwgfOA/IQn84ee/B48/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IQd96e3PRac/Ifyp8dJNYlA/IYi85erHJts/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IctpT8k5saM/Ifyp8dJNYlA/IViP+1brxO4/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IU3XE10XfuU/Ifyp8dJNYlA/IXVWC+wxka4/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IQLsSJrk7KY/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/ITAPmfIhqMw/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IcP1KFyPwuU/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IXsUrkfhetQ/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IZqZmZmZmck/ISkO+5hqwaY/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IY19ycaDLeg/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/Ifyp8dJNYlA/IZqZmZmZmbk/Ifyp8dJNYlA/IZqZmZmZmbk/Ifyp8dJNYlA/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";
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(TimelineUtils.class);
    private final Logger LOGGER;

    private final HmmBayesNetDeserialization.DeserializedSleepHmmBayesNetWithParams allTheData;
    private final Map<String,EventProducer> eventProducers;


    static public Optional<HmmBayesNetPredictor> createHmmBayesNetPredictor(Optional<HmmBayesNetDeserialization.DeserializedSleepHmmBayesNetWithParams> allTheData,final Optional<UUID> uuid) {



        if (allTheData.isPresent()) {
            return Optional.of(new HmmBayesNetPredictor(allTheData.get(),uuid));
        }
        else {
            //deserialize default and use that instead
            try {
                final byte[] decodedBytes = Base64.decodeBase64(DEFAULT_PROTOBUF);
                final SleepHmmBayesNetProtos.HmmBayesNet bayesNet = SleepHmmBayesNetProtos.HmmBayesNet.parseFrom(decodedBytes);
                final HmmBayesNetDeserialization.DeserializedSleepHmmBayesNetWithParams data = HmmBayesNetDeserialization.Deserialize(bayesNet);
                return Optional.of(new HmmBayesNetPredictor(data,uuid));
            }
            catch (InvalidProtocolBufferException exception) {
                STATIC_LOGGER.error("{} {}",uuid.toString(),exception.toString());
            }
        }

        return Optional.absent();
    }

    private HmmBayesNetPredictor(HmmBayesNetDeserialization.DeserializedSleepHmmBayesNetWithParams data, Optional<UUID> uuid) {
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
        eventProducers.put(HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_BED,new BedEventProducer());

        allTheData = data;
    }

    public ImmutableList<Event> getBayesNetHmmEvents(final DateTime targetDate, final DateTime endDate,final long  currentTimeMillis,final long accountId,
                                                      final AllSensorSampleList allSensorSampleList, final List<TrackerMotion> myMotion,final int timezoneOffset) {

        final Long startTimeUTC = targetDate.minusMillis(timezoneOffset).getMillis();
        final Long endTimeUTC = startTimeUTC + 60000L * 60 * 16;

        final List<Event> outputEvents = Lists.newArrayList();

        final Map<String, List<Event>> eventsByOutputId = makePredictions(allSensorSampleList, myMotion, startTimeUTC, endTimeUTC, timezoneOffset);

        final List<Event> events = eventsByOutputId.get(HmmBayesNetMeasurementParameters.CONDITIONAL_PROBABILITY_OF_SLEEP);

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
                //TODO log error
                continue;
            }

            final List<Event> events = producer.getEventsFromProbabilitySequence(probsByOutputId,sensorData,t0,allTheData.params.numMinutesInMeasPeriod,timezoneOffset);

            eventsByOutputId.put(key,events);
        }

        return eventsByOutputId;

    }
}
