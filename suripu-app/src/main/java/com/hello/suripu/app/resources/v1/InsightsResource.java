package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsInsightsDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Insights.AvailableGraph;
import com.hello.suripu.core.models.Insights.DowSample;
import com.hello.suripu.core.models.Insights.InfoInsightCards;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.SleepStatsSample;
import com.hello.suripu.core.models.Insights.TrendGraph;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.insights.IntroductionInsights;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.TrendGraphUtils;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
    private static long DAY_IN_MILLIS = 86400000L;
    private static int MIN_DATAPOINTS = 2;
    private static int MAX_INSIGHTS_NUM = 20;
    private static int DAY_OF_WEEK_LOOKBACK = 90; // days

    private final AccountDAO accountDAO;
    private final TrendsInsightsDAO trendsInsightsDAO;
    private final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
    private final TrackerMotionDAO trackerMotionDAO;
    private final InsightsDAODynamoDB insightsDAODynamoDB;
    private final SleepStatsDAODynamoDB sleepStatsDAODynamoDB;

    public InsightsResource(final AccountDAO accountDAO, final TrendsInsightsDAO trendsInsightsDAO, final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                            final TrackerMotionDAO trackerMotionDAO, InsightsDAODynamoDB insightsDAODynamoDB,
                            final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {
        this.accountDAO = accountDAO;
        this.trendsInsightsDAO = trendsInsightsDAO;
        this.scoreDAODynamoDB = scoreDAODynamoDB;
        this.trackerMotionDAO = trackerMotionDAO;
        this.insightsDAODynamoDB = insightsDAODynamoDB;
        this.sleepStatsDAODynamoDB = sleepStatsDAODynamoDB;
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
            return introCards;
        }

        return cards;
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

        LOGGER.debug("Returning data to plot trends for account id = {}", accessToken.accountId);

        final TrendGraph.TimePeriodType timePeriodType = TrendGraph.TimePeriodType.fromString(timePeriod);
        final TrendGraph.DataType graphDataType = TrendGraph.DataType.fromString(dataType);

        Optional<TrendGraph> graphOptional = getGraph(accessToken.accountId, timePeriodType, graphDataType);

        final List<TrendGraph> graphs = new ArrayList<>();
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
    public List<TrendGraph> getAllTrends(@Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken,
                                         @QueryParam("option") String timePeriodOption) {

        LOGGER.debug("Returning ALL available default graphs for account id = {}", accessToken.accountId);

        TrendGraph.TimePeriodType scoreOverTimePeriod = TrendGraph.TimePeriodType.OVER_TIME_ALL;
        if (timePeriodOption != null) {
            scoreOverTimePeriod = TrendGraph.TimePeriodType.fromString(timePeriodOption);
        }

        final Optional<Account> optionalAccount = accountDAO.getById(accessToken.accountId);
        final List<TrendGraph> graphs = new ArrayList<>();

        if (optionalAccount.isPresent()) {
            final Account account = optionalAccount.get();
            final boolean eligible = TrendGraphUtils.checkEligibility(account.created);
            if (eligible) {
                // add all the default graphs
                final long accountId = account.id.get();

                final Optional<TrendGraph> sleepScoreDayOfWeek = getGraph(accountId, TrendGraph.TimePeriodType.DAY_OF_WEEK, TrendGraph.DataType.SLEEP_SCORE);
                if (sleepScoreDayOfWeek.isPresent()) {
                    graphs.add(sleepScoreDayOfWeek.get());
                }

                final Optional<TrendGraph> sleepDurationDayOfWeek = getGraph(accountId, TrendGraph.TimePeriodType.DAY_OF_WEEK, TrendGraph.DataType.SLEEP_DURATION);
                if (sleepDurationDayOfWeek.isPresent()) {
                    graphs.add(sleepDurationDayOfWeek.get());
                }

                final Optional<TrendGraph> sleepScoreOverTime = getGraph(accountId, scoreOverTimePeriod, TrendGraph.DataType.SLEEP_SCORE);
                if (sleepScoreOverTime.isPresent()) {
                    graphs.add(sleepScoreOverTime.get());
                }
            }
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
                    DateTimeUtil.dateToYmdString(endDate),
                    DAY_OF_WEEK_LOOKBACK);

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
            int daysActive = TrendGraph.PERIOD_TYPE_DAYS.get(TrendGraph.TimePeriodType.OVER_TIME_ALL) + 1;
            if (optionalAccount.isPresent()) {
                final DateTime accountCreated = optionalAccount.get().created;
                daysActive = DateTimeUtil.getDateDiffFromNowInDays(accountCreated) - 1;
            }

            final ImmutableList<AggregateSleepStats> sleepStats = this.sleepStatsDAODynamoDB.getBatchStats(accountId,
                    DateTimeUtil.dateToYmdString(startDate),
                    DateTimeUtil.dateToYmdString(endDate),
                    numDays);

            if (graphType == TrendGraph.DataType.SLEEP_SCORE) {
                // sleep score over time, up to 365 days
                final List<AggregateScore> scores = new ArrayList<>();
                for (final AggregateSleepStats stat : sleepStats) {
                    scores.add(new AggregateScore(accountId,
                            stat.sleepScore,
                            DateTimeUtil.dateToYmdString(stat.dateTime),
                            "sleep", stat.version));
                }

                if (scores.size() < MIN_DATAPOINTS) {
                    return Optional.absent();
                }

                // scores table has no offset, pull timezone offset from tracker-motion
                final Map<DateTime, Integer> userOffsetMillis = getUserTimeZoneOffsetsUTC(accountId, startDate, endDate);

                return Optional.of(TrendGraphUtils.getScoresOverTimeGraph(timePeriod, scores, userOffsetMillis, daysActive));

            } else {
                // sleep duration over time, up to 365 days
                final List<SleepStatsSample> statsSamples = new ArrayList<>();
                for (final AggregateSleepStats stat : sleepStats) {
                    statsSamples.add(new SleepStatsSample(stat.sleepStats, stat.dateTime, stat.offsetMillis));
                }

                if (statsSamples.size() < MIN_DATAPOINTS) {
                    return Optional.absent();
                }

                return Optional.of(TrendGraphUtils.getDurationOverTimeGraph(timePeriod, statsSamples, daysActive));
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
