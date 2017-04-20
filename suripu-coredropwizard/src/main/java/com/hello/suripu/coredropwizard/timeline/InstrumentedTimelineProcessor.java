package com.hello.suripu.coredropwizard.timeline;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.algorithmintegration.AlgorithmConfiguration;
import com.hello.suripu.core.algorithmintegration.AlgorithmFactory;
import com.hello.suripu.core.algorithmintegration.NeuralNetEndpoint;
import com.hello.suripu.core.algorithmintegration.OneDaysSensorData;
import com.hello.suripu.core.algorithmintegration.OneDaysTrackerMotion;
import com.hello.suripu.core.algorithmintegration.TimelineAlgorithm;
import com.hello.suripu.core.algorithmintegration.TimelineAlgorithmResult;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.DefaultModelEnsembleDAO;
import com.hello.suripu.core.db.DeviceDataReadAllSensorsDAO;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.FeedbackReadDAO;
import com.hello.suripu.core.db.MainEventTimesDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.db.PillDataReadDAO;
import com.hello.suripu.core.db.RingTimeHistoryReadDAO;
import com.hello.suripu.core.db.SenseDataDAO;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.db.SleepScoreParametersDAO;
import com.hello.suripu.core.db.SleepStatsDAO;
import com.hello.suripu.core.db.TimeZoneHistoryDAO;
import com.hello.suripu.core.db.UserTimelineTestGroupDAO;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AgitatedSleep;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.DataCompleteness;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.PartnerMotionEvent;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.MainEventTimes;
import com.hello.suripu.core.models.MotionFrequency;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepScore;
import com.hello.suripu.core.models.SleepScoreParameters;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.UserBioInfo;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import com.hello.suripu.core.processors.FeatureFlippedProcessor;
import com.hello.suripu.core.processors.PartnerMotion;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.FeedbackUtils;
import com.hello.suripu.core.util.InBedSearcher;
import com.hello.suripu.core.util.OutlierFilter;
import com.hello.suripu.core.util.PartnerDataUtils;
import com.hello.suripu.core.util.SleepScoreUtils;
import com.hello.suripu.core.util.TimeZoneOffsetMap;
import com.hello.suripu.core.util.TimelineError;
import com.hello.suripu.core.util.TimelineLockdown;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.codahale.metrics.MetricRegistry.name;


public class InstrumentedTimelineProcessor extends FeatureFlippedProcessor {

    public static final String VERSION = "0.0.2";
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(InstrumentedTimelineProcessor.class);
    private final PillDataReadDAO pillDataDAODynamoDB;
    private final DeviceReadDAO deviceDAO;
    private final DeviceDataReadAllSensorsDAO deviceDataDAODynamoDB;
    private final RingTimeHistoryReadDAO ringTimeHistoryDAODynamoDB;
    private final FeedbackReadDAO feedbackDAO;
    private final SleepHmmDAO sleepHmmDAO;
    private final AccountReadDAO accountDAO;
    private final SleepStatsDAO sleepStatsDAODynamoDB;
    private final MainEventTimesDAO mainEventTimesDAO;
    private final TimeZoneHistoryDAO timeZoneHistoryDAO;
    private final Logger LOGGER;
    private final TimelineUtils timelineUtils;
    private final TimelineSafeguards timelineSafeguards;
    private final FeedbackUtils feedbackUtils;
    private final PartnerDataUtils partnerDataUtils;

    private final UserTimelineTestGroupDAO userTimelineTestGroupDAO;
    private final SleepScoreParametersDAO sleepScoreParametersDAO;

    private final SenseDataDAO senseDataDAO;

    private final AlgorithmFactory algorithmFactory;

    protected Histogram scoreDiff;

    public final static int MIN_TRACKER_MOTION_COUNT = 20;
    public final static int MIN_TRACKER_MOTION_COUNT_LOWER_THRESHOLD = 9;
    public final static float MIN_FRACTION_UNIQUE_MOTION = 0.5f;
    public final static int MIN_PARTNER_FILTERED_MOTION_COUNT = 5;
    public final static int MIN_DURATION_OF_TRACKER_MOTION_IN_HOURS = 5;
    public final static int MIN_DURATION_OF_FILTERED_MOTION_IN_HOURS = 3;
    public final static int MIN_MOTION_AMPLITUDE_LOW = 500;
    public final static int MIN_MOTION_AMPLITUDE = 1000;
    public final static int TIMEZONE_HISTORY_LIMIT = 5;
    final static long OUTLIER_GUARD_DURATION = (long)(DateTimeConstants.MILLIS_PER_HOUR * 2.0); //min spacing between motion groups
    final static long DOMINANT_GROUP_DURATION = (long)(DateTimeConstants.MILLIS_PER_HOUR * 6.0); //num hours in a motion group to be considered the dominant one


