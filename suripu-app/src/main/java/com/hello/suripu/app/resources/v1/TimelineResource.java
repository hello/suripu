package com.hello.suripu.app.resources.v1;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.sleep.AwakeDetectionAlgorithm;
import com.hello.suripu.algorithm.sleep.SleepDetectionAlgorithm;
import com.hello.suripu.app.utils.TrackerMotionDataSource;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Insight;
import com.hello.suripu.core.models.SensorReading;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.PartnerMotion;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.SunData;
import com.hello.suripu.core.util.TimelineUtils;
import com.librato.rollout.RolloutClient;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Path("/v1/timeline")
public class TimelineResource extends BaseResource {

    @Inject
    RolloutClient feature;

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResource.class);

    private final TrackerMotionDAO trackerMotionDAO;
    private final DeviceDAO deviceDAO;
    private final SleepScoreDAO sleepScoreDAO;
    private final SleepLabelDAO sleepLabelDAO;
    private final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB;
    private final int dateBucketPeriod;
    private final SunData sunData;
    private final AmazonS3 s3;
    private final String bucketName;

    public TimelineResource(final TrackerMotionDAO trackerMotionDAO,
                            final DeviceDAO deviceDAO,
                            final SleepLabelDAO sleepLabelDAO,
                            final SleepScoreDAO sleepScoreDAO,
                            final AggregateSleepScoreDAODynamoDB aggregateSleepScoreDAODynamoDB,
                            final int dateBucketPeriod,
                            final SunData sunData,
                            final AmazonS3 s3,
                            final String bucketName) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.deviceDAO = deviceDAO;
        this.sleepLabelDAO = sleepLabelDAO;
        this.sleepScoreDAO = sleepScoreDAO;
        this.aggregateSleepScoreDAODynamoDB = aggregateSleepScoreDAODynamoDB;
        this.dateBucketPeriod = dateBucketPeriod;
        this.sunData = sunData;
        this.s3 = s3;
        this.bucketName = bucketName;
    }

    @Timed
    @Path("/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<Timeline> getTimelines(
            @Scope(OAuthScope.SLEEP_TIMELINE)final AccessToken accessToken,
            @PathParam("date") String date) {


        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(20);
        final DateTime endDate = targetDate.plusHours(14);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        // TODO: compute this threshold dynamically
        final int threshold = 10; // events with scores < threshold will be considered motion events
        final int mergeThreshold = 1; // min segment size is 1 minute

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenLocalUTC(accessToken.accountId, targetDate, endDate);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());

        if(trackerMotions.isEmpty()) {
            LOGGER.debug("No data for account_id = {} and day = {}", accessToken.accountId, targetDate);
            final Timeline timeline = new Timeline(0, "You haven't been sleeping!", date, new ArrayList<SleepSegment>(), new ArrayList<Insight>());
            final List<Timeline> timelines = new ArrayList<>();
            timelines.add(timeline);
            return timelines;
        }

        // create sleep-motion segments
        List<SleepSegment> segments = TimelineUtils.generateSleepSegments(trackerMotions, threshold, true);

        // detect sleep time
        final int sleepEventThreshold = 7; // minutes of no-movement to determine that user has fallen asleep
        final Optional<SleepSegment> sleepTimeSegment = TimelineUtils.computeSleepTime(segments, sleepEventThreshold);
        if(sleepTimeSegment.isPresent()) {
            segments.add(sleepTimeSegment.get());
        }

        // A day starts with 8pm local time and ends with 4pm local time next day
        final TrackerMotionDataSource dataSource = new TrackerMotionDataSource(trackerMotions, 20, 16);
        final int smoothWindowSize = 10 * DateTimeConstants.MILLIS_PER_MINUTE;  //TODO: make it configable.

        final SleepDetectionAlgorithm awakeDetectionAlgorithm = new AwakeDetectionAlgorithm(dataSource, smoothWindowSize);

        try {
            final Segment segmentFromAwakeDetection = awakeDetectionAlgorithm.getSleepPeriod(targetDate.withTimeAtStartOfDay());

            if(segmentFromAwakeDetection.getDuration() > 3 * DateTimeConstants.MILLIS_PER_HOUR) {
                final SleepSegment sleepSegmentFromAwakeDetection = new SleepSegment(1L,
                        segmentFromAwakeDetection.getStartTimestamp(),
                        segmentFromAwakeDetection.getOffsetMillis(),
                        60,
                        100,
                        Event.Type.SLEEP,
                        new ArrayList<SensorReading>(), null);
                sleepSegmentFromAwakeDetection.setMessage("You fell asleep");

                final SleepSegment wakeupSegmentFromAwakeDetection = new SleepSegment(1L,
                        segmentFromAwakeDetection.getEndTimestamp(),
                        segmentFromAwakeDetection.getOffsetMillis(),
                        60,
                        0,
                        Event.Type.WAKE_UP,
                        new ArrayList<SensorReading>(), null);

                if(!sleepTimeSegment.isPresent()) {
                    segments.add(sleepSegmentFromAwakeDetection);
                }else {
                    if(feature.userFeatureActive(FeatureFlipper.SLEEP_DETECTION_FROM_AWAKE, accessToken.accountId, new ArrayList<String>())) {
                        segments.add(sleepSegmentFromAwakeDetection);
                    }
                }
                segments.add(wakeupSegmentFromAwakeDetection);
            }

            LOGGER.info("Sleep Time From Awake Detection Algorithm: {} - {}",
                    new DateTime(segmentFromAwakeDetection.getStartTimestamp(), DateTimeZone.forOffsetMillis(segmentFromAwakeDetection.getOffsetMillis())),
                    new DateTime(segmentFromAwakeDetection.getEndTimestamp(), DateTimeZone.forOffsetMillis(segmentFromAwakeDetection.getOffsetMillis())));
        }catch (Exception ex){
            LOGGER.error("Generate sleep period from Awake Detection Algorithm failed: {}", ex.getMessage());
        }

        // add partner movement data, check if there's a partner
        final Optional<Long> optionalPartnerAccountId = this.deviceDAO.getPartnerAccountId(accessToken.accountId);
        if (optionalPartnerAccountId.isPresent()) {
            // get tracker motions for partner, query time is in UTC, not local_utc
            final DateTime startTime;
            if (sleepTimeSegment.isPresent()) {
                startTime = new DateTime(sleepTimeSegment.get().getTimestamp(), DateTimeZone.UTC);
            } else {
                startTime = new DateTime(segments.get(0).getTimestamp(), DateTimeZone.UTC);
            }
            final DateTime endTime = new DateTime(segments.get(segments.size() - 1).getTimestamp(), DateTimeZone.UTC);

            final List<TrackerMotion> partnerMotions = this.trackerMotionDAO.getBetween(optionalPartnerAccountId.get(), startTime, endTime);
            if (partnerMotions.size() > 0) {
                // use un-normalized data segments for comparison
                segments.addAll(PartnerMotion.getPartnerData(segments, partnerMotions, threshold));
            }
        }

        // add sunrise data
        final Optional<DateTime> sunrise = sunData.sunrise(targetDate.plusDays(1).toString(DateTimeFormat.forPattern("yyyy-MM-dd"))); // day + 1
        if(sunrise.isPresent()) {

            SleepSegment.SoundInfo soundInfo = null;
            if(feature.userFeatureActive(FeatureFlipper.SOUND_INFO_TIMELINE, accessToken.accountId, new ArrayList<String>())) {
                final Date expiration = new java.util.Date();
                long msec = expiration.getTime();
                msec += 1000 * 60 * 60; // 1 hour.
                expiration.setTime(msec);
                final URL url = s3.generatePresignedUrl(bucketName, "mario.mp3", expiration, HttpMethod.GET);
                soundInfo = new SleepSegment.SoundInfo(url.toExternalForm(), 2000);
            }

            final SleepSegment sunriseSegment = new SleepSegment(1L,
                    sunrise.get().getMillis(), 0, DateTimeConstants.SECONDS_PER_MINUTE,
                    -1,
                    Event.Type.SUNRISE,
                    new ArrayList<SensorReading>(),
                    soundInfo);
//            final SleepSegment audioSleepSegment = new SleepSegment(99L, sunrise.get().plusMinutes(5).getMillis(), 0, 60, -1, Event.Type.SNORING, "ZzZzZzZzZ", new ArrayList<SensorReading>(), soundInfo);
            segments.add(sunriseSegment);
//            extraSegments.add(audioSleepSegment);

            LOGGER.debug(sunriseSegment.getMessage());
        }

        // TODO: add sound


        // merge similar segments (by motion & event-type), then categorize
