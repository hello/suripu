package com.hello.suripu.core.processors;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.SleepHmmDAODynamoDB;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.PartnerMotionEvent;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.Insights.TrendGraph;
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
import com.hello.suripu.core.util.SleepHmmWithInterpretation;
import com.hello.suripu.core.util.PartnerDataUtils;
import com.hello.suripu.core.util.SunData;
import com.hello.suripu.core.util.TimelineRefactored;
import com.hello.suripu.core.util.TimelineUtils;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Histogram;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB;
    private final int dateBucketPeriod;
    private final SunData sunData;
    private final AmazonS3 s3;
    private final String bucketName;
    private final RingTimeDAODynamoDB ringTimeDAODynamoDB;
    private final Histogram motionEventDistribution;
    private final FeedbackDAO feedbackDAO;
    private final SleepHmmDAODynamoDB sleepHmmDAODynamoDB;

    public TimelineProcessor(final TrackerMotionDAO trackerMotionDAO,
                            final AccountDAO accountDAO,
                            final DeviceDAO deviceDAO,
                            final DeviceDataDAO deviceDataDAO,
                            final SleepLabelDAO sleepLabelDAO,
                            final SleepScoreDAO sleepScoreDAO,
                            final TrendsInsightsDAO trendsInsightsDAO,
                            final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB,
                            final int dateBucketPeriod,
                            final SunData sunData,
                            final AmazonS3 s3,
                            final String bucketName,
                            final RingTimeDAODynamoDB ringTimeDAODynamoDB,
                            final FeedbackDAO feedbackDAO,
                            final SleepHmmDAODynamoDB sleepHmmDAODynamoDB) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.accountDAO = accountDAO;
        this.deviceDAO = deviceDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.sleepLabelDAO = sleepLabelDAO;
        this.sleepScoreDAO = sleepScoreDAO;
        this.trendsInsightsDAO = trendsInsightsDAO;
        this.aggregateSleepScoreDAODynamoDB = aggregateSleepScoreDAODynamoDB;
        this.dateBucketPeriod = dateBucketPeriod;
        this.sunData = sunData;
        this.s3 = s3;
        this.bucketName = bucketName;
        this.motionEventDistribution = Metrics.defaultRegistry().newHistogram(TimelineProcessor.class, "motion_event_distribution");
        this.ringTimeDAODynamoDB = ringTimeDAODynamoDB;
        this.feedbackDAO = feedbackDAO;
        this.sleepHmmDAODynamoDB = sleepHmmDAODynamoDB;
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
        int numPartnerMotion = 0;
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
                numPartnerMotion = partnerMotionEvents.size();
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
        final String timeLineMessage = TimelineUtils.generateMessage(sleepStats, numPartnerMotion, 0, reportSleepDuration);

        LOGGER.debug("Score for account_id = {} is {}", accountId, sleepScore);


        final List<Insight> insights = TimelineUtils.generatePreSleepInsights(allSensorSampleList, sleepStats.sleepTime, accountId);
        final List<SleepSegment>  reversedSegments = Lists.reverse(reversed);
        final Timeline timeline = Timeline.create(sleepScore, timeLineMessage, date, reversedSegments, insights, sleepStats);

        return Lists.newArrayList(timeline);

    }


    public List<Timeline> retrieveTimelinesFast(final Long accountId, final String date, final Integer missingDataDefaultValue,
                                                final Boolean hasAlarmInTimeline,
                                                final Boolean hasSoundInTimeline,
                                                final Boolean hasFeedbackInTimelineEnabled) {


        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        // TODO: compute this threshold dynamically
        final int threshold = 10; // events with scores < threshold will be considered motion events

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

        List<TrackerMotion> trackerMotions = new ArrayList<>();

        /* PARTNER FILTERING --THIS NEEDS TO BE FEATURE FLIPPED */
        if (!partnerMotions.isEmpty() && false) {
            try {
                trackerMotions.addAll(PartnerDataUtils.getMyMotion(originalTrackerMotions, partnerMotions));
            }
            catch (Exception e) {
                LOGGER.info(e.getMessage());
                trackerMotions.addAll(originalTrackerMotions);
            }
        } else {
            trackerMotions.addAll(originalTrackerMotions);
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


        Optional <SleepHmmWithInterpretation> hmmOptional = sleepHmmDAODynamoDB.getLatestModelForDate(accountId,targetDate.getMillis());

        Optional <SleepHmmWithInterpretation.SleepHmmResult> optionalHmmPredictions = Optional.absent();

        if (hmmOptional.isPresent()) {
            optionalHmmPredictions = Optional.of(hmmOptional.get().getSleepEventsUsingHMM(allSensorSampleList,trackerMotions));
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

        final Integer offsetMillis = trackerMotions.get(0).offsetMillis;
        final Map<Event.Type, Event> feedbackEvents = fromFeedback(accountId, targetDate, offsetMillis, hasFeedbackInTimelineEnabled);
        for(final Event event : feedbackEvents.values()) {
            LOGGER.info("Overriding {} with {} for account {}", event.getType().name(), event, accountId);
            timelineEvents.put(event.getStartTimestamp(), event);
        }

        List<Optional<Event>> sleepEventsFromAlgorithm = fromAlgorithm(targetDate, trackerMotions, lightOutTimeOptional, wakeUpWaveTimeOptional);


        // WAKE UP , etc.
        if (optionalHmmPredictions.isPresent()) {
            //MAN WHAT A HACK -- let's clean this up sometime -- BEJ
            //if the HMM is here (i.e. eventually feature-flipped, we use its predictions)
            List<Optional<Event>> eventsList = new ArrayList<Optional<Event>>();
            eventsList.add(optionalHmmPredictions.get().inBed);
            eventsList.add(optionalHmmPredictions.get().fallAsleep);
            eventsList.add(optionalHmmPredictions.get().wakeUp);
            eventsList.add(optionalHmmPredictions.get().outOfBed);

            sleepEventsFromAlgorithm = eventsList;

        }

        for (final Optional<Event> sleepEventOptional : sleepEventsFromAlgorithm) {
            if (sleepEventOptional.isPresent() && !feedbackEvents.containsKey(sleepEventOptional.get().getType())) {
                timelineEvents.put(sleepEventOptional.get().getStartTimestamp(), sleepEventOptional.get());
            }
        }






        // PARTNER MOTION
        final List<PartnerMotionEvent> partnerMotionEvents = getPartnerMotionEvents(sleepEventsFromAlgorithm.get(1), sleepEventsFromAlgorithm.get(2), motionEvents, accountId);
        for(PartnerMotionEvent partnerMotionEvent : partnerMotionEvents) {
            timelineEvents.put(partnerMotionEvent.getStartTimestamp(), partnerMotionEvent);
        }
        final int numPartnerMotion = partnerMotionEvents.size();




        // ALARM
        if(hasAlarmInTimeline) {
            final List<Event> alarmEvents = getAlarmEvents(accountId, targetDate, endDate, trackerMotions.get(0).offsetMillis);
            for(final Event event : alarmEvents) {
                timelineEvents.put(event.getStartTimestamp(), event);
            }
        }

        // SOUND
        int numSoundEvents = 0;
        if (hasSoundInTimeline) {
            final List<Event> soundEvents = getSoundEvents(allSensorSampleList.get(Sensor.SOUND_PEAK_DISTURBANCE),
                    motionEvents, lightOutTimeOptional, sleepEventsFromAlgorithm);
            for (final Event event : soundEvents) {
                timelineEvents.put(event.getStartTimestamp(), event);
            }
            numSoundEvents = soundEvents.size();
        }

        final List<Event> eventsWithSleepEvents = TimelineRefactored.mergeEvents(timelineEvents);
        final List<Event> smoothedEvents = TimelineUtils.smoothEvents(eventsWithSleepEvents);

        final List<Event> cleanedUpEvents = TimelineUtils.removeMotionEventsOutsideBedPeriod(smoothedEvents,
                sleepEventsFromAlgorithm.get(0),
                sleepEventsFromAlgorithm.get(3));

        final List<Event> greyEvents = TimelineUtils.greyNullEventsOutsideBedPeriod(cleanedUpEvents,
                sleepEventsFromAlgorithm.get(0),
                sleepEventsFromAlgorithm.get(3));

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
        final String timeLineMessage = TimelineUtils.generateMessage(sleepStats, numPartnerMotion, numSoundEvents, reportSleepDuration);

        LOGGER.debug("Score for account_id = {} is {}", accountId, sleepScore);


        final List<Insight> insights = TimelineUtils.generatePreSleepInsights(allSensorSampleList, sleepStats.sleepTime, accountId);
        final List<SleepSegment>  reversedSegments = Lists.reverse(reversed);
        final Timeline timeline = Timeline.create(sleepScore, timeLineMessage, date, reversedSegments, insights, sleepStats);

        return Lists.newArrayList(timeline);
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
                                       final List<Optional<Event>> sleepEventsFromAlgorithm) {
        if (soundSamples.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        // TODO: refactor - ¡don't doubt it!
        Optional<DateTime> optionalSleepTime = Optional.absent();
        Optional<DateTime> optionalAwakeTime = Optional.absent();

        if (sleepEventsFromAlgorithm.get(1).isPresent()) {
            // sleep time
            final Event event = sleepEventsFromAlgorithm.get(1).get();
            optionalSleepTime = Optional.of(new DateTime(event.getStartTimestamp(),
                    DateTimeZone.UTC).plusMillis(event.getTimezoneOffset()));
        } else if (sleepEventsFromAlgorithm.get(0).isPresent()) {
            // in-bed time
            final Event event = sleepEventsFromAlgorithm.get(0).get();
            optionalSleepTime = Optional.of(new DateTime(event.getStartTimestamp(),
                    DateTimeZone.UTC).plusMillis(event.getTimezoneOffset()));
        }

        if (sleepEventsFromAlgorithm.get(2).isPresent()) {
            // awake time
            final Event event = sleepEventsFromAlgorithm.get(2).get();
            optionalAwakeTime = Optional.of(new DateTime(event.getStartTimestamp(),
                    DateTimeZone.UTC).plusMillis(event.getTimezoneOffset()));
        } else if (sleepEventsFromAlgorithm.get(3).isPresent()) {
            // out-of-bed time
            final Event event = sleepEventsFromAlgorithm.get(2).get();
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


    private Map<Event.Type, Event> fromFeedback(final Long accountId, final DateTime nightOf, final Integer offsetMillis, Boolean enabled) {
        if(!enabled) {
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
