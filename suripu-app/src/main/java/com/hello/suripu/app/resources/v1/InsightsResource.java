package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Insights.AvailableGraph;
import com.hello.suripu.core.models.Insights.SleepInsight;
import com.hello.suripu.core.models.Insights.TrendGraph;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.processors.TrendGraphProcessor;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
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
            @Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken) {

        LOGGER.debug("Returning list of insights for account id = {}", accessToken.accountId);

        return InsightProcessor.getInsights(accessToken.accountId);
    }

    /**
     * get a specific graph
     * @param accessToken
     * @param dataType
     * @param timePeriod
     * @return
     */
    @Timed
    @GET
    @Path("/trends/graph")
    @Produces(MediaType.APPLICATION_JSON)
    public TrendGraph getTrends(
            @Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken,
            @QueryParam("data_type") String dataType,
            @QueryParam("time_period") String timePeriod) {

        LOGGER.debug("Returning data to plot trends for account id = {}", accessToken.accountId);

        final TrendGraph.TimePeriodType timePeriodType = TrendGraph.TimePeriodType.fromString(timePeriod);

        TrendGraph.GraphType graphType = TrendGraph.GraphType.TIME_SERIES_LINE;
        if (timePeriodType == TrendGraph.TimePeriodType.DAY_OF_WEEK) {
            graphType = TrendGraph.GraphType.HISTOGRAM;
        }

        final TrendGraph.DataType graphDataType = TrendGraph.DataType.fromString(dataType);

        return TrendGraphProcessor.getTrendGraph(accessToken.accountId, graphDataType, graphType, timePeriodType);
    }

    /**
     * get a list of available trend graphs
     * @param accessToken
     * @return
     */
    @Timed
    @GET
    @Path("/trends/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AvailableGraph> getTrendsList(
            @Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken) {
        LOGGER.debug("Returning list of available graphs account id = {}", accessToken.accountId);

        final Optional<Account> optionalAccount = accountDAO.getById(accessToken.accountId);
        if (optionalAccount.isPresent()) {
            return TrendGraphProcessor.getAvailableGraphs(optionalAccount.get());
        }
        return Collections.emptyList();
    }

    /**
     * get all graphs
     */
    @Timed
    @GET
    @Path("/trends/all")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrendGraph> getAllTrends(
            @Scope(OAuthScope.INSIGHTS_READ) final AccessToken accessToken) {
        LOGGER.debug("Returning ALL available graphs account id = {}", accessToken.accountId);

        final Optional<Account> optionalAccount = accountDAO.getById(accessToken.accountId);
        if (optionalAccount.isPresent()) {
            return TrendGraphProcessor.getAllGraphs(optionalAccount.get());
        }
        return Collections.emptyList();
    }

}