    static public InstrumentedTimelineProcessor createTimelineProcessor(final PillDataReadDAO pillDataDAODynamoDB,
                                                                        final DeviceReadDAO deviceDAO,
                                                                        final DeviceDataReadAllSensorsDAO deviceDataDAODynamoDB,
                                                                        final RingTimeHistoryReadDAO ringTimeHistoryDAODynamoDB,
                                                                        final FeedbackReadDAO feedbackDAO,
                                                                        final SleepHmmDAO sleepHmmDAO,
                                                                        final AccountReadDAO accountDAO,
                                                                        final SleepStatsDAO sleepStatsDAODynamoDB,
                                                                        final MainEventTimesDAO mainEventTimesDAO,
                                                                        final SenseDataDAO senseDataDAO,
                                                                        final TimeZoneHistoryDAO timeZoneHistoryDAO,
                                                                        final OnlineHmmModelsDAO priorsDAO,
                                                                        final FeatureExtractionModelsDAO featureExtractionModelsDAO,
                                                                        final DefaultModelEnsembleDAO defaultModelEnsembleDAO,
                                                                        final UserTimelineTestGroupDAO userTimelineTestGroupDAO,
                                                                        final SleepScoreParametersDAO sleepScoreParametersDAO,
                                                                        final Map<AlgorithmType,NeuralNetEndpoint> neuralNetEndpoints,
                                                                        final AlgorithmConfiguration algorithmConfiguration,
                                                                        final MetricRegistry metrics) {
        final LoggerWithSessionId logger = new LoggerWithSessionId(STATIC_LOGGER);

        final AlgorithmFactory algorithmFactory = AlgorithmFactory.create(sleepHmmDAO,priorsDAO,defaultModelEnsembleDAO,featureExtractionModelsDAO,neuralNetEndpoints,algorithmConfiguration,Optional.<UUID>absent());

        final Histogram scoreDiff = metrics.histogram(name(InstrumentedTimelineProcessor.class, "sleep-score-diff"));

        return new InstrumentedTimelineProcessor(pillDataDAODynamoDB,
                deviceDAO,deviceDataDAODynamoDB,ringTimeHistoryDAODynamoDB,
                feedbackDAO,sleepHmmDAO,accountDAO,sleepStatsDAODynamoDB,
                mainEventTimesDAO,
                senseDataDAO,
                timeZoneHistoryDAO,
                Optional.<UUID>absent(),
                userTimelineTestGroupDAO,
                sleepScoreParametersDAO,
                algorithmFactory,
                scoreDiff);
    }

    public InstrumentedTimelineProcessor copyMeWithNewUUID(final UUID uuid) {

        return new InstrumentedTimelineProcessor(pillDataDAODynamoDB, deviceDAO,deviceDataDAODynamoDB,ringTimeHistoryDAODynamoDB,feedbackDAO,sleepHmmDAO,accountDAO,sleepStatsDAODynamoDB, mainEventTimesDAO,senseDataDAO, timeZoneHistoryDAO, Optional.of(uuid),userTimelineTestGroupDAO,sleepScoreParametersDAO,algorithmFactory.cloneWithNewUUID(Optional.of(uuid)), scoreDiff);
    }

    //private SessionLogDebug(final String)

