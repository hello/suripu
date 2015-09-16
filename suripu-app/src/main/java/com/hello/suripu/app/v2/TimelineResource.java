package com.hello.suripu.app.v2;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TimelineLogDAO;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.timeline.v2.EventType;
import com.hello.suripu.core.models.timeline.v2.Timeline;
import com.hello.suripu.core.models.timeline.v2.TimelineEvent;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.FeedbackUtils;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.core.util.PATCH;
import com.hello.suripu.coredw.db.TimelineDAODynamoDB;
import com.librato.rollout.RolloutClient;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/v2/timeline")
public class TimelineResource extends BaseResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResource.class);

    @Inject
    RolloutClient feature;

    private final TimelineProcessor timelineProcessor;
    private final TimelineDAODynamoDB timelineDAODynamoDB;
    private final TimelineLogDAO timelineLogDAO;
    private final FeedbackDAO feedbackDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final DataLogger timelineLogDAOV2;

    public TimelineResource(final TimelineDAODynamoDB timelineDAODynamoDB,
                            final TimelineProcessor timelineProcessor,
                            final TimelineLogDAO timelineLogDAO,
                            final FeedbackDAO feedbackDAO,
                            final TrackerMotionDAO trackerMotionDAO,
                            final SleepStatsDAODynamoDB sleepStatsDAODynamoDB,
                            final DataLogger timelineLogDAOV2) {
        this.timelineProcessor = timelineProcessor;
        this.timelineDAODynamoDB = timelineDAODynamoDB;
        this.timelineLogDAO = timelineLogDAO;
        this.feedbackDAO = feedbackDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.timelineLogDAOV2 = timelineLogDAOV2;
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{date}")
    public Timeline getTimelineForNight(@Scope(OAuthScope.SLEEP_TIMELINE) final AccessToken accessToken,
                                        @PathParam("date") final String night,
                                        @DefaultValue("false") @QueryParam("has_feedback") final Boolean hasFeedback) {


        final DateTime targetDate = DateTimeUtil.ymdStringToDateTime(night);
        final Optional<TimelineResult> timeline = timelineProcessor.retrieveTimelinesFast(accessToken.accountId, targetDate,hasFeedback);
        if(!timeline.isPresent()) {
            return Timeline.createEmpty(targetDate);
        }
        // That's super ugly. Need to find a more elegant way to write this
        if (timeline.get().logV2.isPresent()) {
            final TimelineLog logV2 = timeline.get().logV2.get();
            final String partitionKey = logV2.getPartitionKey();
            timelineLogDAOV2.putAsync(partitionKey, logV2.toProtoBuf());
            timelineLogDAO.putTimelineLog(accessToken.accountId, logV2.getAsV1Log());
        }

        // That's super ugly. Need to find a more elegant way to write this
        final TimelineResult timelineResult = timeline.get();
        return Timeline.fromV1(timelineResult.timelines.get(0), timelineResult.notEnoughData);
    }


    @PATCH
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{date}/events/{type}/{timestamp}")
    public Timeline amendTimeOfEvent(@Scope(OAuthScope.SLEEP_FEEDBACK) final AccessToken accessToken,
                                     @PathParam("date") String date,
                                     @PathParam("type") String type,
                                     @PathParam("timestamp") long timestamp,
                                     @Valid TimelineEvent.TimeAmendment timeAmendment) {


        final Integer offsetMillis = getOffsetMillis(accessToken.accountId, date, timestamp);
        final DateTime oldEventDateTime = new DateTime(timestamp, DateTimeZone.UTC).plusMillis(offsetMillis);
        final String hourMinute = oldEventDateTime.toString(DateTimeFormat.forPattern("HH:mm"));
        final Event.Type eventType = Event.Type.fromInteger(EventType.fromString(type).value);

        final TimelineFeedback timelineFeedback = TimelineFeedback.create(date, hourMinute, timeAmendment.newEventTime, eventType, accessToken.accountId);
        checkValidFeedbackOrThrow(accessToken.accountId,timelineFeedback, offsetMillis);
        feedbackDAO.insertTimelineFeedback(accessToken.accountId, timelineFeedback);
        timelineDAODynamoDB.invalidateCache(accessToken.accountId, timelineFeedback.dateOfNight, DateTime.now());
        return getTimelineForNight(accessToken, date,true);
    }

    @DELETE
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{date}/events/{type}/{timestamp}")
    public Response deleteEvent(@Scope(OAuthScope.SLEEP_FEEDBACK) final AccessToken accessToken,
                                @PathParam("date") String date,
                                @PathParam("type") String type,
                                @PathParam("timestamp") long timestamp) {

        return Response.status(Response.Status.ACCEPTED)
                       .entity(getTimelineForNight(accessToken, date,true))
                       .build();
    }

    @PUT
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{date}/events/{type}/{timestamp}")
    public Response validateEvent(@Scope(OAuthScope.SLEEP_FEEDBACK) final AccessToken accessToken,
                                  @PathParam("date") String date,
                                  @PathParam("type") String type,
                                  @PathParam("timestamp") long timestamp) {



        final Integer offsetMillis = getOffsetMillis(accessToken.accountId, date, timestamp);

        final DateTime correctEvent = new DateTime(timestamp, DateTimeZone.UTC).plusMillis(offsetMillis);
        final String hourMinute = correctEvent.toString(DateTimeFormat.forPattern("HH:mm"));
        final Event.Type eventType = Event.Type.fromInteger(EventType.fromString(type).value);

        // Correct event means feedback = prediction
        final TimelineFeedback timelineFeedback = TimelineFeedback.create(date, hourMinute, hourMinute, eventType, accessToken.accountId);
        checkValidFeedbackOrThrow(accessToken.accountId,timelineFeedback,offsetMillis);
        feedbackDAO.insertTimelineFeedback(accessToken.accountId, timelineFeedback);

        //recalculate with feedback
        getTimelineForNight(accessToken, date,true);

        return Response.status(Response.Status.ACCEPTED).build();
    }


    private Integer getOffsetMillis(final Long accountId, final String date, final Long timestamp) {
        final Optional<AggregateSleepStats> aggregateSleepStatsOptional = sleepStatsDAODynamoDB.getSingleStat(accountId, date);

        if(aggregateSleepStatsOptional.isPresent()) {
            return aggregateSleepStatsOptional.get().offsetMillis;
        }

        LOGGER.warn("Missing aggregateSleepStats for account_id = {} and date = {}", accountId, date);
        LOGGER.warn("Querying trackerMotion table for offset for account_id = {} and date = {}", accountId, date);
        final DateTime startDateTime = DateTimeUtil.ymdStringToDateTime(date);
        final DateTime endDateTime = startDateTime.plusHours(48);

        final List<TrackerMotion> trackerMotionList = trackerMotionDAO.getBetween(accountId, startDateTime, endDateTime);
        if(trackerMotionList.isEmpty()) {
            LOGGER.error("No tracker motion data");
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(new JsonError(404, "Not found")).build());
        }

        Integer offsetMillis = trackerMotionList.get(0).offsetMillis;
        for(final TrackerMotion trackerMotion : trackerMotionList) {
            if(trackerMotion.timestamp >= timestamp) {
                offsetMillis = trackerMotion.offsetMillis;
                break;
            }
        }

        return offsetMillis;
    }

    private void checkValidFeedbackOrThrow(final long accountId, final TimelineFeedback timelineFeedback, final int offsetMillis) {

        
        if (!this.hasTimelineOrderEnforcement(accountId)) {
            return;
        }

        //do not check validity of events that are not sleep events
        final boolean isSleepEvent = timelineFeedback.eventType.equals(Event.Type.IN_BED)
                || timelineFeedback.eventType.equals(Event.Type.SLEEP)
                || timelineFeedback.eventType.equals(Event.Type.WAKE_UP)
                || timelineFeedback.eventType.equals(Event.Type.OUT_OF_BED);

        if (!isSleepEvent) {
            return;
        }

        final FeedbackUtils feedbackUtils = new FeedbackUtils();
        final ImmutableList<TimelineFeedback> existingFeedbacks = feedbackDAO.getForNight(accountId,timelineFeedback.dateOfNight);

        //proposed event is valid
        if (!feedbackUtils.checkEventValidity(timelineFeedback,offsetMillis)) {
            throw new WebApplicationException(Response.status(Response.Status.PRECONDITION_FAILED).entity(new JsonError(Response.Status.PRECONDITION_FAILED.getStatusCode(), English.FEEDBACK_AT_INVALID_TIME)).build());
        }

        //events out of order
        if (!feedbackUtils.checkEventOrdering(existingFeedbacks,timelineFeedback,offsetMillis)) {
            throw new WebApplicationException(Response.status(Response.Status.PRECONDITION_FAILED).entity(new JsonError(Response.Status.PRECONDITION_FAILED.getStatusCode(), English.FEEDBACK_INCONSISTENT)).build());
        }
    }
}
