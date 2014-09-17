package com.hello.suripu.app.resources;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.EventDAODynamoDB;
import com.hello.suripu.core.db.SleepLabelDAO;
import com.hello.suripu.core.db.SleepScoreDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SensorSample;
import com.hello.suripu.core.models.SleepLabel;
import com.hello.suripu.core.models.SleepScore;
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
import java.util.Random;

@Path("/timeline")
public class TimelineResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResource.class);

    private final EventDAODynamoDB eventDAODynamoDB;
    private final TrackerMotionDAO trackerMotionDAO;
    private final AccountDAO accountDAO;
    private final SleepScoreDAO sleepScoreDAO;
    private final SleepLabelDAO sleepLabelDAO;
    private final int scoreThreshold;

    public TimelineResource(final EventDAODynamoDB eventDAODynamoDB,
                            final AccountDAO accountDAO,
                            final TrackerMotionDAO trackerMotionDAO,
                            final SleepLabelDAO sleepLabelDAO,
                            final SleepScoreDAO sleepScoreDAO,
                            final int scoreThreshold
                            ) {
        this.eventDAODynamoDB = eventDAODynamoDB;
        this.accountDAO = accountDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.sleepLabelDAO = sleepLabelDAO;
        this.sleepScoreDAO = sleepScoreDAO;
        this.scoreThreshold = scoreThreshold;
    }

    @Path("/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<Timeline> getTimelines(
            @Scope(OAuthScope.SLEEP_TIMELINE)final AccessToken accessToken,
            @PathParam("date") String date) {


        final DateTime targetDate = DateTime.parse(date, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT)).withHourOfDay(10);
        LOGGER.debug("Target date: {}", targetDate);

        final List<Event> events = new ArrayList<>();

        final List<TrackerMotion> trackerMotions = trackerMotionDAO.getBetweenGrouped(accessToken.accountId, targetDate.minusHours(12), targetDate, 5);
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

        final int sleepScore = this.getSleepScore(accessToken.accountId, targetDate);
        final Timeline timeline = new Timeline(sleepScore, "hello world", date, sleepSegments);
        final List<Timeline> timelines = new ArrayList<>();
        timelines.add(timeline);

        return timelines;
    }

    private int getSleepScore(final Long accountId, final DateTime targetDate) {
        // grab data from 10pm to 10am of the next day
        final DateTime nightDate = targetDate.minusHours(10);
        LOGGER.debug("Target date: {}, Night date: {}", targetDate, nightDate);

        // get sleep and wakeup time from sleep_labels or use default
        DateTime sleepUTC, wakeUTC;
        final Optional<SleepLabel> sleepLabelOptional = sleepLabelDAO.getByAccountAndDate(accountId, nightDate);
        if (!sleepLabelOptional.isPresent()) {
            sleepUTC = targetDate.minusHours(12); // 10pm last night
            wakeUTC = targetDate; // 10am today
        } else {
            sleepUTC = sleepLabelOptional.get().sleepTimeUTC;
            wakeUTC = sleepLabelOptional.get().wakeUpTimeUTC;
        }

        // set minute values to datetime bucket boundaries
        // e.g. sleep at 1:08am, query starts at 1:00am
        // wake at 7:55am, query ends at 8:00am
        final int sleepMinute = (sleepUTC.getMinuteOfHour() / this.scoreThreshold) * this.scoreThreshold;
        final int wakeMinutes = ((wakeUTC.getMinuteOfHour() / this.scoreThreshold) + 1) * this.scoreThreshold;
        final List<SleepScore> scores = sleepScoreDAO.getByAccountBetweenDateBucket(accountId,
                sleepUTC.withMinuteOfHour(sleepMinute),
                wakeUTC.withMinuteOfHour(0).plusMinutes(wakeMinutes));

        if (scores.size() == 0) {
            // for now, shouldn't happen IRL
            LOGGER.debug("Random Score");
            return  new Random().nextInt(100);
        }

        // TODO: continue to work on actual scoring
        float totalScore = 0.0f;
        float totalCounts = 0.0f;

        LOGGER.debug("Length of scores: {}", scores.size());
        for (final SleepScore score: scores) {
            LOGGER.debug("score {}, {}, {}", score.dateBucketUTC, score.bucketScore, score.sleepDuration);
            totalScore += score.bucketScore;
            totalCounts++;
        }
        LOGGER.debug("TOTAL score: {}, {}", String.valueOf(totalScore), String.valueOf(totalCounts));
        return Math.round(totalScore / totalCounts * 100.0f);
    }
}