    private InstrumentedTimelineProcessor(final PillDataReadDAO pillDataDAODynamoDB,
                                          final DeviceReadDAO deviceDAO,
                                          final DeviceDataReadAllSensorsDAO deviceDataDAODynamoDB,
                                          final RingTimeHistoryReadDAO ringTimeHistoryDAODynamoDB,
                                          final FeedbackReadDAO feedbackDAO,
                                          final SleepHmmDAO sleepHmmDAO,
                                          final AccountReadDAO accountDAO,
                                          final SleepStatsDAO sleepStatsDAODynamoDB,
                                          final MainEventTimesDAO mainEventTimesDAO,
                                          final SenseDataDAO senseDataDAO,
                                          final TimeZoneHistoryDAO timeZoneHistoryDAO,
                                          final Optional<UUID> uuid,
                                          final UserTimelineTestGroupDAO userTimelineTestGroupDAO,
                                          final SleepScoreParametersDAO sleepScoreParametersDAO,
                                          final AlgorithmFactory algorithmFactory,
                                          final Histogram scoreDiff) {
        this.pillDataDAODynamoDB = pillDataDAODynamoDB;
        this.deviceDAO = deviceDAO;
        this.deviceDataDAODynamoDB = deviceDataDAODynamoDB;
        this.ringTimeHistoryDAODynamoDB = ringTimeHistoryDAODynamoDB;
        this.feedbackDAO = feedbackDAO;
        this.sleepHmmDAO = sleepHmmDAO;
        this.accountDAO = accountDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.mainEventTimesDAO = mainEventTimesDAO;
        this.senseDataDAO = senseDataDAO;
        this.timeZoneHistoryDAO = timeZoneHistoryDAO;
        this.userTimelineTestGroupDAO = userTimelineTestGroupDAO;
        this.sleepScoreParametersDAO = sleepScoreParametersDAO;
        this.algorithmFactory = algorithmFactory;
        timelineUtils = new TimelineUtils(uuid);
        timelineSafeguards = new TimelineSafeguards(uuid);
        feedbackUtils = new FeedbackUtils(uuid);
        partnerDataUtils = new PartnerDataUtils(uuid);
        this.LOGGER = new LoggerWithSessionId(STATIC_LOGGER, uuid);
        this.scoreDiff = scoreDiff;
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

    private TimelineAlgorithmResult refineInBedTime(final DateTime startTimeLocalUTC, final DateTime endTimeLocalUtc, final long accountId,final OneDaysSensorData sensorData, final TimelineAlgorithmResult origResult, final TimeZoneOffsetMap timeZoneOffsetMap) {

        //return original if not enabled
        if (!this.hasInBedSearchEnabled(accountId)) {
            return origResult;
        }

        //doesn't play nicely with extra events
        if (this.hasExtraEventsEnabled(accountId)) {
            return origResult;
        }

        //only works on ONLINE_HMM
        if (!origResult.algorithmType.equals(AlgorithmType.ONLINE_HMM)) {
            return origResult;
        }

        //make sure events I care about are actually present
        if (!origResult.mainEvents.containsKey(Event.Type.SLEEP)) {
            return origResult;
        }

        if (!origResult.mainEvents.containsKey(Event.Type.IN_BED)) {
            return origResult;
        }

        final DateTime startTimeUTC = startTimeLocalUTC.minusMillis(sensorData.timezoneOffsetMillis);
        final DateTime endTimeUTC = endTimeLocalUtc.minusMillis(sensorData.timezoneOffsetMillis);

        final Event sleepEvent = origResult.mainEvents.get(Event.Type.SLEEP);

        final Event inBedEvent = InBedSearcher.getInBedPlausiblyBeforeSleep(
                startTimeUTC,endTimeUTC,
                sleepEvent,
                origResult.mainEvents.get(Event.Type.IN_BED),
                15, //15 minutes to fall asleep minimum
                sensorData.oneDaysTrackerMotion.processedtrackerMotions);

        //replace original in-bed event with new event
        final List<Event> origEvents = origResult.mainEvents.values().asList();

        final List<Event> newEvents = Lists.newArrayList();
        newEvents.add(timeZoneOffsetMap.getEventWithCorrectOffset(inBedEvent));

        for (final Event event : origEvents) {
            if (event.getType().equals(Event.Type.IN_BED)) {
                continue;
            }

            newEvents.add(timeZoneOffsetMap.getEventWithCorrectOffset(event));
        }

        //sanity check
        if (sleepEvent.getStartTimestamp() < inBedEvent.getStartTimestamp()) {
            return origResult;
        }

        if (startTimeUTC.getMillis() > inBedEvent.getStartTimestamp()) {
            return origResult;
        }

        return new TimelineAlgorithmResult(origResult.algorithmType,newEvents,origResult.extraEvents.asList(), origResult.timelineLockedDown);

    }

    public TimelineResult retrieveTimelinesFast(final Long accountId, final DateTime queryDate, final Optional<TimelineFeedback> newFeedback) {
        return retrieveTimelinesFast(accountId, queryDate, Optional.absent(), newFeedback);
    }

    public TimelineResult retrieveTimelinesFast(final Long accountId, final DateTime queryDate, final Optional<Integer> queryHourOptional, final Optional<TimelineFeedback> newFeedback) {

        final boolean feedbackChanged = newFeedback.isPresent() && this.hasOnlineHmmLearningEnabled(accountId);

        //chain of fail-overs of algorithm (i.e)
        final LinkedList<AlgorithmType> algorithmChain = Lists.newLinkedList();
        algorithmChain.add(AlgorithmType.HMM);
        algorithmChain.add(AlgorithmType.VOTING);

        if (this.hasOnlineHmmAlgorithmEnabled(accountId)) {
            algorithmChain.addFirst(AlgorithmType.ONLINE_HMM);
        }

        //only use the newest NN
        //if (this.hasNeuralNetFourEventsAlgorithmEnabled(accountId)) {
            algorithmChain.addFirst(AlgorithmType.NEURAL_NET_FOUR_EVENT);
        //}
        final TimeZoneOffsetMap timeZoneOffsetMap = TimeZoneOffsetMap.createFromTimezoneHistoryList(timeZoneHistoryDAO.getMostRecentTimeZoneHistory(accountId, queryDate.plusHours(44), TIMEZONE_HISTORY_LIMIT)); //END time UTC - add 12 hours to ensure entire night is within query window
        final DateTime currentTimeLocal = timeZoneOffsetMap.getCurrentLocalDateTimeWithUTCDefault();
        final DateTime targetDate = timelineUtils.getTargetDate(false, queryDate, currentTimeLocal, queryHourOptional, timeZoneOffsetMap);


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
        final boolean userLowerMotionCountThreshold = useNoMotionEnforcement(accountId);
        final boolean useHigherMotionAmplitudeThreshold = useHigherMotionAmplitudeThreshold(accountId);

        final TimelineError discardReason = isValidNight(accountId, sensorData.oneDaysTrackerMotion.filteredOriginalTrackerMotions, sensorData.oneDaysTrackerMotion.processedtrackerMotions, sensorData.oneDaysPartnerMotion.filteredOriginalTrackerMotions, userLowerMotionCountThreshold, useHigherMotionAmplitudeThreshold);

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


        final Optional<MainEventTimes> computedMainEventTimesOptional = mainEventTimesDAO.getEventTimesForSleepPeriod(accountId,targetDate, SleepPeriod.Period.NIGHT);

        /*check if previously generated timeline is valid for lockdown - if valid for lockdown, uses cached main event times instead of running timeline algorithm*/

        final boolean timelineLockedDown = TimelineLockdown.isLockedDown(computedMainEventTimesOptional, sensorData.oneDaysTrackerMotion.processedtrackerMotions, useTimelineLockdown(accountId));
        Optional<TimelineAlgorithmResult> resultOptional = Optional.absent();

        /* DEFAULT VALUE IS CACHED TIMELINE MAIN EVENTS */
        if (computedMainEventTimesOptional.isPresent()) {
            resultOptional = Optional.of(new TimelineAlgorithmResult(computedMainEventTimesOptional.get().algorithmType, computedMainEventTimesOptional.get().getMainEvents(), timelineLockedDown));
            log.addMessage(computedMainEventTimesOptional.get().algorithmType, computedMainEventTimesOptional.get().timelineError);
        }

        if(!timelineLockedDown) {

            /*  GET THE TIMELINE! */
            final Set<String> featureFlips = getTimelineFeatureFlips(accountId);

            for (final AlgorithmType alg : algorithmChain) {
                LOGGER.info("action=try_algorithm algorithm_type={}", alg);
                final Optional<TimelineAlgorithm> timelineAlgorithm = algorithmFactory.get(alg);

                if (!timelineAlgorithm.isPresent()) {
                    //assume error reporting is happening in alg, no need to report it here
                    continue;
                }

                resultOptional = timelineAlgorithm.get().getTimelinePrediction(sensorData,SleepPeriod.night(targetDate), log, accountId, feedbackChanged, featureFlips);

                //got a valid result? poof, we're out.
                if (resultOptional.isPresent()) {
                    break;
                }
            }

            //did events get produced, and did one of the algorithms work?  If not, poof, we are done.
            if (!resultOptional.isPresent()) {
                LOGGER.info("action=discard_timeline reason={} account_id={} date={}", "no-successful-algorithms", accountId, targetDate.toDate());
                log.addMessage(AlgorithmType.NONE, TimelineError.UNEXEPECTED, "no successful algorithms");
                return TimelineResult.createEmpty(log);
            }


            //save to main event times
            final MainEventTimes mainEventTimes = MainEventTimes.createMainEventTimes(accountId, SleepPeriod.night(targetDate), DateTime.now(DateTimeZone.UTC).getMillis(), timeZoneOffsetMap.getOffsetWithDefaultAsZero(DateTime.now(DateTimeZone.UTC).getMillis()), resultOptional.get(), TimelineError.NO_ERROR);
            mainEventTimesDAO.updateEventTimes(mainEventTimes);
        }

        //if we get here we will have a timeline
        final PopulatedTimelines populateTimelines = populateTimeline(accountId,targetDate,startTimeLocalUTC,endTimeLocalUTC,timeZoneOffsetMap, resultOptional.get(), sensorData);

        //if the timeline (too short, but got generated somehow) has an invalid sleep score
        //then we don't want to give out that timeline
        if (!populateTimelines.isValidSleepScore) {
            log.addMessage(TimelineError.INVALID_SLEEP_SCORE);
            LOGGER.warn("invalid sleep score");
            return TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, DataCompleteness.NOT_ENOUGH_DATA);
        }

        return TimelineResult.create(populateTimelines.timelines, log);

    }

