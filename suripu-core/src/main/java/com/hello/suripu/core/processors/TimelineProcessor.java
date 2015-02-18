package com.hello.suripu.core.processors;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.bayes.GaussianDistribution;
import com.hello.suripu.algorithm.bayes.GaussianInference;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.SleepTimePriorsDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.DataScience.GaussianDistributionDataModel;
import com.hello.suripu.core.models.DataScience.WakeProbabilityDistributions;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.PartnerMotionEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.Insights.TrendGraph;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.SunData;
import com.hello.suripu.core.util.TimelineRefactored;
import com.hello.suripu.core.util.TimelineUtils;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Histogram;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TimelineProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineProcessor.class);
    private static final Integer MIN_SLEEP_DURATION_FOR_SLEEP_SCORE_IN_MINUTES = 3 * 60;
    private final AccountDAO accountDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final DeviceDAO deviceDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final SleepScoreDAO sleepScoreDAO;
    private final SleepLabelDAO sleepLabelDAO;
    private final TrendsInsightsDAO trendsInsightsDAO;
    private final SleepTimePriorsDAO sleepPriorsDAO;
    private final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB;
    private final int dateBucketPeriod;
    private final SunData sunData;
    private final AmazonS3 s3;
    private final String bucketName;
    private final RingTimeDAODynamoDB ringTimeDAODynamoDB;
    private final Histogram motionEventDistribution;
    private final WakeProbabilityDistributions defaultWakeDistribution;

    public TimelineProcessor(final TrackerMotionDAO trackerMotionDAO,
                            final AccountDAO accountDAO,
                            final DeviceDAO deviceDAO,
                            final DeviceDataDAO deviceDataDAO,
                            final SleepLabelDAO sleepLabelDAO,
                            final SleepScoreDAO sleepScoreDAO,
                            final TrendsInsightsDAO trendsInsightsDAO,
                            final SleepTimePriorsDAO sleepPriorsDAO,
                            final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB,
                            final int dateBucketPeriod,
                            final SunData sunData,
                            final AmazonS3 s3,
                            final String bucketName,
                            final RingTimeDAODynamoDB ringTimeDAODynamoDB) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.accountDAO = accountDAO;
        this.deviceDAO = deviceDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.sleepLabelDAO = sleepLabelDAO;
        this.sleepScoreDAO = sleepScoreDAO;
        this.trendsInsightsDAO = trendsInsightsDAO;
        this.sleepPriorsDAO = sleepPriorsDAO;
        this.aggregateSleepScoreDAODynamoDB = aggregateSleepScoreDAODynamoDB;
        this.dateBucketPeriod = dateBucketPeriod;
        this.sunData = sunData;
        this.s3 = s3;
        this.bucketName = bucketName;
        this.motionEventDistribution = Metrics.defaultRegistry().newHistogram(TimelineProcessor.class, "motion_event_distribution");
        this.ringTimeDAODynamoDB = ringTimeDAODynamoDB;


        //magical hard-coded constants
        //wake times are in local time, in hours from midnight
       //so -1.0 = 23:00
       //and 23.0 == 23:00
        //we will restrict times between 0 and 24, and just wrap after addition/subtraction

        //8am, with a lot of uncertainty
        final GaussianDistributionDataModel wake = new GaussianDistributionDataModel(
                new GaussianDistribution(8.0,2.0));

        //this is a relative time, so 0 mean means no bias
        final GaussianDistributionDataModel wake_prediction_bias = new GaussianDistributionDataModel(
                new GaussianDistribution(0.0,0.5));

        this.defaultWakeDistribution =  new WakeProbabilityDistributions(wake_prediction_bias,wake,wake_prediction_bias,wake);



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


    private List<Event> getAlarmEvents(final Long accountId, final DateTime evening, final DateTime morning, final Integer offsetMillis) {

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

        final List<RingTime> ringTimes = ringTimeDAODynamoDB.getRingTimesBetween(senseId, evening.minusWeeks(1));

        return TimelineUtils.getAlarmEvents(ringTimes, evening, morning, offsetMillis, DateTime.now(DateTimeZone.UTC));
    }

    public List<Timeline> retrieveTimelines(final Long accountId, final String date, final Integer missingDataDefaultValue, final Boolean hasAlarmInTimeline) {


        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);


        final List<Event> events = new LinkedList<>();

        // TODO: compute this threshold dynamically
        final int threshold = 10; // events with scores < threshold will be considered motion events
        final int mergeThreshold = 1; // min segment size is 1 minute

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenLocalUTC(accountId, targetDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());

        if(trackerMotions.isEmpty()) {
            LOGGER.debug("No data for account_id = {} and day = {}", accountId, targetDate);
            final Timeline timeline = Timeline.createEmpty();
            final List<Timeline> timelines = Lists.newArrayList(timeline);
            return timelines;
        }


        final ArrayList<Event> sleepEvents = new ArrayList<>();

        // get all sensor data, used for light and sound disturbances, and presleep-insights
        AllSensorSampleList allSensorSampleList = new AllSensorSampleList();

        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accountId);

        if (deviceId.isPresent()) {
            final int slotDurationMins = 1;

            allSensorSampleList = deviceDataDAO.generateTimeSeriesByLocalTimeAllSensors(
                    targetDate.getMillis(), endDate.getMillis(),
                    accountId, deviceId.get(), slotDurationMins, missingDataDefaultValue);
        }

        // compute sensor-related events
        Optional<DateTime> lightOutTimeOptional = Optional.absent();
        Optional<DateTime> wakeUpWaveTimeOptional = Optional.absent();

        final List<Event> lightEvents = Lists.newArrayList();
        if (!allSensorSampleList.isEmpty()) {
            lightEvents.addAll(TimelineUtils.getLightEvents(allSensorSampleList.get(Sensor.LIGHT)));
            lightOutTimeOptional = TimelineUtils.getLightsOutTime(lightEvents);

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
        events.addAll(motionEvents);


        final Map<Long, Event> timEvents = TimelineRefactored.populateTimeline(motionEvents);
        for(final Event event : lightEvents) {
            timEvents.put(event.getStartTimestamp(), event);
        }




        Optional<Segment> sleepSegmentOptional = Optional.absent();
        Optional<Segment> inBedSegmentOptional = Optional.absent();
        List<Optional<Event>> sleepEventsFromAlgorithm = new ArrayList<>();
        for(int i = 0; i < 4; i++){
            sleepEventsFromAlgorithm.add(Optional.<Event>absent());
        }

        // A day starts with 8pm local time and ends with 4pm local time next day
        try {
            sleepEventsFromAlgorithm = TimelineUtils.getSleepEvents(targetDate,
                    trackerMotions,
                    lightOutTimeOptional,
                    wakeUpWaveTimeOptional,
                    MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                    MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                    MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                    false);

            for(final Optional<Event> sleepEventOptional:sleepEventsFromAlgorithm){
                if(sleepEventOptional.isPresent()){
                    sleepEvents.add(sleepEventOptional.get());
                    timEvents.put(sleepEventOptional.get().getStartTimestamp(), sleepEventOptional.get());
                }
            }

            if(sleepEventsFromAlgorithm.get(1).isPresent() && sleepEventsFromAlgorithm.get(2).isPresent()){
                sleepSegmentOptional = Optional.of(new Segment(sleepEventsFromAlgorithm.get(1).get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.get(2).get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.get(2).get().getTimezoneOffset()));

                LOGGER.info("Sleep Time From Awake Detection Algorithm: {} - {}",
                        new DateTime(sleepSegmentOptional.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegmentOptional.get().getOffsetMillis())),
                        new DateTime(sleepSegmentOptional.get().getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepSegmentOptional.get().getOffsetMillis())));
            }

            if(sleepEventsFromAlgorithm.get(0).isPresent() && sleepEventsFromAlgorithm.get(3).isPresent()){
                inBedSegmentOptional = Optional.of(new Segment(sleepEventsFromAlgorithm.get(0).get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.get(3).get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.get(3).get().getTimezoneOffset()));
            }


        }catch (Exception ex){ //TODO : catch a more specific exception
            LOGGER.error("Generate sleep period from Awake Detection Algorithm failed: {}", ex.getMessage());
        }

        // add partner movement data, check if there's a partner
        final Optional<Long> optionalPartnerAccountId = this.deviceDAO.getPartnerAccountId(accountId);
        if (optionalPartnerAccountId.isPresent() && events.size() > 0) {
            LOGGER.debug("partner account {}", optionalPartnerAccountId.get());
            // get tracker motions for partner, query time is in UTC, not local_utc
            DateTime startTime = new DateTime(events.get(0).getStartTimestamp(), DateTimeZone.UTC);
            if(sleepSegmentOptional.isPresent()){
                startTime = new DateTime(sleepSegmentOptional.get().getStartTimestamp(), DateTimeZone.UTC);
            }
            final DateTime endTime = new DateTime(events.get(events.size() - 1).getStartTimestamp(), DateTimeZone.UTC);

            final List<TrackerMotion> partnerMotions = this.trackerMotionDAO.getBetween(optionalPartnerAccountId.get(), startTime, endTime);
            if (partnerMotions.size() > 0) {
                // use un-normalized data segments for comparison
                List<PartnerMotionEvent> partnerMotionEvents = PartnerMotion.getPartnerData(motionEvents, partnerMotions, threshold);
//                events.addAll();
                for(PartnerMotionEvent partnerMotionEvent : partnerMotionEvents) {
                    timEvents.put(partnerMotionEvent.getStartTimestamp(), partnerMotionEvent);
                }
            }
        }

