package com.hello.suripu.app.resources.v1;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import com.hello.suripu.coredw.db.TimelineDAODynamoDB;
import com.hello.suripu.core.db.TimelineLogDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Timeline;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.coredw.resources.BaseResource;
import com.hello.suripu.core.translations.English;
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
import java.util.List;
import java.util.UUID;

@Path("/v1/timeline")
public class TimelineResource extends BaseResource {

    @Inject
    RolloutClient feature;

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineResource.class);

    private final TimelineProcessor timelineProcessor;
    private final AccountDAO accountDAO;
    private final TimelineDAODynamoDB timelineDAODynamoDB;
    private final TimelineLogDAO timelineLogDAOV1;
    private final DataLogger timelineLogDAOV2;

    public TimelineResource(final AccountDAO accountDAO,
                            final TimelineDAODynamoDB timelineDAODynamoDB,
                            final TimelineLogDAO timelineLogDAOV1,
                            final DataLogger timelineLogDAOV2,
                            final TimelineProcessor timelineProcessor) {
        this.accountDAO = accountDAO;
        this.timelineProcessor = timelineProcessor;
        this.timelineDAODynamoDB = timelineDAODynamoDB;
        this.timelineLogDAOV1 = timelineLogDAOV1;
        this.timelineLogDAOV2 = timelineLogDAOV2;
    }

    private boolean cacheTimeline(final long accountId, final DateTime targetDateLocalUTC, final TimelineResult result){

        try{
            if(result.timelines == null || result.timelines.isEmpty()){
                // WARNING: DONOT cache empty timeline!
                LOGGER.info("Trying to cache empty timelines for account {} date {}, quit.", accountId, targetDateLocalUTC);
                return false;
            }


            this.timelineDAODynamoDB.saveTimelinesForDate(accountId, targetDateLocalUTC.withTimeAtStartOfDay(), result);


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



    private TimelineResult getTimelinesFromCacheOrReprocess(final UUID sessionUUID, final Long accountId, final String targetDateString){
        final DateTime targetDate = DateTimeUtil.ymdStringToDateTime(targetDateString);
        final TimelineProcessor timelineProcessor = this.timelineProcessor.copyMeWithNewUUID(sessionUUID);
        //if no update forced (i.e. no HMM)

        //first try to get a cached result
        final Optional<TimelineResult> cachedResult = this.timelineDAODynamoDB.getTimelinesForDate(accountId, targetDate);

        if (cachedResult.isPresent() && !this.hasVotingEnabled(accountId)) {

            LOGGER.info("{} Found cached timeline for account {}, date {}",sessionUUID, accountId, targetDate);

            //log the cached result (why here? things can get put in the cache without first going through "timelineProcessor.retrieveTimelinesFast")
            final Optional<TimelineLog> logV2 = cachedResult.get().logV2;

            if (logV2.isPresent()) {
                timelineLogDAOV1.putTimelineLog(accountId, logV2.get().getAsV1Log());

                final String partitionKey = logV2.get().getPartitionKey();
                timelineLogDAOV2.putAsync(partitionKey, logV2.get().toProtoBuf());
            }
            return cachedResult.get();
        }
        else {

            LOGGER.info("{} No cached timeline, reprocess timeline for account {}, date {}",sessionUUID, accountId, targetDate);

            //generate timeline from one of our many algorithms
            final TimelineResult result = timelineProcessor.retrieveTimelinesFast(accountId, targetDate, Optional.<TimelineFeedback>absent());

            // Timeline result could be present but no timeline if not enough data for the night
            if(result.timelines.isEmpty()) {
                return result;
            }

            //place in cache cache, money money, yo.
            cacheTimeline(accountId, targetDate, result);

            //log it, too
            //log the cached result (why here? things can get put in the cache without first going through "timelineProcessor.retrieveTimelinesFast")
            final Optional<TimelineLog> logV2 = result.logV2;

            if (logV2.isPresent()) {
                timelineLogDAOV1.putTimelineLog(accountId, logV2.get().getAsV1Log());

                final String partitionKey = logV2.get().getPartitionKey();
                timelineLogDAOV2.putAsync(partitionKey, logV2.get().toProtoBuf());
            }

            return result;
        }
    }

    @Timed
    @Path("/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<Timeline> getTimelines(
            @Scope(OAuthScope.SLEEP_TIMELINE)final AccessToken accessToken,
            @PathParam("date") String date) {

        if(isTimelineViewUnavailable(accessToken.accountId)) {
            LOGGER.warn("TIMELINE VIEW UNAVAILABLE FOR USER {}", accessToken.accountId);
            final List<Timeline> timelines = Lists.newArrayList(
                    Timeline.createEmpty(English.TIMELINE_UNAVAILABLE)
            );
            return timelines;
        }

        final  TimelineResult result =  getTimelinesFromCacheOrReprocess(UUID.randomUUID(), accessToken.accountId, date);

        return result.timelines;

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

        final TimelineResult timelineResult = timelineProcessor.retrieveTimelinesFast(accountId.get(), DateTimeUtil.ymdStringToDateTime(date), Optional.<TimelineFeedback>absent());

        return timelineResult.timelines;
    }

    @Timed
    @Path("/admin/invalidate/{email}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Boolean invalidateTimelineCache(
            @Scope(OAuthScope.ADMINISTRATION_WRITE)final AccessToken accessToken,
            @PathParam("email") String email,
            @PathParam("date") String date) {
        final Optional<Long> accountId = getAccountIdByEmail(email);
        if (!accountId.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final DateTime targetDate = DateTimeUtil.ymdStringToDateTime(date);
        return this.timelineDAODynamoDB.invalidateCache(accountId.get(), targetDate, DateTime.now());
    }

    @Timed
    @Path("/admin/algo/{email}/{date}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public com.hello.suripu.core.models.TimelineLog getTimelineAlgorithm(
            @Scope(OAuthScope.ADMINISTRATION_READ)final AccessToken accessToken,
            @PathParam("email") String email,
            @PathParam("date") String date) {

        final Optional<Long> accountId = getAccountIdByEmail(email);
        if (!accountId.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return getAlgorithmFromTimelineLog(accountId.get(), date);
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

    private com.hello.suripu.core.models.TimelineLog getAlgorithmFromTimelineLog(final Long accountId, final String date) {

        final DateTime dateTime = DateTimeUtil.ymdStringToDateTime(date);
        final ImmutableList<com.hello.suripu.core.models.TimelineLog> timelineLogs = this.timelineLogDAOV1.getLogsForUserAndDay(accountId, dateTime, Optional.<Integer>absent());

        if (timelineLogs.isEmpty()) {
            return com.hello.suripu.core.models.TimelineLog.createEmpty();
        } else if (timelineLogs.size() == 1) {
            return timelineLogs.get(0);
        }

        // multiple logs, return the latest
        int index = 0;
        Long latestCreated = timelineLogs.get(0).createdDate;
        for (int i = 0; i < timelineLogs.size(); i++) {
            if (timelineLogs.get(i).createdDate > latestCreated) {
                index = i;
                latestCreated = timelineLogs.get(i).createdDate;
            }
        }
        return timelineLogs.get(index);
    }

}
