package com.hello.suripu.core.util;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.Insights.AvailableGraph;
import com.hello.suripu.core.models.Insights.DowSample;
import com.hello.suripu.core.models.Insights.GraphSample;
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

        // return data in a list for Sunday - Saturday
        final List<Integer> dayOfWeek = Lists.newArrayList(7, 1, 2, 3, 4, 5, 6);

        for (final Integer dow : dayOfWeek) {
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
                                                    final List<AggregateSleepStats> stats,
                                                    final int numDaysActive) {
        // aggregate
        final List<GraphSample> dataPoints = new ArrayList<>();

        for (final AggregateSleepStats stat : stats) {
            final float value = (float) stat.sleepScore;
            final TrendGraph.DataLabel label = TrendGraph.getDataLabel(TrendGraph.DataType.SLEEP_SCORE, value);
            final DateTime date = stat.dateTime;
            dataPoints.add(new GraphSample(date.getMillis(), value, stat.offsetMillis, label));
        }

        final List<String> timeSeriesOptions = TrendGraph.TimePeriodType.getTimeSeriesOptions(numDaysActive);
        return new TrendGraph(TrendGraph.DataType.SLEEP_SCORE, TrendGraph.GraphType.TIME_SERIES_LINE, timePeriodType, timeSeriesOptions, dataPoints);
    }

    public static TrendGraph getDurationOverTimeGraph(final TrendGraph.TimePeriodType timePeriodType,
                                                      final List<AggregateSleepStats> sleepStats, final int numDaysActive) {

        final List<GraphSample> dataPoints = new ArrayList<>();
        for (final AggregateSleepStats stat : sleepStats) {
            final Integer value = stat.sleepStats.sleepDurationInMinutes;
            final TrendGraph.DataLabel label = TrendGraph.getDataLabel(TrendGraph.DataType.SLEEP_DURATION, value);
            dataPoints.add(new GraphSample(stat.dateTime.getMillis(), value, stat.offsetMillis, label));
        }

        final List<String> timeSeriesOptions = TrendGraph.TimePeriodType.getTimeSeriesOptions(numDaysActive);
        return new TrendGraph(TrendGraph.DataType.SLEEP_DURATION, TrendGraph.GraphType.TIME_SERIES_LINE, timePeriodType, timeSeriesOptions, dataPoints);
    }

    public static List<AvailableGraph> getGraphList(final Account account) {

        final List<AvailableGraph> graphlist = new ArrayList<>();
        graphlist.add(new AvailableGraph(TrendGraph.DataType.SLEEP_DURATION.getValue(), TrendGraph.TimePeriodType.DAY_OF_WEEK.getValue()));

        for (TrendGraph.TimePeriodType timePeriodType : TrendGraph.TimePeriodType.values()) {
            graphlist.add(new AvailableGraph(TrendGraph.DataType.SLEEP_SCORE.getValue(), timePeriodType.getValue()));
        }
        return graphlist;
    }

    public static List<DowSample> aggregateDOWData(final List<AggregateSleepStats> sleepStats, final TrendGraph.DataType dataType) {
        final List<DowSample> dayOfWeekData = new ArrayList<>();

        final List<Integer> sums = Lists.newArrayList(0, 0, 0, 0, 0, 0, 0);
        final List<Integer> counts = Lists.newArrayList(0, 0, 0, 0, 0, 0, 0);

        for (final AggregateSleepStats stat: sleepStats) {
            Integer value = stat.sleepScore;
            if (dataType == TrendGraph.DataType.SLEEP_DURATION) {
                value = stat.sleepStats.sleepDurationInMinutes;
            }

            final int dayOfWeek = stat.dateTime.getDayOfWeek() - 1;
            value += sums.get(dayOfWeek);
            sums.set(dayOfWeek, value);

            final int count = counts.get(dayOfWeek) + 1;
            counts.set(dayOfWeek, count);
        }

        for (int i = 0; i < 7; i++) {
            final float avgValue = (counts.get(i) > 0) ? ((float) sums.get(i)) / counts.get(i) : 0.0f;
            dayOfWeekData.add(new DowSample(i+1, avgValue));
        }
        return dayOfWeekData;
    }

    private static DateTime getDateTimeFromString(final String dateString) {
        return  DateTime.parse(dateString, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withTimeAtStartOfDay();
    }

}
