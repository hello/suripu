package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Insights.AvailableGraph;
import com.hello.suripu.core.models.Insights.DowSample;
import com.hello.suripu.core.models.Insights.InfoInsightCards;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.TrendGraph;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.processors.insights.IntroductionInsights;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.TrendGraphUtils;
import com.librato.rollout.RolloutClient;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kingshy on 10/24/14.
 */
@Path("/v1/insights")
public class InsightsResource extends BaseResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsightsResource.class);
    private static long DAY_IN_MILLIS = 86400000L;
    private static int MIN_DATAPOINTS = 2;
    private static int MAX_INSIGHTS_NUM = 20;
    private static int DAY_OF_WEEK_LOOKBACK = 90; // days
    private static int TRENDS_AVAILABLE_AFTER_DAYS = 7;

    private final AccountDAO accountDAO;
    private final TrendsInsightsDAO trendsInsightsDAO;
    private final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
    private final TrackerMotionDAO trackerMotionDAO;
    private final InsightsDAODynamoDB insightsDAODynamoDB;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;
    private final InsightProcessor insightProcessor;

    @Inject
    RolloutClient feature;

    public InsightsResource(final AccountDAO accountDAO, final TrendsInsightsDAO trendsInsightsDAO, final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                            final TrackerMotionDAO trackerMotionDAO, InsightsDAODynamoDB insightsDAODynamoDB,
                            final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final InsightProcessor insightProcessor) {
        this.accountDAO = accountDAO;
        this.trendsInsightsDAO = trendsInsightsDAO;
        this.scoreDAODynamoDB = scoreDAODynamoDB;
        this.trackerMotionDAO = trackerMotionDAO;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
        this.insightProcessor = insightProcessor;
    }

    /**
     * get insights
     */
    @Timed
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<InsightCard> getInsights(@Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken) {

        LOGGER.debug("Returning list of insights for account id = {}", accessToken.accountId);
        final Boolean chronological = false; // reverse chronological
        final DateTime queryDate = DateTime.now(DateTimeZone.UTC).plusDays(1);
        final ImmutableList<InsightCard> cards = insightsDAODynamoDB.getInsightsByDate(accessToken.accountId,
                queryDate, chronological, MAX_INSIGHTS_NUM);

        if (cards.size() == 0) {
            // no insights generated yet, probably a new user, send introduction card
            final Optional<Account> optionalAccount = accountDAO.getById(accessToken.accountId);
            int userAgeInYears = 0;
            if (optionalAccount.isPresent()) {
                userAgeInYears = DateTimeUtil.getDateDiffFromNowInDays(optionalAccount.get().DOB) / 365;
            }

            final List<InsightCard> introCards = IntroductionInsights.getIntroCards(accessToken.accountId, userAgeInYears);
            this.insightsDAODynamoDB.insertListOfInsights(introCards);
            return insightCardsWithInfoPreview(introCards);
        } else {
            return insightCardsWithInfoPreview(cards);
        }
    }

    @Timed
    @GET
    @Path("/info/{category}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<InfoInsightCards> getGenericInsightCards(
            @Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken,
            @PathParam("category") final String value) {

        final InsightCard.Category category = InsightCard.Category.fromString(value);

        final List<InfoInsightCards> cards = trendsInsightsDAO.getGenericInsightCardsByCategory(category.toString().toLowerCase());
        return cards;
    }

        @Timed
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/light")
    public ImmutableList<InsightCard> getLightInsights(@Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken) {
        final int limit = 5;
        final ImmutableList<InsightCard> cards = insightsDAODynamoDB.getInsightsByCategory(accessToken.accountId,
                InsightCard.Category.LIGHT, limit);
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
    public List<TrendGraph> getTrends(@Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken,
                                @QueryParam("data_type") String dataType,
                                @QueryParam("time_period") String timePeriod) {

        final List<TrendGraph> graphs = new ArrayList<>();

        if(isTrendsViewUnavailable(accessToken.accountId)) {
            LOGGER.warn("TRENDS VIEW UNAVAILABLE FOR USER {}", accessToken.accountId);
            return graphs;
        }

        LOGGER.debug("Returning data to plot trends for account id = {}", accessToken.accountId);

        final TrendGraph.TimePeriodType timePeriodType = TrendGraph.TimePeriodType.fromString(timePeriod);
        final TrendGraph.DataType graphDataType = TrendGraph.DataType.fromString(dataType);

        Optional<TrendGraph> graphOptional = getGraph(accessToken.accountId, timePeriodType, graphDataType);

        if (graphOptional.isPresent()) {
            graphs.add(graphOptional.get());
        }

        return graphs;
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
            final Account account = optionalAccount.get();
            final boolean eligible = checkTrendsEligibility(account.id.get());
            if (eligible) {
                return TrendGraphUtils.getGraphList(optionalAccount.get());
            }
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
    public List<TrendGraph> getAllTrends(@Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken,
                                         @QueryParam("option") String timePeriodOption) {

        LOGGER.debug("Returning ALL available default graphs for account id = {}", accessToken.accountId);

        TrendGraph.TimePeriodType scoreOverTimePeriod = TrendGraph.TimePeriodType.OVER_TIME_1W;
        if (timePeriodOption != null) {
            scoreOverTimePeriod = TrendGraph.TimePeriodType.fromString(timePeriodOption);
        }

        final Optional<Account> optionalAccount = accountDAO.getById(accessToken.accountId);

        if (optionalAccount.isPresent()) {
            final Account account = optionalAccount.get();
            final long accountId = account.id.get();

            final boolean eligible = checkTrendsEligibility(accountId);

            if (eligible) {
                return getAllTrendsGraphs(accountId, scoreOverTimePeriod);
            }
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * Convenience method to construct a new list of InsightCards that include info preview titles
     * @param insightCards: an array of insight card without info preview titles
     * @return List of InsightCard objects that contain info preview titles
     */
    private List<InsightCard> insightCardsWithInfoPreview(final List<InsightCard> insightCards) {
        final List<InsightCard> cardsWithPreview = new ArrayList<>();
        for (InsightCard card : insightCards) {
            cardsWithPreview.add(card.withInfoPreview(insightProcessor.getInsightPreviewForCategory(card.category)));
        }
        return cardsWithPreview;
    }

    /**
     * get all the graphs for the app's Trends view
     * @param accountId
     * @param scoreOverTimePeriod
     * @return
     */
    private List<TrendGraph> getAllTrendsGraphs(final Long accountId, final TrendGraph.TimePeriodType scoreOverTimePeriod) {

        final List<TrendGraph> graphs = Lists.newArrayList();

        final DateTime endDate = DateTime.now().withTimeAtStartOfDay();
        final DateTime startDate = endDate.minusDays(DAY_OF_WEEK_LOOKBACK);

        final ImmutableList<AggregateSleepStats> sleepStats = this.sleepStatsDAODynamoDB.getBatchStats(accountId,
                DateTimeUtil.dateToYmdString(startDate),
                DateTimeUtil.dateToYmdString(endDate));

        // Sleep Score vs. Day of Week (always use last 30 days)
        final List<DowSample> sleepScoreDOWData = TrendGraphUtils.aggregateDOWData(sleepStats, TrendGraph.DataType.SLEEP_SCORE);
        if (sleepScoreDOWData.size() >= MIN_DATAPOINTS) {
            graphs.add(TrendGraphUtils.getDayOfWeekGraph(TrendGraph.DataType.SLEEP_SCORE, TrendGraph.TimePeriodType.DAY_OF_WEEK, sleepScoreDOWData));
        }

        // Sleep Duration vs. Day of Week (always use last 30 days)
        final List<DowSample> sleepDurationDOWData = TrendGraphUtils.aggregateDOWData(sleepStats, TrendGraph.DataType.SLEEP_DURATION);
        if (sleepDurationDOWData.size() >= MIN_DATAPOINTS) {
            graphs.add(TrendGraphUtils.getDayOfWeekGraph(TrendGraph.DataType.SLEEP_DURATION, TrendGraph.TimePeriodType.DAY_OF_WEEK, sleepDurationDOWData));
        }

        // Sleep Score vs. Time (Default is 1W, max is 90 days)
        // compute date range (now - x days)
        final int numDays = TrendGraph.getTimePeriodDays(scoreOverTimePeriod);
        final DateTime newStartDate = endDate.minusDays(numDays);

        // get relevant data from first batch
        final Interval interval = new Interval(newStartDate, endDate);
        final List<AggregateSleepStats> overTimeSleepStats = Lists.newArrayList();
        for (final AggregateSleepStats stat: sleepStats.reverse()) {
            if (interval.contains(stat.dateTime)) {
                overTimeSleepStats.add(stat);
            } else {
                // we can do this because sleep-stats is sorted reverse chronological
                break;
            }
        }

        // compute user age to present available time period options
        final Optional<Account> optionalAccount = accountDAO.getById(accountId);
        int daysActive = TrendGraph.PERIOD_TYPE_DAYS.get(TrendGraph.TimePeriodType.OVER_TIME_1W) + 1;
        if (optionalAccount.isPresent()) {
            final DateTime accountCreated = optionalAccount.get().created;
            daysActive = DateTimeUtil.getDateDiffFromNowInDays(accountCreated) - 1;
        }

        // construct the graph
        if (overTimeSleepStats.size() >= MIN_DATAPOINTS) {
            Collections.sort(overTimeSleepStats); // chronologically ascending
            graphs.add(TrendGraphUtils.getScoresOverTimeGraph(scoreOverTimePeriod, overTimeSleepStats, daysActive));
        }

        return graphs;
    }

    /**
     * Get data from data-stores, aggregate, create graph
     * @return TrendGraph
     */
    private Optional<TrendGraph> getGraph(final Long accountId, final TrendGraph.TimePeriodType timePeriod, final TrendGraph.DataType graphType) {

        final DateTime endDate = DateTime.now().withTimeAtStartOfDay();

        if (timePeriod == TrendGraph.TimePeriodType.DAY_OF_WEEK) {
            // Histogram
            // always look back 3 months
            final DateTime startDate = endDate.minusDays(DAY_OF_WEEK_LOOKBACK);

            final ImmutableList<AggregateSleepStats> sleepStats = this.sleepStatsDAODynamoDB.getBatchStats(accountId,
                    DateTimeUtil.dateToYmdString(startDate),
                    DateTimeUtil.dateToYmdString(endDate));

            final List<DowSample> rawData = TrendGraphUtils.aggregateDOWData(sleepStats, graphType);

            if (rawData.size() < MIN_DATAPOINTS) {
                return Optional.absent();
            }

            return Optional.of(TrendGraphUtils.getDayOfWeekGraph(graphType, timePeriod, rawData));

        } else {
            // time series

            // compute data date range
            final int numDays = TrendGraph.getTimePeriodDays(timePeriod);
            final DateTime startDate = endDate.minusDays(numDays);

            final Optional<Account> optionalAccount = accountDAO.getById(accountId);
            int daysActive = TrendGraph.PERIOD_TYPE_DAYS.get(TrendGraph.TimePeriodType.OVER_TIME_3M) + 1;
            if (optionalAccount.isPresent()) {
                final DateTime accountCreated = optionalAccount.get().created;
                daysActive = DateTimeUtil.getDateDiffFromNowInDays(accountCreated) - 1;
            }

            final ImmutableList<AggregateSleepStats> sleepStats = this.sleepStatsDAODynamoDB.getBatchStats(accountId,
                    DateTimeUtil.dateToYmdString(startDate),
                    DateTimeUtil.dateToYmdString(endDate));

            if (graphType == TrendGraph.DataType.SLEEP_SCORE) {
                if (sleepStats.size() < MIN_DATAPOINTS) {
                    final List<String> timeSeriesOptions = TrendGraph.TimePeriodType.getTimeSeriesOptions(daysActive);
                    return Optional.of(new TrendGraph(
                            TrendGraph.DataType.SLEEP_SCORE, TrendGraph.GraphType.TIME_SERIES_LINE,
                            timePeriod, timeSeriesOptions, Collections.EMPTY_LIST));
                }

                // scores table has no offset, pull timezone offset from tracker-motion
                return Optional.of(TrendGraphUtils.getScoresOverTimeGraph(timePeriod, sleepStats, daysActive));

            } else {
                // sleep duration over time, up to 365 days
                if (sleepStats.size() < MIN_DATAPOINTS) {
                    return Optional.absent();
                }

                return Optional.of(TrendGraphUtils.getDurationOverTimeGraph(timePeriod, sleepStats, daysActive));
            }

        }

    }

    private Boolean checkTrendsEligibility(final Long accountId) {
        // look back two weeks from now to make sure that user has at least 7 days of data
        final DateTime endDate = DateTime.now().withTimeAtStartOfDay();
        final DateTime startDate = endDate.minusDays(TRENDS_AVAILABLE_AFTER_DAYS * 2);

        final ImmutableList<AggregateSleepStats> sleepStats = this.sleepStatsDAODynamoDB.getBatchStats(accountId,
                DateTimeUtil.dateToYmdString(startDate),
                DateTimeUtil.dateToYmdString(endDate));

        if (sleepStats.size() < TRENDS_AVAILABLE_AFTER_DAYS) {
            LOGGER.warn("checkTrendsEligibility is False for account {}", accountId);
            return false;
        }
        return true;

    }
}
