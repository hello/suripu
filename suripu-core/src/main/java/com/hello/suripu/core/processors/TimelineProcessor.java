package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.PartnerMotionEvent;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.FeedbackUtils;
import com.hello.suripu.core.util.MultiLightOutUtils;
import com.hello.suripu.core.util.PartnerDataUtils;
import com.hello.suripu.core.util.SleepHmmWithInterpretation;
import com.hello.suripu.core.util.SleepScoreUtils;
import com.hello.suripu.core.util.TimelineRefactored;
import com.hello.suripu.core.util.TimelineUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimelineProcessor extends FeatureFlippedProcessor {

    public static final String VERSION = "0.0.2";
    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineProcessor.class);
    private static final Integer MIN_SLEEP_DURATION_FOR_SLEEP_SCORE_IN_MINUTES = 3 * 60;
    private final TrackerMotionDAO trackerMotionDAO;
    private final DeviceDAO deviceDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB;
    private final FeedbackDAO feedbackDAO;
    private final SleepHmmDAO sleepHmmDAO;
    private final AccountDAO accountDAO;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;

    public TimelineProcessor(final TrackerMotionDAO trackerMotionDAO,
                            final DeviceDAO deviceDAO,
                            final DeviceDataDAO deviceDataDAO,
                            final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB,
                            final FeedbackDAO feedbackDAO,
                            final SleepHmmDAO sleepHmmDAO,
                            final AccountDAO accountDAO,
                            final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.deviceDAO = deviceDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.ringTimeHistoryDAODynamoDB = ringTimeHistoryDAODynamoDB;
        this.feedbackDAO = feedbackDAO;
        this.sleepHmmDAO = sleepHmmDAO;
        this.accountDAO = accountDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
    }

    public boolean shouldProcessTimelineByWorker(final long accountId,
                                                 final int maxNoMotionPeriodInMinutes,
                                                 final DateTime currentTime){
        final Optional<TrackerMotion> lastMotion = this.trackerMotionDAO.getLast(accountId);
        if(!lastMotion.isPresent()){
            return false;
        }

        if(currentTime.minusMinutes(maxNoMotionPeriodInMinutes).isAfter(lastMotion.get().timestamp)){
            return true;
        }
        return false;
    }

    private List<Event> getAlarmEvents(final Long accountId, final DateTime startQueryTime, final DateTime endQueryTime, final Integer offsetMillis) {

        final List<DeviceAccountPair> pairs = deviceDAO.getSensesForAccountId(accountId);
        if(pairs.size() > 1) {
            LOGGER.info("Account {} has several sense paired. Not displaying alarm event", accountId);
            return Collections.EMPTY_LIST;
        }
        if(pairs.isEmpty()) {
            LOGGER.warn("Account {} doesn’t have any Sense paired. ", accountId);
            return Collections.EMPTY_LIST;
        }
        final String senseId = pairs.get(0).externalDeviceId;

        final List<RingTime> ringTimes = this.ringTimeHistoryDAODynamoDB.getRingTimesBetween(senseId, startQueryTime, endQueryTime);

        return TimelineUtils.getAlarmEvents(ringTimes, startQueryTime, endQueryTime, offsetMillis, DateTime.now(DateTimeZone.UTC));
    }


