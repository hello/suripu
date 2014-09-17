package com.hello.suripu.app.resources;

import com.hello.suripu.core.db.EventDAODynamoDB;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SensorSample;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
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

@Path("/timeline")
public class TimelineResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResource.class);

    private final EventDAODynamoDB eventDAODynamoDB;
    private final TrackerMotionDAO trackerMotionDAO;
    private final SleepScoreDAO sleepScoreDAO;
    private final SleepLabelDAO sleepLabelDAO;
    private final int dateBucketPeriod;

    public TimelineResource(final EventDAODynamoDB eventDAODynamoDB,
                            final TrackerMotionDAO trackerMotionDAO,
                            final SleepLabelDAO sleepLabelDAO,
                            final SleepScoreDAO sleepScoreDAO,
                            final int dateBucketPeriod
    ) {
        this.eventDAODynamoDB = eventDAODynamoDB;
        this.trackerMotionDAO = trackerMotionDAO;
        this.sleepLabelDAO = sleepLabelDAO;
        this.sleepScoreDAO = sleepScoreDAO;
        this.dateBucketPeriod = dateBucketPeriod;
    }

    @Path("/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<Timeline> getTimelines(
            @Scope(OAuthScope.SLEEP_TIMELINE)final AccessToken accessToken,
            @PathParam("date") String date) {


        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT)).withHourOfDay(22);
        LOGGER.debug("Target date: {}", targetDate);

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenGrouped(accessToken.accountId, targetDate, targetDate.plusHours(12), 5);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());
        final List<SleepSegment> sleepSegments = new ArrayList<>();


        Long maxSVM = 0L;
        for(final TrackerMotion trackerMotion : trackerMotions) {
            maxSVM = Math.max(maxSVM, trackerMotion.value);
        }

        LOGGER.debug("Max SVM = {}", maxSVM);

        for(final TrackerMotion trackerMotion : trackerMotions) {


            int sleepDepth = 100;
            if(trackerMotion.value > -1) {
                sleepDepth = (int) Math.round(new Double(trackerMotion.value)/ maxSVM * 100);
                LOGGER.trace("Ratio = ({} / {}) = {}", trackerMotion.value, maxSVM, new Double(trackerMotion.value) / maxSVM * 100);
                LOGGER.trace("Sleep Depth = {}", sleepDepth);

            }
            final SleepSegment sleepSegment = new SleepSegment(
                    trackerMotion.id,
                    trackerMotion.timestamp,
                    trackerMotion.offsetMillis,
                    60, // in seconds
                    sleepDepth,
                    (sleepDepth != 100) ? Event.Type.MOTION.toString() : null,
                    "something smart",
                    new ArrayList<SensorSample>()
            );
            sleepSegments.add(sleepSegment);
        }
        final int offsetMillis = trackerMotions.get(0).offsetMillis;
        final int sleepScore = sleepScoreDAO.getSleepScoreForNight(accessToken.accountId, targetDate.withTimeAtStartOfDay(), offsetMillis, this.dateBucketPeriod, sleepLabelDAO);
        final Timeline timeline = new Timeline(sleepScore, "hello world", date, sleepSegments);
        final List<Timeline> timelines = new ArrayList<>();
        timelines.add(timeline);

        return timelines;
    }
}
