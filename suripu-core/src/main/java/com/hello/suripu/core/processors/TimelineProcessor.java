package com.hello.suripu.core.processors;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.RingTimeDAODynamoDB;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.TimelineDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.AlarmEvent;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.FallingAsleepEvent;
import com.hello.suripu.core.models.Events.SunRiseEvent;
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
import com.hello.suripu.core.util.TimelineUtils;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Histogram;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimelineProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineProcessor.class);

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
                            final RingTimeDAODynamoDB ringTimeDAODynamoDB) {
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
    }


    public List<List<Timeline>> batchProcessTimelines(final Map<Long, Set<DateTime>> groupedAccountIdTargetDateLocalUTC,
                                                      final Map<Long, Integer> accountIdDefaultSensorValues,
                                                      final long sleepBetweenEachProcessMillis,
                                                      final TimelineDAODynamoDB timelineDAODynamoDB,
                                                      final boolean hasAlarmInTimeline){
        final ArrayList<List<Timeline>> batchedResult = new ArrayList<>();
        for(final Long accountId:groupedAccountIdTargetDateLocalUTC.keySet()){
            final Set<DateTime> targetDatesLocalUTC = groupedAccountIdTargetDateLocalUTC.get(accountId);
            for(final DateTime targetDateLocalUTC:targetDatesLocalUTC) {

                final List<Timeline> timelines = retrieveTimelines(accountId,
                        targetDateLocalUTC.toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT),
                        accountIdDefaultSensorValues.get(accountId), hasAlarmInTimeline);
                batchedResult.add(timelines);
                try {
                    Thread.sleep(sleepBetweenEachProcessMillis);
                } catch (InterruptedException e) {
                    LOGGER.error("Fail to sleep {} millis", sleepBetweenEachProcessMillis);
                }


                try {
                    timelineDAODynamoDB.saveTimelinesForDate(accountId, targetDateLocalUTC, timelines);
                    LOGGER.info("Timeline at {} saved for account {}.", targetDateLocalUTC, accountId);
                } catch (AmazonServiceException aex) {
                    LOGGER.error("AWS error, save timeline for account {} failed, error {}", accountId, aex.getMessage());
                } catch (Exception ex) {
                    LOGGER.error("Save timeline for account {} failed, error {}", accountId, ex.getMessage());
                }
            }
        }
        return batchedResult;
    }


    private List<Event> getAlarmEvents(final Long accountId, final DateTime evening, final DateTime morning, final Integer offsetMillis) {

        final List<DeviceAccountPair> pairs = deviceDAO.getDeviceAccountMapFromAccountId(accountId);
        if(pairs.size() > 1) {
            LOGGER.info("Account {} has several sense paired. Not displaying alarm event", accountId);
            return Collections.EMPTY_LIST;
        }
        if(pairs.isEmpty()) {
            LOGGER.warn("Account {} doesn’t have any Sense paired. ", accountId);
            return Collections.EMPTY_LIST;
        }
        final String senseId = pairs.get(0).externalDeviceId;

        final List<RingTime> ringTimes = ringTimeDAODynamoDB.getRingTimesBetween(senseId, evening, morning);

        final List<Event> events = Lists.newArrayList();
        for(final RingTime ringTime : ringTimes) {
            final AlarmEvent event = (AlarmEvent) Event.createFromType(
                    Event.Type.ALARM,
                    ringTime.expectedRingTimeUTC,
                    new DateTime(ringTime.expectedRingTimeUTC, DateTimeZone.UTC).plusMinutes(1).getMillis(),
                    offsetMillis,
                    Optional.<String>absent(),
                    Optional.<SleepSegment.SoundInfo>absent(),
                    Optional.<Integer>absent());
            events.add(event);
        }
        return events;
    }

    public List<Timeline> retrieveTimelines(final Long accountId, final String date, final Integer missingDataDefaultValue, final Boolean hasAlarmInTimeline) {
        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(16);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        // TODO: compute this threshold dynamically
        final int threshold = 10; // events with scores < threshold will be considered motion events
        final int mergeThreshold = 1; // min segment size is 1 minute

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenLocalUTC(accountId, targetDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());

        if(trackerMotions.isEmpty()) {
            LOGGER.debug("No data for account_id = {} and day = {}", accountId, targetDate);
            final Timeline timeline = new Timeline(0, "No sleep data recorded", date, new ArrayList<SleepSegment>(), new ArrayList<Insight>());
            final List<Timeline> timelines = new ArrayList<>();
            timelines.add(timeline);
            return timelines;
        }

        final List<Event> events = new LinkedList<>();
        final ArrayList<Event> sleepEvents = new ArrayList<>();

        // get all sensor data, used for light and sound disturbances, and presleep-insights
        Optional<AllSensorSampleList> optionalSensorData = Optional.absent();

        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if (deviceId.isPresent()) {
            final int slotDurationMins = 1;

            optionalSensorData = deviceDataDAO.generateTimeSeriesByLocalTimeAllSensors(
                    targetDate.getMillis(), endDate.getMillis(),
                    accountId, deviceId.get(), slotDurationMins, missingDataDefaultValue);
        }

        // compute lights-out events
        Optional<DateTime> lightOutTimeOptional = Optional.absent();
        if (optionalSensorData.isPresent()) {
            final List<Event> lightEvents = TimelineUtils.getLightEvents(optionalSensorData.get().getData(Sensor.LIGHT));

            if (lightEvents.size() > 0) {
                events.addAll(lightEvents);
                lightOutTimeOptional = TimelineUtils.getLightsOutTime(lightEvents);
            }
        }

        if(lightOutTimeOptional.isPresent()){
            LOGGER.info("Light out at {}", lightOutTimeOptional.get());
        } else {
            LOGGER.info("No light out");
        }

        if(hasAlarmInTimeline) {
            final List<Event> alarmEvents = getAlarmEvents(accountId, targetDate, endDate, trackerMotions.get(0).offsetMillis);
            events.addAll(alarmEvents);
        }


        // create sleep-motion segments
        final List<MotionEvent> motionEvents = TimelineUtils.generateMotionEvents(trackerMotions);
        events.addAll(motionEvents);

        Optional<Segment> sleepSegment = Optional.absent();
        // A day starts with 8pm local time and ends with 4pm local time next day
        try {
            final List<Event> sleepEventsFromAlgorithm = TimelineUtils.getSleepEvents(targetDate,
                    trackerMotions, lightOutTimeOptional,
                    MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                    false);
            final FallingAsleepEvent fallingAsleepEvent = (FallingAsleepEvent) sleepEventsFromAlgorithm.get(1);
            final WakeupEvent wakeupEvent = (WakeupEvent) sleepEventsFromAlgorithm.get(2);

            if(wakeupEvent.getStartTimestamp() - fallingAsleepEvent.getStartTimestamp() > 3 * DateTimeConstants.MILLIS_PER_HOUR){
                sleepSegment = Optional.of(new Segment(fallingAsleepEvent.getStartTimestamp(),
                        wakeupEvent.getStartTimestamp(),
                        wakeupEvent.getTimezoneOffset()));

                sleepEvents.addAll(sleepEventsFromAlgorithm);
                LOGGER.info("Sleep Time From Awake Detection Algorithm: {} - {}",
                        new DateTime(sleepSegment.get().getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.get().getOffsetMillis())),
                        new DateTime(sleepSegment.get().getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.get().getOffsetMillis())));
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
            if(sleepSegment.isPresent()){
                startTime = new DateTime(sleepSegment.get().getStartTimestamp(), DateTimeZone.UTC);
            }
            final DateTime endTime = new DateTime(events.get(events.size() - 1).getStartTimestamp(), DateTimeZone.UTC);

            final List<TrackerMotion> partnerMotions = this.trackerMotionDAO.getBetween(optionalPartnerAccountId.get(), startTime, endTime);
            if (partnerMotions.size() > 0) {
                // use un-normalized data segments for comparison
                events.addAll(PartnerMotion.getPartnerData(motionEvents, partnerMotions, threshold));
            }
        }

        // add sunrise data
        final String sunRiseQueryDateString = targetDate.plusDays(1).toString(DateTimeFormat.forPattern("yyyy-MM-dd"));
        final Optional<DateTime> sunrise = sunData.sunrise(sunRiseQueryDateString); // day + 1
        if(sunrise.isPresent() && sleepSegment.isPresent()) {
            final long sunRiseMillis = sunrise.get().getMillis();
            final SunRiseEvent sunriseEvent = new SunRiseEvent(sunRiseMillis,
                    sunRiseMillis + DateTimeConstants.MILLIS_PER_MINUTE,
                    sleepSegment.get().getOffsetMillis(), 0, null);

            // TODO: ADD Feature flipper here
//            if(feature.userFeatureActive(FeatureFlipper.SOUND_INFO_TIMELINE, accountId, new ArrayList<String>())) {
//                final Date expiration = new java.util.Date();
//                long msec = expiration.getTime();
//                msec += 1000 * 60 * 60; // 1 hour.
//                expiration.setTime(msec);
//                final URL url = s3.generatePresignedUrl(bucketName, "mario.mp3", expiration, HttpMethod.GET);
//                final SleepSegment.SoundInfo sunRiseSound = new SleepSegment.SoundInfo(url.toExternalForm(), 2000);
//                sunriseEvent.setSoundInfo(sunRiseSound);
//            }
            events.add(sunriseEvent);
            LOGGER.debug(sunriseEvent.getDescription());
        }else{
            LOGGER.warn("No sun rise data for date {}", sunRiseQueryDateString);
        }

        // TODO: add sound


        // merge similar segments (by motion & event-type), then categorize
