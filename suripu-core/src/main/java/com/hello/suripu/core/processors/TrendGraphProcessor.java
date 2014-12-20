package com.hello.suripu.core.processors;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.Insights.AvailableGraph;
import com.hello.suripu.core.models.Insights.DowSample;
import com.hello.suripu.core.models.Insights.GraphSample;
import com.hello.suripu.core.models.Insights.SleepStatsSample;
import com.hello.suripu.core.models.Insights.TrendGraph;
import com.hello.suripu.core.util.DateTimeUtil;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
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
    private static long DAY_IN_MILLIS = 86400000;


    @Timed
    public static List<AvailableGraph> getGraphList(final Account account) {
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
    public static List<TrendGraph> getAllGraphs(final Account account,
                                                final TrendsDAO trendsDAO,
                                                final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                                                final TrackerMotionDAO trackerMotionDAO) {
        final List<TrendGraph> graphs = new ArrayList<>();
        final boolean eligible = checkEligibility(account.created);
        if (eligible) {
            // add all the default graphs
            final long accountId = account.id.get();

            final TrendGraph sleepScoreDayOfWeek = getTrendGraph(accountId, TrendGraph.DataType.SLEEP_SCORE, TrendGraph.GraphType.HISTOGRAM, TrendGraph.TimePeriodType.DAY_OF_WEEK, trendsDAO, scoreDAODynamoDB, trackerMotionDAO);
            graphs.add(sleepScoreDayOfWeek);

            final TrendGraph sleepDurationDayOfWeek = getTrendGraph(accountId, TrendGraph.DataType.SLEEP_DURATION, TrendGraph.GraphType.HISTOGRAM, TrendGraph.TimePeriodType.DAY_OF_WEEK, trendsDAO, scoreDAODynamoDB, trackerMotionDAO);
            graphs.add(sleepDurationDayOfWeek);

            final TrendGraph sleepScoreOverTime = getTrendGraph(accountId, TrendGraph.DataType.SLEEP_SCORE, TrendGraph.GraphType.TIME_SERIES_LINE, TrendGraph.TimePeriodType.OVER_TIME_ALL, trendsDAO, scoreDAODynamoDB, trackerMotionDAO);
            graphs.add(sleepScoreOverTime);
        }
        return graphs;
    }

    @Timed
    public static TrendGraph getTrendGraph(final long accountId, final TrendGraph.DataType dataType,
                                           final TrendGraph.GraphType graphType,
                                           final TrendGraph.TimePeriodType timePeriodType,
                                           final TrendsDAO trendsDAO,
                                           final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                                           final TrackerMotionDAO trackerMotionDAO) {

        List<GraphSample> dataPoints = new ArrayList<>();

        if (graphType == TrendGraph.GraphType.HISTOGRAM) {
            dataPoints = getTrendsDowData(dataType, accountId, trendsDAO);
            return new TrendGraph(dataType, graphType, timePeriodType, dataPoints);

        } else if (graphType == TrendGraph.GraphType.TIME_SERIES_LINE && timePeriodType != TrendGraph.TimePeriodType.DAY_OF_WEEK) {

            if (dataType == TrendGraph.DataType.SLEEP_SCORE) {
                dataPoints = getScoreOverTimeData(accountId, timePeriodType, scoreDAODynamoDB, trackerMotionDAO);
            } else if (dataType == TrendGraph.DataType.SLEEP_DURATION) {
                dataPoints = getDurationOverTimeData(accountId, timePeriodType, trendsDAO);
            }
            return new TrendGraph(dataType, graphType, timePeriodType, TrendGraph.TimePeriodType.getTimeSeriesOptions(), dataPoints);

        }
        return new TrendGraph(dataType, graphType, timePeriodType, dataPoints);
    }


    private static List<GraphSample> getTrendsDowData(final TrendGraph.DataType dataType, final long accountId, final TrendsDAO trendsDAO) {
        // histogram data
        List<DowSample> dowSamples = new ArrayList<>();
        if (dataType == TrendGraph.DataType.SLEEP_SCORE) {
            dowSamples.addAll(trendsDAO.getSleepScoreDow(accountId));
        } else if (dataType == TrendGraph.DataType.SLEEP_DURATION) {
            dowSamples.addAll(trendsDAO.getSleepDurationDow(accountId));
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

    private static List<GraphSample> getScoreOverTimeData(final long accountId, TrendGraph.TimePeriodType timePeriodType,
                                                          final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                                                          final TrackerMotionDAO trackerMotionDAO) {
        // over time graph is only available for sleep-score at this time
        final int numDays = TrendGraph.getTimePeriodDays(timePeriodType);
        final DateTime endDateTime = DateTime.now().withTimeAtStartOfDay();
        final DateTime startDateTime = endDateTime.minusDays(numDays);

        // get timezone offsets for the required dates
        final Map<DateTime, Integer> userOffsetMillis = getUserTimeZoneOffsetsUTC(accountId, startDateTime, endDateTime, trackerMotionDAO);

        // get daily scores
        final ImmutableList<AggregateScore> scores = scoreDAODynamoDB.getBatchScores(accountId,
                DateTimeUtil.dateToYmdString(startDateTime),
                DateTimeUtil.dateToYmdString(endDateTime), numDays);

        // aggregate
        final List<GraphSample> sampleData = new ArrayList<>();
        for (final AggregateScore score : scores) {
            final float value = (float) score.score;
            final TrendGraph.DataLabel label = TrendGraph.getDataLabel(TrendGraph.DataType.SLEEP_SCORE, value);
            final DateTime date = getDateTimeFromString(score.date);
            int offsetMillis = 0;
            if (userOffsetMillis.containsKey(date)) {
                offsetMillis = userOffsetMillis.get(date);
            }
            sampleData.add(new GraphSample(date.getMillis(), value, offsetMillis, label));
        }
        return sampleData;
    }

    private static List<GraphSample> getDurationOverTimeData(final long accountId, TrendGraph.TimePeriodType timePeriodType, final TrendsDAO trendsDAO) {
        ImmutableList<SleepStatsSample> statsSamples;
        if (timePeriodType == TrendGraph.TimePeriodType.OVER_TIME_ALL) {
            statsSamples = trendsDAO.getAccountSleepStatsAll(accountId);
        } else {
            final int numDays = TrendGraph.getTimePeriodDays(timePeriodType);
            final DateTime endDateTime = DateTime.now().withTimeAtStartOfDay();
            final DateTime startDateTime = endDateTime.minusDays(numDays);
            statsSamples = trendsDAO.getAccountSleepStatsBetweenDates(accountId, startDateTime, endDateTime);
        }

        final List<GraphSample> sampleData = new ArrayList<>();
        for (final SleepStatsSample sample : statsSamples) {
            final Integer value = sample.stats.sleepDurationInMinutes;
            final TrendGraph.DataLabel label = TrendGraph.getDataLabel(TrendGraph.DataType.SLEEP_DURATION, value);
            sampleData.add(new GraphSample(sample.localUTCDate.getMillis(), value, sample.timeZoneOffset, label));
        }
        return sampleData;
    }

    // map keys in UTC
    private static Map<DateTime, Integer> getUserTimeZoneOffsetsUTC(final long accountId, final DateTime startDate, final DateTime endDate, final TrackerMotionDAO trackerMotionDAO) {
        final long daysDiff = (endDate.getMillis() - startDate.getMillis()) / DAY_IN_MILLIS;
        final List<DateTime> dates = new ArrayList<>();
        for (int i = 0; i < (int) daysDiff; i++) {
            dates.add(startDate.withZone(DateTimeZone.UTC).withTimeAtStartOfDay().plusDays(i));
        }
        return trackerMotionDAO.getOffsetMillisForDates(accountId, dates);
    }

    private static boolean checkEligibility(final DateTime accountCreated) {
        if (accountCreated.plusDays(TRENDS_AVAILABLE_AFTER_DAYS).isBeforeNow()) {
            return true;
        }
        return false;
    }

    private static DateTime getDateTimeFromString(final String dateString) {
        return  DateTime.parse(dateString, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withTimeAtStartOfDay();
    }
}
