package com.hello.suripu.app.resources.v1;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.TimelineDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.DateTimeUtil;
import com.librato.rollout.RolloutClient;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
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
import java.util.Collections;
import java.util.List;

@Path("/v1/timeline")
public class TimelineResource extends BaseResource {

    @Inject
    RolloutClient feature;

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResource.class);

    private final TimelineProcessor timelineProcessor;
    private final AccountDAO accountDAO;
    private final TimelineDAODynamoDB timelineDAODynamoDB;

    public TimelineResource(final AccountDAO accountDAO,
                            final TimelineDAODynamoDB timelineDAODynamoDB,
                            final TimelineProcessor timelineProcessor) {
        this.accountDAO = accountDAO;
        this.timelineProcessor = timelineProcessor;
        this.timelineDAODynamoDB = timelineDAODynamoDB;
    }

    private boolean cacheTimeline(final long accountId, final DateTime targetDateLocalUTC, final List<Timeline> timelines){

        try{
            if(timelines == null || timelines.isEmpty()){
                // WARNING: DONOT cache empty timeline!
                LOGGER.info("Trying to cache empty timelines for account {} date {}, quit.", accountId, targetDateLocalUTC);
                return false;
            }
            this.timelineDAODynamoDB.saveTimelinesForDate(accountId, targetDateLocalUTC.withTimeAtStartOfDay(), timelines);
            return true;
        }catch (AmazonServiceException awsExp){
            LOGGER.error("AWS error, Save timeline for account {} date {} failed, {}",
                    accountId,
                    targetDateLocalUTC,
                    awsExp.getErrorMessage());
        }catch (Exception ex){
            LOGGER.error("General error, saving timeline for account {}, date {}, failed, {}",
                    accountId,
                    targetDateLocalUTC,
                    ex.getMessage());
        }

        return false;
    }

    private List<Timeline> getCachedTimelines(final Long accountId, final DateTime targetDate){
        final ImmutableList<Timeline> cachedTimelines = this.timelineDAODynamoDB.getTimelinesForDate(accountId, targetDate);
        if (!cachedTimelines.isEmpty()) {
            LOGGER.info("Timeline for account {}, date {} returned from cache.", accountId, targetDate);
            return cachedTimelines;
        }

        return Collections.EMPTY_LIST;
    }

    private List<Timeline> getTimelinesFromCacheOrReprocess(final Long accountId, final String targetDateString){
        final DateTime targetDate = DateTimeUtil.ymdStringToDateTime(targetDateString);
        final List<Timeline> timelinesFromCache = getCachedTimelines(accountId, targetDate);
        if(!timelinesFromCache.isEmpty()){
            return timelinesFromCache;
        }

        LOGGER.info("No cached timeline, reprocess timeline for account {}, date {}", accountId, targetDate);
        // TODO: Pass a config/map object to avoid changing the signature of this method for the next FeatureFlipper
        final List<Timeline> timelines = timelineProcessor.retrieveTimelinesFast(accountId, targetDate, missingDataDefaultValue(accountId), getFlipperParams(accountId));
        cacheTimeline(accountId, targetDate, timelines);
        return timelines;
    }

    @Timed
    @Path("/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<Timeline> getTimelines(
            @Scope(OAuthScope.SLEEP_TIMELINE)final AccessToken accessToken,
            @PathParam("date") String date) {

        return getTimelinesFromCacheOrReprocess(accessToken.accountId, date);

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

        return getTimelinesFromCacheOrReprocess(accessToken.accountId, date);
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
