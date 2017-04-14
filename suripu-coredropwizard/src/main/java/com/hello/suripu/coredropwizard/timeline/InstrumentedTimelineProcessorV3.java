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
import com.hello.suripu.core.models.SleepDay;
import com.hello.suripu.core.models.SleepPeriod;
import com.hello.suripu.core.models.SleepPeriodResults;
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
import com.hello.suripu.core.util.MotionMaskPartnerFilter;
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


public class InstrumentedTimelineProcessorV3 extends FeatureFlippedProcessor {

    public static final String VERSION = "0.0.2";
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(InstrumentedTimelineProcessorV3.class);
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
    public final static int MIN_MOTION_AMPLITUDE = 500;
    public final static int TIMEZONE_HISTORY_LIMIT = 5;
    final static long OUTLIER_GUARD_DURATION = (long) (DateTimeConstants.MILLIS_PER_HOUR * 2.0); //min spacing between motion groups
    final static long DOMINANT_GROUP_DURATION_ALL_PERIODS = (long) (DateTimeConstants.MILLIS_PER_HOUR * 36.0); //num hours in a motion group for full day sensor data

    private final static int MIN_NUM_EVENTS = 4;

    static public InstrumentedTimelineProcessorV3 createTimelineProcessor(final PillDataReadDAO pillDataDAODynamoDB,
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
                                                                        final Map<AlgorithmType, NeuralNetEndpoint> neuralNetEndpoints,
                                                                        final AlgorithmConfiguration algorithmConfiguration,
                                                                        final MetricRegistry metrics) {
        final LoggerWithSessionId logger = new LoggerWithSessionId(STATIC_LOGGER);

        final AlgorithmFactory algorithmFactory = AlgorithmFactory.create(sleepHmmDAO, priorsDAO, defaultModelEnsembleDAO, featureExtractionModelsDAO, neuralNetEndpoints, algorithmConfiguration, Optional.<UUID>absent());

        final Histogram scoreDiff = metrics.histogram(name(InstrumentedTimelineProcessorV3.class, "sleep-score-diff"));

        return new InstrumentedTimelineProcessorV3(pillDataDAODynamoDB,
                deviceDAO, deviceDataDAODynamoDB, ringTimeHistoryDAODynamoDB,
                feedbackDAO, sleepHmmDAO, accountDAO, sleepStatsDAODynamoDB,
                mainEventTimesDAO,
                senseDataDAO,
                timeZoneHistoryDAO,
                Optional.<UUID>absent(),
                userTimelineTestGroupDAO,
                sleepScoreParametersDAO,
                algorithmFactory,
                scoreDiff);
    }

    public InstrumentedTimelineProcessorV3 copyMeWithNewUUID(final UUID uuid) {

        return new InstrumentedTimelineProcessorV3(pillDataDAODynamoDB, deviceDAO, deviceDataDAODynamoDB, ringTimeHistoryDAODynamoDB, feedbackDAO, sleepHmmDAO, accountDAO, sleepStatsDAODynamoDB, mainEventTimesDAO, senseDataDAO, timeZoneHistoryDAO, Optional.of(uuid), userTimelineTestGroupDAO, sleepScoreParametersDAO, algorithmFactory.cloneWithNewUUID(Optional.of(uuid)), scoreDiff);

    }

    //private SessionLogDebug(final String)

    private InstrumentedTimelineProcessorV3(final PillDataReadDAO pillDataDAODynamoDB,
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
        final Optional<Long> groupIdOptional = userTimelineTestGroupDAO.getUserGestGroup(accountId, dateOfNightUTC);

        if (groupIdOptional.isPresent()) {
            return groupIdOptional.get();
        }

        return TimelineLog.DEFAULT_TEST_GROUP;
    }

    public TimelineResult retrieveTimelinesFast(final Long accountId, final DateTime targetDate,final Optional<Integer> queryHourOptional, final Optional<TimelineFeedback> newFeedback) {

        if (useTimelineSleepPeriods(accountId)) {
            return retrieveTimelineForAllSleepPeriods(accountId, targetDate, queryHourOptional, newFeedback);
        }
        return retrieveTimelineForSingleSleepPeriod(accountId, targetDate, SleepPeriod.night(targetDate), Optional.absent(), newFeedback);

    }

