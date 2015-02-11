package com.hello.suripu.core.util;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.Insights.AvailableGraph;
import com.hello.suripu.core.models.Insights.DowSample;
import com.hello.suripu.core.models.Insights.GraphSample;
import com.hello.suripu.core.models.Insights.SleepStatsSample;
import com.hello.suripu.core.models.Insights.TrendGraph;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 12/22/14.
 */
public class TrendGraphUtils {

    private static int TRENDS_AVAILABLE_AFTER_DAYS = 7; // no trends before collecting 10 days of data

    public static TrendGraph getDayOfWeekGraph(final TrendGraph.DataType dataType, final TrendGraph.TimePeriodType timePeriodType, final List<DowSample> rawData) {

        if (rawData.size() == 0) {
            return new TrendGraph(dataType, TrendGraph.GraphType.HISTOGRAM, timePeriodType, Collections.<GraphSample>emptyList());
        }

        // create
        final Map<Integer, Float> samplesMap = new HashMap<>();
        for (final DowSample sample : rawData) {
            samplesMap.put(sample.dayOfWeek, sample.value);
        }

        final List<GraphSample> dataPoints = new ArrayList<>();

        for (int dow = 1; dow <= 7; dow++) {
            final String xLabel = TrendGraph.DayOfWeekLabel.fromInt(dow);
            float value = 0.0f;
            TrendGraph.DataLabel label = TrendGraph.DataLabel.OK;

            if (samplesMap.containsKey(dow)) {
                value = samplesMap.get(dow);
                label = TrendGraph.getDataLabel(dataType, value);
            }

            dataPoints.add(new GraphSample(xLabel, value, label));
        }

        return new TrendGraph(dataType, TrendGraph.GraphType.HISTOGRAM, timePeriodType, dataPoints);
    }

    public static TrendGraph getScoresOverTimeGraph(final TrendGraph.TimePeriodType timePeriodType,
                                                    final ImmutableList<AggregateScore> scores,
                                                    final Map<DateTime, Integer> userOffsetMillis,
                                                    final int numDaysActive) {
        // aggregate
        final List<GraphSample> dataPoints = new ArrayList<>();

        for (final AggregateScore score : scores) {
            final float value = (float) score.score;
            final TrendGraph.DataLabel label = TrendGraph.getDataLabel(TrendGraph.DataType.SLEEP_SCORE, value);
            final DateTime date = getDateTimeFromString(score.date);

            int offsetMillis = 0;
            if (userOffsetMillis.containsKey(date)) {
                offsetMillis = userOffsetMillis.get(date);
            }

            dataPoints.add(new GraphSample(date.getMillis(), value, offsetMillis, label));
        }

        final List<String> timeSeriesOptions = TrendGraph.TimePeriodType.getTimeSeriesOptions(numDaysActive);
        return new TrendGraph(TrendGraph.DataType.SLEEP_SCORE, TrendGraph.GraphType.TIME_SERIES_LINE, timePeriodType, timeSeriesOptions, dataPoints);
    }

    public static TrendGraph getDurationOverTimeGraph(final TrendGraph.TimePeriodType timePeriodType,
                                                      final ImmutableList<SleepStatsSample> statsSamples, final int numDaysActive) {

        final List<GraphSample> dataPoints = new ArrayList<>();
        for (final SleepStatsSample sample : statsSamples) {
            final Integer value = sample.stats.sleepDurationInMinutes;
            final TrendGraph.DataLabel label = TrendGraph.getDataLabel(TrendGraph.DataType.SLEEP_DURATION, value);
            dataPoints.add(new GraphSample(sample.localUTCDate.getMillis(), value, sample.timeZoneOffset, label));
        }

        final List<String> timeSeriesOptions = TrendGraph.TimePeriodType.getTimeSeriesOptions(numDaysActive);
        return new TrendGraph(TrendGraph.DataType.SLEEP_DURATION, TrendGraph.GraphType.TIME_SERIES_LINE, timePeriodType, timeSeriesOptions, dataPoints);
    }

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

    public static boolean checkEligibility(final DateTime accountCreated) {
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
