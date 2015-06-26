package com.hello.suripu.app.v2;

import com.hello.suripu.core.models.timeline.v2.Timeline;
import com.hello.suripu.core.models.timeline.v2.TimelineFeedback;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.util.PATCH;
import com.yammer.metrics.annotation.Timed;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v2/timeline")
public class TimelineResource {

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{date}")
    public Timeline getTimelineForNight(@Scope(OAuthScope.SLEEP_TIMELINE) final AccessToken accessToken,
                                        @PathParam("date") final String night) {
        return Timeline.create();
    }


    @PATCH
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{date}/event")
    public Response amendTimeOfEvent(/*@Scope(OAuthScope.SLEEP_FEEDBACK) final AccessToken accessToken,*/
                                     @PathParam("date") String date,
                                     @Valid TimelineFeedback timelineFeedback) {
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @DELETE
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{date}/event")
    public Response deleteEvent(/*@Scope(OAuthScope.SLEEP_FEEDBACK) final AccessToken accessToken,*/
                                     @Valid TimelineFeedback timelineFeedback) {
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @PUT
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{date}/event")
    public Response validateEvent(/*@Scope(OAuthScope.SLEEP_FEEDBACK) final AccessToken accessToken,*/
                                     @Valid TimelineFeedback timelineFeedback) {
        return Response.status(Response.Status.ACCEPTED).build();
    }
}
