package com.hello.suripu.core.processors;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsDAO;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.InBedEvent;
import com.hello.suripu.core.models.Events.MotionEvent;
import com.hello.suripu.core.models.Events.OutOfBedEvent;
import com.hello.suripu.core.models.Events.SleepEvent;
import com.hello.suripu.core.models.Events.SunRiseEvent;
import com.hello.suripu.core.models.Events.WakeupEvent;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.Insights.TrendGraph;
import com.hello.suripu.core.models.Sample;
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
import java.util.LinkedList;
import java.util.List;

public class TimelineProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineProcessor.class);

    private final AccountDAO accountDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final DeviceDAO deviceDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final SleepScoreDAO sleepScoreDAO;
    private final SleepLabelDAO sleepLabelDAO;
    private final TrendsDAO trendsDAO;
    private final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB;
    private final int dateBucketPeriod;
    private final SunData sunData;
    private final AmazonS3 s3;
    private final String bucketName;
    private final Histogram motionEventDistribution;

    public TimelineProcessor(final TrackerMotionDAO trackerMotionDAO,
                            final AccountDAO accountDAO,
                            final DeviceDAO deviceDAO,
                            final DeviceDataDAO deviceDataDAO,
                            final SleepLabelDAO sleepLabelDAO,
                            final SleepScoreDAO sleepScoreDAO,
                            final TrendsDAO trendsDAO,
                            final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB,
                            final int dateBucketPeriod,
                            final SunData sunData,
                            final AmazonS3 s3,
                            final String bucketName) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.accountDAO = accountDAO;
        this.deviceDAO = deviceDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.sleepLabelDAO = sleepLabelDAO;
        this.sleepScoreDAO = sleepScoreDAO;
        this.trendsDAO = trendsDAO;
        this.aggregateSleepScoreDAODynamoDB = aggregateSleepScoreDAODynamoDB;
        this.dateBucketPeriod = dateBucketPeriod;
        this.sunData = sunData;
        this.s3 = s3;
        this.bucketName = bucketName;
        this.motionEventDistribution = Metrics.defaultRegistry().newHistogram(TimelineProcessor.class, "motion_event_distribution");
    }


    public List<Timeline> retrieveTimelines(Long accountId, String date) {
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
            final Timeline timeline = new Timeline(0, "You haven't been sleeping!", date, new ArrayList<SleepSegment>(), new ArrayList<Insight>());
            final List<Timeline> timelines = new ArrayList<>();
            timelines.add(timeline);
            return timelines;
        }

        final List<Event> events = new LinkedList<>();

        //TODO: get light data by the minute, compute lights out
        Optional<DateTime> lightOutTimeOptional = Optional.absent();
        final Optional<Long> deviceId = deviceDAO.getMostRecentSenseByAccountId(accountId);
        if (deviceId.isPresent()) {
            final int slotDurationMins = 1;

            final List<Sample> senseData = deviceDataDAO.generateTimeSeriesByLocalTime(targetDate.getMillis(),
                    endDate.getMillis(), accountId, deviceId.get(), slotDurationMins, "light");
            LOGGER.info("Light data size {}", senseData.size());
            if (senseData.size() > 0) {
                final List<Event> lightEvents = TimelineUtils.getLightEvents(senseData);
                if (lightEvents.size() > 0) {
                    events.addAll(lightEvents);
                    lightOutTimeOptional = TimelineUtils.getLightsOutTime(lightEvents);
                }
            }
        }

        if(lightOutTimeOptional.isPresent()){
            LOGGER.info("Light out at {}", lightOutTimeOptional.get());
        }else{
            LOGGER.info("No light out");
        }
        // create sleep-motion segments
        final List<MotionEvent> motionEvents = TimelineUtils.generateMotionEvents(trackerMotions);
        events.addAll(motionEvents);

        // A day starts with 8pm local time and ends with 4pm local time next day
        Segment sleepSegment = null;
        try {
            sleepSegment = TimelineUtils.getSleepPeriod(targetDate, trackerMotions, lightOutTimeOptional);

            if(sleepSegment.getDuration() > 3 * DateTimeConstants.MILLIS_PER_HOUR) {
                final SleepEvent sleepEventFromAwakeDetection = new SleepEvent(
                        sleepSegment.getStartTimestamp(),
                        sleepSegment.getStartTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                        sleepSegment.getOffsetMillis(),
                        "You fell asleep");

                final WakeupEvent wakeupSegmentFromAwakeDetection = new WakeupEvent(
                        sleepSegment.getEndTimestamp(),
                        sleepSegment.getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                        sleepSegment.getOffsetMillis());

                final InBedEvent inBedFromAwakeDetection = new InBedEvent(
                        sleepSegment.getStartTimestamp(),
                        sleepSegment.getStartTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                        sleepSegment.getOffsetMillis(),
                        "You went to bed");

                final OutOfBedEvent outOfBedFromAwakeDetection = new OutOfBedEvent(
                        sleepSegment.getEndTimestamp(),
                        sleepSegment.getEndTimestamp() + DateTimeConstants.MILLIS_PER_MINUTE,
                        sleepSegment.getOffsetMillis());

                // TODO: don't use sleep/awake until we have tune the algorithm....
                events.add(inBedFromAwakeDetection);
                events.add(outOfBedFromAwakeDetection);
//                events.add(sleepEventFromAwakeDetection);
//                events.add(wakeupSegmentFromAwakeDetection);
            }

            LOGGER.info("Sleep Time From Awake Detection Algorithm: {} - {}",
                    new DateTime(sleepSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis())),
                    new DateTime(sleepSegment.getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepSegment.getOffsetMillis())));
        }catch (Exception ex){
            LOGGER.error("Generate sleep period from Awake Detection Algorithm failed: {}", ex.getMessage());
        }

        // add partner movement data, check if there's a partner
        final Optional<Long> optionalPartnerAccountId = this.deviceDAO.getPartnerAccountId(accountId);
        if (optionalPartnerAccountId.isPresent() && events.size() > 0) {
            LOGGER.debug("partner account {}", optionalPartnerAccountId.get());
            // get tracker motions for partner, query time is in UTC, not local_utc
            DateTime startTime = new DateTime(events.get(0).getStartTimestamp(), DateTimeZone.UTC);
            if(sleepSegment != null){
                startTime = new DateTime(sleepSegment.getStartTimestamp(), DateTimeZone.UTC);
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
        if(sunrise.isPresent() && sleepSegment != null) {
            final long sunRiseMillis = sunrise.get().getMillis();
            final SunRiseEvent sunriseEvent = new SunRiseEvent(sunRiseMillis,
                    sunRiseMillis + DateTimeConstants.MILLIS_PER_MINUTE,
                    sleepSegment.getOffsetMillis(), 0, null);

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
        final List<Event> cleanedUpEvents = TimelineUtils.removeMotionEventsOutsideSleepPeriod(convertedEvents);

        List<SleepSegment> sleepSegments = TimelineUtils.eventsToSegments(cleanedUpEvents);

        final SleepStats sleepStats = TimelineUtils.computeStats(sleepSegments, 70);
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
                    this.trendsDAO.updateDayOfWeekData(accountId, sleepScore, targetDate.withTimeAtStartOfDay(), userOffsetMillis, TrendGraph.DataType.SLEEP_SCORE);
                }

                if (sleepStats.sleepDurationInMinutes > 0) {
                    this.trendsDAO.updateSleepStats(accountId, userOffsetMillis, targetDate.withTimeAtStartOfDay(), sleepStats);
                }
            }
        }

        final String timeLineMessage = TimelineUtils.generateMessage(sleepStats);

        LOGGER.debug("Score for account_id = {} is {}", accountId, sleepScore);


        final List<Insight> insights = TimelineUtils.generateRandomInsights(targetDate.getDayOfMonth());
        final Timeline timeline = new Timeline(sleepScore, timeLineMessage, date, reversed, insights);
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