//        // add sunrise data
//        final String sunRiseQueryDateString = targetDate.plusDays(1).toString(DateTimeFormat.forPattern("yyyy-MM-dd"));
//        final Optional<DateTime> sunrise = sunData.sunrise(sunRiseQueryDateString); // day + 1
//        if(sunrise.isPresent() && sleepSegmentOptional.isPresent()) {
//            final long sunRiseMillis = sunrise.get().getMillis();
//            final SunRiseEvent sunriseEvent = new SunRiseEvent(sunRiseMillis,
//                    sunRiseMillis + DateTimeConstants.MILLIS_PER_MINUTE,
//                    sleepSegmentOptional.get().getOffsetMillis(), 0, null);
//
//            // TODO: ADD Feature flipper here
////            if(feature.userFeatureActive(FeatureFlipper.SOUND_INFO_TIMELINE, accountId, new ArrayList<String>())) {
////                final Date expiration = new java.util.Date();
////                long msec = expiration.getTime();
////                msec += 1000 * 60 * 60; // 1 hour.
////                expiration.setTime(msec);
////                final URL url = s3.generatePresignedUrl(bucketName, "mario.mp3", expiration, HttpMethod.GET);
////                final SleepSegment.SoundInfo sunRiseSound = new SleepSegment.SoundInfo(url.toExternalForm(), 2000);
////                sunriseEvent.setSoundInfo(sunRiseSound);
////            }
//            events.add(sunriseEvent);
//            LOGGER.debug(sunriseEvent.getDescription());
//        }else{
//            LOGGER.warn("No sun rise data for date {}", sunRiseQueryDateString);
//        }
//
//        // TODO: add sound
//
//
//        // merge similar segments (by motion & event-type), then categorize
////        final List<SleepSegment> mergedSegments = TimelineUtils.mergeConsecutiveSleepSegments(segments, mergeThreshold);
//        final List<Event> mergedEvents = TimelineUtils.generateAlignedSegmentsByTypeWeight(events, DateTimeConstants.MILLIS_PER_MINUTE, 15, false);
//        final List<Event> convertedEvents = TimelineUtils.convertLightMotionToNone(mergedEvents, threshold);
//        writeMotionMetrics(this.motionEventDistribution, convertedEvents);

