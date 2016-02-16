package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.algorithmintegration.AlgorithmFactory;
import com.hello.suripu.core.algorithmintegration.OneDaysSensorData;
import com.hello.suripu.core.algorithmintegration.TimelineAlgorithm;
import com.hello.suripu.core.algorithmintegration.TimelineAlgorithmResult;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DefaultModelEnsembleDAO;
import com.hello.suripu.core.db.DeviceDataReadAllSensorsDAO;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.FeedbackReadDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.db.PillDataReadDAO;
import com.hello.suripu.core.db.RingTimeHistoryReadDAO;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.db.SleepStatsDAO;
import com.hello.suripu.core.db.UserTimelineTestGroupDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.DataCompleteness;
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
import com.hello.suripu.core.util.OutlierFilter;
import com.hello.suripu.core.util.PartnerDataUtils;
import com.hello.suripu.core.util.SensorDataTimezoneMap;
import com.hello.suripu.core.util.SleepScoreUtils;
import com.hello.suripu.core.util.TimelineError;
import com.hello.suripu.core.util.TimelineRefactored;
import com.hello.suripu.core.util.TimelineSafeguards;
import com.hello.suripu.core.util.TimelineUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TimelineProcessor extends FeatureFlippedProcessor {

    public static final String VERSION = "0.0.2";
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(TimelineProcessor.class);
    private final PillDataReadDAO pillDataDAODynamoDB;
    private final DeviceReadDAO deviceDAO;
    private final DeviceDataReadAllSensorsDAO deviceDataDAODynamoDB;
    private final RingTimeHistoryReadDAO ringTimeHistoryDAODynamoDB;
    private final FeedbackReadDAO feedbackDAO;
    private final SleepHmmDAO sleepHmmDAO;
    private final AccountReadDAO accountDAO;
    private final SleepStatsDAO sleepStatsDAODynamoDB;
    private final Logger LOGGER;
    private final TimelineUtils timelineUtils;
    private final TimelineSafeguards timelineSafeguards;
    private final FeedbackUtils feedbackUtils;
    private final PartnerDataUtils partnerDataUtils;
    private final SenseColorDAO senseColorDAO;
    private final CalibrationDAO calibrationDAO;
    private final UserTimelineTestGroupDAO userTimelineTestGroupDAO;

    private final AlgorithmFactory algorithmFactory;

    final private static int SLOT_DURATION_MINUTES = 1;
    public final static int MIN_TRACKER_MOTION_COUNT = 20;
    public final static int MIN_PARTNER_FILTERED_MOTION_COUNT = 5;
    public final static int MIN_DURATION_OF_TRACKER_MOTION_IN_HOURS = 5;
    public final static int MIN_DURATION_OF_FILTERED_MOTION_IN_HOURS = 3;
    public final static int MIN_MOTION_AMPLITUDE = 1000;
    final static long OUTLIER_GUARD_DURATION = (long)(DateTimeConstants.MILLIS_PER_HOUR * 2.0); //min spacing between motion groups
    final static long DOMINANT_GROUP_DURATION = (long)(DateTimeConstants.MILLIS_PER_HOUR * 6.0); //num hours in a motion group to be considered the dominant one


    static public TimelineProcessor createTimelineProcessor(final PillDataReadDAO pillDataDAODynamoDB,
                                                            final DeviceReadDAO deviceDAO,
                                                            final DeviceDataReadAllSensorsDAO deviceDataDAODynamoDB,
                                                            final RingTimeHistoryReadDAO ringTimeHistoryDAODynamoDB,
                                                            final FeedbackReadDAO feedbackDAO,
                                                            final SleepHmmDAO sleepHmmDAO,
                                                            final AccountReadDAO accountDAO,
                                                            final SleepStatsDAO sleepStatsDAODynamoDB,
                                                            final SenseColorDAO senseColorDAO,
                                                            final OnlineHmmModelsDAO priorsDAO,
                                                            final FeatureExtractionModelsDAO featureExtractionModelsDAO,
                                                            final CalibrationDAO calibrationDAO,
                                                            final DefaultModelEnsembleDAO defaultModelEnsembleDAO,
                                                            final UserTimelineTestGroupDAO userTimelineTestGroupDAO) {

        final LoggerWithSessionId logger = new LoggerWithSessionId(STATIC_LOGGER);

        final AlgorithmFactory algorithmFactory = AlgorithmFactory.create(sleepHmmDAO,priorsDAO,defaultModelEnsembleDAO,featureExtractionModelsDAO,Optional.<UUID>absent());

        return new TimelineProcessor(pillDataDAODynamoDB,
                deviceDAO,deviceDataDAODynamoDB,ringTimeHistoryDAODynamoDB,
                feedbackDAO,sleepHmmDAO,accountDAO,sleepStatsDAODynamoDB,
                senseColorDAO,
                Optional.<UUID>absent(),
                calibrationDAO,
                userTimelineTestGroupDAO,
                algorithmFactory);
    }

    public TimelineProcessor copyMeWithNewUUID(final UUID uuid) {

        return new TimelineProcessor(pillDataDAODynamoDB, deviceDAO,deviceDataDAODynamoDB,ringTimeHistoryDAODynamoDB,feedbackDAO,sleepHmmDAO,accountDAO,sleepStatsDAODynamoDB,senseColorDAO,Optional.of(uuid),calibrationDAO,userTimelineTestGroupDAO,algorithmFactory.cloneWithNewUUID(Optional.of(uuid)));
    }

    //private SessionLogDebug(final String)

    private TimelineProcessor(final PillDataReadDAO pillDataDAODynamoDB,
                              final DeviceReadDAO deviceDAO,
                              final DeviceDataReadAllSensorsDAO deviceDataDAODynamoDB,
                              final RingTimeHistoryReadDAO ringTimeHistoryDAODynamoDB,
                              final FeedbackReadDAO feedbackDAO,
                              final SleepHmmDAO sleepHmmDAO,
                              final AccountReadDAO accountDAO,
                              final SleepStatsDAO sleepStatsDAODynamoDB,
                              final SenseColorDAO senseColorDAO,
                              final Optional<UUID> uuid,
                              final CalibrationDAO calibrationDAO,
                              final UserTimelineTestGroupDAO userTimelineTestGroupDAO,
                              final AlgorithmFactory algorithmFactory) {
        this.pillDataDAODynamoDB = pillDataDAODynamoDB;
        this.deviceDAO = deviceDAO;
        this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
        this.ringTimeHistoryDAODynamoDB = ringTimeHistoryDAODynamoDB;
        this.feedbackDAO = feedbackDAO;
        this.sleepHmmDAO = sleepHmmDAO;
        this.accountDAO = accountDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.senseColorDAO = senseColorDAO;
        this.calibrationDAO = calibrationDAO;
        this.userTimelineTestGroupDAO = userTimelineTestGroupDAO;
        this.algorithmFactory = algorithmFactory;

        timelineUtils = new TimelineUtils(uuid);
        timelineSafeguards = new TimelineSafeguards(uuid);
        feedbackUtils = new FeedbackUtils(uuid);
        partnerDataUtils = new PartnerDataUtils(uuid);
        this.LOGGER = new LoggerWithSessionId(STATIC_LOGGER, uuid);
    }

    private Long getTestGroup(final Long accountId, final DateTime targetDateLocalUTC, final int timezoneOffsetMillis) {

        final DateTime dateOfNightUTC = targetDateLocalUTC.withZone(DateTimeZone.UTC).minusMillis(timezoneOffsetMillis);

        //find out user test group
        final Optional<Long> groupIdOptional = userTimelineTestGroupDAO.getUserGestGroup(accountId,dateOfNightUTC);

        if (groupIdOptional.isPresent()) {
            return groupIdOptional.get();
        }

        return TimelineLog.DEFAULT_TEST_GROUP;
    }

    public TimelineResult retrieveTimelinesFast(final Long accountId, final DateTime targetDate, final Optional<TimelineFeedback> newFeedback) {

        final boolean feedbackChanged = newFeedback.isPresent() && this.hasOnlineHmmLearningEnabled(accountId);

        //chain of fail-overs of algorithm (i.e)
        AlgorithmType[] algorithmChain = new AlgorithmType[]{AlgorithmType.HMM, AlgorithmType.VOTING};

        if (this.hasOnlineHmmAlgorithmEnabled(accountId)) {
            algorithmChain = new AlgorithmType[]{AlgorithmType.ONLINE_HMM, AlgorithmType.HMM, AlgorithmType.VOTING};
        }

        final DateTime startTimeLocalUTC = targetDate.withTimeAtStartOfDay().withHourOfDay(DateTimeUtil.DAY_STARTS_AT_HOUR);
        final DateTime endTimeLocalUTC = targetDate.withTimeAtStartOfDay().plusDays(1).withHourOfDay(DateTimeUtil.DAY_ENDS_AT_HOUR);
        final DateTime currentTimeUTC = DateTime.now().withZone(DateTimeZone.UTC);

        LOGGER.info("action=get_timeline date={} account_id={} start_time={} end_time={}", targetDate.toDate(),accountId,startTimeLocalUTC,endTimeLocalUTC);

        final Optional<OneDaysSensorData> sensorDataOptional = getSensorData(accountId, targetDate, startTimeLocalUTC, endTimeLocalUTC, currentTimeUTC, newFeedback);

        if (!sensorDataOptional.isPresent()) {
            LOGGER.info("account_id = {} and day = {}", accountId, startTimeLocalUTC);
            final TimelineLog log = new TimelineLog(accountId, startTimeLocalUTC.withZone(DateTimeZone.UTC).getMillis());
            log.addMessage(TimelineError.NO_DATA);
            return TimelineResult.createEmpty(log, English.TIMELINE_NO_SLEEP_DATA, DataCompleteness.NO_DATA);
        }

        final OneDaysSensorData sensorData = sensorDataOptional.get();

        //get test group of user, will be default if no entries exist in database
        final Long testGroup = getTestGroup(accountId, startTimeLocalUTC, sensorData.timezoneOffsetMillis);

        //create log with test grup
        final TimelineLog log = new TimelineLog(accountId, startTimeLocalUTC.withZone(DateTimeZone.UTC).getMillis(), DateTime.now(DateTimeZone.UTC).getMillis(), testGroup);


        //check to see if there's an issue with the data
        final TimelineError discardReason = isValidNight(accountId, sensorData.originalTrackerMotions, sensorData.trackerMotions);

        if (!discardReason.equals(TimelineError.NO_ERROR)) {
            LOGGER.info("action=discard_timeline reason={} account_id={} date={}", discardReason,accountId, targetDate.toDate());
        }

        switch (discardReason) {
            case TIMESPAN_TOO_SHORT:
                log.addMessage(discardReason);
                return TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, DataCompleteness.NOT_ENOUGH_DATA);

            case NOT_ENOUGH_DATA:
                log.addMessage(discardReason);
                return TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, DataCompleteness.NOT_ENOUGH_DATA);

            case NO_DATA:
                log.addMessage(discardReason);
                return TimelineResult.createEmpty(log, English.TIMELINE_NO_SLEEP_DATA, DataCompleteness.NO_DATA);

            case LOW_AMP_DATA:
                log.addMessage(discardReason);
                return TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, DataCompleteness.NOT_ENOUGH_DATA);

            case PARTNER_FILTER_REJECTED_DATA:
                log.addMessage(discardReason);
                return TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, DataCompleteness.NOT_ENOUGH_DATA);

            default:
                break;
        }


        /*  GET THE TIMELINE! */
        Optional<TimelineAlgorithmResult> resultOptional = Optional.absent();

        for (final AlgorithmType alg : algorithmChain) {
            final Optional<TimelineAlgorithm> timelineAlgorithm = algorithmFactory.get(alg);

            if (!timelineAlgorithm.isPresent()) {
                //assume error reporting is happening in alg, no need to report it here
                continue;
            }

            resultOptional = timelineAlgorithm.get().getTimelinePrediction(sensorData, log, accountId, feedbackChanged);

            //got a valid result? poof, we're out.
            if (resultOptional.isPresent()) {
                break;
            }
        }


        //did events get produced, and did one of the algorithms work?  If not, poof, we are done.
        if (!resultOptional.isPresent()) {
            LOGGER.info("action=discard_timeline reason={} account_id={} date={}", "no-successful-algorithms",accountId, targetDate.toDate());
            log.addMessage(AlgorithmType.NONE,TimelineError.UNEXEPECTED,"no successful algorithms");
            return TimelineResult.createEmpty(log);
        }

        final TimelineAlgorithmResult result = resultOptional.get();
        List<Event> extraEvents = result.extraEvents;

            /* FEATURE FLIP EXTRA EVENTS */
        if (!this.hasExtraEventsEnabled(accountId)) {
            extraEvents = Collections.EMPTY_LIST;
        }

        final PopulatedTimelines populateTimelines = populateTimeline(accountId,targetDate,startTimeLocalUTC,endTimeLocalUTC, result, sensorData);


        if (!populateTimelines.isValidSleepScore) {
            log.addMessage(TimelineError.INVALID_SLEEP_SCORE);
            LOGGER.warn("invalid sleep score");
            return TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, DataCompleteness.NOT_ENOUGH_DATA);
        }

        return TimelineResult.create(populateTimelines.timelines, log);

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








    protected Optional<OneDaysSensorData> getSensorData(final long accountId,final DateTime date, final DateTime starteTimeLocalUTC, final DateTime endTimeLocalUTC, final DateTime currentTimeUTC,final Optional<TimelineFeedback> newFeedback) {
        final List<TrackerMotion> originalTrackerMotions = pillDataDAODynamoDB.getBetweenLocalUTC(accountId, starteTimeLocalUTC, endTimeLocalUTC);

        if (originalTrackerMotions.isEmpty()) {
            LOGGER.warn("No original tracker motion data for account {} on {}, returning optional absent", accountId, starteTimeLocalUTC);
            return Optional.absent();
        }

        LOGGER.debug("Length of originalTrackerMotion is {} for {} on {}", originalTrackerMotions.size(), accountId, starteTimeLocalUTC);

        // get partner tracker motion, if available
        final List<TrackerMotion> originalPartnerMotions = getPartnerTrackerMotion(accountId, starteTimeLocalUTC, endTimeLocalUTC);


        List<TrackerMotion> filteredOriginalMotions = originalTrackerMotions;
        List<TrackerMotion> filteredOriginalPartnerMotions = originalPartnerMotions;

        if (this.hasOutlierFilterEnabled(accountId)) {
            filteredOriginalMotions = OutlierFilter.removeOutliers(originalTrackerMotions,OUTLIER_GUARD_DURATION,DOMINANT_GROUP_DURATION);
            filteredOriginalPartnerMotions = OutlierFilter.removeOutliers(originalPartnerMotions,OUTLIER_GUARD_DURATION,DOMINANT_GROUP_DURATION);
        }


        final List<TrackerMotion> trackerMotions = Lists.newArrayList();

        if (!filteredOriginalPartnerMotions.isEmpty()) {

            final int tzOffsetMillis = originalTrackerMotions.get(0).offsetMillis;

            if (this.hasPartnerFilterEnabled(accountId)) {
                LOGGER.info("using original partner filter");
                try {
                    PartnerDataUtils.PartnerMotions motions = partnerDataUtils.getMyMotion(filteredOriginalMotions, filteredOriginalPartnerMotions);
                    trackerMotions.addAll(motions.myMotions);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                    trackerMotions.addAll(filteredOriginalMotions);
                }
            }
            else if (this.hasHmmPartnerFilterEnabled(accountId)) {
                LOGGER.info("using hmm partner filter");
                try {
                    trackerMotions.addAll(
                            partnerDataUtils.partnerFilterWithDurationsDiffHmm(
                                    starteTimeLocalUTC.minusMillis(tzOffsetMillis),
                                    endTimeLocalUTC.minusMillis(tzOffsetMillis),
                                    ImmutableList.copyOf(filteredOriginalMotions),
                                    ImmutableList.copyOf(filteredOriginalPartnerMotions)));

                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                    trackerMotions.addAll(filteredOriginalMotions);
                }
            }
            else {
                trackerMotions.addAll(filteredOriginalMotions);
            }
        }
        else {
            trackerMotions.addAll(filteredOriginalMotions);
        }

        if (trackerMotions.isEmpty()) {
            LOGGER.debug("No tracker motion data ID for account_id = {} and day = {}", accountId, starteTimeLocalUTC);
            return Optional.absent();
        }


        final int tzOffsetMillis = trackerMotions.get(0).offsetMillis;

        // get all sensor data, used for light and sound disturbances, and presleep-insights

        final Optional<DeviceAccountPair> deviceIdPair = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        Optional<DateTime> wakeUpWaveTimeOptional = Optional.absent();

        if (!deviceIdPair.isPresent()) {
            LOGGER.debug("No device ID for account_id = {} and day = {}", accountId, starteTimeLocalUTC);
            return Optional.absent();
        }

        final String externalDeviceId = deviceIdPair.get().externalDeviceId;
        final Long deviceId = deviceIdPair.get().internalDeviceId;

        // get color of sense, yes this matters for the light sensor
        final Optional<Device.Color> optionalColor = senseColorDAO.getColorForSense(externalDeviceId);

        final Optional<Calibration> calibrationOptional = this.hasCalibrationEnabled(externalDeviceId) ? calibrationDAO.getStrict(externalDeviceId) : Optional.<Calibration>absent();

        // query dates in utc_ts (table has an index for this)
        LOGGER.debug("Query all sensors with utc ts for account {}", accountId);

        final AllSensorSampleList allSensorSampleList = deviceDataDAODynamoDB.generateTimeSeriesByUTCTimeAllSensors(
                starteTimeLocalUTC.minusMillis(tzOffsetMillis).getMillis(),
                endTimeLocalUTC.minusMillis(tzOffsetMillis).getMillis(),
                accountId, externalDeviceId, SLOT_DURATION_MINUTES, missingDataDefaultValue(accountId),optionalColor, calibrationOptional
        );
        LOGGER.info("Sensor data for timeline generated by DynamoDB for account {}", accountId);

        if (allSensorSampleList.isEmpty()) {
            LOGGER.debug("No sense sensor data ID for account_id = {} and day = {}", accountId, starteTimeLocalUTC);
            return Optional.absent();
        }

        final List<TimelineFeedback> feedbackList = Lists.newArrayList(getFeedbackList(accountId, starteTimeLocalUTC, tzOffsetMillis));

        if (newFeedback.isPresent()) {
            feedbackList.add(newFeedback.get());
        }

        return Optional.of(new OneDaysSensorData(allSensorSampleList,
                ImmutableList.copyOf(trackerMotions),ImmutableList.copyOf(filteredOriginalPartnerMotions),
                ImmutableList.copyOf(feedbackList),
                ImmutableList.copyOf(filteredOriginalMotions),ImmutableList.copyOf(filteredOriginalPartnerMotions),
                date,starteTimeLocalUTC,endTimeLocalUTC,currentTimeUTC,
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


    public PopulatedTimelines populateTimeline(final long accountId,final DateTime date,final DateTime targetDate, final DateTime endDate, final TimelineAlgorithmResult result,
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

        //MOVE EVENTS BASED ON FEEDBACK
        FeedbackUtils.ReprocessedEvents reprocessedEvents = null;

        LOGGER.info("action=apply_feedback num_items={} account_id={} date={}", feedbackList.size(),accountId,sensorData.date.toDate());

        if (this.hasTimelineOrderEnforcement(accountId)) {
            reprocessedEvents = feedbackUtils.reprocessEventsBasedOnFeedback(feedbackList, result.mainEvents.values(), result.extraEvents, sensorData.timezoneOffsetMillis);
        }
        else {
            reprocessedEvents = feedbackUtils.reprocessEventsBasedOnFeedbackTheOldWay(feedbackList, ImmutableList.copyOf(result.mainEvents.values()), ImmutableList.copyOf(result.extraEvents), sensorData.timezoneOffsetMillis);
        }

        //GET SPECIFIC EVENTS
        final Optional<Event> inBed = Optional.fromNullable(reprocessedEvents.mainEvents.get(Event.Type.IN_BED));
        final Optional<Event> sleep = Optional.fromNullable(reprocessedEvents.mainEvents.get(Event.Type.SLEEP));
        final Optional<Event> wake = Optional.fromNullable(reprocessedEvents.mainEvents.get(Event.Type.WAKE_UP));
        final Optional<Event> outOfBed = Optional.fromNullable(reprocessedEvents.mainEvents.get(Event.Type.OUT_OF_BED));


        //CREATE SLEEP MOTION EVENTS
        final List<MotionEvent> motionEvents = timelineUtils.generateMotionEvents(trackerMotions);

        final Map<Long, Event> timelineEvents = TimelineRefactored.populateTimeline(motionEvents);

        // LIGHT
        for(final Event event : lightEvents) {
            timelineEvents.put(event.getStartTimestamp(), event);
        }

        // PARTNER MOTION
        final List<PartnerMotionEvent> partnerMotionEvents = getPartnerMotionEvents(sleep, wake, ImmutableList.copyOf(motionEvents), partnerMotions);
        for(PartnerMotionEvent partnerMotionEvent : partnerMotionEvents) {
            timelineEvents.put(partnerMotionEvent.getStartTimestamp(), partnerMotionEvent);
        }
        final int numPartnerMotion = partnerMotionEvents.size();

        // SOUND
        int numSoundEvents = 0;
        if (this.hasSoundInTimeline(accountId)) {
            final SleepEvents<Optional<Event>> sleepEventsFromAlgorithm = SleepEvents.create(inBed,sleep,wake,outOfBed);

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

        /* add main events  */
        for (final Event event : reprocessedEvents.mainEvents.values()) {
            timelineEvents.put(event.getStartTimestamp(), event);
        }

        /*  add "additional" events -- which is wake/sleep/get up to pee events */
        for (final Event event : reprocessedEvents.extraEvents) {
            timelineEvents.put(event.getStartTimestamp(), event);
        }


        /*  Benjo says: I am 100% sure that his is responsible for sorting events in proper order,
            plus some other shit that only Pang knew about.  Embrace the mystery!  */
        final List<Event> eventsWithSleepEvents = TimelineRefactored.mergeEvents(timelineEvents);

        /*  Benjo asks: what does this do? Fuuuck no comments? The fuck. */
        final List<Event> smoothedEvents = timelineUtils.smoothEvents(eventsWithSleepEvents);


        /* clean up timeline */

        // 1. remove motion & null events outside sleep/in-bed period
        List<Event> cleanedUpEvents;
        if (this.hasRemoveMotionEventsOutsideSleep(accountId)) {
            // remove motion events outside of sleep and awake
            cleanedUpEvents = timelineUtils.removeMotionEventsOutsideSleep(smoothedEvents, sleep, wake);
        } else {
            // remove motion events outside of in-bed and out-bed
            cleanedUpEvents = timelineUtils.removeMotionEventsOutsideBedPeriod(smoothedEvents, inBed, outOfBed);
        }

        // 2. Grey out events outside in-bed time
        final Boolean removeGreyOutEvents = this.hasRemoveGreyOutEvents(accountId); // rm grey events totally

        final List<Event> greyEvents = timelineUtils.greyNullEventsOutsideBedPeriod(cleanedUpEvents,
                inBed, outOfBed, removeGreyOutEvents);

        // 3. remove non-significant that are more than 1/3 of the entire night's time-span
        final List<Event> nonSignificantFilteredEvents = timelineUtils.removeEventBeforeSignificant(greyEvents);


        /* convert valid events to segment, compute sleep stats and score */

        final List<SleepSegment> sleepSegments = timelineUtils.eventsToSegments(nonSignificantFilteredEvents);

        final int lightSleepThreshold = 70; // TODO: Generate dynamically instead of hard threshold
        final SleepStats sleepStats = timelineUtils.computeStats(sleepSegments, lightSleepThreshold, hasSleepStatMediumSleep(accountId));
        final List<SleepSegment> reversed = Lists.reverse(sleepSegments);


        Integer sleepScore = computeAndMaybeSaveScore(trackerMotions, numSoundEvents, allSensorSampleList, targetDate, accountId, sleepStats);

        //if there is no feedback, we have a "natural" timeline
        //check if this natural timeline makes sense.  If not, set sleep score to zero.
        if (feedbackList.isEmpty() && sleepStats.sleepDurationInMinutes < TimelineSafeguards.MINIMUM_SLEEP_DURATION_MINUTES) {
            LOGGER.warn("action=zeroing-score account_id={} reason=sleep-duration-too-short sleep_duration={}", accountId, sleepStats.sleepDurationInMinutes);
            sleepScore = 0;
        }


        boolean isValidSleepScore = sleepScore > 0;

        //if there's feedback, sleep score can never be invalid
        if (!feedbackList.isEmpty()) {
            isValidSleepScore = true;
        }

        final String timeLineMessage = timelineUtils.generateMessage(sleepStats, numPartnerMotion, numSoundEvents);

        LOGGER.info("action=compute_sleep_score score={} account_id={}", sleepScore,accountId);


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
            return pillDataDAODynamoDB.getBetweenLocalUTC(partnerAccountId, startTime, endTime);
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

        final Integer timesAwakePenalty;
        if (this.hasTimesAwakeSleepScorePenalty(accountId)) {
            timesAwakePenalty = SleepScoreUtils.calculateTimesAwakePenaltyScore(sleepStats.numberOfMotionEvents);
        } else {
            timesAwakePenalty = 0;
        }

        // Calculate the sleep score based on the sub scores and weighting
        final SleepScore sleepScore = new SleepScore.Builder()
                .withMotionScore(motionScore)
                .withSleepDurationScore(durationScore)
                .withEnvironmentalScore(environmentScore)
                .withWeighting(sleepScoreWeighting(accountId))
                .withTimesAwakePenaltyScore(timesAwakePenalty)
                .build();

        // Always update stats and scores to Dynamo
        final Integer userOffsetMillis = trackerMotions.get(0).offsetMillis;
        final Boolean updatedStats = this.sleepStatsDAODynamoDB.updateStat(accountId,
                targetDate.withTimeAtStartOfDay(), sleepScore.value, sleepScore, sleepStats, userOffsetMillis);

        LOGGER.debug("Updated Stats-score: status {}, account {}, score {}, stats {}",
                updatedStats, accountId, sleepScore, sleepStats);

        return sleepScore.value;
    }

    private Integer computeSleepDurationScore(final Long accountId, final SleepStats sleepStats) {
        final Optional<Account> optionalAccount = accountDAO.getById(accountId);
        final int userAge = (optionalAccount.isPresent()) ? DateTimeUtil.getDateDiffFromNowInDays(optionalAccount.get().DOB) / 365 : 0;

        if (hasSleepScoreDurationV2(accountId)) {
            return SleepScoreUtils.getSleepDurationScoreV2(userAge, sleepStats.sleepDurationInMinutes);
        }

        return SleepScoreUtils.getSleepDurationScore(userAge, sleepStats.sleepDurationInMinutes);
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
        } else if (hasSleepScoreDurationWeightingV2(accountId)) {
            weighting = new SleepScore.DurationHeavyWeightingV2();
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
        return feedbackDAO.getCorrectedForNight(accountId, nightOfUTC);
    }

}
