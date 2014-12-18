package com.hello.suripu.core.processors;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.TrendsDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Insights.AvailableGraph;
import com.hello.suripu.core.models.Insights.DowSample;
import com.hello.suripu.core.models.Insights.GraphSample;
import com.hello.suripu.core.models.Insights.TrendGraph;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 12/15/14.
 */
public class TrendGraphProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrendGraphProcessor.class);
    private static int TRENDS_AVAILABLE_AFTER_DAYS = 10; // no trends before collecting 10 days of data


    private static boolean checkEligibility(final DateTime accountCreated) {
        if (accountCreated.plusDays(TRENDS_AVAILABLE_AFTER_DAYS).isBeforeNow()) {
            return true;
        }
        return false;
    }

    @Timed
    public static List<AvailableGraph> getAvailableGraphs(final Account account) {
        final List<AvailableGraph> graphlist = new ArrayList<>();
        final boolean eligible = checkEligibility(account.created);
        if (eligible) {
            graphlist.add(new AvailableGraph(TrendGraph.DataType.SLEEP_DURATION.getValue(), TrendGraph.TimePeriodType.DAY_OF_WEEK.getValue()));

            for (TrendGraph.TimePeriodType timePeriodType : TrendGraph.TimePeriodType.values()) {
                graphlist.add(new AvailableGraph(TrendGraph.DataType.SLEEP_SCORE.getValue(), timePeriodType.getValue()));
            }
        }
        return graphlist;
    }

    @Timed
    public static List<TrendGraph> getAllGraphs(final Account account, final TrendsDAO trendsDAO, final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB) {
        final List<TrendGraph> graphs = new ArrayList<>();
        final boolean eligible = checkEligibility(account.created);
        if (eligible) {
            final long accountId = account.id.get();
            graphs.add(getTrendGraph(accountId, TrendGraph.DataType.SLEEP_SCORE, TrendGraph.GraphType.HISTOGRAM, TrendGraph.TimePeriodType.DAY_OF_WEEK, trendsDAO, scoreDAODynamoDB));
            graphs.add(getTrendGraph(accountId, TrendGraph.DataType.SLEEP_DURATION, TrendGraph.GraphType.HISTOGRAM, TrendGraph.TimePeriodType.DAY_OF_WEEK, trendsDAO, scoreDAODynamoDB));
            graphs.add(getTrendGraph(accountId, TrendGraph.DataType.SLEEP_SCORE, TrendGraph.GraphType.TIME_SERIES_LINE, TrendGraph.TimePeriodType.OVER_TIME_ALL, trendsDAO, scoreDAODynamoDB));
        }
        return graphs;
    }

    @Timed
    public static TrendGraph getTrendGraph(final long accountId, final TrendGraph.DataType dataType,
                                           final TrendGraph.GraphType graphType,
                                           final TrendGraph.TimePeriodType timePeriodType,
                                           final TrendsDAO trendsDAO,
                                           final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB) {

        final List<GraphSample> dataPoints = new ArrayList<>();

        if (graphType == TrendGraph.GraphType.HISTOGRAM) {
            dataPoints.addAll(getTrendsDowData(dataType, accountId, trendsDAO));

        } else if (graphType == TrendGraph.GraphType.TIME_SERIES_LINE && timePeriodType != TrendGraph.TimePeriodType.DAY_OF_WEEK) {
            if (dataType == TrendGraph.DataType.SLEEP_SCORE) {
                dataPoints.addAll(getScoreOverTimeData(accountId, scoreDAODynamoDB));
            }
        }
        return new TrendGraph(dataType, graphType, timePeriodType, dataPoints);
    }


    private static List<GraphSample> getTrendsDowData(final TrendGraph.DataType dataType, final long accountId, final TrendsDAO trendsDAO) {
        // histogram data
        ImmutableList<DowSample> dowSamples = ImmutableList.copyOf(new ArrayList<DowSample>());
        if (dataType == TrendGraph.DataType.SLEEP_SCORE) {
            dowSamples = trendsDAO.getSleepScoreDow(accountId);
        } else if (dataType == TrendGraph.DataType.SLEEP_DURATION) {
            dowSamples = trendsDAO.getSleepDurationDow(accountId);
        }

        if (dowSamples.size() > 0) {
            final Map<Integer, Float> samplesMap = new HashMap<>();
            for (final DowSample sample : dowSamples) {
                samplesMap.put(sample.dayOfWeek, sample.value);
            }

            final List<GraphSample> sampleData = new ArrayList<>();
            for (int dow = 1; dow <= 7; dow++) {
                final String xLabel = TrendGraph.DayOfWeekLabel.fromInt(dow);
                if (samplesMap.containsKey(dow)) {
                    final float value = samplesMap.get(dow);
                    final TrendGraph.DataLabel label = TrendGraph.getDataLabel(dataType, value);
                    sampleData.add(new GraphSample(xLabel, value, label));
                    continue;
                }
                sampleData.add(new GraphSample(xLabel, 0.0f, TrendGraph.DataLabel.OK));
            }
            return sampleData;
        }

        return Collections.emptyList();
    }

    private static List<GraphSample> getScoreOverTimeData(final long accountId, final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB) {
        final List<GraphSample> sampleData = new ArrayList<>();

        return sampleData;
    }
}