    private Set<String> getTimelineFeatureFlips(final long accountId) {
        final Set<String> featureFlips = Sets.newHashSet();
        if (hasOffBedFilterEnabled(accountId)) {
            featureFlips.add(FeatureFlipper.OFF_BED_HMM_MOTION_FILTER);
        }
        return featureFlips;
    }


    private List<Event> getAlarmEvents(final Long accountId, final DateTime startQueryTime, final DateTime endQueryTime, final TimeZoneOffsetMap timeZoneOffsetMap) {

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

        return timelineUtils.getAlarmEvents(ringTimes, startQueryTime, endQueryTime, timeZoneOffsetMap, DateTime.now(DateTimeZone.UTC));
    }

    protected TimelineAlgorithmResult remapEventOffset(final TimelineAlgorithmResult result, final TimeZoneOffsetMap timeZoneOffsetMap){


        return result;
    }

    protected ImmutableList<TrackerMotion> filterPillPairingMotions(final ImmutableList<TrackerMotion> motions, final long accountId) {
        final List<DateTime> pairTimes =  Lists.newArrayList();
        final ImmutableList<DeviceAccountPair> pills = deviceDAO.getPillsForAccountId(accountId); //get pills


        for (final DeviceAccountPair pill : pills) {
            pairTimes.add(pill.created);
        }

        LOGGER.info("action=try_filter_pairing_motion account_id={} num_pairing_times={}",accountId,pairTimes.size());

        return timelineUtils.filterPillPairingMotionsWithTimes(motions,pairTimes);
    }