    public TimelineResult retrieveTimelineForSingleSleepPeriod(final Long accountId, final DateTime targetDate, final SleepPeriod sleepPeriod, final Optional<Long> previousOutOfBedTimeOptional, final Optional<TimelineFeedback> newFeedback) {

        final TimeZoneOffsetMap timeZoneOffsetMap = TimeZoneOffsetMap.createFromTimezoneHistoryList(timeZoneHistoryDAO.getMostRecentTimeZoneHistory(accountId, targetDate.withTimeAtStartOfDay().plusHours(44), TIMEZONE_HISTORY_LIMIT)); //END time UTC - add 12 hours to ensure entire night is within query window
        final DateTime startTimeLocalUTC = sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.START, 0);
        final DateTime endTimeLocalUTC = sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.END_DATA, 0);
        final DateTime currentTimeUTC = DateTime.now().withZone(DateTimeZone.UTC);

        LOGGER.info("action=get_timeline date={} account_id={} start_time={} end_time={} time_zone={}", targetDate.toDate(), accountId, startTimeLocalUTC, endTimeLocalUTC, timeZoneOffsetMap.getTimeZoneIdWithUTCDefault(targetDate.getMillis()));

        final Optional<OneDaysSensorData> sensorDataOptional = getSensorData(accountId, startTimeLocalUTC, endTimeLocalUTC, currentTimeUTC, newFeedback);

        if (!sensorDataOptional.isPresent()) {
            LOGGER.info("account_id = {} and day = {}", accountId, startTimeLocalUTC);
            final TimelineLog log = new TimelineLog(accountId, startTimeLocalUTC.withZone(DateTimeZone.UTC).getMillis());
            log.addMessage(TimelineError.NO_DATA);

            return TimelineResult.createEmpty(log, English.TIMELINE_NO_SLEEP_DATA, DataCompleteness.NO_DATA);
        }

        final OneDaysSensorData sensorData = sensorDataOptional.get();
        //retrieve / populate timeline and save MainEventTimes
        final SleepPeriodResults sleepPeriodResults = retrievePopulateAndSaveSleepPeriodResult(accountId, targetDate, sleepPeriod, sensorData, timeZoneOffsetMap, previousOutOfBedTimeOptional, newFeedback);
        LOGGER.debug("msg=retreived-timeline-results date={} sleep_period={} success={}", targetDate, sleepPeriodResults.resultsOptional.isPresent());

        if (!sleepPeriodResults.resultsOptional.isPresent() || !sleepPeriodResults.processed) {
            LOGGER.warn("msg=invalid-timeline account_id={}  date={} sleep_period={} results_present={} processed={}", accountId, targetDate, sleepPeriod.period.shortName(), sleepPeriodResults.resultsOptional.isPresent(), sleepPeriodResults.processed);
            return TimelineResult.createEmpty(sleepPeriodResults.timelineLog, English.TIMELINE_NO_SLEEP_DATA, sleepPeriodResults.dataCompleteness);
        }

        //save sleep stats
        final Boolean updatedStats = this.sleepStatsDAODynamoDB.updateStat(accountId,targetDate.withTimeAtStartOfDay(), sleepPeriodResults.resultsOptional.get().sleepScore.value, sleepPeriodResults.resultsOptional.get().sleepScore, sleepPeriodResults.resultsOptional.get().sleepStats, sleepPeriodResults.resultsOptional.get().timeZoneOffsetMap.getOffsetWithDefaultAsZero(currentTimeUTC.getMillis()));
        LOGGER.debug("action=updated-stats-score updated={} account_id={} score={}, stats={}", updatedStats, accountId,  sleepPeriodResults.resultsOptional.get().sleepScore, sleepPeriodResults.resultsOptional.get().sleepStats);

        return TimelineResult.create(Lists.newArrayList(sleepPeriodResults.resultsOptional.get().timeline), sleepPeriodResults.timelineLog);
    }

    public TimelineResult retrieveTimelineForAllSleepPeriods(final Long accountId,final DateTime queryDate, final Optional<Integer> queryHourOptional, final Optional<TimelineFeedback> newFeedback) {

        final TimeZoneOffsetMap timeZoneOffsetMap = TimeZoneOffsetMap.createFromTimezoneHistoryList(timeZoneHistoryDAO.getMostRecentTimeZoneHistory(accountId, queryDate.plusHours(44), TIMEZONE_HISTORY_LIMIT)); //END time UTC - add 12 hours to ensure entire night is within query window
        final DateTime currentTimeLocal = timeZoneOffsetMap.getCurrentLocalDateTimeWithUTCDefault();
        final DateTime targetDate = timelineUtils.getTargetDate(isDaySleeper(accountId), queryDate, currentTimeLocal, queryHourOptional, timeZoneOffsetMap);


        //generates List of possible sleep periods for given day
        final List<SleepPeriod> sleepPeriodQueue = SleepPeriod.getSleepPeriodQueue(targetDate, currentTimeLocal);
        final int numSleepPeriods = sleepPeriodQueue.size();

        if(numSleepPeriods == 0){
            LOGGER.error("msg=no-available-sleep-periods-for-target-date account_id={} current_local_time={} target_date={}", accountId, currentTimeLocal, targetDate);
            final TimelineLog log = new TimelineLog(accountId, targetDate.withZone(DateTimeZone.UTC).getMillis());
            log.addMessage(TimelineError.NO_DATA);
            return TimelineResult.createEmpty(log, English.TIMELINE_NO_SLEEP_DATA, DataCompleteness.NO_DATA);
        }

        //gets sensor data for range of sleepPeriod Queue - from start of first period (morning) to end of data window of last period
        final Optional<OneDaysSensorData> fullDaysSensorDataOptional = getSensorData(accountId, sleepPeriodQueue.get(0).getSleepPeriodTime(SleepPeriod.Boundary.START,0), sleepPeriodQueue.get(numSleepPeriods-1).getSleepPeriodTime(SleepPeriod.Boundary.END_DATA, 0), currentTimeLocal, Optional.absent());

        if (!fullDaysSensorDataOptional.isPresent()) {
            LOGGER.info("msg=no-timeline-generated reason=missing-sensor-data account_id = {} day = {}", accountId, targetDate);
            final TimelineLog log = new TimelineLog(accountId, targetDate.withZone(DateTimeZone.UTC).getMillis());
            log.addMessage(TimelineError.NO_DATA);
            return TimelineResult.createEmpty(log, English.TIMELINE_NO_SLEEP_DATA, DataCompleteness.NO_DATA);
        }

        final OneDaysSensorData fullDaySensorData = fullDaysSensorDataOptional.get();

        //Gets previously generated MainEventTimes for target day and target day -1 for possible OOB time of previous night
        final List<MainEventTimes> generatedMainEventTimesList = mainEventTimesDAO.getEventTimes(accountId, targetDate.minusDays(1), targetDate.plusDays(1));
        //get sleepPeriodResults for all Sleep Periods in queue
        final SleepDay targetSleepDay = getSleepPeriodResultsForAllSleepPeriods(accountId, targetDate, numSleepPeriods, fullDaySensorData, generatedMainEventTimesList, timeZoneOffsetMap);

        //construct single timeline from sleep period results
        final AllPeriodTimelineResults allSleepPeriodsTimelineResult = generateTimelineResultsForAllSleepPeriods(accountId, targetDate, numSleepPeriods, targetSleepDay);

        //save sleep stats and sleepscore on valid timeline
        if(allSleepPeriodsTimelineResult.isValid) {
            final boolean successfulUpdate = sleepStatsDAODynamoDB.updateStat(accountId, targetDate, allSleepPeriodsTimelineResult.sleepScore.value, allSleepPeriodsTimelineResult.sleepScore, allSleepPeriodsTimelineResult.sleepStats, timeZoneOffsetMap.getOffsetWithDefaultAsZero(targetDate.getMillis()));
            if(!successfulUpdate){
                LOGGER.error("msg=failed-to-save-sleep-stats account_id={} target_date={}", accountId, targetDate);
            }
        }


        return allSleepPeriodsTimelineResult.timelineResult;
    }

    private static class AllPeriodTimelineResults{
        final TimelineResult timelineResult;
        final SleepScore sleepScore;
        final SleepStats sleepStats;
        final boolean isValid;
        private AllPeriodTimelineResults(final TimelineResult timelineResults, final SleepScore sleepScore, final SleepStats sleepStats, final boolean isValid){
            this.timelineResult = timelineResults;
            this.sleepScore = sleepScore;
            this.sleepStats = sleepStats;
            this.isValid = isValid;
        }
        public static AllPeriodTimelineResults create(final TimelineResult timelineResults, final SleepScore sleepScore, final SleepStats sleepStats, final boolean isValid){
            return new AllPeriodTimelineResults(timelineResults, sleepScore, sleepStats, isValid);
        }
    }

    /*
    Creates the TimelineResult for ALL sleep periods
    Loops through sleepPeriodResults
    creates new list of ALL sleepSegements from ALL sleep periods
    records the sleep stats, score and message from the sleep period with the highest sleep score
    creates list of log messages
    gets the data completeness for targetDate.
     */
    public AllPeriodTimelineResults generateTimelineResultsForAllSleepPeriods(final long accountId, final DateTime targetDate, final int numSleepPeriods, final SleepDay targetSleepDay){

        //CURRENTLY sleep_score, sleep_stats, sleep_message, and sleep_insights are set to the sleep period with the highest sleep score
        //in case of tie, selects the most recent sleep period
        final List<SleepSegment> allSleepPeriodsEvents = new ArrayList<>();
        SleepScore sleepScore = new SleepScore(0,new MotionScore(0,0,0F,0,0),0,0,0,"");
        SleepStats sleepStats = SleepStats.create(0,0,0,0,0);
        String timelineMessage = "";
        List<Insight> timelineInsight = new ArrayList<>();
        final TimelineLog log = new TimelineLog(accountId, targetDate.getMillis());

        final List<DataCompleteness> dataCompletenessList = Lists.newArrayList();
        final List<TimelineLog> timelineLogs = Lists.newArrayList();
        final List<SleepPeriod.Period> sleepPeriods = Lists.newArrayList();

        //loops through sleepPeriodResults, extracts timeline events, highest sleepscore (w/ sleepStats), timeline logs and datacompleteness
        for (int i = 0; i < numSleepPeriods; i++) {
            final SleepPeriod.Period period = SleepPeriod.Period.fromInteger(i);
            final SleepPeriodResults targetSleepPeriodResults = targetSleepDay.getSleepPeriod(period);

            if (!targetSleepPeriodResults.processed){
                continue;
            }

            //results are not present - continue
            if(!targetSleepPeriodResults.resultsOptional.isPresent()){
                continue;
            }

            if (!targetSleepPeriodResults.processed || !targetSleepPeriodResults.resultsOptional.get().timeline.statistics.isPresent()) {
                timelineLogs.add(targetSleepPeriodResults.timelineLog);
                dataCompletenessList.add(DataCompleteness.NOT_ENOUGH_DATA);
                continue;
            }

            timelineLogs.add(targetSleepPeriodResults.timelineLog);
            dataCompletenessList.add(DataCompleteness.ENOUGH_DATA);
            sleepPeriods.add(targetSleepPeriodResults.mainEventTimes.sleepPeriod.period);

            final int targetScore = targetSleepPeriodResults.resultsOptional.get().timeline.score;
            if (targetScore >= sleepScore.value){
                sleepScore = targetSleepPeriodResults.resultsOptional.get().sleepScore;
                sleepStats = targetSleepPeriodResults.resultsOptional.get().sleepStats;
                timelineMessage = targetSleepPeriodResults.resultsOptional.get().timeline.message;
                timelineInsight = targetSleepPeriodResults.resultsOptional.get().timeline.insights;
            }
            allSleepPeriodsEvents.addAll(targetSleepPeriodResults.resultsOptional.get().timeline.events);
        }

        //if timeline events > 4, we have a valid timeline.
        if (allSleepPeriodsEvents.size() > MIN_NUM_EVENTS) {
            final Timeline timeline = Timeline.create(sleepScore.value, timelineMessage, targetDate.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT), sleepPeriods, allSleepPeriodsEvents, timelineInsight,sleepStats);
            final TimelineResult timelineResult = TimelineResult.create(Lists.newArrayList(timeline), new ArrayList(timelineLogs));
            final AllPeriodTimelineResults allPeriodTimelineResults = AllPeriodTimelineResults.create(timelineResult, sleepScore, sleepStats, true);
            return allPeriodTimelineResults;
        }

        final DataCompleteness overallDataCompleteness = timelineUtils.getDataCompletenessForAllSleepPeriods(dataCompletenessList, numSleepPeriods);

        final String message;
        if (overallDataCompleteness == DataCompleteness.NO_DATA){
            message = English.TIMELINE_NO_SLEEP_DATA;
        } else{
            message = English.TIMELINE_NOT_ENOUGH_SLEEP_DATA;
        }

        final TimelineResult timelineResultEmpty = TimelineResult.createEmpty(log, message, overallDataCompleteness);
        final AllPeriodTimelineResults allPeriodTimelineResultsEmpty = AllPeriodTimelineResults.create(timelineResultEmpty, sleepScore, sleepStats, false);
        return allPeriodTimelineResultsEmpty;
    }

    /*
    Loops through sleep periods in queue,
    Checks if a locked down timeline (valid or invalid) has been generated for the period
    if locked down valid timeline has been generated, populate sleep results with that timeline for period
    otherwise generate and populate a new timeline for that period
     */
    //todo add check for new feedback - need to regenerate entire sleep period on new feedback
    public SleepDay getSleepPeriodResultsForAllSleepPeriods(final Long accountId, final DateTime targetDate, final int numSleepPeriods, final OneDaysSensorData fullDaySensorData, final List<MainEventTimes> generatedMainEventTimesList, final TimeZoneOffsetMap timeZoneOffsetMap ) {

        //construct SleepPeriod - MainEventTimes map of target day and previous day Main Event Times
        final MainEventTimes prevNightMainEventTimes = TimelineUtils.getPrevNightMainEventTimes(accountId, generatedMainEventTimesList, targetDate);
        SleepDay targetSleepDay = SleepDay.createSleepDay(accountId, targetDate, generatedMainEventTimesList );

        //loops through potential sleep periods
        for (int i = 0; i < numSleepPeriods; i++) {
            final SleepPeriod targetSleepPeriod = SleepPeriod.createSleepPeriod(SleepPeriod.Period.fromInteger(i), targetDate);
            //Placeholder for feedback - current defaults to no new feedback
            final Optional<TimelineFeedback> newFeedback = Optional.absent();
            //check if new timeline needs to be attempted
            final Optional<Long> prevOutOfBedTimeOptional = targetSleepDay.getPreviousOutOfBedTime(targetSleepPeriod.period, prevNightMainEventTimes);

            final boolean attemptLockDown = !newFeedback.isPresent() && useTimelineLockdown(accountId);
            final boolean attemptTimeline = TimelineLockdown.isAttemptNeededForSleepPeriod(targetSleepDay, targetSleepPeriod,  prevOutOfBedTimeOptional, fullDaySensorData.oneDaysTrackerMotion.processedtrackerMotions, attemptLockDown);

            final SleepPeriodResults targetSleepPeriodResults;
            if(attemptTimeline){
                targetSleepPeriodResults = retrievePopulateAndSaveSleepPeriodResult(accountId, targetDate, targetSleepPeriod, fullDaySensorData, timeZoneOffsetMap, prevOutOfBedTimeOptional, newFeedback);
                targetSleepDay.updateSleepPeriod(targetSleepPeriodResults);
                //update targetDateSleepPeriodsMainEventsMap for prevOutOfBedTime

            } else {
                //targetDateSleepPeriodsMainEventsMap will contain the target SleepPeriod at this point
                final MainEventTimes generatedTargetPeriodMainEventTimes = targetSleepDay.getSleepPeriod(targetSleepPeriod.period).mainEventTimes;
                final OneDaysSensorData targetSleepPeriodSensorData = fullDaySensorData.getForSleepPeriod(prevOutOfBedTimeOptional, targetSleepPeriod, hasOutlierFilterEnabled(accountId));
                targetSleepPeriodResults = populateSingleSleepPeriodTimeline(accountId, targetSleepPeriodSensorData, timeZoneOffsetMap, generatedTargetPeriodMainEventTimes, new TimelineLog(accountId, targetDate.getMillis(), DateTime.now(DateTimeZone.UTC).getMillis()), false);
            }

            //put all results - valid and invalid timelines
            targetSleepDay.updateSleepPeriod(targetSleepPeriodResults);
        }
        LOGGER.debug("msg=retrieved-all-sleep-periods date={} morning_success={} afternoon_success={}, night_success={}", targetDate, targetSleepDay.getSleepPeriod(SleepPeriod.Period.MORNING).resultsOptional.isPresent(),targetSleepDay.getSleepPeriod(SleepPeriod.Period.AFTERNOON).resultsOptional.isPresent(), targetSleepDay.getSleepPeriod(SleepPeriod.Period.NIGHT).resultsOptional.isPresent() );

        return targetSleepDay;
    }

    //todo check if save all new results here
    public SleepPeriodResults retrievePopulateAndSaveSleepPeriodResult(final Long accountId, final DateTime targetDate, final SleepPeriod sleepPeriod, final OneDaysSensorData sensorData, final TimeZoneOffsetMap timeZoneOffsetMap, final Optional<Long> prevOutOfBedTimeOptional, final Optional<TimelineFeedback> newFeedback) {

        final DateTime startTimeLocalUTC = sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.START, timeZoneOffsetMap.getOffsetWithDefaultAsZero(targetDate.getMillis()));
        final DateTime endTimeLocalUTC = sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.END_DATA, timeZoneOffsetMap.getOffsetWithDefaultAsZero(targetDate.getMillis()));
        final boolean feedbackChanged = newFeedback.isPresent() && this.hasOnlineHmmLearningEnabled(accountId);

        if(prevOutOfBedTimeOptional.isPresent()) {
            LOGGER.debug("msg=previous-out-of-bed-time-present target_date={} sleep_period={} oob_time={}", sleepPeriod.targetDate, sleepPeriod.period.shortName(), prevOutOfBedTimeOptional.get());
        }
        final OneDaysSensorData sensorDataSleepPeriod = sensorData.getForSleepPeriod(prevOutOfBedTimeOptional, sleepPeriod, hasOutlierFilterEnabled(accountId));

        //chain of fail-overs of algorithm (i.e)
        final LinkedList<AlgorithmType> algorithmChain = Lists.newLinkedList();

        algorithmChain.addFirst(AlgorithmType.NEURAL_NET_FOUR_EVENT);


        LOGGER.info("action=get_timeline date={} sleep_period={} account_id={} start_time={} end_time={}", targetDate.toDate(),sleepPeriod.period.shortName(), accountId, startTimeLocalUTC, endTimeLocalUTC);

        //get test group of user, will be default if no entries exist in database
        final Long testGroup = getTestGroup(accountId, startTimeLocalUTC, sensorDataSleepPeriod.timezoneOffsetMillis);

        //create log with test grup
        final TimelineLog log = new TimelineLog(accountId, startTimeLocalUTC.withZone(DateTimeZone.UTC).getMillis(), DateTime.now(DateTimeZone.UTC).getMillis(), testGroup);


        //check to see if there's an issue with the data
        final int minMotionCountThreshold;
        if (useNoMotionEnforcement(accountId)) {
            minMotionCountThreshold = MIN_TRACKER_MOTION_COUNT_LOWER_THRESHOLD;
        } else {
            minMotionCountThreshold = MIN_TRACKER_MOTION_COUNT;
        }

        final TimelineError discardReason = isValidNight(accountId, sensorDataSleepPeriod, sleepPeriod, minMotionCountThreshold);
        final MainEventTimes mainEventTimesEmpty = MainEventTimes.createMainEventTimesEmpty(accountId, sleepPeriod, DateTime.now(DateTimeZone.UTC).getMillis(), 0);

        if (!discardReason.equals(TimelineError.NO_ERROR)) {
            LOGGER.info("action=discard_timeline reason={} account_id={} date={} sleep_period={}", discardReason, accountId, targetDate.toDate(), sleepPeriod.period.shortName());
        }

        switch (discardReason) {

            case TIMESPAN_TOO_SHORT:
                log.addMessage(discardReason);
                //TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, DataCompleteness.NOT_ENOUGH_DATA);
                mainEventTimesDAO.updateEventTimes(mainEventTimesEmpty);
                return SleepPeriodResults.createEmpty(accountId, sleepPeriod, log, DataCompleteness.NOT_ENOUGH_DATA, true);


            case NOT_ENOUGH_DATA:
                log.addMessage(discardReason);
                //TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, DataCompleteness.NOT_ENOUGH_DATA);
                mainEventTimesDAO.updateEventTimes(mainEventTimesEmpty);
                return SleepPeriodResults.createEmpty(accountId, sleepPeriod, log,  DataCompleteness.NOT_ENOUGH_DATA, true);

            case NO_DATA:
                log.addMessage(discardReason);
                //TimelineResult.createEmpty(log, English.TIMELINE_NO_SLEEP_DATA, DataCompleteness.NO_DATA);
                mainEventTimesDAO.updateEventTimes(mainEventTimesEmpty);
                return SleepPeriodResults.createEmpty(accountId, sleepPeriod, log, DataCompleteness.NO_DATA, true);

            case LOW_AMP_DATA:
                log.addMessage(discardReason);
                //TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, DataCompleteness.NOT_ENOUGH_DATA);
                mainEventTimesDAO.updateEventTimes(mainEventTimesEmpty);
                return SleepPeriodResults.createEmpty(accountId, sleepPeriod, log, DataCompleteness.NOT_ENOUGH_DATA, true);

            case IMPROBABLE_SLEEP_PERIOD:
                log.addMessage(discardReason);
                //TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, DataCompleteness.NOT_ENOUGH_DATA);
                mainEventTimesDAO.updateEventTimes(mainEventTimesEmpty);
                return SleepPeriodResults.createEmpty(accountId, sleepPeriod, log, DataCompleteness.ENOUGH_DATA, true);

            case PARTNER_FILTER_REJECTED_DATA:
                log.addMessage(discardReason);
                //TimelineResult.createEmpty(log, English.TIMELINE_NOT_ENOUGH_SLEEP_DATA, DataCompleteness.NOT_ENOUGH_DATA);
                mainEventTimesDAO.updateEventTimes(mainEventTimesEmpty);
                return SleepPeriodResults.createEmpty(accountId, sleepPeriod, log,  DataCompleteness.NOT_ENOUGH_DATA, true);

            default:
                break;
        }

        final Optional<MainEventTimes> computedMainEventTimesOptional = mainEventTimesDAO.getEventTimesForSleepPeriod(accountId, targetDate, sleepPeriod.period);
        /*check if previously generated timeline is valid for lockdown - if valid for lockdown, uses cached main event times instead of running timeline algorithm*/

        final boolean timelineLockedDown = TimelineLockdown.isLockedDown(computedMainEventTimesOptional, sensorDataSleepPeriod.oneDaysTrackerMotion.processedtrackerMotions, useTimelineLockdown(accountId));

        Optional<TimelineAlgorithmResult> resultOptional = Optional.absent();

        /* DEFAULT VALUE IS CACHED TIMELINE MAIN EVENTS */
        if (computedMainEventTimesOptional.isPresent()) {
            resultOptional = Optional.of(new TimelineAlgorithmResult(AlgorithmType.NONE, computedMainEventTimesOptional.get().getMainEvents()));
        }

        if (!timelineLockedDown) {
            /*  GET THE TIMELINE! */
            final Set<String> featureFlips = getTimelineFeatureFlips(accountId);

            for (final AlgorithmType alg : algorithmChain) {
                LOGGER.info("action=try_algorithm algorithm_type={}", alg);
                final Optional<TimelineAlgorithm> timelineAlgorithm = algorithmFactory.get(alg);

                if (!timelineAlgorithm.isPresent()) {
                    //assume error reporting is happening in alg, no need to report it here
                    continue;
                }

                resultOptional = timelineAlgorithm.get().getTimelinePrediction(sensorDataSleepPeriod, sleepPeriod, log, accountId, feedbackChanged, featureFlips);

                //got a valid result? poof, we're out.
                if (resultOptional.isPresent()) {
                    break;
                }
            }
        }
        if (!resultOptional.isPresent()) {
            mainEventTimesDAO.updateEventTimes(mainEventTimesEmpty);
            LOGGER.info("msg=no-successful-algorithm account_id={} date={} sleep_period={}", accountId, targetDate, sleepPeriod.period.shortName());

            if(!timelineLockedDown) {
                mainEventTimesDAO.updateEventTimes(mainEventTimesEmpty);
            }

            return SleepPeriodResults.createEmpty(accountId, sleepPeriod, log, DataCompleteness.ENOUGH_DATA, true);
        }
        final MainEventTimes mainEventTimes = MainEventTimes.createMainEventTimes(accountId, sleepPeriod, DateTime.now(DateTimeZone.UTC).getMillis(), 0, resultOptional.get());

        //populate timelines now
        final SleepPeriodResults sleepPeriodResults = populateSingleSleepPeriodTimeline(accountId, sensorDataSleepPeriod, timeZoneOffsetMap, mainEventTimes, log, !timelineLockedDown);

        //we should never hit this points - already should have returned empty timeline
        if (!sleepPeriodResults.resultsOptional.isPresent()) {
            LOGGER.info("msg=missing-populated-timeline-results account_id={} date={} sleep_period={}", accountId, targetDate, sleepPeriod.period.shortName());
            if(!timelineLockedDown) {
                mainEventTimesDAO.updateEventTimes(mainEventTimesEmpty);
            }
            return SleepPeriodResults.createEmpty(accountId, sleepPeriod, log, DataCompleteness.ENOUGH_DATA, false);
        }

        //we should never hit this points either
        if (!sleepPeriodResults.processed) {
            LOGGER.info("msg=invalid-timeline-generated account_id={} date={} sleep_period={}", accountId, targetDate, sleepPeriod.period.shortName());
            if(!timelineLockedDown) {
                mainEventTimesDAO.updateEventTimes(mainEventTimesEmpty);
            }
            return SleepPeriodResults.createEmpty(accountId, sleepPeriod, log, DataCompleteness.ENOUGH_DATA, false);
        }

        //regenerated maineventtimes after populating to incorporate user feedback.
        //save populated main event times to ensure that the OOB time affiliates the correct data to sleep period
        if(!timelineLockedDown) {
            mainEventTimesDAO.updateEventTimes(sleepPeriodResults.mainEventTimes);
        }
        return sleepPeriodResults;
    }

    private Set<String> getTimelineFeatureFlips(final long accountId) {
        final Set<String> featureFlips = Sets.newHashSet();
        if (hasOffBedFilterEnabled(accountId)) {
            featureFlips.add(FeatureFlipper.OFF_BED_HMM_MOTION_FILTER);
        }
        if (useTimelineSleepPeriods(accountId)) {
            featureFlips.add(FeatureFlipper.TIMELINE_SLEEP_PERIOD);
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

    protected ImmutableList<TrackerMotion> filterPillPairingMotions(final ImmutableList<TrackerMotion> motions, final long accountId) {
        final List<DateTime> pairTimes =  Lists.newArrayList();
        final ImmutableList<DeviceAccountPair> pills = deviceDAO.getPillsForAccountId(accountId); //get pills


        for (final DeviceAccountPair pill : pills) {
            pairTimes.add(pill.created);
        }

        LOGGER.info("action=try_filter_pairing_motion account_id={} num_pairing_times={}",accountId,pairTimes.size());

        return timelineUtils.filterPillPairingMotionsWithTimes(motions,pairTimes);
    }

    protected Optional<OneDaysSensorData> getSensorData(final long accountId, final DateTime startTimeLocalUTC, final DateTime endTimeLocalUTC, final DateTime currentTimeUTC,final Optional<TimelineFeedback> newFeedback) {

        final DateTime targetDate = startTimeLocalUTC.withTimeAtStartOfDay();
        ImmutableList<TrackerMotion> originalTrackerMotions = pillDataDAODynamoDB.getBetweenLocalUTC(accountId, startTimeLocalUTC, endTimeLocalUTC);

        if (originalTrackerMotions.isEmpty()) {
            LOGGER.warn("No original tracker motion data for account {} on {}, returning optional absent", accountId, startTimeLocalUTC);
            return Optional.absent();
        }

        LOGGER.debug("Length of originalTrackerMotion is {} for {} on {}", originalTrackerMotions.size(), accountId, startTimeLocalUTC);

        // get partner tracker motion, if available
        ImmutableList<TrackerMotion> originalPartnerMotions = getPartnerTrackerMotion(accountId, startTimeLocalUTC, endTimeLocalUTC);

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
                LOGGER.warn("msg=no-original-tracker-motion-data-after-pairing-filter account_id={} datetime={}", accountId, startTimeLocalUTC);
                return Optional.absent();
            }
        }

        final Optional<Account> accountOptional = accountDAO.getById(accountId);
        final UserBioInfo userBioInfo = UserBioInfo.getUserBioInfo(accountOptional, isDaySleeper(accountId), !originalPartnerMotions.isEmpty());


        List<TrackerMotion> filteredOriginalMotions = originalTrackerMotions;
        List<TrackerMotion> filteredOriginalPartnerMotions = originalPartnerMotions;

        //removes motion events less than 2 seconds and < 300 val. groups the remaining motions into groups separated by 2 hours. If largest motion groups is greater than 6 hours hours, drop all motions afterward this motion group.
        //unless we are evaluating a full day. Then we dont want to omit tracker motion data. will refilter when partioning by sleepPeriod. define dominant group as 36 hours.
        if (this.hasOutlierFilterEnabled(accountId)) {
            filteredOriginalMotions = OutlierFilter.removeOutliers(originalTrackerMotions, OUTLIER_GUARD_DURATION, DOMINANT_GROUP_DURATION_ALL_PERIODS);
            filteredOriginalPartnerMotions = OutlierFilter.removeOutliers(originalPartnerMotions, OUTLIER_GUARD_DURATION, DOMINANT_GROUP_DURATION_ALL_PERIODS);
        }

        List<TrackerMotion> trackerMotions = Lists.newArrayList();

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
                                    startTimeLocalUTC.minusMillis(tzOffsetMillis),
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


        //motion mask filtering additive with other filters
        final boolean hasMotionMasks;
        if (trackerMotions.size()> 0 && filteredOriginalPartnerMotions.size()>0){
            hasMotionMasks = trackerMotions.get(0).motionMask.isPresent() &&  filteredOriginalPartnerMotions.get(0).motionMask.isPresent();
        } else {
            hasMotionMasks = false;
        }
        if (this.hasMotionMaskPartnerFilter(accountId) && hasMotionMasks){
            trackerMotions = MotionMaskPartnerFilter.partnerFiltering(trackerMotions, filteredOriginalPartnerMotions);
            LOGGER.info("action=using-motion-mask-partner-filter account_id={} removed-motions={}", accountId, filteredOriginalMotions.size() - trackerMotions.size());
        }



        if (trackerMotions.isEmpty()) {
            LOGGER.debug("No tracker motion data ID for account_id = {} and day = {}", accountId, startTimeLocalUTC);
            return Optional.absent();
        }

        final OneDaysTrackerMotion oneDaysTrackerMotion = new OneDaysTrackerMotion(ImmutableList.copyOf(trackerMotions), ImmutableList.copyOf(filteredOriginalMotions), originalTrackerMotions);
        final OneDaysTrackerMotion oneDaysPartnerMotion = new OneDaysTrackerMotion(ImmutableList.copyOf(filteredOriginalPartnerMotions), ImmutableList.copyOf(filteredOriginalPartnerMotions), originalPartnerMotions);


        final int tzOffsetMillis = trackerMotions.get(0).offsetMillis;
        final Optional<AllSensorSampleList> allSensorSampleList = senseDataDAO.get(accountId,targetDate,startTimeLocalUTC, endTimeLocalUTC, currentTimeUTC, tzOffsetMillis);
        LOGGER.info("Sensor data for timeline generated by DynamoDB for account {}", accountId);

        if (!allSensorSampleList.isPresent() || allSensorSampleList.get().isEmpty()) {
            LOGGER.debug("No sense sensor data ID for account_id = {} and day = {}", accountId, startTimeLocalUTC);
            return Optional.absent();
        }

        final List<TimelineFeedback> feedbackList = Lists.newArrayList(getFeedbackList(accountId, startTimeLocalUTC, tzOffsetMillis));

        if (newFeedback.isPresent()) {
            feedbackList.add(newFeedback.get());
        }

        return Optional.of(new OneDaysSensorData(allSensorSampleList.get(),oneDaysTrackerMotion, oneDaysPartnerMotion,
                ImmutableList.copyOf(feedbackList),targetDate,startTimeLocalUTC,endTimeLocalUTC,currentTimeUTC,
                tzOffsetMillis,userBioInfo));
    }


    public SleepPeriodResults populateSingleSleepPeriodTimeline(final long accountId,final OneDaysSensorData sensorData, final TimeZoneOffsetMap timeZoneOffsetMap, final MainEventTimes mainEventTimes, final TimelineLog timelineLog, final boolean isNewResult) {

        if(!mainEventTimes.hasValidEventTimes()){
            //if no valid period in day, it will result in not enough data for all periods once all periods are locked down
            final DataCompleteness dataCompletenessInvalidMainEventTimes;
            if(sensorData.oneDaysTrackerMotion.originalTrackerMotions.isEmpty()){
                dataCompletenessInvalidMainEventTimes = DataCompleteness.NO_DATA;
            } else{
                dataCompletenessInvalidMainEventTimes = DataCompleteness.NOT_ENOUGH_DATA;
            }
            return SleepPeriodResults.createEmpty(accountId, mainEventTimes.sleepPeriod,timelineLog,dataCompletenessInvalidMainEventTimes, true);
        }

        final DateTime targetDate = mainEventTimes.sleepPeriod.targetDate;
        final DateTime endDate = mainEventTimes.sleepPeriod.getSleepPeriodTime(SleepPeriod.Boundary.END_DATA, mainEventTimes.eventTimeMap.get(Event.Type.OUT_OF_BED).offset);
        final List<Event> mainEvents = mainEventTimes.getMainEvents();


        final ImmutableList<TrackerMotion> trackerMotions = sensorData.oneDaysTrackerMotion.processedtrackerMotions;
        final AllSensorSampleList allSensorSampleList = sensorData.allSensorSampleList;
        final ImmutableList<TrackerMotion> partnerMotions = sensorData.oneDaysPartnerMotion.processedtrackerMotions;
        final ImmutableList<TimelineFeedback> feedbackList = sensorData.feedbackList;

        //MOVE EVENTS BASED ON FEEDBACK
        FeedbackUtils.ReprocessedEvents reprocessedEvents = null;

        LOGGER.info("action=apply_feedback num_items={} account_id={} date={}", feedbackList.size(),accountId,sensorData.date.toDate());
        //removed FF  TIMELINE_EVENT_ORDER_ENFORCEMENT - at 100 percent
        reprocessedEvents = feedbackUtils.reprocessEventsBasedOnFeedback(feedbackList, ImmutableList.copyOf(mainEvents), ImmutableList.copyOf(Collections.EMPTY_LIST), timeZoneOffsetMap);

        //GET SPECIFIC EVENTS
        Optional<Event> inBed = Optional.fromNullable(reprocessedEvents.mainEvents.get(Event.Type.IN_BED));
        Optional<Event> sleep = Optional.fromNullable(reprocessedEvents.mainEvents.get(Event.Type.SLEEP));
        Optional<Event> wake= Optional.fromNullable(reprocessedEvents.mainEvents.get(Event.Type.WAKE_UP));
        Optional<Event> outOfBed= Optional.fromNullable(reprocessedEvents.mainEvents.get(Event.Type.OUT_OF_BED));

        //CREATE SLEEP MOTION EVENTS
        final List<MotionEvent> motionEvents = timelineUtils.generateMotionEvents(trackerMotions);

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
            lightEvents.addAll(timelineUtils.getLightEvents(sleepTime, allSensorSampleList.get(Sensor.LIGHT)));
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

        final SleepScore sleepScore = computeScore(sensorData.oneDaysTrackerMotion.processedtrackerMotions, sensorData.oneDaysTrackerMotion.filteredOriginalTrackerMotions, numSoundEvents, allSensorSampleList, targetDate, accountId, sleepStats);
        int sleepScoreValue = sleepScore.value;


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
        final MainEventTimes populatedMainEventTimes = MainEventTimes.createMainEventTimes(accountId, mainEventTimes.sleepPeriod, DateTime.now(DateTimeZone.UTC).getMillis(), 0, sleepSegments);
        final Timeline timeline = Timeline.create(sleepScoreValue, timeLineMessage, targetDate.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT), Lists.newArrayList(mainEventTimes.sleepPeriod.period),reversedSegments, insights, sleepStats);

        return SleepPeriodResults.create(populatedMainEventTimes, timeline, sleepScore,sleepStats, sensorData, timeZoneOffsetMap, timelineLog, DataCompleteness.ENOUGH_DATA, true);
    }

    /*
     * PRELIMINARY SANITY CHECK (static and public for testing purposes)
     */
    static public TimelineError isValidNight(final Long accountId, final OneDaysSensorData sensorData, final SleepPeriod sleepPeriod, final int minMotionCountThreshold){

        if(sensorData.oneDaysTrackerMotion.filteredOriginalTrackerMotions.size() == 0){
            return TimelineError.NO_DATA;
        }

        //CHECK TO SEE IF THERE ARE "ENOUGH" MOTION EVENTS
        //If greater than MinTracker Motion Count - continue
        // if FF for lower motion count threshold, if between lower motion threshold and high motion threshold, check percent unique motions for parter data.
        // if lower than lower motion count threshold, reject
        if(sensorData.oneDaysTrackerMotion.filteredOriginalTrackerMotions.size() < minMotionCountThreshold){
            return TimelineError.NOT_ENOUGH_DATA;
        } else if (sensorData.oneDaysTrackerMotion.filteredOriginalTrackerMotions.size() >= MIN_TRACKER_MOTION_COUNT_LOWER_THRESHOLD && sensorData.oneDaysTrackerMotion.filteredOriginalTrackerMotions.size() < MIN_TRACKER_MOTION_COUNT) {
            final float percentUniqueMotions = PartnerDataUtils.getPercentUniqueMovements(sensorData.oneDaysTrackerMotion.filteredOriginalTrackerMotions, sensorData.oneDaysPartnerMotion.filteredOriginalTrackerMotions);

            if (percentUniqueMotions < MIN_FRACTION_UNIQUE_MOTION){
                return TimelineError.NOT_ENOUGH_DATA;
            }
        } else if (sensorData.oneDaysTrackerMotion.originalTrackerMotions.size() < MIN_TRACKER_MOTION_COUNT_LOWER_THRESHOLD){
            return TimelineError.NOT_ENOUGH_DATA;
        }

        //CHECK TO SEE IF MOTION AMPLITUDE IS EVER ABOVE MINIMUM THRESHOLD
        boolean isMotionAmplitudeAboveMinimumThreshold = false;

        for(final TrackerMotion trackerMotion : sensorData.oneDaysTrackerMotion.filteredOriginalTrackerMotions){
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
        if(sensorData.oneDaysTrackerMotion.originalTrackerMotions.get(sensorData.oneDaysTrackerMotion.filteredOriginalTrackerMotions.size() - 1).timestamp - sensorData.oneDaysTrackerMotion.filteredOriginalTrackerMotions.get(0).timestamp < MIN_DURATION_OF_TRACKER_MOTION_IN_HOURS * DateTimeConstants.MILLIS_PER_HOUR) {
            return TimelineError.TIMESPAN_TOO_SHORT;
        }

        //CHECK IF PROBABLE SLEEP BASED ON SENSOR DATA
        final boolean daySleeper = sensorData.userBioInfo.primarySleepPeriod != SleepPeriod.Period.NIGHT;
        if (!TimelineSafeguards.isProbableNight(accountId, daySleeper, sleepPeriod, sensorData)){
            return TimelineError.IMPROBABLE_SLEEP_PERIOD;

        }

        //IF THE FILTERING WAS NOT USED (OR HAD NO EFFECT) WE ARE DONE
        if (sensorData.oneDaysTrackerMotion.filteredOriginalTrackerMotions.size() == sensorData.oneDaysTrackerMotion.processedtrackerMotions.size()) {
            return TimelineError.NO_ERROR;
        }
        ///////////////////////////
        //PARTNER FILTERED DATA CHECKS ////
        //////////////////////////

        //"not enough", not "no data", because there must have been some original data to get to this point
        if (sensorData.oneDaysTrackerMotion.processedtrackerMotions.isEmpty()) {
            return TimelineError.PARTNER_FILTER_REJECTED_DATA;
        }

        //CHECK TO SEE IF THERE ARE "ENOUGH" MOTION EVENTS, post partner-filtering.  trying to avoid case where partner filter lets a couple through even though the user is not there.
        if (sensorData.oneDaysTrackerMotion.processedtrackerMotions.size() < MIN_PARTNER_FILTERED_MOTION_COUNT) {
            return TimelineError.PARTNER_FILTER_REJECTED_DATA;
        }

        //CHECK TO SEE IF TIME SPAN FROM FIRST TO LAST MEASUREMENT OF PARTNER-FILTERED DATA IS ABOVE 3 HOURS
        if(sensorData.oneDaysTrackerMotion.processedtrackerMotions.get(sensorData.oneDaysTrackerMotion.processedtrackerMotions.size() - 1).timestamp - sensorData.oneDaysTrackerMotion.processedtrackerMotions.get(0).timestamp < MIN_DURATION_OF_FILTERED_MOTION_IN_HOURS * DateTimeConstants.MILLIS_PER_HOUR) {
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
            final List<MotionEvent> partnerMotionEvents = timelineUtils.generateMotionEvents(partnerMotionsWithinSleepBounds);

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
                lightOutTimeOptional, optionalSleepTime, optionalAwakeTime, usePeakEnergyThreshold);
    }

    /**
     * Sleep score - always compute and update dynamo
     * @param trackerMotions
     * @param targetDate
     * @param accountId
     * @param sleepStats
     * @return
     */
    private SleepScore computeScore(final List<TrackerMotion> trackerMotions,
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
//        final Boolean updatedStats = this.sleepStatsDAODynamoDB.updateStat(accountId,targetDate.withTimeAtStartOfDay(), sleepScore.value, sleepScore, sleepStats, userOffsetMillis);
//        LOGGER.debug("action=updated-stats-score updated={} account_id={} score={}, stats={}", updatedStats, accountId, sleepScore, sleepStats);
        return sleepScore;
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