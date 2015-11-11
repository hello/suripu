package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.core.algorithmintegration.OneDaysSensorData;
import com.hello.suripu.core.algorithmintegration.OnlineHmm;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DefaultModelEnsembleDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.DeviceDataDAODynamoDB;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.FeedbackReadDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Calibration;
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
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.FeedbackUtils;
import com.hello.suripu.core.util.MultiLightOutUtils;
import com.hello.suripu.core.util.PartnerDataUtils;
import com.hello.suripu.core.util.SensorDataTimezoneMap;
import com.hello.suripu.core.util.SleepHmmWithInterpretation;
import com.hello.suripu.core.util.SleepScoreUtils;
import com.hello.suripu.core.util.TimelineError;
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
    private final DeviceReadDAO deviceDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final DeviceDataDAODynamoDB deviceDataDAODynamoDB;
    private final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB;
    private final FeedbackReadDAO feedbackDAO;
    private final SleepHmmDAO sleepHmmDAO;
    private final AccountReadDAO accountDAO;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final Logger LOGGER;
    private final TimelineUtils timelineUtils;
    private final TimelineSafeguards timelineSafeguards;
    private final FeedbackUtils feedbackUtils;
    private final PartnerDataUtils partnerDataUtils;
    private final SenseColorDAO senseColorDAO;
    private final OnlineHmmModelsDAO priorsDAO;
    private final FeatureExtractionModelsDAO featureExtractionModelsDAO;
    private final Optional<UUID> uuidOptional;
    private final CalibrationDAO calibrationDAO;
    private final DefaultModelEnsembleDAO defaultModelEnsembleDAO;

    final private static int SLOT_DURATION_MINUTES = 1;
    public final static int MIN_TRACKER_MOTION_COUNT = 20;
    public final static int MIN_PARTNER_FILTERED_MOTION_COUNT = 5;
    public final static int MIN_DURATION_OF_TRACKER_MOTION_IN_HOURS = 5;
    public final static int MIN_DURATION_OF_FILTERED_MOTION_IN_HOURS = 3;
    public final static int MIN_MOTION_AMPLITUDE = 1000;

    public final static String ALGORITHM_NAME_REGULAR = "wupang";
    public final static String ALGORITHM_NAME_VOTING = "voting";
    public final static String ALGORITHM_NAME_BAYESNET = "bayesnet";
    public final static String ALGORITHM_NAME_HMM = "hmm";
    public final static String VERSION_BACKUP = "wupang_backup_for_hmm"; //let us know the HMM had some issues


    static public TimelineProcessor createTimelineProcessor(final TrackerMotionDAO trackerMotionDAO,
                                                            final DeviceReadDAO deviceDAO,
                                                            final DeviceDataDAO deviceDataDAO,
                                                            final DeviceDataDAODynamoDB deviceDataDAODynamoDB,
                                                            final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB,
                                                            final FeedbackReadDAO feedbackDAO,
                                                            final SleepHmmDAO sleepHmmDAO,
                                                            final AccountReadDAO accountDAO,
                                                            final SleepStatsDAODynamoDB sleepStatsDAODynamoDB,
                                                            final SenseColorDAO senseColorDAO,
                                                            final OnlineHmmModelsDAO priorsDAO,
                                                            final FeatureExtractionModelsDAO featureExtractionModelsDAO,
                                                            final CalibrationDAO calibrationDAO,
                                                            final DefaultModelEnsembleDAO defaultModelEnsembleDAO) {

        final LoggerWithSessionId logger = new LoggerWithSessionId(STATIC_LOGGER);
        return new TimelineProcessor(trackerMotionDAO,
                deviceDAO,deviceDataDAO,deviceDataDAODynamoDB,ringTimeHistoryDAODynamoDB,
                feedbackDAO,sleepHmmDAO,accountDAO,sleepStatsDAODynamoDB,
                senseColorDAO,priorsDAO, featureExtractionModelsDAO,
                Optional.<UUID>absent(), calibrationDAO,defaultModelEnsembleDAO);
    }

    public TimelineProcessor copyMeWithNewUUID(final UUID uuid) {

        return new TimelineProcessor(trackerMotionDAO,deviceDAO,deviceDataDAO,deviceDataDAODynamoDB,ringTimeHistoryDAODynamoDB,feedbackDAO,sleepHmmDAO,accountDAO,sleepStatsDAODynamoDB,senseColorDAO,priorsDAO,featureExtractionModelsDAO,Optional.of(uuid),calibrationDAO,defaultModelEnsembleDAO);
    }

    //private SessionLogDebug(final String)

    private TimelineProcessor(final TrackerMotionDAO trackerMotionDAO,
                            final DeviceReadDAO deviceDAO,
                            final DeviceDataDAO deviceDataDAO,
                              final DeviceDataDAODynamoDB deviceDataDAODynamoDB,
                            final RingTimeHistoryDAODynamoDB ringTimeHistoryDAODynamoDB,
                            final FeedbackReadDAO feedbackDAO,
                            final SleepHmmDAO sleepHmmDAO,
                            final AccountReadDAO accountDAO,
                            final SleepStatsDAODynamoDB sleepStatsDAODynamoDB,
                              final SenseColorDAO senseColorDAO,
                              final OnlineHmmModelsDAO priorsDAO,
                              final FeatureExtractionModelsDAO featureExtractionModelsDAO,
                              final Optional<UUID> uuid,
                              final CalibrationDAO calibrationDAO,
                              final DefaultModelEnsembleDAO defaultModelEnsembleDAO) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.deviceDAO = deviceDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
        this.ringTimeHistoryDAODynamoDB = ringTimeHistoryDAODynamoDB;
        this.feedbackDAO = feedbackDAO;
        this.sleepHmmDAO = sleepHmmDAO;
        this.accountDAO = accountDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.senseColorDAO = senseColorDAO;
        this.priorsDAO = priorsDAO;
        this.featureExtractionModelsDAO = featureExtractionModelsDAO;
        this.calibrationDAO = calibrationDAO;
        this.defaultModelEnsembleDAO = defaultModelEnsembleDAO;

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

    public TimelineResult retrieveTimelinesFast(final Long accountId, final DateTime date, final Optional<TimelineFeedback> newFeedback) {
        final DateTime targetDate = date.withTimeAtStartOfDay().withHourOfDay(DateTimeUtil.DAY_STARTS_AT_HOUR);
        final DateTime endDate = date.withTimeAtStartOfDay().plusDays(1).withHourOfDay(DateTimeUtil.DAY_ENDS_AT_HOUR);
        final DateTime  currentTime = DateTime.now().withZone(DateTimeZone.UTC);

        final TimelineLog log = new TimelineLog(accountId,targetDate.withZone(DateTimeZone.UTC).getMillis());
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);



        final Optional<OneDaysSensorData> sensorDataOptional = getSensorData(accountId, targetDate, endDate,currentTime.getMillis(),newFeedback);

        if (!sensorDataOptional.isPresent()) {
            LOGGER.debug("returning empty timeline for account_id = {} and day = {}", accountId, targetDate);
            log.addMessage(TimelineError.NO_DATA);
            return TimelineResult.createEmpty(log, English.TIMELINE_NO_SLEEP_DATA, true);
        }


        final OneDaysSensorData sensorData = sensorDataOptional.get();
        final TimelineError discardReason = isValidNight(accountId, sensorData.originalTrackerMotions,sensorData.trackerMotions);

        switch (discardReason){
            case TIMESPAN_TOO_SHORT:
                log.addMessage(discardReason);
                LOGGER.info("Tracker motion span too short for account_id = {} and day = {}", accountId, targetDate);
                return TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, true);

            case NOT_ENOUGH_DATA:
                log.addMessage(discardReason);
                LOGGER.info("Not enough tracker motion seen for account_id = {} and day = {}", accountId, targetDate);
                return TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, true);

            case NO_DATA:
                log.addMessage(discardReason);
                LOGGER.info("No tracker motion data for account_id = {} and day = {}", accountId, targetDate);
                return TimelineResult.createEmpty(log, English.TIMELINE_NO_SLEEP_DATA, true);

            case LOW_AMP_DATA:
                log.addMessage(discardReason);
                LOGGER.info("tracker motion did not exceed minimum threshold for account_id = {} and day = {}", accountId, targetDate);
                return TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, true);

            case PARTNER_FILTER_REJECTED_DATA:
                log.addMessage(discardReason);
                LOGGER.info("tracker motion was discarded because of partner filter account_id = {} and day = {}", accountId, targetDate);
                return TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, true);


            default:
                break;
        }


        try {
            boolean algorithmWorked = false;

            Optional<SleepEvents<Optional<Event>>> sleepEventsFromAlgorithmOptional = Optional.absent();
            List<Event> extraEvents = Collections.EMPTY_LIST;

            final DateTime currentTimeInLocalUtc = currentTime.plus(sensorData.timezoneOffsetMillis);

            ONLINEHMM:
            if (this.hasOnlineHmmAlgorithmEnabled(accountId)) {
                try {
                    LOGGER.info("TRYING THE ONLINE HMM");

                    final OnlineHmm onlineHmm = new OnlineHmm(defaultModelEnsembleDAO, featureExtractionModelsDAO, priorsDAO, uuidOptional);


                    final boolean feedbackChanged = newFeedback.isPresent() && this.hasOnlineHmmAlgorithmEnabled(accountId);

                    
                    final SleepEvents<Optional<Event>> events = onlineHmm.predictAndUpdateWithLabels(
                            accountId,
                            date,
                            targetDate,
                            endDate,
                            currentTimeInLocalUtc,
                            sensorData,
                            feedbackChanged,
                            false);

                    sleepEventsFromAlgorithmOptional = Optional.of(events);

                    //verify that algorithm produced something useable
                    final TimelineError error = timelineSafeguards.checkIfValidTimeline(
                            sleepEventsFromAlgorithmOptional.get(),
                            ImmutableList.copyOf(Collections.EMPTY_LIST),
                            ImmutableList.copyOf(sensorData.allSensorSampleList.get(Sensor.LIGHT)));

                    if (!error.equals(TimelineError.NO_ERROR)) {
                        log.addMessage(AlgorithmType.ONLINE_HMM, error);
                        LOGGER.info("online HMM error {}",error);
                        break ONLINEHMM;
                    }

                    algorithmWorked = true;
                    log.addMessage(AlgorithmType.ONLINE_HMM, timelineUtils.eventsFromOptionalEvents(sleepEventsFromAlgorithmOptional.get().toList()));

                }
                catch (Exception e) {
                    log.addMessage(AlgorithmType.ONLINE_HMM, TimelineError.UNEXEPECTED);
                    LOGGER.error(e.getMessage());
                }
            }




          /* Default if Online HMM is not enabled, otherwise Backup #1  */
            HMM:
            if (!algorithmWorked) {
                try {
                    LOGGER.info("TRYING THE OLD HMM");

                    final Optional<HmmAlgorithmResults> results = fromHmm(accountId, currentTime, targetDate, endDate,
                            sensorData.trackerMotions,
                            sensorData.allSensorSampleList);

                    if (!results.isPresent()) {
                        log.addMessage(AlgorithmType.HMM, TimelineError.MISSING_KEY_EVENTS);
                        break HMM;
                    }

                    sleepEventsFromAlgorithmOptional = Optional.of(results.get().mainEvents);
                    extraEvents = results.get().allTheOtherWakesAndSleeps.asList();

                    //verify that algorithm produced something useable
                    final TimelineError error = timelineSafeguards.checkIfValidTimeline(
                            sleepEventsFromAlgorithmOptional.get(),
                            ImmutableList.copyOf(extraEvents),
                            ImmutableList.copyOf(sensorData.allSensorSampleList.get(Sensor.LIGHT)));

                    if (!error.equals(TimelineError.NO_ERROR)) {
                        log.addMessage(AlgorithmType.HMM, error);
                        LOGGER.info("ye olde HMM error {}",error);
                        break HMM;
                    }

                    log.addMessage(AlgorithmType.HMM, timelineUtils.eventsFromOptionalEvents(sleepEventsFromAlgorithmOptional.get().toList()));
                    algorithmWorked = true;

                }
                catch (Exception e) {
                    log.addMessage(AlgorithmType.HMM, TimelineError.UNEXEPECTED);
                    LOGGER.error(e.getMessage());
                }
            }


          /* Backup 2  */
            VOTING:
            if (!algorithmWorked) {
                LOGGER.info("TRYING VOTING ALGORITHM");

                try {
                    //reset state
                    final Optional<VotingSleepEvents> votingSleepEventsOptional = fromVotingAlgorithm(sensorData.trackerMotions,
                            sensorData.allSensorSampleList.get(Sensor.SOUND),
                            sensorData.allSensorSampleList.get(Sensor.LIGHT),
                            sensorData.allSensorSampleList.get(Sensor.WAVE_COUNT));

                    if (!votingSleepEventsOptional.isPresent()) {
                        LOGGER.warn("backup algorithm did not produce ANY events, account_id = {} and day = {}", accountId, targetDate);
                        log.addMessage(AlgorithmType.VOTING, TimelineError.UNEXEPECTED, "optional.absent from fromVotingAlgorithm");
                        break VOTING;
                    }

                    sleepEventsFromAlgorithmOptional = Optional.of(votingSleepEventsOptional.get().sleepEvents);
                    extraEvents = votingSleepEventsOptional.get().extraEvents;

                    final SleepEvents<Optional<Event>> sleepEventsFromAlgorithm = sleepEventsFromAlgorithmOptional.get();

                    //make sure all events are created, otherwise fail, but this is the only check we are running
                    for (final Optional<Event> eventOptional : sleepEventsFromAlgorithm.toList()) {

                        if (!eventOptional.isPresent()) {
                            LOGGER.info("backup algorithm did not produce all four events, account_id = {} and day = {}", accountId, targetDate);
                            log.addMessage(AlgorithmType.VOTING, TimelineError.MISSING_KEY_EVENTS);
                            break VOTING;
                        }
                    }

                    log.addMessage(AlgorithmType.VOTING, timelineUtils.eventsFromOptionalEvents(sleepEventsFromAlgorithm.toList()));
                    algorithmWorked = true;
                }
                catch (Exception e) {
                    log.addMessage(AlgorithmType.VOTING, TimelineError.UNEXEPECTED);
                    LOGGER.error(e.getMessage());
                }
            }

            //did events get produced, and did one of the algorithms work?  If not, poof, we are done.
            if (!sleepEventsFromAlgorithmOptional.isPresent() || !algorithmWorked) {
                LOGGER.error("returning empty timeline for account_id = {} and day = {}", accountId, targetDate);
                log.addMessage(AlgorithmType.NONE,TimelineError.UNEXEPECTED,"no successful algorithms");
                return TimelineResult.createEmpty(log);
            }

            final SleepEvents<Optional<Event>> sleepEvents = sleepEventsFromAlgorithmOptional.get();

            /* FEATURE FLIP EXTRA EVENTS */
            if (!this.hasExtraEventsEnabled(accountId)) {
                LOGGER.info("not using {} extra events", extraEvents.size());
                extraEvents = Collections.EMPTY_LIST;
            }


            final PopulatedTimelines populateTimelines = populateTimeline(accountId,date,targetDate,endDate,sleepEventsFromAlgorithmOptional.get(),ImmutableList.copyOf(extraEvents), sensorData);


            if (!populateTimelines.isValidSleepScore) {
                log.addMessage(TimelineError.INVALID_SLEEP_SCORE);
                LOGGER.warn("invalid sleep score");
                return TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, true);
            }

            return TimelineResult.create(populateTimelines.timelines, log);
        }
        catch (Exception e) {
            log.addMessage(AlgorithmType.NONE,TimelineError.UNEXEPECTED,"unexpected exception on outer try/catch");
            LOGGER.error(e.toString());
        }

        LOGGER.debug("returning empty timeline for account_id = {} and day = {}", accountId, targetDate);
        return TimelineResult.createEmpty(log);

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








    protected Optional<OneDaysSensorData> getSensorData(final long accountId, final DateTime targetDate, final DateTime endDate, final long currentTimeUTC,final Optional<TimelineFeedback> newFeedback) {
        final List<TrackerMotion> originalTrackerMotions = trackerMotionDAO.getBetweenLocalUTC(accountId, targetDate, endDate);
        if (originalTrackerMotions.isEmpty()) {
            LOGGER.warn("No original tracker motion data for account {} on {}, returning optional absent", accountId, targetDate);
            return Optional.absent();
        }

        LOGGER.debug("Length of originalTrackerMotion is {} for {} on {}", originalTrackerMotions.size(), accountId, targetDate);

        // get partner tracker motion, if available
        final List<TrackerMotion> originalPartnerMotions = getPartnerTrackerMotion(accountId, targetDate, endDate);
        final List<TrackerMotion> trackerMotions = new ArrayList<>();

        if (!originalPartnerMotions.isEmpty()) {

            final int tzOffsetMillis = originalTrackerMotions.get(0).offsetMillis;

            if (this.hasPartnerFilterEnabled(accountId)) {
                LOGGER.info("using original partner filter");
                try {
                    PartnerDataUtils.PartnerMotions motions = partnerDataUtils.getMyMotion(originalTrackerMotions, originalPartnerMotions);
                    trackerMotions.addAll(motions.myMotions);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                    trackerMotions.addAll(originalTrackerMotions);
                }
            }
            else if (this.hasHmmPartnerFilterEnabled(accountId)) {
                LOGGER.info("using hmm partner filter");
                try {
                    trackerMotions.addAll(
                            partnerDataUtils.partnerFilterWithDurationsDiffHmm(
                                    targetDate.minusMillis(tzOffsetMillis),
                                    endDate.minusMillis(tzOffsetMillis),
                                    ImmutableList.copyOf(originalTrackerMotions),
                                    ImmutableList.copyOf(originalPartnerMotions)));

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

        if (trackerMotions.isEmpty()) {
            LOGGER.debug("No tracker motion data ID for account_id = {} and day = {}", accountId, targetDate);
            return Optional.absent();
        }


        final int tzOffsetMillis = trackerMotions.get(0).offsetMillis;

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

        final Optional<Calibration> calibrationOptional = this.hasCalibrationEnabled(externalDeviceId) ? calibrationDAO.getStrict(externalDeviceId) : Optional.<Calibration>absent();

        AllSensorSampleList allSensorSampleList;
        // query dates in utc_ts (table has an index for this)
        LOGGER.debug("Query all sensors with utc ts for account {}", accountId);

        if (hasDeviceDataDynamoDBTimelineEnabled(accountId)) {
            allSensorSampleList = deviceDataDAODynamoDB.generateTimeSeriesByUTCTimeAllSensors(
                    targetDate.minusMillis(tzOffsetMillis).getMillis(),
                    endDate.minusMillis(tzOffsetMillis).getMillis(),
                    accountId, externalDeviceId, SLOT_DURATION_MINUTES, missingDataDefaultValue(accountId),optionalColor, calibrationOptional
            );
            LOGGER.info("Sensor data for timeline generated by DynamoDB for account {}", accountId);
        } else {
            allSensorSampleList = deviceDataDAO.generateTimeSeriesByUTCTimeAllSensors(
                    targetDate.minusMillis(tzOffsetMillis).getMillis(),
                    endDate.minusMillis(tzOffsetMillis).getMillis(),
                    accountId, deviceId, SLOT_DURATION_MINUTES, missingDataDefaultValue(accountId),optionalColor, calibrationOptional
            );
        }

        if (allSensorSampleList.isEmpty()) {
            LOGGER.debug("No sense sensor data ID for account_id = {} and day = {}", accountId, targetDate);
            return Optional.absent();
        }

        final List<TimelineFeedback> feedbackList = Lists.newArrayList(getFeedbackList(accountId, targetDate, tzOffsetMillis));

        if (newFeedback.isPresent()) {
            feedbackList.add(newFeedback.get());
        }

        return Optional.of(new OneDaysSensorData(allSensorSampleList,
                ImmutableList.copyOf(trackerMotions),ImmutableList.copyOf(originalPartnerMotions),
                ImmutableList.copyOf(feedbackList),
                ImmutableList.copyOf(originalTrackerMotions),ImmutableList.copyOf(originalPartnerMotions),
                tzOffsetMillis));

    }

    private static class PopulatedTimelines {
        public final List<Timeline> timelines;
        public final boolean isValidSleepScore;

        public PopulatedTimelines(List<Timeline> timelines, boolean isValidSleepScore) {
            this.timelines = timelines;
            this.isValidSleepScore = isValidSleepScore;
        }
    }


    public PopulatedTimelines populateTimeline(final long accountId,final DateTime date,final DateTime targetDate, final DateTime endDate, final SleepEvents<Optional<Event>> sleepEventsFromAlgorithm, final ImmutableList<Event> extraEvents,
                                           final OneDaysSensorData sensorData) {

        // compute lights-out and sound-disturbance events
        Optional<DateTime> lightOutTimeOptional = Optional.absent();
        final List<Event> lightEvents = Lists.newArrayList();

        final ImmutableList<TrackerMotion> trackerMotions = sensorData.trackerMotions;
        final AllSensorSampleList allSensorSampleList = sensorData.allSensorSampleList;
        final ImmutableList<TrackerMotion> partnerMotions = sensorData.partnerMotions;
        final ImmutableList<TimelineFeedback> feedbackList = sensorData.feedbackList;

        final SensorDataTimezoneMap sensorDataTimezoneMap = SensorDataTimezoneMap.create(sensorData.allSensorSampleList.get(Sensor.LIGHT));


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

        //MOVE EVENTS BASED ON FEEDBACK
        FeedbackUtils.ReprocessedEvents reprocessedEvents = null;

        if (this.hasTimelineOrderEnforcement(accountId)) {
            reprocessedEvents = feedbackUtils.reprocessEventsBasedOnFeedback(feedbackList, ImmutableList.copyOf(sleepEvents), extraEvents, offsetMillis);

        }
        else {
            reprocessedEvents = feedbackUtils.reprocessEventsBasedOnFeedbackTheOldWay(feedbackList, ImmutableList.copyOf(sleepEvents), extraEvents, offsetMillis);
        }

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

        if (!this.hasInvalidSleepScoreFromFeedbackChecking(accountId)) {
            //ORIGINAL BEHAVIOR
            if (sleepStats.sleepDurationInMinutes < TimelineSafeguards.MINIMUM_SLEEP_DURATION_MINUTES) {
                LOGGER.warn("Score for account id {} was set to zero because sleep duration is too short ({} min)", accountId, sleepStats.sleepDurationInMinutes);
                sleepScore = 0;
            }
        }

        boolean isValidSleepScore = sleepScore > 0;

        //if there's feedback, sleep score can never be invalid
        if (!feedbackList.isEmpty()) {
            isValidSleepScore = true;
        }

        final String timeLineMessage = timelineUtils.generateMessage(sleepStats, numPartnerMotion, numSoundEvents);

        LOGGER.debug("Score for account_id = {} is {}", accountId, sleepScore);


        final List<Insight> insights;
        if (hasTimelineInSleepInsights(accountId)) {
            insights = timelineUtils.generateInSleepInsights(allSensorSampleList, numSoundEvents,
                    sleepStats.sleepTime, sleepStats.wakeTime);
        } else {
            insights = timelineUtils.generatePreSleepInsights(allSensorSampleList,
                    sleepStats.sleepTime, accountId);
        }

        List<SleepSegment> reversedSegments = Lists.reverse(reversed);

        if (hasSleepSegmentOffsetRemapping(accountId)) {
            reversedSegments = sensorDataTimezoneMap.remapSleepSegmentOffsets(reversedSegments);
        }

        final Timeline timeline = Timeline.create(sleepScore, timeLineMessage, date.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT), reversedSegments, insights, sleepStats);

        return new PopulatedTimelines(Lists.newArrayList(timeline),isValidSleepScore);
    }

    /*
     * PRELIMINARY SANITY CHECK (static and public for testing purposes)
     */
    static public TimelineError isValidNight(final Long accountId, final List<TrackerMotion> originalMotionData, final List<TrackerMotion> filteredMotionData){

        if(originalMotionData.size() == 0){
            return TimelineError.NO_DATA;
        }

        //CHECK TO SEE IF THERE ARE "ENOUGH" MOTION EVENTS
        if(originalMotionData.size() < MIN_TRACKER_MOTION_COUNT){
            return TimelineError.NOT_ENOUGH_DATA;
        }

        //CHECK TO SEE IF MOTION AMPLITUDE IS EVER ABOVE MINIMUM THRESHOLD
        boolean isMotionAmplitudeAboveMinimumThreshold = false;

        for(final TrackerMotion trackerMotion : originalMotionData){
            if(trackerMotion.value > MIN_MOTION_AMPLITUDE){
                isMotionAmplitudeAboveMinimumThreshold = true;
                break;
            }
        }

        //NEVER ABOVE THRESHOLD?  REJECT.
        if (!isMotionAmplitudeAboveMinimumThreshold) {
            return TimelineError.LOW_AMP_DATA;
        }

        //CHECK TO SEE IF TIME SPAN FROM FIRST TO LAST MEASUREMENT IS ABOVE 5 HOURS
        if(originalMotionData.get(originalMotionData.size() - 1).timestamp - originalMotionData.get(0).timestamp < MIN_DURATION_OF_TRACKER_MOTION_IN_HOURS * DateTimeConstants.MILLIS_PER_HOUR) {
            return TimelineError.TIMESPAN_TOO_SHORT;
        }

        //IF THE FILTERING WAS NOT USED (OR HAD NO EFFECT) WE ARE DONE
        if (originalMotionData.size() == filteredMotionData.size()) {
            return TimelineError.NO_ERROR;
        }

        ///////////////////////////
        //PARTNER FILTERED DATA CHECKS ////
        //////////////////////////

        //"not enough", not "no data", because there must have been some original data to get to this point
        if (filteredMotionData.isEmpty()) {
            return TimelineError.PARTNER_FILTER_REJECTED_DATA;
        }

        //CHECK TO SEE IF THERE ARE "ENOUGH" MOTION EVENTS, post partner-filtering.  trying to avoid case where partner filter lets a couple through even though the user is not there.
        if (filteredMotionData.size() < MIN_PARTNER_FILTERED_MOTION_COUNT) {
            return TimelineError.PARTNER_FILTER_REJECTED_DATA;
        }

        //CHECK TO SEE IF TIME SPAN FROM FIRST TO LAST MEASUREMENT OF PARTNER-FILTERED DATA IS ABOVE 3 HOURS
        if(filteredMotionData.get(filteredMotionData.size() - 1).timestamp - filteredMotionData.get(0).timestamp < MIN_DURATION_OF_FILTERED_MOTION_IN_HOURS * DateTimeConstants.MILLIS_PER_HOUR) {
            return TimelineError.PARTNER_FILTER_REJECTED_DATA;
        }

        return TimelineError.NO_ERROR;
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
        MotionScore motionScore = SleepScoreUtils.getSleepMotionScore(targetDate.withTimeAtStartOfDay(),
                trackerMotions, sleepStats.sleepTime, sleepStats.wakeTime);





        if (this.hasInvalidSleepScoreFromFeedbackChecking(accountId)) {
            if (motionScore.score < (int) SleepScoreUtils.MOTION_SCORE_MIN)  {
                LOGGER.warn("enforced minimum motion score for {} on {}", accountId, targetDate);
                motionScore = new MotionScore(motionScore.numMotions, motionScore.motionPeriodMinutes, motionScore.avgAmplitude, motionScore.maxAmplitude, (int) SleepScoreUtils.MOTION_SCORE_MIN);
            }
        }
        else {
            //original behavior
            if (motionScore.score < (int) SleepScoreUtils.MOTION_SCORE_MIN) {
                // if motion score is zero, something is not quite right, don't save score
                LOGGER.error("No motion score generated for {} on {}", accountId, targetDate);
                return 0;
            }
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
