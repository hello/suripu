package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.SleepInsight;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.InsightProcessor;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Created by kingshy on 10/24/14.
 */
@Path("/v1/insights")
public class InsightsResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsightsResource.class);

    private final AccountDAO accountDAO;

    public InsightsResource(final AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    @Timed
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<SleepInsight> getInsights(
            @Scope(OAuthScope.QUESTIONS_READ) final AccessToken accessToken) {

        LOGGER.debug("Returning list of insights for account id = {}", accessToken.accountId);
        final Optional<Account> accountOptional = accountDAO.getById(accessToken.accountId);
        if (!accountOptional.isPresent()) {
            throw new WebApplicationException(404);
        }

        return InsightProcessor.getInsights(accessToken.accountId);
    }
}