//        final List<SleepSegment> mergedSegments = TimelineUtils.mergeConsecutiveSleepSegments(segments, mergeThreshold);
        final List<SleepSegment> mergedSegments = TimelineUtils.generateAlignedSegmentsByTypeWeight(segments, DateTimeConstants.MILLIS_PER_MINUTE, 15, false);
        final SleepStats sleepStats = TimelineUtils.computeStats(mergedSegments);
        final List<SleepSegment> reversed = Lists.reverse(mergedSegments);

        // get scores - check dynamoDB first
        final int userOffsetMillis = trackerMotions.get(0).offsetMillis;
        final String targetDateString = DateTimeUtil.dateToYmdString(targetDate);

        final AggregateScore targetDateScore = this.aggregateSleepScoreDAODynamoDB.getSingleScore(accessToken.accountId, targetDateString);
        Integer sleepScore = targetDateScore.score;

        if (sleepScore == 0) {
            // score may not have been computed yet, recompute
            sleepScore = sleepScoreDAO.getSleepScoreForNight(accessToken.accountId, targetDate.withTimeAtStartOfDay(),
                    userOffsetMillis, this.dateBucketPeriod, sleepLabelDAO);

            final DateTime lastNight = new DateTime(DateTime.now(), DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1);
            if (targetDate.isBefore(lastNight)) {
                // write data to Dynamo if targetDate is old
                this.aggregateSleepScoreDAODynamoDB.writeSingleScore(
                        new AggregateScore(accessToken.accountId,
                                sleepScore,
                                DateTimeUtil.dateToYmdString(targetDate.withTimeAtStartOfDay()),
                                targetDateScore.scoreType, targetDateScore.version));
            }
        }

        final String timeLineMessage = TimelineUtils.generateMessage(sleepStats);

        LOGGER.debug("Score for account_id = {} is {}", accessToken.accountId, sleepScore);


        final List<Insight> insights = TimelineUtils.generateRandomInsights(targetDate.getDayOfMonth());
        final Timeline timeline = new Timeline(sleepScore, timeLineMessage, date, reversed, insights);
        final List<Timeline> timelines = new ArrayList<>();
        timelines.add(timeline);

        return timelines;
    }
}
