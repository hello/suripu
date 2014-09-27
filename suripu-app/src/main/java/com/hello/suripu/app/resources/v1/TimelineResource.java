package com.hello.suripu.app.resources.v1;

import com.google.common.collect.Lists;
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
import java.util.Random;

@Path("/v1/timeline")
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

    @Timed
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

        int groupBy = 5; // group by 5 minutes

        int threshold = 10; // events with scores < threshold will be considered motion events
        // TODO: compute this threshold dynamically

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenGrouped(accessToken.accountId, targetDate, endDate, groupBy);
        LOGGER.debug("Length of trackerMotion: {}", trackerMotions.size());
        final List<SleepSegment> sleepSegments = new ArrayList<>();


        Long maxSVM = 0L;
        for(final TrackerMotion trackerMotion : trackerMotions) {
            maxSVM = Math.max(maxSVM, trackerMotion.value);
        }

        LOGGER.debug("Max SVM = {}", maxSVM);

        int i = 0;
        long tracker_id = trackerMotions.get(0).trackerId;
        for(final TrackerMotion trackerMotion : trackerMotions) {
            if (trackerMotion.trackerId != tracker_id) {
                break; // if user has multiple pill, only use data from the latest tracker_id
            }

            int sleepDepth = 100;
            if(trackerMotion.value == -1) {
                sleepDepth = 100;
            } else if(trackerMotion.value > -1) {
                sleepDepth = 100 - (int) (new Double(trackerMotion.value) / maxSVM * 100);
                LOGGER.trace("Ratio = ({} / {}) = {}", trackerMotion.value, maxSVM, new Double(trackerMotion.value) / maxSVM * 100);
                LOGGER.trace("Sleep Depth = {}", sleepDepth);

            }

            String eventType = (sleepDepth <= threshold) ? Event.Type.MOTION.toString() : null; // TODO: put these in a config file or DB
            if(i == 0) {
                eventType = "SLEEP";
            } else if (i == trackerMotions.size() -1) {
                eventType = "WAKE_UP";
            }

            // TODO: make this work
            if (trackerMotion.value == maxSVM) {
                eventType = Event.Type.MOTION.toString();
            }

            if( sleepDepth <=10) {
                sleepDepth = 10;
            } else if(sleepDepth > 10 && sleepDepth <= 40) {
                sleepDepth = 40;
            } else if(sleepDepth > 40 && sleepDepth <= 70) {
                sleepDepth = 70;
            } else if (sleepDepth > 70 && sleepDepth <= 100) {
                sleepDepth = 100;
            }


            final SleepSegment sleepSegment = new SleepSegment(
                    trackerMotion.id,
                    trackerMotion.timestamp,
                    trackerMotion.offsetMillis,
                    60 * groupBy, // in seconds
                    sleepDepth,
                    eventType,
                    "something smart",
                    new ArrayList<SensorSample>()
            );
            sleepSegments.add(sleepSegment);
            i++;
        }

        final List<SleepSegment> reversed = Lists.reverse(sleepSegments);

        final List<String> messages = new ArrayList<>();

        messages.add("You slept for an hour more than usual");
        messages.add("You were in bed for 0-24 hours and asleep for 0-24 hours");
        messages.add("You went to bed a bit late and your sleep quality suffered as a result");
        messages.add("It was a bit warmer than usual");
        messages.add("Maybe cut down on the Netflix binges?");

        final Random r = new Random();
        String timeLineMessage = messages.get(r.nextInt(messages.size()));

        int sleepScore = 0;
        if (trackerMotions.size() > 0) {
            final int userOffsetMillis = trackerMotions.get(0).offsetMillis;
            sleepScore = sleepScoreDAO.getSleepScoreForNight(accessToken.accountId, targetDate.withTimeAtStartOfDay(), userOffsetMillis, this.dateBucketPeriod, sleepLabelDAO);
        } else {
            timeLineMessage = "You haven't been sleeping!";
        }

        final Timeline timeline = new Timeline(sleepScore, timeLineMessage, date, reversed );
        final List<Timeline> timelines = new ArrayList<>();
        timelines.add(timeline);

        return timelines;
    }
}
