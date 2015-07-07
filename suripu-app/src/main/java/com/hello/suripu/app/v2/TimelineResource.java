package com.hello.suripu.app.v2;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.TimelineDAODynamoDB;
import com.hello.suripu.core.db.TimelineLogDAO;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.models.timeline.v2.Timeline;
import com.hello.suripu.core.models.timeline.v2.TimelineFeedback;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.PATCH;
import com.librato.rollout.RolloutClient;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v2/timeline")
public class TimelineResource extends BaseResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResource.class);

    @Inject
    RolloutClient feature;

    private final TimelineProcessor timelineProcessor;
    private final AccountDAO accountDAO;
    private final TimelineDAODynamoDB timelineDAODynamoDB;
    private final TimelineLogDAO timelineLogDAO;

    public TimelineResource(final AccountDAO accountDAO,
                            final TimelineDAODynamoDB timelineDAODynamoDB,
                            final TimelineLogDAO timelineLogDAO,
                            final TimelineProcessor timelineProcessor) {
        this.timelineProcessor = timelineProcessor;
        this.timelineLogDAO = timelineLogDAO;
        this.accountDAO = accountDAO;
        this.timelineDAODynamoDB = timelineDAODynamoDB;
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{date}")
    public Timeline getTimelineForNight(@Scope(OAuthScope.SLEEP_TIMELINE) final AccessToken accessToken,
                                        @PathParam("date") final String night) {

        if(!isTimelineV2Enabled(accessToken.accountId)) {
            LOGGER.warn("Timeline V2 isn't enabled for {}", accessToken.accountId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final DateTime targetDate = DateTimeUtil.ymdStringToDateTime(night);
        final Optional<TimelineResult> timeline = timelineProcessor.retrieveTimelinesFast(accessToken.accountId, targetDate);
        if(!timeline.isPresent()) {
            return Timeline.createEmpty();
        }
        // That's super ugly. Need to find a more elegant way to write this
        return Timeline.fromV1(timeline.get().timelines.get(0));
    }


    @PATCH
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{date}/event")
    public Response amendTimeOfEvent(@Scope(OAuthScope.SLEEP_FEEDBACK) final AccessToken accessToken,
                                     @PathParam("date") String date,
                                     @Valid TimelineFeedback timelineFeedback) {
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @DELETE
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{date}/event")
    public Response deleteEvent(@Scope(OAuthScope.SLEEP_FEEDBACK) final AccessToken accessToken,
                                     @Valid TimelineFeedback timelineFeedback) {
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @PUT
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{date}/event")
    public Response validateEvent(@Scope(OAuthScope.SLEEP_FEEDBACK) final AccessToken accessToken,
                                     @Valid TimelineFeedback timelineFeedback) {
        return Response.status(Response.Status.ACCEPTED).build();
    }
}
