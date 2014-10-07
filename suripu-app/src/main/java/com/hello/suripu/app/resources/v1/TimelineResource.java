package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.TimelineUtils;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/v1/timeline")
public class TimelineResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResource.class);

    private final TrackerMotionDAO trackerMotionDAO;
    private final SleepScoreDAO sleepScoreDAO;
    private final SleepLabelDAO sleepLabelDAO;
    private final int dateBucketPeriod;
    private final DeviceDataDAO deviceDataDAO;

    public TimelineResource(final TrackerMotionDAO trackerMotionDAO,
                            final SleepLabelDAO sleepLabelDAO,
                            final SleepScoreDAO sleepScoreDAO,
                            final int dateBucketPeriod,
                            final DeviceDataDAO deviceDataDAO) {
        this.trackerMotionDAO = trackerMotionDAO;
        this.sleepLabelDAO = sleepLabelDAO;
        this.sleepScoreDAO = sleepScoreDAO;
        this.dateBucketPeriod = dateBucketPeriod;
        this.deviceDataDAO = deviceDataDAO;
    }

    @Timed
    @Path("/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<Timeline> getTimelines(
            @Scope(OAuthScope.SLEEP_TIMELINE)final AccessToken accessToken,
            @PathParam("date") String date) {


        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withHourOfDay(22);
        final DateTime endDate = targetDate.plusHours(12);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        final List<Event> events = new ArrayList<>();

        int groupBy = 5; // group by 5 minutes

        int threshold = 10; // events with scores < threshold will be considered motion events
        // TODO: compute this threshold dynamically

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenGrouped(accessToken.accountId, targetDate, endDate, groupBy);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());

        if(trackerMotions.isEmpty()) {
            LOGGER.debug("No data for account_id = {} and day = {}", accessToken.accountId, targetDate);
            final Timeline timeline = new Timeline(0, "You haven't been sleeping!", date, new ArrayList<SleepSegment>());
            final List<Timeline> timelines = new ArrayList<>();
            timelines.add(timeline);
            return timelines;
        }

        final Optional<DeviceData> data = deviceDataDAO.getAverageForNight(accessToken.accountId, targetDate, endDate);

        final List<SleepSegment> segments = TimelineUtils.generateSleepSegments(trackerMotions, threshold, groupBy, data);
        final List<SleepSegment> normalized = TimelineUtils.categorizeSleepDepth(segments);
        final List<SleepSegment> mergedSegments = TimelineUtils.mergeConsecutiveSleepSegments(normalized, threshold);
        final SleepStats sleepStats = TimelineUtils.computeStats(mergedSegments);
        final List<SleepSegment> reversed = Lists.reverse(mergedSegments);

        final int userOffsetMillis = trackerMotions.get(0).offsetMillis;
        final Integer sleepScore = sleepScoreDAO.getSleepScoreForNight(accessToken.accountId, targetDate.withTimeAtStartOfDay(),
                this.dateBucketPeriod, sleepLabelDAO);
        final String timeLineMessage = TimelineUtils.generateMessage(sleepStats);

        LOGGER.debug("Score for account_id = {} is {}", accessToken.accountId, sleepScore);
        final Timeline timeline = new Timeline(sleepScore, timeLineMessage, date, reversed);
        final List<Timeline> timelines = new ArrayList<>();
        timelines.add(timeline);

        return timelines;
    }
}
