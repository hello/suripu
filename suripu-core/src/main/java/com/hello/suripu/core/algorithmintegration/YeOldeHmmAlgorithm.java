package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.SleepHmmWithInterpretation;
import com.hello.suripu.core.util.TimelineError;
import com.hello.suripu.core.util.TimelineSafeguards;
import com.hello.suripu.core.util.TimelineUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by benjo on 1/20/16.
 */
class YeOldeHmmAlgorithm implements TimelineAlgorithm{
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(YeOldeHmmAlgorithm.class);

    final TimelineUtils timelineUtils;
    final TimelineSafeguards timelineSafeguards;
    final Optional<UUID> uuid;
    final Logger LOGGER;
    final SleepHmmDAO sleepHmmDAO;

    public YeOldeHmmAlgorithm(final SleepHmmDAO sleepHmmDAO,final Optional<UUID> uuid) {
        timelineUtils = new TimelineUtils(uuid);
        timelineSafeguards = new TimelineSafeguards(uuid);
        LOGGER = new LoggerWithSessionId(STATIC_LOGGER,uuid);
        this.uuid = uuid;
        this.sleepHmmDAO = sleepHmmDAO;
    }

    static private class HmmAlgorithmResults {
        final public SleepEvents<Optional<Event>> mainEvents;
        final public ImmutableList<Event> allTheOtherWakesAndSleeps;

        public HmmAlgorithmResults(SleepEvents<Optional<Event>> mainEvents, ImmutableList<Event> allTheOtherWakesAndSleeps) {
            this.mainEvents = mainEvents;
            this.allTheOtherWakesAndSleeps = allTheOtherWakesAndSleeps;
        }
    }

    @Override
    public Optional<TimelineAlgorithmResult> getTimelinePrediction(final OneDaysSensorData sensorData, final SleepPeriod sleepPeriod, final TimelineLog log, final long accountId, final boolean feedbackChanged, final Set<String> features) {

        LOGGER.info("algorithm=HMM account_id={}",accountId);

        try {

            final Optional<HmmAlgorithmResults> results = fromHmm(accountId, sensorData.currentTimeUTC, sensorData.startTimeLocalUTC, sensorData.endTimeLocalUTC,
                    sensorData.oneDaysTrackerMotion.processedtrackerMotions,
                    sensorData.allSensorSampleList);

            if (!results.isPresent()) {
                log.addMessage(AlgorithmType.HMM, TimelineError.MISSING_KEY_EVENTS);
                return Optional.absent();
            }

            final boolean isPrimarySleepPeriod = true;
            //verify that algorithm produced something useable
            final TimelineError error = timelineSafeguards.checkIfValidTimeline(
                    accountId,
                    true,
                    Optional.absent(),
                    AlgorithmType.HMM,
                    results.get().mainEvents,
                    ImmutableList.copyOf(results.get().allTheOtherWakesAndSleeps.asList()),
                    ImmutableList.copyOf(sensorData.allSensorSampleList.get(Sensor.LIGHT)),
                    ImmutableList.copyOf(sensorData.oneDaysTrackerMotion.processedtrackerMotions));

            LOGGER.info("alg_status={} account_id={} date={}",error,accountId,sensorData.date.toDate());

            if (!error.equals(TimelineError.NO_ERROR)) {
                log.addMessage(AlgorithmType.HMM, error);
                return Optional.absent();
            }

            final List<Event> eventList = timelineUtils.eventsFromOptionalEvents(results.get().mainEvents.toList());

            log.addMessage(AlgorithmType.HMM, eventList);

            return Optional.of(new TimelineAlgorithmResult(AlgorithmType.HMM,eventList, false));

        }
        catch (Exception e) {
            log.addMessage(AlgorithmType.HMM, TimelineError.UNEXEPECTED);
            LOGGER.error("alg_status={} account_id={} date={}","exception",accountId,sensorData.date.toDate());
        }

        return Optional.absent();
    }

    @Override
    public  TimelineAlgorithm cloneWithNewUUID(Optional<UUID> uuid) {
        return new YeOldeHmmAlgorithm(sleepHmmDAO,uuid);
    }


    private Optional<HmmAlgorithmResults> fromHmm(final long accountId, final DateTime currentTime, final DateTime targetDate, final DateTime endDate, final ImmutableList<TrackerMotion> trackerMotions, final AllSensorSampleList allSensorSampleList) {

        /*  GET THE GODDAMNED HMM */
        final Optional<SleepHmmWithInterpretation> hmmOptional = sleepHmmDAO.getLatestModelForDate(accountId, targetDate.getMillis());

        if (!hmmOptional.isPresent()) {
            LOGGER.error("Failed to retrieve HMM model for account_id {} on date {}", accountId, targetDate);
            return Optional.absent();
        }

        /*  EVALUATE THE HMM */
        final Optional<SleepHmmWithInterpretation.SleepHmmResult> optionalHmmPredictions = hmmOptional.get().getSleepEventsUsingHMM(
                allSensorSampleList, trackerMotions,targetDate.getMillis(),endDate.getMillis(),currentTime.getMillis());

        if (!optionalHmmPredictions.isPresent()) {
            LOGGER.error("Failed to get predictions from HMM for account_id {} on date {}", accountId, targetDate);
            return Optional.absent();
        }


        /* turn the HMM results into "main events" and other events */
        Optional<Event> inBed = Optional.absent();
        Optional<Event> sleep = Optional.absent();
        Optional<Event> wake = Optional.absent();
        Optional<Event> outOfBed = Optional.absent();

        final ImmutableList<Event> events = optionalHmmPredictions.get().sleepEvents;

        //find first sleep, inBed, and last outOfBed and wake
        for(final Event e : events) {

            //find first
            if (e.getType() == Event.Type.IN_BED && !inBed.isPresent()) {
                inBed = Optional.of(e);
            }

            //find first
            if (e.getType() == Event.Type.SLEEP && !sleep.isPresent()) {
                sleep = Optional.of(e);
            }

            //get last, so copy every on we find
            if (e.getType() == Event.Type.WAKE_UP) {
                wake = Optional.of(e);
            }

            if (e.getType() == Event.Type.OUT_OF_BED) {
                outOfBed = Optional.of(e);
            }

        }

        //find the events that aren't the main events
        final SleepEvents<Optional<Event>> sleepEvents = SleepEvents.create(inBed, sleep, wake, outOfBed);
        final Set<Long> takenTimes = new HashSet<Long>();

        for (final Optional<Event> e : sleepEvents.toList()) {
            if (!e.isPresent()) {
                continue;
            }

            takenTimes.add(e.get().getStartTimestamp());
        }


        final List<Event> otherEvents = new ArrayList<>();
        for(final Event e : events) {
            if (!takenTimes.contains(e.getStartTimestamp())) {
                otherEvents.add(e);
            }
        }


        return Optional.of(new HmmAlgorithmResults(sleepEvents,ImmutableList.copyOf(otherEvents)));

    }
}
