package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.resources.BaseResource;
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
import java.util.List;

@Path("/v1/timeline")
public class TimelineResource extends BaseResource {

    @Inject
    RolloutClient feature;

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResource.class);

    private final TimelineProcessor timelineProcessor;
    private final AccountDAO accountDAO;

    public TimelineResource(final AccountDAO accountDAO, final TimelineProcessor timelineProcessor) {
        this.accountDAO = accountDAO;
        this.timelineProcessor = timelineProcessor;
    }

    @Timed
    @Path("/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<Timeline> getTimelines(
            @Scope(OAuthScope.SLEEP_TIMELINE)final AccessToken accessToken,
            @PathParam("date") String date) {

        return timelineProcessor.retrieveTimelines(accessToken.accountId, date);

    }

    @Timed
    @Path("/admin/{email}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<Timeline> getAdminTimelines(
            @Scope(OAuthScope.ADMINISTRATION_READ)final AccessToken accessToken,
            @PathParam("email") String email,
            @PathParam("date") String date) {
        final Optional<Long> accountId = getAccountIdByEmail(email);
        if (!accountId.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return timelineProcessor.retrieveTimelines(accountId.get(), date);
    }

    private Optional<Long> getAccountIdByEmail(final String email) {
        final Optional<Account> accountOptional = accountDAO.getByEmail(email);

        if (!accountOptional.isPresent()) {
            LOGGER.debug("Account {} not found", email);
            return Optional.absent();
        }
        final Account account = accountOptional.get();
        if (!account.id.isPresent()) {
            LOGGER.debug("ID not found for account {}", email);
            return Optional.absent();
        }
        return account.id;
    }
}