//        List<Event> eventsWithSleepEvents = smoothedEvents;
//        for (final Event sleepEvent : sleepEvents){
//            eventsWithSleepEvents = TimelineUtils.insertOneMinuteDurationEvents(eventsWithSleepEvents, sleepEvent);
//        }



        final List<Event> eventsWithSleepEvents = TimelineRefactored.mergeEvents(timEvents);
        final List<Event> smoothedEvents = TimelineUtils.smoothEvents(eventsWithSleepEvents);

        final List<Event> cleanedUpEvents = TimelineUtils.removeMotionEventsOutsideBedPeriod(smoothedEvents,
                                                            sleepEventsFromAlgorithm.get(0),
                                                            sleepEventsFromAlgorithm.get(3));

        final List<Event> greyEvents = TimelineUtils.greyNullEventsOutsideBedPeriod(cleanedUpEvents,
                sleepEventsFromAlgorithm.get(0),
                sleepEventsFromAlgorithm.get(3));

        List<SleepSegment> sleepSegments = TimelineUtils.eventsToSegments(greyEvents);

        final int lightSleepThreshold = 70; // TODO: Generate dynamically instead of hard threshold
        final SleepStats sleepStats = TimelineUtils.computeStats(sleepSegments, lightSleepThreshold);
        final List<SleepSegment> reversed = Lists.reverse(sleepSegments);

        // get scores - check dynamoDB first
        final int userOffsetMillis = trackerMotions.get(0).offsetMillis;
        final String targetDateString = DateTimeUtil.dateToYmdString(targetDate);

        final AggregateScore targetDateScore = this.aggregateSleepScoreDAODynamoDB.getSingleScore(accountId, targetDateString);
        Integer sleepScore = targetDateScore.score;

        if (sleepScore == 0) {
            // score may not have been computed yet, recompute
            sleepScore = sleepScoreDAO.getSleepScoreForNight(accountId, targetDate.withTimeAtStartOfDay(),
                    userOffsetMillis, this.dateBucketPeriod, sleepLabelDAO);

            final DateTime lastNight = new DateTime(DateTime.now(), DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1);
            if (targetDate.isBefore(lastNight)) {
                // write data to Dynamo if targetDate is old
                this.aggregateSleepScoreDAODynamoDB.writeSingleScore(
                        new AggregateScore(accountId,
                                sleepScore,
                                DateTimeUtil.dateToYmdString(targetDate.withTimeAtStartOfDay()),
                                targetDateScore.scoreType, targetDateScore.version));

                // add sleep-score and duration to day-of-week, over time tracking table
                if (sleepScore > 0) {
                    this.trendsInsightsDAO.updateDayOfWeekData(accountId, sleepScore, targetDate.withTimeAtStartOfDay(), userOffsetMillis, TrendGraph.DataType.SLEEP_SCORE);
                }

                if (sleepStats.sleepDurationInMinutes > 0) {
                    this.trendsInsightsDAO.updateSleepStats(accountId, userOffsetMillis, targetDate.withTimeAtStartOfDay(), sleepStats);
                }
            }
        }

        if(sleepStats.sleepDurationInMinutes < MIN_SLEEP_DURATION_FOR_SLEEP_SCORE_IN_MINUTES) {
            LOGGER.warn("Score for account id {} was set to zero because sleep duration is too short ({} min)", accountId, sleepStats.sleepDurationInMinutes);
            sleepScore = 0;
        }

        final Boolean reportSleepDuration = false;
        final String timeLineMessage = TimelineUtils.generateMessage(sleepStats, reportSleepDuration);

        LOGGER.debug("Score for account_id = {} is {}", accountId, sleepScore);


        final List<Insight> insights = TimelineUtils.generatePreSleepInsights(allSensorSampleList, sleepStats.sleepTime, accountId);
        final List<SleepSegment>  reversedSegments = Lists.reverse(reversed);
        final Timeline timeline = Timeline.create(sleepScore, timeLineMessage, date, reversedSegments, insights, sleepStats);

        return Lists.newArrayList(timeline);

    }


    public List<Timeline> retrieveTimelinesFast(final Long accountId, final String date, final Integer missingDataDefaultValue, final Boolean hasAlarmInTimeline) {


        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        // TODO: compute this threshold dynamically
        final int threshold = 10; // events with scores < threshold will be considered motion events

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenLocalUTC(accountId, targetDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());

        if(trackerMotions.size() < 20) {
            LOGGER.debug("No data for account_id = {} and day = {}", accountId, targetDate);
            final Timeline timeline = Timeline.createEmpty();
            final List<Timeline> timelines = Lists.newArrayList(timeline);
            return timelines;
        }


        // get all sensor data, used for light and sound disturbances, and presleep-insights
        AllSensorSampleList allSensorSampleList = new AllSensorSampleList();

        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accountId);

        if (deviceId.isPresent()) {
            final int slotDurationMins = 1;

            allSensorSampleList = deviceDataDAO.generateTimeSeriesByLocalTimeAllSensors(
                    targetDate.getMillis(), endDate.getMillis(),
                    accountId, deviceId.get(), slotDurationMins, missingDataDefaultValue);
        }

        // compute lights-out events
        Optional<DateTime> lightOutTimeOptional = Optional.absent();
        Optional<DateTime> wakeUpWaveTimeOptional = Optional.absent();
        final List<Event> lightEvents = Lists.newArrayList();

        if (!allSensorSampleList.isEmpty()) {
            lightEvents.addAll(TimelineUtils.getLightEvents(allSensorSampleList.get(Sensor.LIGHT)));

            if (lightEvents.size() > 0) {
                lightOutTimeOptional = TimelineUtils.getLightsOutTime(lightEvents);
            }


            // TODO: refactor

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

        // ALARM
        Optional<List<Event>> alarmEventsOptional = Optional.absent();

        if(hasAlarmInTimeline) {
            alarmEventsOptional = Optional.of(getAlarmEvents(accountId, targetDate, endDate, trackerMotions.get(0).offsetMillis));

            //if we are here, there are events and the get is not null
            for(final Event event : alarmEventsOptional.get()) {
                timelineEvents.put(event.getStartTimestamp(), event);
            }
        }

        final List<Optional<Event>> sleepEventsFromAlgorithm = fromAlgorithm(targetDate, trackerMotions, lightOutTimeOptional, wakeUpWaveTimeOptional);
        Optional<DateTime> feedbackWakeTime = Optional.absent();

        //BEJ - post-process sleep predictions using priors from previous day, if available
        final List<Optional<Event>> updatedSleepEvents  = BayesUpdate(accountId,targetDate,sleepEventsFromAlgorithm,alarmEventsOptional,feedbackWakeTime);

        // WAKE UP , etc.
        for(final Optional<Event> sleepEventOptional: updatedSleepEvents){
            if(sleepEventOptional.isPresent()){
                timelineEvents.put(sleepEventOptional.get().getStartTimestamp(), sleepEventOptional.get());
            }
        }


        // PARTNER MOTION
        final List<PartnerMotionEvent> partnerMotionEvents = getPartnerMotionEvents(updatedSleepEvents.get(1), updatedSleepEvents.get(2), motionEvents, accountId);
        for(PartnerMotionEvent partnerMotionEvent : partnerMotionEvents) {
            timelineEvents.put(partnerMotionEvent.getStartTimestamp(), partnerMotionEvent);
        }




        // TODO: SOUND


        final List<Event> eventsWithSleepEvents = TimelineRefactored.mergeEvents(timelineEvents);
        final List<Event> smoothedEvents = TimelineUtils.smoothEvents(eventsWithSleepEvents);

        final List<Event> cleanedUpEvents = TimelineUtils.removeMotionEventsOutsideBedPeriod(smoothedEvents,
                updatedSleepEvents.get(0),
                updatedSleepEvents.get(3));

        final List<Event> greyEvents = TimelineUtils.greyNullEventsOutsideBedPeriod(cleanedUpEvents,
                updatedSleepEvents.get(0),
                updatedSleepEvents.get(3));

        final List<SleepSegment> sleepSegments = TimelineUtils.eventsToSegments(greyEvents);

        final int lightSleepThreshold = 70; // TODO: Generate dynamically instead of hard threshold
        final SleepStats sleepStats = TimelineUtils.computeStats(sleepSegments, lightSleepThreshold);
        final List<SleepSegment> reversed = Lists.reverse(sleepSegments);


        Integer sleepScore = computeAndMaybeSaveScore(trackerMotions.get(0).offsetMillis, targetDate, accountId, sleepStats);

        if(sleepStats.sleepDurationInMinutes < MIN_SLEEP_DURATION_FOR_SLEEP_SCORE_IN_MINUTES) {
            LOGGER.warn("Score for account id {} was set to zero because sleep duration is too short ({} min)", accountId, sleepStats.sleepDurationInMinutes);
            sleepScore = 0;
        }

        final Boolean reportSleepDuration = false;
        final String timeLineMessage = TimelineUtils.generateMessage(sleepStats, reportSleepDuration);

        LOGGER.debug("Score for account_id = {} is {}", accountId, sleepScore);


        final List<Insight> insights = TimelineUtils.generatePreSleepInsights(allSensorSampleList, sleepStats.sleepTime, accountId);
        final List<SleepSegment>  reversedSegments = Lists.reverse(reversed);
        final Timeline timeline = Timeline.create(sleepScore, timeLineMessage, date, reversedSegments, insights, sleepStats);

        return Lists.newArrayList(timeline);
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



    static private double getLocalTimeInFloatingPointHoursFromEvent(final Event event) {
        final DateTime dateTime  = new DateTime(event.getStartTimestamp());
        final DateTimeZone zone = DateTimeZone.forOffsetMillis(event.getTimezoneOffset());
        final DateTime withZone = dateTime.withZone(zone);
        final LocalDateTime local = withZone.toLocalDateTime();


        final double time_in_hours_local_time =
                ((double)local.getHourOfDay()) +
                ((double)local.getMinuteOfHour()) / 60.0; //which hour, floating point.

        return time_in_hours_local_time;
    }


    static private double addHours(final double h1, final double h2) {

        //check for wrapping
        double h3 = h1 + h2;
        if (h3 > 12.0) {
            h3 -= 24.0;
        }

        if (h3 < -12.0) {
            h3 += 24.0;
        }

        return h3;
    }

    /*
     * Bayes' magic
     * @param targetDate
     * @param predictions sleep predictions, list of optional events
     * @param alarmTime optional list of events (confusing!)
     * @param wakeFeedbackTime time user said they woke up
     * @return
     */
    private final List<Optional<Event>>  BayesUpdate(final Long accountId,final DateTime targetDate,final List<Optional<Event>> predictions, final Optional<List<Event>> alarmTime, final Optional<DateTime> wakeFeedbackTime) {


        //TODO put hard-coded values somewhere else
        //units are in hours
        final double K_MIN_SIGMA_OF_PREDICTION = 0.5;
        final double K_MIN_SIGMA_OF_BIAS = 0.1;

        final double K_PREDICTION_SIGMA = 0.5;
        final double K_ALARM_SIGMA = 0.17;

        ArrayList<Optional<Event>> updatedSleepEvents = new ArrayList<Optional<Event>>();

        Optional<Event> newWakeEvent = Optional.absent();

        WakeProbabilityDistributions latestDayDist = this.defaultWakeDistribution;

        //go back a week, stop if you find a day that had something
        for (int i = 1; i < 7; i++) {
            Optional<WakeProbabilityDistributions> prevDist = this.sleepPriorsDAO.getWakeDistributionByDay(accountId,targetDate.minusDays(i));

            if (prevDist.isPresent()) {
                latestDayDist = prevDist.get();
                break;
            }

        }

        GaussianDistributionDataModel wakeTimePosterior = latestDayDist.wakeTimePosterior;
        GaussianDistributionDataModel predictionBiasPosterior = latestDayDist.predictionBiasPosterior;

        WakeProbabilityDistributions todaysDistributions = latestDayDist;

        //iterate through all predictions, looking for wake

        for (Optional<Event> ev : predictions) {
            if (ev.isPresent()) {
                if (ev.get().getType() == Event.Type.WAKE_UP) {
                    Event wake = ev.get();
                    boolean isInferingWakeFromPrediction = true;

                    final double wakePredictionTimeInHoursLocalTime = getLocalTimeInFloatingPointHoursFromEvent(wake);

                    //infer wake up from prediction if no alarm of feedback is present

                    //if wake is present with feedback, or alarm, then we will try and estimate the prediction bias
                    if (alarmTime.isPresent() && alarmTime.get().size() > 0) {
                        isInferingWakeFromPrediction = false;

                        //FIND ALARM THAT HAS OCCURRED -- so it must be in the past.  Assume this is already taken care of.
                        //MULTIPLE ALARMS?  Agh.  Just pick the latest one.
                        //ASSUME last one was the latest.  Is this true?
                        List<Event> alarmTimesList  = alarmTime.get();

                        final Event alarm_time = alarmTimesList.get(alarmTimesList.size()-1);

                        final double alarmTimeHours = getLocalTimeInFloatingPointHoursFromEvent(alarm_time);



                        //TODO perform sanity check on likeliness of alarm given prior.
                        //if disagreement, then do what?  It's something uncharacteristic of the user.

                        //perform inference on posterior
                        wakeTimePosterior = new GaussianDistributionDataModel(
                                GaussianInference.GetInferredDistribution(latestDayDist.wakeTimePosterior.asGaussian(),
                                        alarmTimeHours, K_ALARM_SIGMA, K_MIN_SIGMA_OF_PREDICTION)
                        );

                        final double predictionBias = addHours(alarmTimeHours,-wakePredictionTimeInHoursLocalTime);


                        //TODO perform sanity check on likeliness of prediction bias given prior.
                        //if disagreement, then do what? reject measurement.

                        //perform inference on posterior of bias estimate
                        predictionBiasPosterior = new GaussianDistributionDataModel(
                                GaussianInference.GetInferredDistribution(latestDayDist.predictionBiasPosterior.asGaussian(),
                                        alarmTimeHours, K_ALARM_SIGMA, K_MIN_SIGMA_OF_BIAS)
                        );

                    }


                    // TODO implement wake-time feedback
                    if (wakeFeedbackTime.isPresent()) {
                      //  isInferingWakeFromPrediction = false;
                    }

                    //infer on wake prediction, because an alarm did not sound
                    if (isInferingWakeFromPrediction) {

                        //add predicted bias compensation to prior
                        final GaussianDistribution priorWithoutBiasCompensation = latestDayDist.wakeTimePosterior.asGaussian();

                        final double unBiasedMean = addHours(latestDayDist.predictionBiasPosterior.mean,priorWithoutBiasCompensation.mean);


                        GaussianDistribution priorWithBiasCompensation = new GaussianDistribution(unBiasedMean,
                                priorWithoutBiasCompensation.sigma,priorWithoutBiasCompensation.alpha,priorWithoutBiasCompensation.beta,
                                priorWithoutBiasCompensation.kappa,priorWithoutBiasCompensation.modelType);

                        //inference
                        wakeTimePosterior = new GaussianDistributionDataModel(
                                GaussianInference.GetInferredDistribution(priorWithBiasCompensation,
                                        wakePredictionTimeInHoursLocalTime, K_PREDICTION_SIGMA, K_MIN_SIGMA_OF_PREDICTION)
                        );

                    }


                    //compute new wake time in absolute terms
                    //B - A = C ---> A + C = B
                    final double timeDeltaFromPredictionToPosteriorInHours = addHours(-getLocalTimeInFloatingPointHoursFromEvent(wake),wakeTimePosterior.mean);
                    final long timeDeltaInMillis = (long)(timeDeltaFromPredictionToPosteriorInHours * 3600 * 1000);
                    final long newStartTimestamp = wake.getStartTimestamp() + timeDeltaInMillis;
                    final long newEndTimestamp = newStartTimestamp + 1 * DateTimeConstants.MILLIS_PER_MINUTE;

                    newWakeEvent = Optional.of((Event)new WakeupEvent(newStartTimestamp,newEndTimestamp,wake.getTimezoneOffset()));


                    //copy over posteriors and priors
                    todaysDistributions = new WakeProbabilityDistributions(
                                                        latestDayDist.predictionBiasPosterior,
                                                        latestDayDist.wakeTimePosterior,
                                                        predictionBiasPosterior,
                                                        wakeTimePosterior);

                    LOGGER.debug(String.format("new posterior wake time %f %f    %f, %f",
                            latestDayDist.wakeTimePosterior.mean, latestDayDist.wakeTimePosterior.sigma,
                            wakeTimePosterior.mean, wakeTimePosterior.sigma));



                }
            }
        }



        this.sleepPriorsDAO.updateWakeProbabilityDistributions(accountId,targetDate,todaysDistributions);

        //repopulate, substituting the new wake event
        for (Optional<Event> ev : predictions) {
            //if (ev.isPresent() && ev.get().getType() == Event.Type.WAKE_UP && newWakeEvent.isPresent()) {
            //    updatedSleepEvents.add(newWakeEvent);
            //}
            //else {
                updatedSleepEvents.add(ev);
            //}
        }

        return updatedSleepEvents;
    }

    /**
     * Pang magic
     * @param targetDate
     * @param trackerMotions
     * @param lightOutTimeOptional
     * @param wakeUpWaveTimeOptional
     * @return
     */
    private List<Optional<Event>> fromAlgorithm(final DateTime targetDate, final List<TrackerMotion> trackerMotions, final Optional<DateTime> lightOutTimeOptional, final Optional<DateTime> wakeUpWaveTimeOptional) {
        Optional<Segment> sleepSegmentOptional;
        Optional<Segment> inBedSegmentOptional = Optional.absent();
        List<Optional<Event>> sleepEventsFromAlgorithm = new ArrayList<>();
        for(int i = 0; i < 4; i++){
            sleepEventsFromAlgorithm.add(Optional.<Event>absent());
        }

        // A day starts with 8pm local time and ends with 4pm local time next day
        try {
            sleepEventsFromAlgorithm = TimelineUtils.getSleepEvents(targetDate,
                    trackerMotions,
                    lightOutTimeOptional,
                    wakeUpWaveTimeOptional,
                    MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                    MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                    MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                    false);



            if(sleepEventsFromAlgorithm.get(1).isPresent() && sleepEventsFromAlgorithm.get(2).isPresent()){
                sleepSegmentOptional = Optional.of(new Segment(sleepEventsFromAlgorithm.get(1).get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.get(2).get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.get(2).get().getTimezoneOffset()));

                LOGGER.info("Sleep Time From Awake Detection Algorithm: {} - {}",
                        new DateTime(sleepSegmentOptional.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegmentOptional.get().getOffsetMillis())),
                        new DateTime(sleepSegmentOptional.get().getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepSegmentOptional.get().getOffsetMillis())));
            }

            if(sleepEventsFromAlgorithm.get(0).isPresent() && sleepEventsFromAlgorithm.get(3).isPresent()){
                inBedSegmentOptional = Optional.of(new Segment(sleepEventsFromAlgorithm.get(0).get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.get(3).get().getStartTimestamp(),
                        sleepEventsFromAlgorithm.get(3).get().getTimezoneOffset()));
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
     * Sleep score
     * @param userOffsetMillis
     * @param targetDate
     * @param accountId
     * @param sleepStats
     * @return
     */
    private Integer computeAndMaybeSaveScore(final Integer userOffsetMillis, final DateTime targetDate, final Long accountId, final SleepStats sleepStats) {
        // get scores - check dynamoDB first
        final String targetDateString = DateTimeUtil.dateToYmdString(targetDate);

        final AggregateScore targetDateScore = this.aggregateSleepScoreDAODynamoDB.getSingleScore(accountId, targetDateString);
        Integer sleepScore = targetDateScore.score;

        if (sleepScore == 0) {
            // score may not have been computed yet, recompute
            sleepScore = sleepScoreDAO.getSleepScoreForNight(accountId, targetDate.withTimeAtStartOfDay(),
                    userOffsetMillis, this.dateBucketPeriod, sleepLabelDAO);

            final DateTime lastNight = new DateTime(DateTime.now(), DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1);
            if (targetDate.isBefore(lastNight)) {
                // write data to Dynamo if targetDate is old
                this.aggregateSleepScoreDAODynamoDB.writeSingleScore(
                        new AggregateScore(accountId,
                                sleepScore,
                                DateTimeUtil.dateToYmdString(targetDate.withTimeAtStartOfDay()),
                                targetDateScore.scoreType, targetDateScore.version));

                // add sleep-score and duration to day-of-week, over time tracking table
                if (sleepScore > 0) {
                    this.trendsInsightsDAO.updateDayOfWeekData(accountId, sleepScore, targetDate.withTimeAtStartOfDay(), userOffsetMillis, TrendGraph.DataType.SLEEP_SCORE);
                }

                if (sleepStats.sleepDurationInMinutes > 0) {
                    this.trendsInsightsDAO.updateSleepStats(accountId, userOffsetMillis, targetDate.withTimeAtStartOfDay(), sleepStats);
                }
            }
        }

        return sleepScore;
    }

    private static void writeMotionMetrics(final Histogram histogram, final List<Event> alignedAndConvertedEvents){
        int count = 0;
        for(final Event event:alignedAndConvertedEvents){
            if(event.getType() == Event.Type.MOTION){
                count++;
            }
        }

        histogram.update(count);
    }
}