public List<Timeline> retrieveHmmTimeline(final Long accountId, final String date) {

        final Timeline emptyTimeline = Timeline.createEmpty();
        final List<Timeline> emptyTimeLines = Lists.newArrayList(emptyTimeline);

        final long  currentTimeMillis = DateTime.now().withZone(DateTimeZone.UTC).getMillis();
        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);

        LOGGER.info("Using HMM for account {}",accountId);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);


        /*  SENSOR DATA  */
        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenLocalUTC(accountId, targetDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());

        if(trackerMotions.size() < 20) {
            LOGGER.debug("No data for account_id = {} and day = {}", accountId, targetDate);
            return emptyTimeLines;
        }



        // get all sensor data, used for light and sound disturbances, and presleep-insights
        AllSensorSampleList allSensorSampleList = new AllSensorSampleList();

        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accountId);

        if (deviceId.isPresent()) {
            final int slotDurationMins = 1;

            allSensorSampleList = deviceDataDAO.generateTimeSeriesByLocalTimeAllSensors(
                    targetDate.getMillis(), endDate.getMillis(),
                    accountId, deviceId.get(), slotDurationMins, this.missingDataDefaultValue(accountId));
        }

        if (allSensorSampleList.isEmpty()) {
            LOGGER.debug("No sensor data found for user {}",accountId);
            return emptyTimeLines;
        }




    /* EVENTS FOR TIMELINE  */


        //events for the timeline
        final List<MotionEvent> motionEvents = TimelineUtils.generateMotionEvents(trackerMotions);


        // Light
        final List<Event> lightEvents = Lists.newArrayList();
        lightEvents.addAll(TimelineUtils.getLightEvents(allSensorSampleList.get(Sensor.LIGHT)));


        final Map<Long, Event> timelineEvents = TimelineRefactored.populateTimeline(motionEvents);



        /*  THE GODDAMNED HMM */

        final Optional<SleepHmmWithInterpretation> hmmOptional = sleepHmmDAO.getLatestModelForDate(accountId, targetDate.getMillis());

        if (!hmmOptional.isPresent()) {
            LOGGER.debug("No HMM model found, or deserialization error for user {}",accountId);
            return emptyTimeLines;
        }


        final Optional<SleepHmmWithInterpretation.SleepHmmResult> optionalHmmPredictions = hmmOptional.get().getSleepEventsUsingHMM(
                allSensorSampleList, trackerMotions,targetDate.getMillis(),endDate.getMillis(),currentTimeMillis);

        if (!optionalHmmPredictions.isPresent()) {
            LOGGER.debug("HMM did not return any predictions");
            return emptyTimeLines;
        }


        SleepHmmWithInterpretation.SleepHmmResult res = optionalHmmPredictions.get();

        // insert IN-BED, SLEEP, WAKE, OUT-of-BED
        for (final Event e : res.sleepEvents) {
            timelineEvents.put(e.getStartTimestamp(), e);
        }



        //sleep stats
        //(final Integer soundSleepDurationInMinutes, final Integer lightSleepDurationInMinutes,
        //final Integer sleepDurationInMinutes,
        //final Integer numberOfMotionEvents,
        //final Long sleepTime, final Long wakeTime, final Integer sleepOnsetTimeMinutes) {

        final SleepStats stats = new SleepStats(0,0,res.stats.minutesSpentSleeping,0,0L,0L,0);


        Integer sleepScore = computeAndMaybeSaveScore(trackerMotions, targetDate, accountId, stats);

        if(stats.sleepDurationInMinutes < MIN_SLEEP_DURATION_FOR_SLEEP_SCORE_IN_MINUTES) {
            LOGGER.warn("Score for account id {} was set to zero because sleep duration is too short ({} min)", accountId, stats.sleepDurationInMinutes);
            sleepScore = 0;
        }

        final Boolean reportSleepDuration = false;
        final String timeLineMessage = TimelineUtils.generateMessage(stats, 0, 0, reportSleepDuration);

        LOGGER.debug("Score for account_id = {} is {}", accountId, sleepScore);


        final List<Insight> insights = TimelineUtils.generatePreSleepInsights(allSensorSampleList, stats.sleepTime, accountId);
        final List<SleepSegment>  reversedSegments = Lists.reverse(TimelineUtils.eventsToSegments(res.sleepEvents));

        final Timeline timeline = Timeline.create(sleepScore, timeLineMessage, date, reversedSegments, insights, stats);

        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return timelines;


    }


    public List<Timeline> retrieveTimelinesFast(final Long accountId, final DateTime date) {


        final long  currentTimeMillis = DateTime.now().withZone(DateTimeZone.UTC).getMillis();
        final DateTime targetDate = date.withTimeAtStartOfDay().withHourOfDay(DateTimeUtil.DAY_STARTS_AT_HOUR);
        final DateTime endDate = date.withTimeAtStartOfDay().plusDays(1).withHourOfDay(DateTimeUtil.DAY_ENDS_AT_HOUR);

        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        final List<TrackerMotion> originalTrackerMotions = trackerMotionDAO.getBetweenLocalUTC(accountId, targetDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", originalTrackerMotions.size());

        if(originalTrackerMotions.size() < 20) {
            LOGGER.debug("No data for account_id = {} and day = {}", accountId, targetDate);
            final Timeline timeline = Timeline.createEmpty();
            final List<Timeline> timelines = Lists.newArrayList(timeline);
            return timelines;
        }

        // get partner tracker motion, if available
        final List<TrackerMotion> partnerMotions = getPartnerTrackerMotion(accountId, targetDate, endDate);
        final List<TrackerMotion> trackerMotions = new ArrayList<>();

        if (!partnerMotions.isEmpty() && this.hasPartnerFilterEnabled(accountId)) {
            try {
                PartnerDataUtils.PartnerMotions motions = PartnerDataUtils.getMyMotion(originalTrackerMotions, partnerMotions);
                trackerMotions.addAll(motions.myMotions);
            }
            catch (Exception e) {
                LOGGER.info(e.getMessage());
                trackerMotions.addAll(originalTrackerMotions);
            }
        }
        else {
            trackerMotions.addAll(originalTrackerMotions);
        }

        // get all sensor data, used for light and sound disturbances, and presleep-insights
        AllSensorSampleList allSensorSampleList = new AllSensorSampleList();

        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accountId);

        if (deviceId.isPresent()) {
            final int slotDurationMins = 1;

            allSensorSampleList = deviceDataDAO.generateTimeSeriesByLocalTimeAllSensors(
                    targetDate.getMillis(), endDate.getMillis(),
                    accountId, deviceId.get(), slotDurationMins, missingDataDefaultValue(accountId));
        }


        // compute lights-out and sound-disturbance events
        Optional<DateTime> lightOutTimeOptional = Optional.absent();
        Optional<DateTime> wakeUpWaveTimeOptional = Optional.absent();
        final List<Event> lightEvents = Lists.newArrayList();

        if (!allSensorSampleList.isEmpty()) {

            // Light
            lightEvents.addAll(TimelineUtils.getLightEvents(allSensorSampleList.get(Sensor.LIGHT)));
            if (lightEvents.size() > 0) {
                lightOutTimeOptional = TimelineUtils.getLightsOutTime(lightEvents);
            }

            if(!allSensorSampleList.get(Sensor.WAVE_COUNT).isEmpty() && trackerMotions.size() > 0){
                wakeUpWaveTimeOptional = TimelineUtils.getFirstAwakeWaveTime(trackerMotions.get(0).timestamp,
                        trackerMotions.get(trackerMotions.size() - 1).timestamp,
                        allSensorSampleList.get(Sensor.WAVE_COUNT));
            }
        }

        if(lightOutTimeOptional.isPresent()){
            LOGGER.info("Light out at {}", lightOutTimeOptional.get());
        } else {
            LOGGER.info("No light out");
        }


        // create sleep-motion segments
        final List<MotionEvent> motionEvents = TimelineUtils.generateMotionEvents(trackerMotions);
        final Map<Long, Event> timelineEvents = TimelineRefactored.populateTimeline(motionEvents);

        // LIGHT
        for(final Event event : lightEvents) {
            timelineEvents.put(event.getStartTimestamp(), event);
        }

        final Integer offsetMillis = trackerMotions.get(0).offsetMillis;
        final Map<Event.Type, Event> feedbackEvents = fromFeedback(accountId, targetDate, offsetMillis);
        for(final Event event : feedbackEvents.values()) {
            LOGGER.info("Overriding {} with {} for account {}", event.getType().name(), event, accountId);
            timelineEvents.put(event.getStartTimestamp(), event);
        }


        /*  This can get overided by the HMM if the feature is enabled */
        SleepEvents<Optional<Event>> sleepEventsFromAlgorithm = fromAlgorithm(targetDate,
                trackerMotions,
                allSensorSampleList.get(Sensor.LIGHT),
                wakeUpWaveTimeOptional);

        if (this.hasHmmEnabled(accountId)) {
            LOGGER.info("Using HMM for account {}", accountId);
        }

        // PARTNER MOTION
        final List<PartnerMotionEvent> partnerMotionEvents = getPartnerMotionEvents(sleepEventsFromAlgorithm.fallAsleep, sleepEventsFromAlgorithm.wakeUp, motionEvents, accountId);
        for(PartnerMotionEvent partnerMotionEvent : partnerMotionEvents) {
            timelineEvents.put(partnerMotionEvent.getStartTimestamp(), partnerMotionEvent);
        }
        final int numPartnerMotion = partnerMotionEvents.size();

        // SOUND
        int numSoundEvents = 0;
        if (this.hasSoundInTimeline(accountId)) {
            final List<Event> soundEvents = getSoundEvents(allSensorSampleList.get(Sensor.SOUND_PEAK_DISTURBANCE),
                    motionEvents, lightOutTimeOptional, sleepEventsFromAlgorithm);
            for (final Event event : soundEvents) {
                timelineEvents.put(event.getStartTimestamp(), event);
            }
            numSoundEvents = soundEvents.size();
        }

        // insert IN-BED, SLEEP, WAKE, OUT-of-BED
        final List<Optional<Event>> eventList = sleepEventsFromAlgorithm.toList();
        for(final Optional<Event> sleepEventOptional: eventList){
            if(sleepEventOptional.isPresent() && !feedbackEvents.containsKey(sleepEventOptional.get().getType())){
                timelineEvents.put(sleepEventOptional.get().getStartTimestamp(), sleepEventOptional.get());
            }
        }


        // ALARM
        if(this.hasAlarmInTimeline(accountId) && trackerMotions.size() > 0) {
            final DateTimeZone userTimeZone = DateTimeZone.forOffsetMillis(trackerMotions.get(0).offsetMillis);
            final DateTime alarmQueryStartTime = new DateTime(targetDate.getYear(),
                    targetDate.getMonthOfYear(),
                    targetDate.getDayOfMonth(),
                    targetDate.getHourOfDay(),
                    targetDate.getMinuteOfHour(),
                    0,
                    userTimeZone).minusMinutes(1);

            final DateTime alarmQueryEndTime = new DateTime(endDate.getYear(),
                    endDate.getMonthOfYear(),
                    endDate.getDayOfMonth(),
                    endDate.getHourOfDay(),
                    endDate.getMinuteOfHour(),
                    0,
                    userTimeZone).plusMinutes(1);

            final List<Event> alarmEvents = getAlarmEvents(accountId, alarmQueryStartTime, alarmQueryEndTime, userTimeZone.getOffset(alarmQueryEndTime));
            for(final Event event : alarmEvents) {
                timelineEvents.put(event.getStartTimestamp(), event);
            }
        }


        final List<Event> eventsWithSleepEvents = TimelineRefactored.mergeEvents(timelineEvents);
        final List<Event> smoothedEvents = TimelineUtils.smoothEvents(eventsWithSleepEvents);

        final List<Event> cleanedUpEvents = TimelineUtils.removeMotionEventsOutsideBedPeriod(smoothedEvents,
                sleepEventsFromAlgorithm.goToBed,
                sleepEventsFromAlgorithm.outOfBed);

        final List<Event> greyEvents = TimelineUtils.greyNullEventsOutsideBedPeriod(cleanedUpEvents,
                sleepEventsFromAlgorithm.goToBed,
                sleepEventsFromAlgorithm.outOfBed);
        final List<Event> nonSignificantFilteredEvents = TimelineUtils.removeEventBeforeSignificant(greyEvents);

        final List<SleepSegment> sleepSegments = TimelineUtils.eventsToSegments(nonSignificantFilteredEvents);

        final int lightSleepThreshold = 70; // TODO: Generate dynamically instead of hard threshold
        final SleepStats sleepStats = TimelineUtils.computeStats(sleepSegments, lightSleepThreshold);
        final List<SleepSegment> reversed = Lists.reverse(sleepSegments);


        Integer sleepScore = computeAndMaybeSaveScore(trackerMotions, targetDate, accountId, sleepStats);

        if(sleepStats.sleepDurationInMinutes < MIN_SLEEP_DURATION_FOR_SLEEP_SCORE_IN_MINUTES) {
            LOGGER.warn("Score for account id {} was set to zero because sleep duration is too short ({} min)", accountId, sleepStats.sleepDurationInMinutes);
            sleepScore = 0;
        }

        final Boolean reportSleepDuration = false;
        final String timeLineMessage = TimelineUtils.generateMessage(sleepStats, numPartnerMotion, numSoundEvents, reportSleepDuration);

        LOGGER.debug("Score for account_id = {} is {}", accountId, sleepScore);


        final List<Insight> insights = TimelineUtils.generatePreSleepInsights(allSensorSampleList, sleepStats.sleepTime, accountId);
        final List<SleepSegment>  reversedSegments = Lists.reverse(reversed);
        final Timeline timeline = Timeline.create(sleepScore, timeLineMessage, date.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT), reversedSegments, insights, sleepStats);

        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return timelines;
    }




    private List<TrackerMotion> getPartnerTrackerMotion(final Long accountId, final DateTime startTime, final DateTime endTime) {
        final Optional<Long> optionalPartnerAccountId = this.deviceDAO.getPartnerAccountId(accountId);
        if (optionalPartnerAccountId.isPresent()) {
            final Long partnerAccountId = optionalPartnerAccountId.get();
            LOGGER.debug("partner account {}", partnerAccountId);
            return this.trackerMotionDAO.getBetweenLocalUTC(partnerAccountId, startTime, endTime);
        }
        return Collections.EMPTY_LIST;
    }
    /**
     * Fetch partner motion events
     * @param fallingAsleepEvent
     * @param wakeupEvent
     * @param motionEvents
     * @param accountId
     * @return
     */
    private List<PartnerMotionEvent> getPartnerMotionEvents(final Optional<Event> fallingAsleepEvent, final Optional<Event> wakeupEvent, final List<MotionEvent> motionEvents, final Long accountId) {
        // add partner movement data, check if there's a partner

        final Optional<Long> optionalPartnerAccountId = this.deviceDAO.getPartnerAccountId(accountId);
        if (optionalPartnerAccountId.isPresent() && fallingAsleepEvent.isPresent() && wakeupEvent.isPresent()) {
            LOGGER.debug("partner account {}", optionalPartnerAccountId.get());
            // get tracker motions for partner, query time is in UTC, not local_utc

            final DateTime startTime = new DateTime(fallingAsleepEvent.get().getStartTimestamp(), DateTimeZone.UTC);
            final DateTime endTime = new DateTime(wakeupEvent.get().getStartTimestamp(), DateTimeZone.UTC);

            final List<TrackerMotion> partnerMotions = this.trackerMotionDAO.getBetween(optionalPartnerAccountId.get(), startTime, endTime);
            if (partnerMotions.size() > 0) {
                // use un-normalized data segments for comparison
                return PartnerMotion.getPartnerData(motionEvents, partnerMotions, 0);
            }
        }
        return Collections.EMPTY_LIST;
    }

    private List<Event> getSoundEvents(final List<Sample> soundSamples,
                                       final List<MotionEvent> motionEvents,
                                       final Optional<DateTime> lightOutTimeOptional,
                                       final SleepEvents<Optional<Event>> sleepEventsFromAlgorithm) {
        if (soundSamples.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        // TODO: refactor - ¡don't doubt it!
        Optional<DateTime> optionalSleepTime = Optional.absent();
        Optional<DateTime> optionalAwakeTime = Optional.absent();

        if (sleepEventsFromAlgorithm.fallAsleep.isPresent()) {
            // sleep time
            final Event event = sleepEventsFromAlgorithm.fallAsleep.get();
            optionalSleepTime = Optional.of(new DateTime(event.getStartTimestamp(),
                    DateTimeZone.UTC).plusMillis(event.getTimezoneOffset()));
        } else if (sleepEventsFromAlgorithm.goToBed.isPresent()) {
            // in-bed time
            final Event event = sleepEventsFromAlgorithm.goToBed.get();
            optionalSleepTime = Optional.of(new DateTime(event.getStartTimestamp(),
                    DateTimeZone.UTC).plusMillis(event.getTimezoneOffset()));
        }

        if (sleepEventsFromAlgorithm.wakeUp.isPresent()) {
            // awake time
            final Event event = sleepEventsFromAlgorithm.wakeUp.get();
            optionalAwakeTime = Optional.of(new DateTime(event.getStartTimestamp(),
                    DateTimeZone.UTC).plusMillis(event.getTimezoneOffset()));
        } else if (sleepEventsFromAlgorithm.outOfBed.isPresent()) {
            // out-of-bed time
            final Event event = sleepEventsFromAlgorithm.outOfBed.get();
            optionalAwakeTime = Optional.of(new DateTime(event.getStartTimestamp(),
                    DateTimeZone.UTC).plusMillis(event.getTimezoneOffset()));
        }

        final Map<Long, Integer> sleepDepths = new HashMap<>();
        for (final MotionEvent event : motionEvents) {
            if (event.getSleepDepth() > 0) {
                sleepDepths.put(event.getStartTimestamp(), event.getSleepDepth());
            }
        }

        return TimelineUtils.getSoundEvents(soundSamples, sleepDepths, lightOutTimeOptional, optionalSleepTime, optionalAwakeTime);
    }

    /**
     * Pang magic
     * @param targetDate
     * @param trackerMotions
     * @param rawLight
     * @param wakeUpWaveTimeOptional
     * @return
     */
    private SleepEvents<Optional<Event>> fromAlgorithm(final DateTime targetDate,
                                                       final List<TrackerMotion> trackerMotions,
                                                       final List<Sample> rawLight,
                                                       final Optional<DateTime> wakeUpWaveTimeOptional) {
        Optional<Segment> sleepSegmentOptional;
        Optional<Segment> inBedSegmentOptional = Optional.absent();
        SleepEvents<Optional<Event>> sleepEventsFromAlgorithm = SleepEvents.create(Optional.<Event>absent(),
                Optional.<Event>absent(),
                Optional.<Event>absent(),
                Optional.<Event>absent());

        final List<Event> rawLightEvents = TimelineUtils.getLightEventsWithMultipleLightOut(rawLight);
        final List<Event> smoothedLightEvents = MultiLightOutUtils.smoothLight(rawLightEvents, MultiLightOutUtils.DEFAULT_SMOOTH_GAP_MIN);
        final List<Event> lightOuts = MultiLightOutUtils.getValidLightOuts(smoothedLightEvents, trackerMotions, MultiLightOutUtils.DEFAULT_LIGHT_DELTA_WINDOW_MIN);

        final List<DateTime> lightOutTimes = MultiLightOutUtils.getLightOutTimes(lightOuts);

        // A day starts with 8pm local time and ends with 4pm local time next day
        try {
            sleepEventsFromAlgorithm = TimelineUtils.getSleepEvents(targetDate,
                    trackerMotions,
                    lightOutTimes,
                    wakeUpWaveTimeOptional,
                    MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                    MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                    MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                    false);



            if(sleepEventsFromAlgorithm.fallAsleep.isPresent() && sleepEventsFromAlgorithm.wakeUp.isPresent()){
                sleepSegmentOptional = Optional.of(new Segment(sleepEventsFromAlgorithm.fallAsleep.get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.wakeUp.get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.wakeUp.get().getTimezoneOffset()));

                LOGGER.info("Sleep Time From Awake Detection Algorithm: {} - {}",
                        new DateTime(sleepSegmentOptional.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegmentOptional.get().getOffsetMillis())),
                        new DateTime(sleepSegmentOptional.get().getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepSegmentOptional.get().getOffsetMillis())));
            }

            if(sleepEventsFromAlgorithm.goToBed.isPresent() && sleepEventsFromAlgorithm.outOfBed.isPresent()){
                inBedSegmentOptional = Optional.of(new Segment(sleepEventsFromAlgorithm.goToBed.get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.outOfBed.get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.outOfBed.get().getTimezoneOffset()));
                LOGGER.info("In Bed Time From Awake Detection Algorithm: {} - {}",
                        new DateTime(inBedSegmentOptional.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(inBedSegmentOptional.get().getOffsetMillis())),
                        new DateTime(inBedSegmentOptional.get().getEndTimestamp(), DateTimeZone.forOffsetMillis(inBedSegmentOptional.get().getOffsetMillis())));
            }


        }catch (Exception ex){ //TODO : catch a more specific exception
            LOGGER.error("Generate sleep period from Awake Detection Algorithm failed: {}", ex.getMessage());
        }

        return  sleepEventsFromAlgorithm;
    }


    /**
     * Sleep score - always compute and update dynamo
     * @param trackerMotions
     * @param targetDate
     * @param accountId
     * @param sleepStats
     * @return
     */
    private Integer computeAndMaybeSaveScore(final List<TrackerMotion> trackerMotions, final DateTime targetDate, final Long accountId, final SleepStats sleepStats) {

        // Movement score
        final MotionScore motionScore = SleepScoreUtils.getSleepMotionScore(targetDate.withTimeAtStartOfDay(),
                trackerMotions, sleepStats.sleepTime, sleepStats.wakeTime);

        // Sleep duration score
        final Optional<Account> optionalAccount = accountDAO.getById(accountId);
        final int userAge = (optionalAccount.isPresent()) ? DateTimeUtil.getDateDiffFromNowInDays(optionalAccount.get().DOB) / 365 : 0;
        final Integer durationScore = SleepScoreUtils.getSleepDurationScore(userAge, sleepStats.sleepDurationInMinutes);

        // TODO: Environment score
        final Integer environmentScore = 100;

        // Aggregate all scores
        final Integer sleepScore = SleepScoreUtils.aggregateSleepScore(motionScore.score, durationScore, environmentScore);

        // Always update stats and scores to Dynamo
        final Integer userOffsetMillis = trackerMotions.get(0).offsetMillis;
        final Boolean updatedStats = this.sleepStatsDAODynamoDB.updateStat(accountId,
                targetDate.withTimeAtStartOfDay(), sleepScore, motionScore, sleepStats, userOffsetMillis);

        LOGGER.debug("Updated Stats-score: status {}, account {}, motion {}, duration {}, score {}, stats {}",
                updatedStats, accountId, motionScore, durationScore, sleepScore, sleepStats);

        return sleepScore;
    }


    private Map<Event.Type, Event> fromFeedback(final Long accountId, final DateTime nightOf, final Integer offsetMillis) {
        if(!hasFeedbackInTimeline(accountId)) {
            LOGGER.debug("Timeline feedback not enabled for account {}", accountId);
            return Maps.newHashMap();
        }
        // this is needed to match the datetime created when receiving user feedback
        // I believe we should change how we create datetime in feedback once we have time
        // TODO: tim
        final DateTime nightOfUTC = new DateTime(nightOf.getYear(),
                nightOf.getMonthOfYear(), nightOf.getDayOfMonth(), 0, 0, 0, DateTimeZone.UTC);
        final ImmutableList<TimelineFeedback> feedbackList = feedbackDAO.getForNight(accountId, nightOfUTC);
        return FeedbackUtils.convertFeedbackToDateTime(feedbackList, offsetMillis);
    }

}