//        final List<SleepSegment> mergedSegments = TimelineUtils.mergeConsecutiveSleepSegments(segments, mergeThreshold);
        final List<Event> mergedEvents = TimelineUtils.generateAlignedSegmentsByTypeWeight(events, DateTimeConstants.MILLIS_PER_MINUTE, 15, false);
        final List<Event> convertedEvents = TimelineUtils.convertLightMotionToNone(mergedEvents, threshold);
        writeMotionMetrics(this.motionEventDistribution, convertedEvents);
        final List<Event> smoothedEvents = TimelineUtils.smoothEvents(convertedEvents);
        List<Event> eventsWithSleepEvents = smoothedEvents;
        if(sleepSegment.isPresent()) {
            for (final Event sleepEvent : sleepEvents){
                eventsWithSleepEvents = TimelineUtils.insertOneMinuteDurationEvents(eventsWithSleepEvents, sleepEvent);
            }
        }
        final List<Event> cleanedUpEvents = TimelineUtils.removeMotionEventsOutsideBedPeriod(eventsWithSleepEvents);
        List<SleepSegment> sleepSegments = TimelineUtils.eventsToSegments(cleanedUpEvents);

        final int lightSleepThreshold = 70; // todo: configurable
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

        final Boolean reportSleepDuration = false;
        final String timeLineMessage = TimelineUtils.generateMessage(sleepStats, reportSleepDuration);

        LOGGER.debug("Score for account_id = {} is {}", accountId, sleepScore);


        final List<Insight> insights = TimelineUtils.generatePreSleepInsights(optionalSensorData, sleepStats.sleepTime);
        List<SleepSegment>  reversedSegments = Lists.reverse(reversed);
        final Timeline timeline = new Timeline(sleepScore, timeLineMessage, date, reversedSegments, insights);
        final List<Timeline> timelines = new ArrayList<>();
        timelines.add(timeline);

        return timelines;

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
