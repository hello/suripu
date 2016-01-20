package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
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
    public Optional<TimelineAlgorithmResult> getTimelinePrediction(final OneDaysSensorData sensorData, final TimelineLog log, final long accountId, final boolean feedbackChanged) {

        try {
            //reset state
            final Optional<VotingSleepEvents> votingSleepEventsOptional = fromVotingAlgorithm(sensorData.trackerMotions,
                    sensorData.allSensorSampleList.get(Sensor.SOUND),
                    sensorData.allSensorSampleList.get(Sensor.LIGHT),
                    sensorData.allSensorSampleList.get(Sensor.WAVE_COUNT));

            if (!votingSleepEventsOptional.isPresent()) {
                LOGGER.warn("voting algorithm did not produce ANY events");
                log.addMessage(AlgorithmType.VOTING, TimelineError.UNEXEPECTED, "optional.absent from fromVotingAlgorithm");
                return Optional.absent();
            }

            final TimelineError timelineError = timelineSafeguards.checkIfValidTimeline(votingSleepEventsOptional.get().sleepEvents,
                    ImmutableList.copyOf(Collections.EMPTY_LIST),
                    ImmutableList.copyOf(sensorData.allSensorSampleList.get(Sensor.LIGHT)));



            //data gap errors are ignored, these are the only two I care about
            if (timelineError.equals(TimelineError.EVENTS_OUT_OF_ORDER) ) {
                LOGGER.warn("voting events out of order");
                log.addMessage(AlgorithmType.VOTING,timelineError);
                return Optional.absent();
            }

            if (timelineError.equals(TimelineError.MISSING_KEY_EVENTS)) {
                LOGGER.warn("missing a key event from voting");
                log.addMessage(AlgorithmType.VOTING,timelineError);
                return Optional.absent();
            }

            final List<Event> events = timelineUtils.eventsFromOptionalEvents(votingSleepEventsOptional.get().sleepEvents.toList());

            log.addMessage(AlgorithmType.VOTING, events);

            return Optional.of(new TimelineAlgorithmResult(events));


        }
        catch (Exception e) {
            log.addMessage(AlgorithmType.VOTING, TimelineError.UNEXEPECTED);
            LOGGER.error(e.getMessage());
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

        // A day starts with 8pm local time and ends with 4pm local time next day
        try {
            Optional<DateTime> wakeUpWaveTimeOptional = timelineUtils.getFirstAwakeWaveTime(trackerMotions.get(0).timestamp,
                    trackerMotions.get(trackerMotions.size() - 1).timestamp,
                    rawWave);
            votingSleepEventsOptional = timelineUtils.getSleepEventsFromVoting(trackerMotions,
                    rawSound,
                    lightOutTimes,
                    wakeUpWaveTimeOptional);
        }catch (Exception ex){ //TODO : catch a more specific exception
            LOGGER.error("Generate sleep period from Voting Algorithm failed: {}", ex.getMessage());
        }

        return  votingSleepEventsOptional;
    }
}
