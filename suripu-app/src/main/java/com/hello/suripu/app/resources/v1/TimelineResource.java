package com.hello.suripu.app.resources.v1;

import com.hello.suripu.core.db.EventDAODynamoDB;
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
import java.util.Random;

@Path("/v1/timeline")
public class TimelineResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResource.class);

    private final EventDAODynamoDB eventDAODynamoDB;
    private final TrackerMotionDAO trackerMotionDAO;

    public TimelineResource(final EventDAODynamoDB eventDAODynamoDB, final TrackerMotionDAO trackerMotionDAO) {
        this.eventDAODynamoDB = eventDAODynamoDB;
        this.trackerMotionDAO = trackerMotionDAO;
    }

    @Path("/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<Timeline> getTimelines(
            @Scope(OAuthScope.SLEEP_TIMELINE)final AccessToken accessToken,
            @PathParam("date") String date) {


        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT)).withZone(DateTimeZone.UTC).withHourOfDay(22);
        final DateTime endDate = targetDate.plusHours(12);
        LOGGER.debug("Target date: {}", targetDate);
        LOGGER.debug("End date: {}", endDate);

        final List<Event> events = new ArrayList<>();

        final Random r = new Random();

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenGrouped(accessToken.accountId, targetDate, endDate, 5);
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
                    (sleepDepth < 50) ? Event.Type.MOTION.toString() : null, // TODO: put these in a config file or DB
                    "something smart",
                    new ArrayList<SensorSample>()
            );
            sleepSegments.add(sleepSegment);
        }

        final Timeline timeline = new Timeline(r.nextInt(100), "hello world", date, sleepSegments);
        final List<Timeline> timelines = new ArrayList<>();
        timelines.add(timeline);

        return timelines;
    }
}
