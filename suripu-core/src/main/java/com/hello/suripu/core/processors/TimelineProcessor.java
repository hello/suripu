package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.BayesNetHmmModelPriorsDAO;
import com.hello.suripu.core.db.BayesNetModelDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.BayesNetHmmMultipleModelsPriors;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.PartnerMotionEvent;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepScore;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TimelineLog;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.FeedbackUtils;
import com.hello.suripu.core.util.HmmBayesNetData;
import com.hello.suripu.core.util.HmmBayesNetPredictor;
import com.hello.suripu.core.util.InvalidNightType;
import com.hello.suripu.core.util.MultiLightOutUtils;
import com.hello.suripu.core.util.PartnerDataUtils;
import com.hello.suripu.core.util.SleepHmmWithInterpretation;
import com.hello.suripu.core.util.SleepScoreUtils;
import com.hello.suripu.core.util.TimelineRefactored;
import com.hello.suripu.core.util.TimelineSafeguards;
import com.hello.suripu.core.util.TimelineUtils;
import com.hello.suripu.core.util.VotingSleepEvents;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TimelineProcessor extends FeatureFlippedProcessor {

    public static final String VERSION = "0.0.2";
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(TimelineProcessor.class);
    private final TrackerMotionDAO trackerMotionDAO;
    private final DeviceDAO deviceDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB;
    private final FeedbackDAO feedbackDAO;
    private final SleepHmmDAO sleepHmmDAO;
    private final AccountDAO accountDAO;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final Logger LOGGER;
    private final TimelineUtils timelineUtils;
    private final TimelineSafeguards timelineSafeguards;
    private final FeedbackUtils feedbackUtils;
    private final PartnerDataUtils partnerDataUtils;
    private final SenseColorDAO senseColorDAO;
    private final BayesNetHmmModelPriorsDAO priorsDAO;
    private final BayesNetModelDAO bayesNetModelDAO;
    private final Optional<UUID> uuidOptional;

    final private static int SLOT_DURATION_MINUTES = 1;
    public final static int MIN_TRACKER_MOTION_COUNT = 20;
    public final static int MIN_MOTION_AMPLITUDE = 1000;

    public final static String ALGORITHM_NAME_REGULAR = "wupang";
    public final static String ALGORITHM_NAME_VOTING = "voting";
    public final static String ALGORITHM_NAME_BAYESNET = "bayesnet";
    public final static String ALGORITHM_NAME_HMM = "hmm";
    public final static String VERSION_BACKUP = "wupang_backup_for_hmm"; //let us know the HMM had some issues


    static public TimelineProcessor createTimelineProcessor(final TrackerMotionDAO trackerMotionDAO,
                                                            final DeviceDAO deviceDAO,
                                                            final DeviceDataDAO deviceDataDAO,
                                                            final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB,
                                                            final FeedbackDAO feedbackDAO,
                                                            final SleepHmmDAO sleepHmmDAO,
                                                            final AccountDAO accountDAO,
                                                            final SleepStatsDAODynamoDB sleepStatsDAODynamoDB,
                                                            final SenseColorDAO senseColorDAO,
                                                            final BayesNetHmmModelPriorsDAO priorsDAO,
                                                            final BayesNetModelDAO bayesNetModelDAO) {

        final LoggerWithSessionId logger = new LoggerWithSessionId(STATIC_LOGGER);
        return new TimelineProcessor(trackerMotionDAO,
                deviceDAO,deviceDataDAO,ringTimeHistoryDAODynamoDB,
                feedbackDAO,sleepHmmDAO,accountDAO,sleepStatsDAODynamoDB,
                senseColorDAO,priorsDAO,bayesNetModelDAO,
                Optional.<UUID>absent());
    }

    public TimelineProcessor copyMeWithNewUUID(final UUID uuid) {

        return new TimelineProcessor(trackerMotionDAO,deviceDAO,deviceDataDAO,ringTimeHistoryDAODynamoDB,feedbackDAO,sleepHmmDAO,accountDAO,sleepStatsDAODynamoDB,senseColorDAO,priorsDAO,bayesNetModelDAO,Optional.of(uuid));
    }

    //private SessionLogDebug(final String)

    private TimelineProcessor(final TrackerMotionDAO trackerMotionDAO,
                            final DeviceDAO deviceDAO,
                            final DeviceDataDAO deviceDataDAO,
                            final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB,
                            final FeedbackDAO feedbackDAO,
                            final SleepHmmDAO sleepHmmDAO,
                            final AccountDAO accountDAO,
                            final SleepStatsDAODynamoDB sleepStatsDAODynamoDB,
                              final SenseColorDAO senseColorDAO,
                              final BayesNetHmmModelPriorsDAO priorsDAO,
                              final BayesNetModelDAO bayesNetModelDAO,
                              final Optional<UUID> uuid) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.deviceDAO = deviceDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.ringTimeHistoryDAODynamoDB = ringTimeHistoryDAODynamoDB;
        this.feedbackDAO = feedbackDAO;
        this.sleepHmmDAO = sleepHmmDAO;
        this.accountDAO = accountDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.senseColorDAO = senseColorDAO;
        this.priorsDAO = priorsDAO;
        this.bayesNetModelDAO = bayesNetModelDAO;

        if (uuid.isPresent()) {
            this.LOGGER = new LoggerWithSessionId(STATIC_LOGGER, uuid.get());
            timelineUtils = new TimelineUtils(uuid.get());
            timelineSafeguards = new TimelineSafeguards(uuid.get());
            feedbackUtils = new FeedbackUtils(uuid.get());
            partnerDataUtils = new PartnerDataUtils(uuid.get());

        }
        else {
            this.LOGGER = new LoggerWithSessionId(STATIC_LOGGER);
            timelineUtils = new TimelineUtils();
            timelineSafeguards = new TimelineSafeguards();
            feedbackUtils = new FeedbackUtils();
            partnerDataUtils = new PartnerDataUtils();
        }
        uuidOptional = uuid;
    }

    public Optional<TimelineResult> retrieveTimelinesFast(final Long accountId, final DateTime date) {
        final DateTime targetDate = date.withTimeAtStartOfDay().withHourOfDay(DateTimeUtil.DAY_STARTS_AT_HOUR);
        final DateTime endDate = date.withTimeAtStartOfDay().plusDays(1).withHourOfDay(DateTimeUtil.DAY_ENDS_AT_HOUR);
        final DateTime  currentTime = DateTime.now().withZone(DateTimeZone.UTC);

        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);



        final Optional<OneDaysSensorData> sensorDataOptional = getSensorData(accountId, targetDate, endDate);

        if (!sensorDataOptional.isPresent()) {
            LOGGER.debug("returning empty timeline for account_id = {} and day = {}", accountId, targetDate);
            return Optional.absent();
        }



        final OneDaysSensorData sensorData = sensorDataOptional.get();
        final InvalidNightType discardReason = isValidNight(accountId, sensorData.trackerMotions);

        switch (discardReason){
            case TIMESPAN_TOO_SHORT:
                LOGGER.info("Tracker motion span too short for account_id = {} and day = {}", accountId, targetDate);
                return Optional.of(TimelineResult.createEmpty(English.TIMELINE_NOT_ENOUGH_SLEEP_DATA));

            case NOT_ENOUGH_DATA:
                LOGGER.info("Not enough tracker motion seen for account_id = {} and day = {}", accountId, targetDate);
                return Optional.of(TimelineResult.createEmpty(English.TIMELINE_NOT_ENOUGH_SLEEP_DATA));

            case NO_DATA:
                LOGGER.info("No tracker motion data for account_id = {} and day = {}", accountId, targetDate);
                return Optional.absent();

            case LOW_AMP_DATA:
                LOGGER.debug("tracker motion did not exceed minimu threshold for account_id = {} and day = {}", accountId, targetDate);
                return Optional.absent();

            default:
                break;
        }

        String algorithm = TimelineLog.NO_ALGORITHM;
        String version = TimelineLog.NO_VERSION;

        try {
            boolean algorithmWorked = false;

            Optional<SleepEvents<Optional<Event>>> sleepEventsFromAlgorithmOptional = Optional.absent();
            List<Event> extraEvents = Collections.EMPTY_LIST;


            if(this.hasVotingEnabled(accountId)){
                // Voting algorithm feature
                final Optional<VotingSleepEvents> votingSleepEventsOptional = fromVotingAlgorithm(sensorData.trackerMotions,
                        sensorData.allSensorSampleList.get(Sensor.SOUND),
                        sensorData.allSensorSampleList.get(Sensor.LIGHT),
                        sensorData.allSensorSampleList.get(Sensor.WAVE_COUNT));
                sleepEventsFromAlgorithmOptional = Optional.of(votingSleepEventsOptional.get().sleepEvents);
                extraEvents = votingSleepEventsOptional.get().extraEvents;
                algorithm = ALGORITHM_NAME_VOTING;
                algorithmWorked = true;

            }
            else if (this.hasBayesNetEnabled(accountId)) {

                //get model from DB
                final HmmBayesNetData bayesNetData = bayesNetModelDAO.getLatestModelForDate(accountId,date,uuidOptional);

                if (bayesNetData.isValid()) {

                    //get priors from DB
                    final Optional<BayesNetHmmMultipleModelsPriors> modelsPriorsOptional = priorsDAO.getModelPriorsByAccountIdAndDate(accountId, date);

                    if (modelsPriorsOptional.isPresent()) {
                        //update priors
                        bayesNetData.updateModelPriors(modelsPriorsOptional.get().modelPriorList);
                    }

                    //save first priors for day
                    if (!modelsPriorsOptional.isPresent() || modelsPriorsOptional.get().source.equals(priorsDAO.CURRENT_RANGE_KEY)) {
                        priorsDAO.updateModelPriorsByAccountIdForDate(accountId,date,bayesNetData.getModelPriors());
                    }

                    //get the predictor, which will turn the model output into events via some kind of segmenter
                    final HmmBayesNetPredictor predictor = new HmmBayesNetPredictor(bayesNetData.getDeserializedData(), uuidOptional);

                    //run the predictor--so the HMMs will decode, the output interpreted and segmented, and then turned into events
                    final List<Event> events = predictor.getBayesNetHmmEvents(targetDate, endDate, currentTime.getMillis(), accountId, sensorData.allSensorSampleList, sensorData.trackerMotions,sensorData.partnerMotions,sensorData.trackerMotions.get(0).offsetMillis);

                    /*  NOTE THAT THIS ONLY DOES SLEEP RIGHT NOW, NOT ON-BED */
                    if (events.size() >= 2) {

                        final SleepEvents<Optional<Event>> sleepEventsFromAlgorithm = SleepEvents.<Optional<Event>>create(Optional.<Event>absent(), Optional.of(events.get(0)), Optional.of(events.get(1)), Optional.<Event>absent());

                        sleepEventsFromAlgorithmOptional = Optional.of(sleepEventsFromAlgorithm);

                        algorithm = ALGORITHM_NAME_BAYESNET;
                        algorithmWorked = true;
                    }

                }
            }
            else {

                // HMM is **DEFAULT** algorithm, revert to wupang if there's no result
                Optional<HmmAlgorithmResults> results = fromHmm(accountId, currentTime, targetDate, endDate,
                        sensorData.trackerMotions,
                        sensorData.allSensorSampleList);

                if (results.isPresent()) {
                    LOGGER.debug("HMM Suceeded.");
                    sleepEventsFromAlgorithmOptional = Optional.of(results.get().mainEvents);
                    extraEvents = results.get().allTheOtherWakesAndSleeps.asList();
                    algorithm = ALGORITHM_NAME_HMM;

                    //verify that algorithm produced something useable
                    if (timelineSafeguards.checkIfValidTimeline(
                            sleepEventsFromAlgorithmOptional.get(),
                            ImmutableList.copyOf(extraEvents),
                            ImmutableList.copyOf(sensorData.allSensorSampleList.get(Sensor.LIGHT)))) {

                        algorithmWorked = true;
                    }
                }
            }



            /* TRY THE BACKUP PLAN!  */
            if (!algorithmWorked) {
                LOGGER.warn("ALGORITHM FAILED, trying regular algorithm instead");

                //reset state
                extraEvents = Collections.EMPTY_LIST;
                algorithm = ALGORITHM_NAME_REGULAR;
                version = VERSION_BACKUP;

                sleepEventsFromAlgorithmOptional = Optional.of(fromAlgorithm(targetDate,
                        sensorData.trackerMotions,
                        sensorData.allSensorSampleList.get(Sensor.LIGHT),
                        sensorData.allSensorSampleList.get(Sensor.WAVE_COUNT)));

            }

            if (!sleepEventsFromAlgorithmOptional.isPresent()) {
                LOGGER.debug("returning empty timeline for account_id = {} and day = {} and algo = {}", accountId, targetDate, algorithm);
                return Optional.absent();
            }

            /* FEATURE FLIP EXTRA EVENTS */
            if (!this.hasExtraEventsEnabled(accountId)) {
                LOGGER.info("not using {} extra events", extraEvents.size());
                extraEvents = Collections.EMPTY_LIST;
            }

            final List<Timeline> timelines = populateTimeline(accountId,date,targetDate,endDate,sleepEventsFromAlgorithmOptional.get(),ImmutableList.copyOf(extraEvents), sensorData);

            final TimelineLog log = new TimelineLog(algorithm,version,currentTime.getMillis(),targetDate.getMillis());

            return Optional.of(TimelineResult.create(timelines, log));
        }
        catch (Exception e) {
            LOGGER.error(e.toString());
        }

        LOGGER.debug("returning empty timeline for account_id = {} and day = {}", accountId, targetDate);
        return Optional.absent();

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

        final List<RingTime> ringTimes = this.ringTimeHistoryDAODynamoDB.getRingTimesBetween(senseId, accountId, startQueryTime, endQueryTime);

        return timelineUtils.getAlarmEvents(ringTimes, startQueryTime, endQueryTime, offsetMillis, DateTime.now(DateTimeZone.UTC));
    }

    static protected class OneDaysSensorData {
        final AllSensorSampleList allSensorSampleList;
        final ImmutableList<TrackerMotion> trackerMotions;
        final ImmutableList<TrackerMotion> partnerMotions;

        public OneDaysSensorData(AllSensorSampleList allSensorSampleList, ImmutableList<TrackerMotion> trackerMotions, ImmutableList<TrackerMotion> partnerMotions) {
            this.allSensorSampleList = allSensorSampleList;
            this.trackerMotions = trackerMotions;
            this.partnerMotions = partnerMotions;
        }
    }






    protected Optional<OneDaysSensorData> getSensorData(final long accountId, final DateTime targetDate, final DateTime endDate) {
        final List<TrackerMotion> originalTrackerMotions = trackerMotionDAO.getBetweenLocalUTC(accountId, targetDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", originalTrackerMotions.size());

        // get partner tracker motion, if available
        final List<TrackerMotion> partnerMotions = getPartnerTrackerMotion(accountId, targetDate, endDate);
        final List<TrackerMotion> trackerMotions = new ArrayList<>();

        if (!partnerMotions.isEmpty()) {

            final int tzOffsetMillis = originalTrackerMotions.get(0).offsetMillis;

            if (this.hasPartnerFilterEnabled(accountId)) {
                LOGGER.info("using original partner filter");
                try {
                    PartnerDataUtils.PartnerMotions motions = partnerDataUtils.getMyMotion(originalTrackerMotions, partnerMotions);
                    trackerMotions.addAll(motions.myMotions);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                    trackerMotions.addAll(originalTrackerMotions);
                }
            }
            else if (this.hasBayesianPartnerFilterEnabled(accountId)) {
                LOGGER.info("using bayesian partner filter");
                try {
                    trackerMotions.addAll(
                            partnerDataUtils.partnerFilterWithDurationsDiffHmm(
                                    targetDate.minusMillis(tzOffsetMillis),
                                    endDate.minusMillis(tzOffsetMillis),
                                    ImmutableList.copyOf(originalTrackerMotions),
                                    ImmutableList.copyOf(partnerMotions)));

                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                    trackerMotions.addAll(originalTrackerMotions);
                }
            }
            else {
                trackerMotions.addAll(originalTrackerMotions);
            }
        }
        else {
            trackerMotions.addAll(originalTrackerMotions);
        }

        if (trackerMotions.size() == 0) {
            LOGGER.debug("No tracker motion data ID for account_id = {} and day = {}", accountId, targetDate);
            return Optional.absent();
        }



        // get all sensor data, used for light and sound disturbances, and presleep-insights

        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        Optional<DateTime> wakeUpWaveTimeOptional = Optional.absent();

        if (!deviceIdPair.isPresent()) {
            LOGGER.debug("No device ID for account_id = {} and day = {}", accountId, targetDate);
            return Optional.absent();
        }

        final String externalDeviceId = deviceIdPair.get().externalDeviceId;
        final Long deviceId = deviceIdPair.get().internalDeviceId;

        // get color of sense, yes this matters for the light sensor
        final Optional<Device.Color> optionalColor = senseColorDAO.getColorForSense(externalDeviceId);


        AllSensorSampleList allSensorSampleList;
        if (hasAllSensorQueryUseUTCTs(accountId)) {
            // query dates in utc_ts (table has an index for this)
            LOGGER.debug("Query all sensors with utc ts for account {}", accountId);

            final int tzOffsetMillis = trackerMotions.get(0).offsetMillis;
            allSensorSampleList = deviceDataDAO.generateTimeSeriesByUTCTimeAllSensors(
                    targetDate.minusMillis(tzOffsetMillis).getMillis(),
                    endDate.minusMillis(tzOffsetMillis).getMillis(),
                    accountId, deviceId, SLOT_DURATION_MINUTES, missingDataDefaultValue(accountId),optionalColor);
        } else {
            // query dates are in local_utc_ts
            LOGGER.debug("Query all sensors with local_utc_ts for account {}", accountId);

            allSensorSampleList = deviceDataDAO.generateTimeSeriesByLocalTimeAllSensors(
                    targetDate.getMillis(),
                    endDate.getMillis(),
                    accountId, deviceId, SLOT_DURATION_MINUTES, missingDataDefaultValue(accountId),optionalColor);
        }

        if (allSensorSampleList.isEmpty()) {
            LOGGER.debug("No sense sensor data ID for account_id = {} and day = {}", accountId, targetDate);
            return Optional.absent();
        }

        return Optional.of(new OneDaysSensorData(allSensorSampleList,ImmutableList.copyOf(trackerMotions),ImmutableList.copyOf(partnerMotions)));

    }


    public List<Timeline> populateTimeline(final long accountId,final DateTime date,final DateTime targetDate, final DateTime endDate, final SleepEvents<Optional<Event>> sleepEventsFromAlgorithm, final ImmutableList<Event> extraEvents,
                                           final OneDaysSensorData sensorData) {

        // compute lights-out and sound-disturbance events
        Optional<DateTime> lightOutTimeOptional = Optional.absent();
        final List<Event> lightEvents = Lists.newArrayList();

        final ImmutableList<TrackerMotion> trackerMotions = sensorData.trackerMotions;
        final AllSensorSampleList allSensorSampleList = sensorData.allSensorSampleList;
        final ImmutableList<TrackerMotion> partnerMotions = sensorData.partnerMotions;

        if (!allSensorSampleList.isEmpty()) {

            // Light
            lightEvents.addAll(timelineUtils.getLightEvents(allSensorSampleList.get(Sensor.LIGHT)));
            if (lightEvents.size() > 0) {
                lightOutTimeOptional = timelineUtils.getLightsOutTime(lightEvents);
            }

        }

        if(lightOutTimeOptional.isPresent()){
            LOGGER.info("Light out at {}", lightOutTimeOptional.get());
        } else {
            LOGGER.info("No light out");
        }


        // create sleep-motion segments
        final List<MotionEvent> motionEvents = timelineUtils.generateMotionEvents(trackerMotions);

        final Map<Long, Event> timelineEvents = TimelineRefactored.populateTimeline(motionEvents);

        // LIGHT
        for(final Event event : lightEvents) {
            timelineEvents.put(event.getStartTimestamp(), event);
        }

        //USER-GENERATED FEEDBACK
        final Integer offsetMillis = trackerMotions.get(0).offsetMillis;


        // get sleep events as list
        final List<Optional<Event>> eventList = sleepEventsFromAlgorithm.toList();
        final List<Event> sleepEvents = new ArrayList<>();

        for(final Optional<Event> sleepEventOptional: eventList){
            if(sleepEventOptional.isPresent()){
                sleepEvents.add(sleepEventOptional.get());
            }
        }


        //  get the feedback in one form or another
        final ImmutableList<TimelineFeedback> feedbackList = getFeedbackList(accountId, targetDate, offsetMillis);

        //MOVE EVENTS BASED ON FEEDBACK
        final FeedbackUtils.ReprocessedEvents reprocessedEvents = feedbackUtils.reprocessEventsBasedOnFeedback(feedbackList, ImmutableList.copyOf(sleepEvents),extraEvents, offsetMillis);


        // PARTNER MOTION
        final List<PartnerMotionEvent> partnerMotionEvents = getPartnerMotionEvents(sleepEventsFromAlgorithm.fallAsleep, sleepEventsFromAlgorithm.wakeUp, ImmutableList.copyOf(motionEvents), partnerMotions);
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

        /* add sleep/wake events  */

        // if user feedback available, use them, otherwise stick to times from algorithm
        Optional<Event> inBedEventOptional = sleepEventsFromAlgorithm.goToBed;
        Optional<Event> outBedOptional = sleepEventsFromAlgorithm.outOfBed;
        Optional<Event> sleepEventOptional = sleepEventsFromAlgorithm.fallAsleep;
        Optional<Event> awakeEventOptional = sleepEventsFromAlgorithm.wakeUp;

        for (final Event event : reprocessedEvents.mainEvents) {
            timelineEvents.put(event.getStartTimestamp(), event);

            // adjust event times to use feedback times
            if (event.getType() == Event.Type.IN_BED) {
                inBedEventOptional = Optional.of(event);
            } else if (event.getType() == Event.Type.OUT_OF_BED) {
                outBedOptional = Optional.of(event);
            } else if (event.getType() == Event.Type.SLEEP) {
                sleepEventOptional = Optional.of(event);
            } else if (event.getType() == Event.Type.WAKE_UP) {
                awakeEventOptional = Optional.of(event);
            }
            LOGGER.debug("Adding feedback type {}, time {}", event.getType(), event.getStartTimestamp());
        }

        /*  add "additional" events -- which is wake/sleep/get up to pee events */
        for (final Event event : reprocessedEvents.extraEvents) {
            timelineEvents.put(event.getStartTimestamp(), event);
        }


        final List<Event> eventsWithSleepEvents = TimelineRefactored.mergeEvents(timelineEvents);
        final List<Event> smoothedEvents = timelineUtils.smoothEvents(eventsWithSleepEvents);


        /* clean up timeline */

        // 1. remove motion & null events outside sleep/in-bed period
        List<Event> cleanedUpEvents;
        if (this.hasRemoveMotionEventsOutsideSleep(accountId)) {
            // remove motion events outside of sleep and awake
            cleanedUpEvents = timelineUtils.removeMotionEventsOutsideSleep(smoothedEvents, sleepEventOptional, awakeEventOptional);
        } else {
            // remove motion events outside of in-bed and out-bed
            cleanedUpEvents = timelineUtils.removeMotionEventsOutsideBedPeriod(smoothedEvents, inBedEventOptional, outBedOptional);
        }

        // 2. Grey out events outside in-bed time
        final Boolean removeGreyOutEvents = this.hasRemoveGreyOutEvents(accountId); // rm grey events totally

        final List<Event> greyEvents = timelineUtils.greyNullEventsOutsideBedPeriod(cleanedUpEvents,
                inBedEventOptional, outBedOptional, removeGreyOutEvents);

        // 3. remove non-significant that are more than 1/3 of the entire night's time-span
        final List<Event> nonSignificantFilteredEvents = timelineUtils.removeEventBeforeSignificant(greyEvents);


        /* convert valid events to segment, compute sleep stats and score */

        final List<SleepSegment> sleepSegments = timelineUtils.eventsToSegments(nonSignificantFilteredEvents);

        final int lightSleepThreshold = 70; // TODO: Generate dynamically instead of hard threshold
        final SleepStats sleepStats = timelineUtils.computeStats(sleepSegments, lightSleepThreshold);
        final List<SleepSegment> reversed = Lists.reverse(sleepSegments);


        Integer sleepScore = computeAndMaybeSaveScore(trackerMotions, numSoundEvents, allSensorSampleList, targetDate, accountId, sleepStats);

        if(sleepStats.sleepDurationInMinutes < TimelineSafeguards.MINIMUM_SLEEP_DURATION_MINUTES) {
            LOGGER.warn("Score for account id {} was set to zero because sleep duration is too short ({} min)", accountId, sleepStats.sleepDurationInMinutes);
            sleepScore = 0;
        }

        final String timeLineMessage = timelineUtils.generateMessage(sleepStats, numPartnerMotion, numSoundEvents);

        LOGGER.debug("Score for account_id = {} is {}", accountId, sleepScore);


        final List<Insight> insights = timelineUtils.generatePreSleepInsights(allSensorSampleList, sleepStats.sleepTime, accountId);
        final List<SleepSegment>  reversedSegments = Lists.reverse(reversed);
        final Timeline timeline = Timeline.create(sleepScore, timeLineMessage, date.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT), reversedSegments, insights, sleepStats);

        final List<Timeline> timelines = Lists.newArrayList(timeline);
        return timelines;
    }

    /*
     * PRELIMINARY SANITY CHECK
     */
    protected InvalidNightType isValidNight(final Long accountId, final List<TrackerMotion> motionData){
        if(!hasNewInvalidNightFilterEnabled(accountId)){
            if(motionData.size() >= MIN_TRACKER_MOTION_COUNT){
                return InvalidNightType.VALID;
            }
            else {
                return InvalidNightType.NOT_ENOUGH_DATA;  // This needs to align to the old behavior before the new filter has been discussed.
            }
        }

        if(motionData.size() == 0){
            return InvalidNightType.NO_DATA;
        }

        //CHECK TO SEE IF MOTION AMPLITUDE IS EVER ABOVE MINIMUM THRESHOLD
        boolean isMotionAmplitudeAboveMinimumThreshold = false;

        for(final TrackerMotion trackerMotion : motionData){
            if(trackerMotion.value > MIN_MOTION_AMPLITUDE){
                isMotionAmplitudeAboveMinimumThreshold = true;
                break;
            }
        }

        //NEVER ABOVE THRESHOLD?  REJECT.
        if (!isMotionAmplitudeAboveMinimumThreshold) {
            return InvalidNightType.LOW_AMP_DATA;
        }

        //CHECK TO SEE IF TIME SPAN FROM FIRST TO LAST MEASUREMENT IS ABOVE 5 HOURS
        if(motionData.get(motionData.size() - 1).timestamp - motionData.get(0).timestamp < 5 * DateTimeConstants.MILLIS_PER_HOUR) {
            return InvalidNightType.TIMESPAN_TOO_SHORT;
        }

        //LAST, CHECK TO SEE IF THERE ARE "ENOUGH" MOTION EVENTS
        if(motionData.size() < MIN_TRACKER_MOTION_COUNT){
            return InvalidNightType.NOT_ENOUGH_DATA;
        }

        return InvalidNightType.VALID;
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
     * @return
     */
    private List<PartnerMotionEvent> getPartnerMotionEvents(final Optional<Event> fallingAsleepEvent, final Optional<Event> wakeupEvent, final ImmutableList<MotionEvent> motionEvents, final ImmutableList<TrackerMotion> partnerMotions) {
        // add partner movement data, check if there's a partner
        List<TrackerMotion> partnerMotionsWithinSleepBounds = new ArrayList<>();

        if (!fallingAsleepEvent.isPresent() || !wakeupEvent.isPresent()) {
            return Collections.EMPTY_LIST;
        }

        final long t1 = fallingAsleepEvent.get().getStartTimestamp();
        final long t2 = wakeupEvent.get().getStartTimestamp();

        for (final TrackerMotion pm : partnerMotions) {
            final long t = pm.timestamp;
            if (t >= t1 && t <= t2) {
                partnerMotionsWithinSleepBounds.add(pm);
            }
        }

        if (partnerMotionsWithinSleepBounds.size() > 0) {
            // use un-normalized data segments for comparison
            final List<MotionEvent> partnerMotionEvents = timelineUtils.generateMotionEvents(partnerMotionsWithinSleepBounds);

            return PartnerMotion.getPartnerData(partnerMotionEvents,motionEvents, 0);
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

        return timelineUtils.getSoundEvents(soundSamples, sleepDepths, lightOutTimeOptional, optionalSleepTime, optionalAwakeTime);
    }

    static private class HmmAlgorithmResults {
        final public SleepEvents<Optional<Event>> mainEvents;
        final public ImmutableList<Event> allTheOtherWakesAndSleeps;

        public HmmAlgorithmResults(SleepEvents<Optional<Event>> mainEvents, ImmutableList<Event> allTheOtherWakesAndSleeps) {
            this.mainEvents = mainEvents;
            this.allTheOtherWakesAndSleeps = allTheOtherWakesAndSleeps;
        }
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

    /**
     * Pang magic
     * @param targetDate
     * @param trackerMotions
     * @param rawLight
     * @param waves
     * @return
     */
    private SleepEvents<Optional<Event>> fromAlgorithm(final DateTime targetDate,
                                                       final List<TrackerMotion> trackerMotions,
                                                       final List<Sample> rawLight,
                                                       final List<Sample> waves) {
        Optional<Segment> sleepSegmentOptional;
        Optional<Segment> inBedSegmentOptional = Optional.absent();
        SleepEvents<Optional<Event>> sleepEventsFromAlgorithm = SleepEvents.create(Optional.<Event>absent(),
                Optional.<Event>absent(),
                Optional.<Event>absent(),
                Optional.<Event>absent());

        final List<Event> rawLightEvents = timelineUtils.getLightEventsWithMultipleLightOut(rawLight);
        final List<Event> smoothedLightEvents = MultiLightOutUtils.smoothLight(rawLightEvents, MultiLightOutUtils.DEFAULT_SMOOTH_GAP_MIN);
        final List<Event> lightOuts = MultiLightOutUtils.getValidLightOuts(smoothedLightEvents, trackerMotions, MultiLightOutUtils.DEFAULT_LIGHT_DELTA_WINDOW_MIN);

        final List<DateTime> lightOutTimes = MultiLightOutUtils.getLightOutTimes(lightOuts);

        // A day starts with 8pm local time and ends with 4pm local time next day
        try {

            Optional<DateTime> wakeUpWaveTimeOptional = timelineUtils.getFirstAwakeWaveTime(trackerMotions.get(0).timestamp,
                    trackerMotions.get(trackerMotions.size() - 1).timestamp,
                    waves );

            sleepEventsFromAlgorithm = timelineUtils.getSleepEvents(targetDate,
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


    /**
     * Sleep score - always compute and update dynamo
     * @param trackerMotions
     * @param targetDate
     * @param accountId
     * @param sleepStats
     * @return
     */
    private Integer computeAndMaybeSaveScore(final List<TrackerMotion> trackerMotions,
                                             final int numberSoundEvents,
                                             final AllSensorSampleList sensors,
                                             final DateTime targetDate,
                                             final Long accountId,
                                             final SleepStats sleepStats) {

        // Movement score
        final MotionScore motionScore = SleepScoreUtils.getSleepMotionScore(targetDate.withTimeAtStartOfDay(),
                trackerMotions, sleepStats.sleepTime, sleepStats.wakeTime);

        if (motionScore.score < (int) SleepScoreUtils.MOTION_SCORE_MIN) {
            // if motion score is zero, something is not quite right, don't save score
            LOGGER.error("No motion score generated for {} on {}", accountId, targetDate);
            return 0;
        }

        final Integer durationScore = computeSleepDurationScore(accountId, sleepStats);
        final Integer environmentScore = computeEnvironmentScore(accountId, sleepStats, numberSoundEvents, sensors);

        // Calculate the sleep score based on the sub scores and weighting
        final SleepScore sleepScore = new SleepScore.Builder()
                .withMotionScore(motionScore)
                .withSleepDurationScore(durationScore)
                .withEnvironmentalScore(environmentScore)
                .withWeighting(sleepScoreWeighting(accountId))
                .build();

        // Always update stats and scores to Dynamo
        final Integer userOffsetMillis = trackerMotions.get(0).offsetMillis;
        final Boolean updatedStats = this.sleepStatsDAODynamoDB.updateStat(accountId,
                targetDate.withTimeAtStartOfDay(), sleepScore.value, motionScore, sleepStats, userOffsetMillis);

        LOGGER.debug("Updated Stats-score: status {}, account {}, score {}, stats {}",
                updatedStats, accountId, sleepScore, sleepStats);

        return sleepScore.value;
    }

    private Integer computeSleepDurationScore(final Long accountId, final SleepStats sleepStats) {
        final Optional<Account> optionalAccount = accountDAO.getById(accountId);
        final int userAge = (optionalAccount.isPresent()) ? DateTimeUtil.getDateDiffFromNowInDays(optionalAccount.get().DOB) / 365 : 0;
        final Integer durationScore = SleepScoreUtils.getSleepDurationScore(userAge, sleepStats.sleepDurationInMinutes);
        return durationScore;
    }

    private Integer computeEnvironmentScore(final Long accountId,
                                            final SleepStats sleepStats,
                                            final int numberSoundEvents,
                                            final AllSensorSampleList sensors) {
        final Integer environmentScore;
        if (hasEnvironmentInTimelineScore(accountId) && sleepStats.sleepTime > 0L && sleepStats.wakeTime > 0L) {
            final int soundScore = SleepScoreUtils.calculateSoundScore(numberSoundEvents);
            final int temperatureScore = SleepScoreUtils.calculateTemperatureScore(sensors.get(Sensor.TEMPERATURE), sleepStats.sleepTime, sleepStats.wakeTime);
            final int humidityScore = SleepScoreUtils.calculateHumidityScore(sensors.get(Sensor.HUMIDITY), sleepStats.sleepTime, sleepStats.wakeTime);
            final int lightScore = SleepScoreUtils.calculateLightScore(sensors.get(Sensor.LIGHT), sleepStats.sleepTime, sleepStats.wakeTime);
            final int particulateScore = SleepScoreUtils.calculateParticulateScore(sensors.get(Sensor.PARTICULATES), sleepStats.sleepTime, sleepStats.wakeTime);

            environmentScore = SleepScoreUtils.calculateAggregateEnvironmentScore(soundScore, temperatureScore, humidityScore, lightScore, particulateScore);
        } else {
            environmentScore = 100;
        }
        return environmentScore;
    }

    private SleepScore.Weighting sleepScoreWeighting(final Long accountId) {
        final SleepScore.Weighting weighting;
        if (hasSleepScoreDurationWeighting(accountId)) {
            weighting = new SleepScore.DurationHeavyWeighting();
        } else {
            weighting = new SleepScore.Weighting();
        }
        return weighting;
    }

    private ImmutableList<TimelineFeedback> getFeedbackList(final Long accountId, final DateTime nightOf, final Integer offsetMillis) {

        if(!hasFeedbackInTimeline(accountId)) {
            LOGGER.debug("Timeline feedback not enabled for account {}", accountId);
            return ImmutableList.copyOf(Collections.EMPTY_LIST);
        }
        // this is needed to match the datetime created when receiving user feedback
        // I believe we should change how we create datetime in feedback once we have time
        // TODO: tim
        final DateTime nightOfUTC = new DateTime(nightOf.getYear(),
                nightOf.getMonthOfYear(), nightOf.getDayOfMonth(), 0, 0, 0, DateTimeZone.UTC);
        return feedbackDAO.getForNight(accountId, nightOfUTC);
    }

}
