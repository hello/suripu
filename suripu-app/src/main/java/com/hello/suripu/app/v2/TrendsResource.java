package com.hello.suripu.app.v2;

import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.coredw.resources.BaseResource;
import com.hello.suripu.core.trends.v2.TimeScale;
import com.hello.suripu.core.trends.v2.TrendsProcessor;
import com.hello.suripu.core.trends.v2.TrendsResult;
import com.hello.suripu.core.util.JsonError;
import com.librato.rollout.RolloutClient;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v2/trends")
public class TrendsResource extends BaseResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrendsResource.class);

    private final TrendsProcessor trendsProcessor;

    @Inject
    RolloutClient feature;

    public TrendsResource(final TrendsProcessor trendsProcessor) {
        this.trendsProcessor = trendsProcessor;
    }


    @GET
    @Timed
    @Path("/{time_scale}")
    @Produces(MediaType.APPLICATION_JSON)
    public TrendsResult getTrends(@Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken,
                                      @PathParam("time_scale") String timeScaleString) {

        try {
            final TimeScale timeScale = TimeScale.fromString(timeScaleString);
            return trendsProcessor.getAllTrends(accessToken.accountId, timeScale);
        } catch (IllegalArgumentException iae) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), iae.getMessage())).build());
        }
    }
}
