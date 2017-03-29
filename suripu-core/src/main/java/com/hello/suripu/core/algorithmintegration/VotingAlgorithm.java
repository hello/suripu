package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.MultiLightOutUtils;
import com.hello.suripu.core.util.TimelineError;
import com.hello.suripu.core.util.TimelineSafeguards;
import com.hello.suripu.core.util.TimelineUtils;
import com.hello.suripu.core.util.VotingSleepEvents;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by benjo on 1/20/16.
 */
public class VotingAlgorithm implements TimelineAlgorithm {

    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(VotingAlgorithm.class);

    final TimelineUtils timelineUtils;
    final TimelineSafeguards timelineSafeguards;

    final Logger LOGGER;

    public VotingAlgorithm(final Optional<UUID> uuid) {
        timelineUtils = new TimelineUtils(uuid);
        timelineSafeguards = new TimelineSafeguards(uuid);
        LOGGER = new LoggerWithSessionId(STATIC_LOGGER,uuid);
    }


    @Override
    public Optional<TimelineAlgorithmResult> getTimelinePrediction(final OneDaysSensorData sensorData, final SleepPeriod sleepPeriod, final TimelineLog log, final long accountId, final boolean feedbackChanged, final Set<String> features) {

        LOGGER.info("algorithm=VOTING account_id={} date={}",accountId,sensorData.date.toDate());

        try {
            //reset state
            final Optional<VotingSleepEvents> votingSleepEventsOptional = fromVotingAlgorithm(sensorData.oneDaysTrackerMotion.processedtrackerMotions,
                    sensorData.allSensorSampleList.get(Sensor.SOUND),
                    sensorData.allSensorSampleList.get(Sensor.LIGHT),
                    sensorData.allSensorSampleList.get(Sensor.WAVE_COUNT));

            if (!votingSleepEventsOptional.isPresent()) {
                LOGGER.info("alg_status={} account_id={} date={}","no-events",accountId,sensorData.date.toDate());
                log.addMessage(AlgorithmType.VOTING, TimelineError.UNEXEPECTED, "optional.absent from fromVotingAlgorithm");
                return Optional.absent();
            }

            final boolean isPrimarySleepPeriod = true;
            final TimelineError timelineError = timelineSafeguards.checkIfValidTimeline(
                    accountId,
                    true,
                    Optional.absent(),
                    AlgorithmType.VOTING,
                    votingSleepEventsOptional.get().sleepEvents,
                    ImmutableList.copyOf(Collections.EMPTY_LIST),
                    ImmutableList.copyOf(sensorData.allSensorSampleList.get(Sensor.LIGHT)),
                    ImmutableList.copyOf(sensorData.oneDaysTrackerMotion.processedtrackerMotions));

            LOGGER.info("alg_status={} account_id={} date={}",timelineError,accountId,sensorData.date.toDate());

            //we now care about all errors
            if (!timelineError.equals(TimelineError.NO_ERROR) ) {
                log.addMessage(AlgorithmType.VOTING,timelineError);
                return Optional.absent();
            }



            final List<Event> events = timelineUtils.eventsFromOptionalEvents(votingSleepEventsOptional.get().sleepEvents.toList());

            log.addMessage(AlgorithmType.VOTING, events);

            return Optional.of(new TimelineAlgorithmResult(AlgorithmType.VOTING,events));
        }
        catch (Exception e) {
            LOGGER.error("alg_status={} account_id={} date={}","exception",accountId,sensorData.date.toDate());
            log.addMessage(AlgorithmType.VOTING, TimelineError.UNEXEPECTED);
        }

        return Optional.absent();

    }

    @Override
    public  TimelineAlgorithm cloneWithNewUUID(Optional<UUID> uuid) {
        return new VotingAlgorithm(uuid);
    }

    private Optional<VotingSleepEvents> fromVotingAlgorithm(final List<TrackerMotion> trackerMotions,
                                                            final List<Sample> rawSound,
                                                            final List<Sample> rawLight,
                                                            final List<Sample> rawWave) {



        Optional<VotingSleepEvents> votingSleepEventsOptional = Optional.absent();

        final List<Event> rawLightEvents = timelineUtils.getLightEventsWithMultipleLightOut(rawLight);
        final List<Event> smoothedLightEvents = MultiLightOutUtils.smoothLight(rawLightEvents, MultiLightOutUtils.DEFAULT_SMOOTH_GAP_MIN);
        final List<Event> lightOuts = MultiLightOutUtils.getValidLightOuts(smoothedLightEvents, trackerMotions, MultiLightOutUtils.DEFAULT_LIGHT_DELTA_WINDOW_MIN);

        final List<DateTime> lightOutTimes = MultiLightOutUtils.getLightOutTimes(lightOuts);


        Optional<DateTime> wakeUpWaveTimeOptional = timelineUtils.getFirstAwakeWaveTime(trackerMotions.get(0).timestamp,
                trackerMotions.get(trackerMotions.size() - 1).timestamp,
                rawWave);
        votingSleepEventsOptional = timelineUtils.getSleepEventsFromVoting(trackerMotions,
                rawSound,
                lightOutTimes,
                wakeUpWaveTimeOptional);


        return  votingSleepEventsOptional;
    }
}