    protected Optional<OneDaysSensorData> getSensorData(final long accountId,final DateTime date, final DateTime starteTimeLocalUTC, final DateTime endTimeLocalUTC, final DateTime currentTimeUTC,final Optional<TimelineFeedback> newFeedback) {


        ImmutableList<TrackerMotion> originalTrackerMotions = pillDataDAODynamoDB.getBetweenLocalUTC(accountId, starteTimeLocalUTC, endTimeLocalUTC);

        if (originalTrackerMotions.isEmpty()) {
            LOGGER.warn("No original tracker motion data for account {} on {}, returning optional absent", accountId, starteTimeLocalUTC);
            return Optional.absent();
        }

        LOGGER.debug("Length of originalTrackerMotion is {} for {} on {}", originalTrackerMotions.size(), accountId, starteTimeLocalUTC);

        // get partner tracker motion, if available
        ImmutableList<TrackerMotion> originalPartnerMotions = getPartnerTrackerMotion(accountId, starteTimeLocalUTC, endTimeLocalUTC);

        //filter pairing motions for a good first night's experience
        if (this.hasRemovePairingMotions(accountId)) {
            //my motions
            originalTrackerMotions = filterPillPairingMotions(originalTrackerMotions,accountId);

            //my partner's motions
            if (!originalPartnerMotions.isEmpty()) {
                final long partnerAccountId = originalPartnerMotions.get(0).accountId;
                originalPartnerMotions = filterPillPairingMotions(originalPartnerMotions, partnerAccountId);
            }

            //lets check this again
            if (originalTrackerMotions.isEmpty()) {
                LOGGER.warn("msg=no-original-tracker-motion-data-after-pairing-filter account_id={} datetime={}", accountId, starteTimeLocalUTC);
                return Optional.absent();
            }
        }

        final Optional<Account> accountOptional = accountDAO.getById(accountId);
        final UserBioInfo userBioInfo = UserBioInfo.forNightSleeper(accountOptional, !originalPartnerMotions.isEmpty());


        List<TrackerMotion> filteredOriginalMotions = originalTrackerMotions;
        List<TrackerMotion> filteredOriginalPartnerMotions = originalPartnerMotions;

        //removes motion events less than 2 seconds and < 300 val. groups the remaining motions into groups separated by 2 hours. If largest motion groups is greater than 6 hours hours, drop all motions afterward this motion group.
        if (this.hasOutlierFilterEnabled(accountId)) {
            filteredOriginalMotions = OutlierFilter.removeOutliers(originalTrackerMotions, OUTLIER_GUARD_DURATION, DOMINANT_GROUP_DURATION);
            filteredOriginalPartnerMotions = OutlierFilter.removeOutliers(originalPartnerMotions, OUTLIER_GUARD_DURATION, DOMINANT_GROUP_DURATION);
        }
        if (filteredOriginalMotions.isEmpty()){
            LOGGER.warn("action=outlier-filter-removed-all-motion account_id={}", accountId);
            return Optional.absent();
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

        final OneDaysTrackerMotion oneDaysTrackerMotion = new OneDaysTrackerMotion(ImmutableList.copyOf(trackerMotions), ImmutableList.copyOf(filteredOriginalMotions), originalTrackerMotions);
        final OneDaysTrackerMotion oneDaysPartnerMotion = new OneDaysTrackerMotion(ImmutableList.copyOf(filteredOriginalPartnerMotions), ImmutableList.copyOf(filteredOriginalPartnerMotions), originalPartnerMotions);


        final int tzOffsetMillis = trackerMotions.get(0).offsetMillis;
        final Optional<AllSensorSampleList> allSensorSampleList = senseDataDAO.get(accountId,date,starteTimeLocalUTC, endTimeLocalUTC, currentTimeUTC, tzOffsetMillis);
        LOGGER.info("Sensor data for timeline generated by DynamoDB for account {}", accountId);

        if (!allSensorSampleList.isPresent() || allSensorSampleList.get().isEmpty()) {
            LOGGER.debug("No sense sensor data ID for account_id = {} and day = {}", accountId, starteTimeLocalUTC);
            return Optional.absent();
        }

        final List<TimelineFeedback> feedbackList = Lists.newArrayList(getFeedbackList(accountId, starteTimeLocalUTC, tzOffsetMillis));

        if (newFeedback.isPresent()) {
            feedbackList.add(newFeedback.get());
        }

        return Optional.of(new OneDaysSensorData(allSensorSampleList.get(),oneDaysTrackerMotion, oneDaysPartnerMotion,
                ImmutableList.copyOf(feedbackList),date,starteTimeLocalUTC,endTimeLocalUTC,currentTimeUTC,
                tzOffsetMillis,userBioInfo));
    }

    private static class PopulatedTimelines {
        public final List<Timeline> timelines;
        public final boolean isValidSleepScore;

        public PopulatedTimelines(List<Timeline> timelines, boolean isValidSleepScore) {
            this.timelines = timelines;
            this.isValidSleepScore = isValidSleepScore;
        }
    }


    public PopulatedTimelines populateTimeline(final long accountId,final DateTime date,final DateTime targetDate, final DateTime endDate, final TimeZoneOffsetMap timeZoneOffsetMap, final TimelineAlgorithmResult result,
                                               final OneDaysSensorData sensorData) {

        final ImmutableList<TrackerMotion> trackerMotions = sensorData.oneDaysTrackerMotion.processedtrackerMotions;
        final AllSensorSampleList allSensorSampleList = sensorData.allSensorSampleList;
        final ImmutableList<TrackerMotion> partnerMotions = sensorData.oneDaysPartnerMotion.processedtrackerMotions;
        final ImmutableList<TimelineFeedback> feedbackList = sensorData.feedbackList;

        //MOVE EVENTS BASED ON FEEDBACK
        FeedbackUtils.ReprocessedEvents reprocessedEvents = null;

        LOGGER.info("action=apply_feedback num_items={} account_id={} date={}", feedbackList.size(),accountId,sensorData.date.toDate());
        //removed FF  TIMELINE_EVENT_ORDER_ENFORCEMENT - at 100 percent
        //TODO update place holder period with actual period
        reprocessedEvents = feedbackUtils.reprocessEventsBasedOnFeedback(SleepPeriod.Period.NIGHT, feedbackList, result.mainEvents.values(), result.extraEvents, timeZoneOffsetMap);

        //GET SPECIFIC EVENTS
        Optional<Event> inBed = Optional.fromNullable(reprocessedEvents.mainEvents.get(Event.Type.IN_BED));
        Optional<Event> sleep = Optional.fromNullable(reprocessedEvents.mainEvents.get(Event.Type.SLEEP));
        Optional<Event> wake= Optional.fromNullable(reprocessedEvents.mainEvents.get(Event.Type.WAKE_UP));
        Optional<Event> outOfBed= Optional.fromNullable(reprocessedEvents.mainEvents.get(Event.Type.OUT_OF_BED));

        //CREATE SLEEP MOTION EVENTS
        final List<MotionEvent> motionEvents = timelineUtils.generateMotionEvents(trackerMotions, SleepPeriod.Period.NIGHT);

        final Map<Long, Event> timelineEvents = TimelineRefactored.populateTimeline(motionEvents, timeZoneOffsetMap);

        Optional<Long> sleepTime = Optional.absent();
        if (sleep.isPresent()){
            sleepTime = Optional.of(sleep.get().getEndTimestamp());
        }

        // LIGHT

        // compute lights-out and sound-disturbance events
        Optional<DateTime> lightOutTimeOptional = Optional.absent();
        final List<Event> lightEvents = Lists.newArrayList();

        if (!allSensorSampleList.isEmpty()) {
            // Light
            lightEvents.addAll(timelineUtils.getLightEvents(sleepTime, allSensorSampleList.get(Sensor.LIGHT), SleepPeriod.Period.NIGHT));
            if (!lightEvents.isEmpty()){
                lightOutTimeOptional = timelineUtils.getLightsOutTime(lightEvents);
            }

        }

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
        //removed FF SOUND_EVENTS_IN_TIMELINE - at 100 percent
        final SleepEvents<Optional<Event>> sleepEventsFromAlgorithm = SleepEvents.create(inBed,sleep,wake,outOfBed);

        LOGGER.debug("action=get-sound-events account_id={} use_higher_threshold={}", accountId, this.useHigherThesholdForSoundEvents(accountId));
        final List<Event> soundEvents = getSoundEvents(
                allSensorSampleList.get(Sensor.SOUND_PEAK_ENERGY),
                motionEvents,
                lightOutTimeOptional,
                sleepEventsFromAlgorithm,
                this.useHigherThesholdForSoundEvents(accountId));

        for (final Event event : soundEvents) {
            timelineEvents.put(event.getStartTimestamp(), event);
        }
        numSoundEvents = soundEvents.size();


        // ALARM
        //removed FF ALARM_IN_TIMELINE at 100 percent
        if(trackerMotions.size() > 0) {
            final DateTimeZone timeZone = DateTimeZone.forID(timeZoneOffsetMap.getTimeZoneIdWithUTCDefault(targetDate.getMillis()));
            final DateTime alarmQueryStartTime = new DateTime(targetDate.getYear(),
                    targetDate.getMonthOfYear(),
                    targetDate.getDayOfMonth(),
                    targetDate.getHourOfDay(),
                    targetDate.getMinuteOfHour(), timeZone).minusMinutes(1);

            final DateTime alarmQueryEndTime = new DateTime(endDate.getYear(),
                    endDate.getMonthOfYear(),
                    endDate.getDayOfMonth(),
                    endDate.getHourOfDay(),
                    endDate.getMinuteOfHour(), timeZone).plusMinutes(1);

            final List<Event> alarmEvents = getAlarmEvents(accountId, alarmQueryStartTime, alarmQueryEndTime,
                    timeZoneOffsetMap);

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
        cleanedUpEvents = timelineUtils.removeMotionEventsOutsideSleep(smoothedEvents, sleep, wake);

        // 2. Grey out events outside in-bed time

        final List<Event> greyEvents = timelineUtils.greyNullEventsOutsideBedPeriod(cleanedUpEvents,
                inBed, outOfBed);

        // 3. remove non-significant that are more than 1/3 of the entire night's time-span
        final List<Event> nonSignificantFilteredEvents = timelineUtils.removeEventBeforeSignificant(greyEvents);


        /* convert valid events to segment, compute sleep stats and score */

        final List<SleepSegment> sleepSegments = timelineUtils.eventsToSegments(nonSignificantFilteredEvents);

        final int lightSleepThreshold = 70; // TODO: Generate dynamically instead of hard threshold
        final boolean useUninterruptedDuration = useUninterruptedDuration(accountId);

        final SleepStats sleepStats = timelineUtils.computeStats(sleepSegments, trackerMotions, lightSleepThreshold, hasSleepStatMediumSleep(accountId), useUninterruptedDuration);
        final List<SleepSegment> reversed = Lists.reverse(sleepSegments);

        Integer sleepScore = computeAndMaybeSaveScore(sensorData.oneDaysTrackerMotion.processedtrackerMotions, sensorData.oneDaysTrackerMotion.filteredOriginalTrackerMotions, numSoundEvents, allSensorSampleList, targetDate, accountId, sleepStats);

        //if there is no feedback, we have a "natural" timeline
        //there should be no case where the sleep duration is less than the min sleep duration at this point - already checked during timeline algorithm stage.
        if (feedbackList.isEmpty() && sleepStats.sleepDurationInMinutes < TimelineSafeguards.MINIMUM_SLEEP_DURATION_MINUTES) {
            LOGGER.warn("action=zeroing-score account_id={} reason=sleep-duration-too-short sleep_duration={}", accountId, sleepStats.sleepDurationInMinutes);
            sleepScore = 0;
        }

        boolean isValidSleepScore = sleepScore > 0;

        //if there's feedback, sleep score can never be invalid
        if (!feedbackList.isEmpty()) {
            isValidSleepScore = true;
        }

        if (!isValidSleepScore){
            //there should not be any invalid sleep scores at this point
            LOGGER.error("msg=invalid-sleep-score account_id={} date={}", accountId, targetDate);
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

        final Timeline timeline = Timeline.create(sleepScore, timeLineMessage, date.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT), reversedSegments, insights, sleepStats,result.timelineLockedDown);

        return new PopulatedTimelines(Lists.newArrayList(timeline),isValidSleepScore);
    }

    /*
     * PRELIMINARY SANITY CHECK (static and public for testing purposes)
     */
    static public TimelineError  isValidNight(final Long accountId, final List<TrackerMotion> originalMotionData, final List<TrackerMotion> filteredMotionData, final List<TrackerMotion> originalPartnerTrackerMotionData,final boolean useLowerMotionCountThreshold, final boolean useHighMotionAmplitudeThreshold){
        final int minMotionAmplitude;
        if(useHighMotionAmplitudeThreshold){
            minMotionAmplitude = MIN_MOTION_AMPLITUDE;
        } else{
            minMotionAmplitude = MIN_MOTION_AMPLITUDE_LOW;
        }

        if(originalMotionData.size() == 0){
            return TimelineError.NO_DATA;
        }

        //CHECK TO SEE IF THERE ARE "ENOUGH" MOTION EVENTS
        //If greater than MinTracker Motion Count - continue
        // if FF for lower motion count threshold, if between lower motion threshold and high motion threshold, check percent unique motions for parter data.
        // if lower than lower motion count threshold, reject
        if(originalMotionData.size() < MIN_TRACKER_MOTION_COUNT && !useLowerMotionCountThreshold ){
            return TimelineError.NOT_ENOUGH_DATA;
        } else if (originalMotionData.size() >= MIN_TRACKER_MOTION_COUNT_LOWER_THRESHOLD && originalMotionData.size() < MIN_TRACKER_MOTION_COUNT) {
            final float percentUniqueMotions = PartnerDataUtils.getPercentUniqueMovements(originalMotionData, originalPartnerTrackerMotionData);

            if (percentUniqueMotions < MIN_FRACTION_UNIQUE_MOTION){
                return TimelineError.NOT_ENOUGH_DATA;
            }
        } else if (originalMotionData.size() < MIN_TRACKER_MOTION_COUNT_LOWER_THRESHOLD){
            return TimelineError.NOT_ENOUGH_DATA;
        }

        //CHECK TO SEE IF MOTION AMPLITUDE IS EVER ABOVE MINIMUM THRESHOLD
        boolean isMotionAmplitudeAboveMinimumThreshold = false;

        for(final TrackerMotion trackerMotion : originalMotionData){
            if(trackerMotion.value > minMotionAmplitude){
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


    private ImmutableList<TrackerMotion> getPartnerTrackerMotion(final Long accountId, final DateTime startTime, final DateTime endTime) {
        final Optional<Long> optionalPartnerAccountId = this.deviceDAO.getPartnerAccountId(accountId);
        if (optionalPartnerAccountId.isPresent()) {
            final Long partnerAccountId = optionalPartnerAccountId.get();
            return pillDataDAODynamoDB.getBetweenLocalUTC(partnerAccountId, startTime, endTime);
        }
        return ImmutableList.copyOf(Collections.EMPTY_LIST);
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
            //tz offset should be correct
            final List<MotionEvent> partnerMotionEvents = timelineUtils.generateMotionEvents(partnerMotionsWithinSleepBounds, SleepPeriod.Period.NIGHT);

            return PartnerMotion.getPartnerData(partnerMotionEvents,motionEvents, 0);
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * Get sound disturbance events for timeline
     * @param soundSamples sound values
     * @param motionEvents motion values (for populating segments)
     * @param lightOutTimeOptional lightOut event time
     * @param sleepEventsFromAlgorithm sleep/wake times
     * @param usePeakEnergyThreshold use higher threshold ff
     * @return list of events
     */
    private List<Event> getSoundEvents(final List<Sample> soundSamples,
                                       final List<MotionEvent> motionEvents,
                                       final Optional<DateTime> lightOutTimeOptional,
                                       final SleepEvents<Optional<Event>> sleepEventsFromAlgorithm,
                                       final Boolean usePeakEnergyThreshold) {
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

        return timelineUtils.getSoundEvents(soundSamples, sleepDepths,
                lightOutTimeOptional, optionalSleepTime, optionalAwakeTime, SleepPeriod.Period.NIGHT, usePeakEnergyThreshold);
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
                                             final List<TrackerMotion> originalTrackerMotions,
                                             final int numberSoundEvents,
                                             final AllSensorSampleList sensors,
                                             final DateTime targetDate,
                                             final Long accountId,
                                             final SleepStats sleepStats) {

        // Movement score
        MotionScore motionScore = SleepScoreUtils.getSleepMotionScore(targetDate.withTimeAtStartOfDay(),
                trackerMotions, sleepStats.sleepTime, sleepStats.wakeTime);

        //if motion score is less than the min score - ff'd removed, at 100 percent deployement
        if (motionScore.score < (int) SleepScoreUtils.MOTION_SCORE_MIN)  {
            LOGGER.warn("action=enforced-minimum-motion-score: account_id={} night_of={}", accountId, targetDate);
            motionScore = new MotionScore(motionScore.numMotions, motionScore.motionPeriodMinutes, motionScore.avgAmplitude, motionScore.maxAmplitude, (int) SleepScoreUtils.MOTION_SCORE_MIN);
        }

        final Integer environmentScore = computeEnvironmentScore(accountId, sleepStats, numberSoundEvents, sensors);
        final long targetDateEpoch = targetDate.getMillis();
        final String targetDateStr = DateTimeUtil.dateToYmdString(targetDate);

        float sleepScoreV2V4Weighting = SleepScoreUtils.getSleepScoreV2V4Weighting(targetDateEpoch);
        float sleepScoreV4V5Weighting = SleepScoreUtils.getSleepScoreV4V5Weighting(targetDateEpoch);

        final Optional<Account> optionalAccount = accountDAO.getById(accountId);

        final int userAge = (optionalAccount.isPresent()) ? DateTimeUtil.getDateDiffFromNowInDays(optionalAccount.get().DOB) / 365 : 0;
        final SleepScoreParameters sleepScoreParameters= sleepScoreParametersDAO.getSleepScoreParametersByDate(accountId,targetDate);

        final boolean usesV4 = sleepScoreV2V4Weighting==1.0f;
        final boolean isInTransitionV2V4 = (sleepScoreV2V4Weighting > 0.0f) || useSleepScoreV4(accountId);

        final boolean usesV5 = sleepScoreV4V5Weighting==1.0f;
        final boolean isInTransitionV4V5 = (sleepScoreV4V5Weighting > 0.0f) || useSleepScoreV5(accountId);

        /*
         sleepscoreV4: Duration score penalized for motion frequency and agitated sleep
         sleepscorev5: Duration score penalized for motion frequency based on personalized threshold and agitated sleep
         sleepscorev2: Duration Score, Motion Score and Environmental score discrete,
        */

        final SleepScore sleepScoreV2 = computeSleepScoreV2(userAge, sleepStats, environmentScore, motionScore);
        final SleepScore sleepScoreV4 = computeSleepScoreV4(accountId, userAge, sleepScoreParameters.durationThreshold, sleepStats, originalTrackerMotions, environmentScore,  motionScore);
        final SleepScore sleepScoreV5 = computeSleepScoreV5(accountId, userAge, sleepScoreParameters, sleepStats, originalTrackerMotions, environmentScore,  motionScore);


        SleepScore sleepScore = sleepScoreV2;

        if (usesV5){
            //calculates sleep duration score v5 and sleep score
            sleepScore = sleepScoreV5;
            //calculates sleep score v4 and v5 linear blend score
        } else if (isInTransitionV4V5){
            //full v5 for users who are FF v5
            if (useSleepScoreV5(accountId)){
                sleepScoreV4V5Weighting = 1.0f;
            }

            sleepScore = computeSleepScoreVersionTransition(sleepScoreV4, sleepScoreV5, sleepScoreV4V5Weighting, "v4-v5");
            //creates log and histogram differences between v4 and v5
            final int sleepScoreDiff = sleepScoreV5.value - sleepScoreV4.value;
            STATIC_LOGGER.info("action=sleep-score-v4-v5-difference night_of={} account_id={} v4={} v5={} difference={}", targetDateStr, accountId, sleepScoreV4, sleepScoreV5, sleepScoreDiff);
            scoreDiff.update(sleepScoreDiff);
            //calculates sleep duration score v4
        } else if (usesV4){
            sleepScore = sleepScoreV4;
            //calculates sleep score v4 and v2 linear blend score
        } else if (isInTransitionV2V4){
            //ensures sure that users who are feature flipped get full V4 score
            if (useSleepScoreV4(accountId)){
                sleepScoreV2V4Weighting = 1.0f;
            }

            sleepScore = computeSleepScoreVersionTransition(sleepScoreV2, sleepScoreV4, sleepScoreV2V4Weighting, "v2-v4");
            //creates log and histogram differences between v2 and v4
            final int sleepScoreDiff = sleepScoreV4.value - sleepScoreV2.value;
            STATIC_LOGGER.info("action=sleep-score-v2-v4-difference night_of={} account_id={} v2={} v4={} difference={}", targetDateStr, accountId, sleepScoreV2, sleepScoreV4, sleepScoreDiff);
            scoreDiff.update(sleepScoreDiff);
        }

        // Always update stats and scores to Dynamo
        final Integer userOffsetMillis = trackerMotions.get(0).offsetMillis;
        final Boolean updatedStats = this.sleepStatsDAODynamoDB.updateStat(accountId,
                targetDate.withTimeAtStartOfDay(), sleepScore.value, sleepScore, sleepStats, userOffsetMillis);

        LOGGER.debug("action=updated-stats-score updated={} account_id={} score={}, stats={}", updatedStats, accountId, sleepScore, sleepStats);
        return sleepScore.value;
    }

    private static SleepScore computeSleepScoreV2(final int userAge, final SleepStats sleepStats, final Integer environmentScore, final MotionScore motionScore){
        final Integer timesAwakePenalty= SleepScoreUtils.calculateTimesAwakePenaltyScore(sleepStats.numberOfMotionEvents);
        final SleepScore.Weighting sleepScoreWeightingV2 = new SleepScore.DurationHeavyWeightingV2();
        final Integer durationScoreV2 = SleepScoreUtils.getSleepDurationScoreV2(userAge, sleepStats.sleepDurationInMinutes);

        final SleepScore sleepScore = new SleepScore.Builder()
                .withMotionScore(motionScore)
                .withSleepDurationScore(durationScoreV2)
                .withEnvironmentalScore(environmentScore)
                .withWeighting(sleepScoreWeightingV2)
                .withTimesAwakePenaltyScore(timesAwakePenalty)
                .withVersion("v2")
                .build();
        return sleepScore;
    }


    private static SleepScore computeSleepScoreV5(final Long accountId, final int userAge, final SleepScoreParameters sleepScoreParameters, final SleepStats sleepStats, final List<TrackerMotion> originalTrackerMotions, final Integer environmentScore, final MotionScore motionScore){
        //timesAwakePenalty accounted for in durv5 score
        final Integer timesAwakePenalty = 0;
        final SleepScore.Weighting sleepScoreWeightingV5 =  new SleepScore.DurationWeightingV5();

        // Calculate the sleep score based on the sub scores and weighting
        final float sleepDurationScoreV3 =  SleepScoreUtils.getSleepScoreDurationV3(userAge, sleepScoreParameters.durationThreshold, sleepStats.sleepDurationInMinutes);
        final AgitatedSleep agitatedSleep = SleepScoreUtils.getAgitatedSleep(originalTrackerMotions, sleepStats.sleepTime, sleepStats.wakeTime);
        final MotionFrequency motionFrequency = SleepScoreUtils.getMotionFrequency(originalTrackerMotions, sleepStats.sleepDurationInMinutes, sleepStats.sleepTime, sleepStats.wakeTime);
        final float motionFrequencyPenalty = SleepScoreUtils.getMotionFrequencyPenalty(motionFrequency, sleepScoreParameters.motionFrequencyThreshold);
        final Integer durationScoreV5 = SleepScoreUtils.getSleepScoreDurationV5(accountId, sleepDurationScoreV3, motionFrequencyPenalty, sleepStats.numberOfMotionEvents, agitatedSleep);

        final SleepScore sleepScore = new SleepScore.Builder()
                .withMotionScore(motionScore)
                .withSleepDurationScore(durationScoreV5)
                .withEnvironmentalScore(environmentScore)
                .withWeighting(sleepScoreWeightingV5)
                .withTimesAwakePenaltyScore(timesAwakePenalty)
                .withVersion("v5")
                .build();
        return sleepScore;
    }

    private static SleepScore computeSleepScoreV4(final Long accountId, final int userAge, final int sleepDurationThreshold, final SleepStats sleepStats, final List<TrackerMotion> originalTrackerMotions, final Integer environmentScore, final MotionScore motionScore){
        //timesAwakePenalty accounted for in durv4 score
        final Integer timesAwakePenalty = 0;
        final SleepScore.Weighting sleepScoreWeightingV4 =  new SleepScore.DurationWeightingV4();

        // Calculate the sleep score based on the sub scores and weighting
        final float sleepDurationScoreV3 =  SleepScoreUtils.getSleepScoreDurationV3(userAge, sleepDurationThreshold, sleepStats.sleepDurationInMinutes);
        final int agitatedSleepDuration = SleepScoreUtils.getAgitatedSleepDuration(originalTrackerMotions, sleepStats.sleepTime, sleepStats.wakeTime);
        final MotionFrequency motionFrequency = SleepScoreUtils.getMotionFrequency(originalTrackerMotions, sleepStats.sleepDurationInMinutes, sleepStats.sleepTime, sleepStats.wakeTime);
        final Integer durationScoreV4 = SleepScoreUtils.getSleepScoreDurationV4(accountId, sleepDurationScoreV3, motionFrequency, sleepStats.numberOfMotionEvents, agitatedSleepDuration);

        final SleepScore sleepScore = new SleepScore.Builder()
                .withMotionScore(motionScore)
                .withSleepDurationScore(durationScoreV4)
                .withEnvironmentalScore(environmentScore)
                .withWeighting(sleepScoreWeightingV4)
                .withTimesAwakePenaltyScore(timesAwakePenalty)
                .withVersion("v4")
                .build();
        return sleepScore;
    }

    public static SleepScore computeSleepScoreVersionTransition(final SleepScore sleepScoreOld, final SleepScore sleepScoreNew, final Float sleepScoreVersionWeighting, final String version){
        final Integer transitionScoreValue = Math.round(sleepScoreNew.value * sleepScoreVersionWeighting + sleepScoreOld.value * (1 - sleepScoreVersionWeighting));
        final SleepScore sleepScoreTransition = new SleepScore(transitionScoreValue, sleepScoreNew.motionScore, sleepScoreNew.sleepDurationScore, sleepScoreNew.environmentalScore, sleepScoreNew.timesAwakePenaltyScore,version);
        return sleepScoreTransition;
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