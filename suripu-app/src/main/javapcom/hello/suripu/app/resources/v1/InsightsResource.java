package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.Insights.AvailableGraph;
import com.hello.suripu.core.models.Insights.DowSample;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.SleepStatsSample;
import com.hello.suripu.core.models.Insights.TrendGraph;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.TrendGraphUtils;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 10/24/14.
 */
@Path("/v1/insights")
public class InsightsResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsightsResource.class);
    private static long DAY_IN_MILLIS = 86400000;

    private final AccountDAO accountDAO;
    private final TrendsDAO trendsDAO;
    private final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
    private final TrackerMotionDAO trackerMotionDAO;
    private final InsightsDAODynamoDB insightsDAODynamoDB;

    public InsightsResource(final AccountDAO accountDAO, final TrendsDAO trendsDAO, final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB, final TrackerMotionDAO trackerMotionDAO, InsightsDAODynamoDB insightsDAODynamoDB) {
        this.accountDAO = accountDAO;
        this.trendsDAO = trendsDAO;
        this.scoreDAODynamoDB = scoreDAODynamoDB;
        this.trackerMotionDAO = trackerMotionDAO;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
    }

    /**
     * get insights
     */
    @Timed
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<InsightCard> getInsights(@Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken) {

        LOGGER.debug("Returning list of insights for account id = {}", accessToken.accountId);
        // TODO: real insights
        return InsightProcessor.getInsights(accessToken.accountId);
    }

    @Timed
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/light")
    public ImmutableList<InsightCard> getLightInsights(@Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken) {
        final ImmutableList<InsightCard> cards = insightsDAODynamoDB.getInsightsByCategory(accessToken.accountId,
                InsightCard.Category.LIGHT);
        // TODO: fetch generic cards.
        return cards;
    }
    /**
     * get a specific graph
     */
    @Timed
    @GET
    @Path("/trends/graph")
    @Produces(MediaType.APPLICATION_JSON)
    public TrendGraph getTrends(@Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken,
                                @QueryParam("data_type") String dataType,
                                @QueryParam("time_period") String timePeriod) {

        LOGGER.debug("Returning data to plot trends for account id = {}", accessToken.accountId);

        final TrendGraph.TimePeriodType timePeriodType = TrendGraph.TimePeriodType.fromString(timePeriod);
        final TrendGraph.DataType graphDataType = TrendGraph.DataType.fromString(dataType);

        TrendGraph graph = getGraph(accessToken.accountId, timePeriodType, graphDataType);
        return graph;

    }


    /**
     * get a list of available trend graphs
     */
    @Timed
    @GET
    @Path("/trends/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AvailableGraph> getTrendsList(@Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken) {

        LOGGER.debug("Returning list of available graphs account id = {}", accessToken.accountId);

        final Optional<Account> optionalAccount = accountDAO.getById(accessToken.accountId);
        if (optionalAccount.isPresent()) {
            return TrendGraphUtils.getGraphList(optionalAccount.get());
        }
        return Collections.emptyList();
    }

    /**
     * get all default graphs
     */
    @Timed
    @GET
    @Path("/trends/all")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrendGraph> getAllTrends(@Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken) {

        LOGGER.debug("Returning ALL available default graphs for account id = {}", accessToken.accountId);

        final Optional<Account> optionalAccount = accountDAO.getById(accessToken.accountId);
        final List<TrendGraph> graphs = new ArrayList<>();

        if (optionalAccount.isPresent()) {
            final Account account = optionalAccount.get();
            final boolean eligible = TrendGraphUtils.checkEligibility(account.created);
            if (eligible) {
                // add all the default graphs
                final long accountId = account.id.get();

                final TrendGraph sleepScoreDayOfWeek = getGraph(accountId, TrendGraph.TimePeriodType.DAY_OF_WEEK, TrendGraph.DataType.SLEEP_SCORE);
                graphs.add(sleepScoreDayOfWeek);

                final TrendGraph sleepDurationDayOfWeek = getGraph(accountId, TrendGraph.TimePeriodType.DAY_OF_WEEK, TrendGraph.DataType.SLEEP_DURATION);
                graphs.add(sleepDurationDayOfWeek);

                final TrendGraph sleepScoreOverTime = getGraph(accountId, TrendGraph.TimePeriodType.OVER_TIME_ALL, TrendGraph.DataType.SLEEP_SCORE);
                graphs.add(sleepScoreOverTime);
            }
        }

        return graphs;
    }

    /**
     * Get data from data-stores, aggregate, create graph
     * @return TrendGraph
     */
    private TrendGraph getGraph(final Long accountId, final TrendGraph.TimePeriodType timePeriod, final TrendGraph.DataType graphType) {

        if (timePeriod == TrendGraph.TimePeriodType.DAY_OF_WEEK) {
            // Histogram
            List<DowSample> rawData;
            if (graphType == TrendGraph.DataType.SLEEP_SCORE) {
                rawData = trendsDAO.getSleepScoreDow(accountId);
            } else {
                rawData = trendsDAO.getSleepDurationDow(accountId);
            }
            return TrendGraphUtils.getDayOfWeekGraph(graphType, timePeriod, rawData);

        } else {
            // time series

            // compute data date range
            final int numDays = TrendGraph.getTimePeriodDays(timePeriod);
            final DateTime endDate = DateTime.now().withTimeAtStartOfDay();
            final DateTime startDate = endDate.minusDays(numDays);

            if (graphType == TrendGraph.DataType.SLEEP_SCORE) {
                // sleep score over time, up to 365 days
                final ImmutableList<AggregateScore> scores = scoreDAODynamoDB.getBatchScores(accountId,
                        DateTimeUtil.dateToYmdString(startDate),
                        DateTimeUtil.dateToYmdString(endDate), numDays);

                // scores table has no offset, pull timezone offset from tracker-motion
                final Map<DateTime, Integer> userOffsetMillis = getUserTimeZoneOffsetsUTC(accountId, startDate, endDate);

                return TrendGraphUtils.getScoresOverTimeGraph(timePeriod, scores, userOffsetMillis);

            } else {
                // sleep duration over time, up to 365 days
                ImmutableList<SleepStatsSample> statsSamples = trendsDAO.getAccountSleepStatsBetweenDates(accountId, startDate, endDate);
                return TrendGraphUtils.getDurationOverTimeGraph(timePeriod, statsSamples);
            }

        }

    }

    // map keys in UTC
    private Map<DateTime, Integer> getUserTimeZoneOffsetsUTC(final long accountId, final DateTime startDate, final DateTime endDate) {
        final long daysDiff = (endDate.getMillis() - startDate.getMillis()) / DAY_IN_MILLIS;

        final List<DateTime> dates = new ArrayList<>();
        for (int i = 0; i < (int) daysDiff; i++) {
            dates.add(startDate.withZone(DateTimeZone.UTC).withTimeAtStartOfDay().plusDays(i));
        }
        return trackerMotionDAO.getOffsetMillisForDates(accountId, dates);
    }

}
