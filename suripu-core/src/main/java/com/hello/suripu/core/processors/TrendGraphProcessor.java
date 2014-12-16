package com.hello.suripu.core.processors;

import com.hello.suripu.core.models.Insights.GraphSample;
import com.hello.suripu.core.models.Insights.TrendGraph;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kingshy on 12/15/14.
 */
public class TrendGraphProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrendGraphProcessor.class);

    @Timed
    public static TrendGraph getTrendGraph(final long account_id, final TrendGraph.DataType dataType,
                                           final TrendGraph.GraphType graphType,
                                           final TrendGraph.TimePeriodType timePeriodType) {

        final List<GraphSample> dataPoints = new ArrayList<>();

        if (graphType == TrendGraph.GraphType.HISTOGRAM) {
            // PLACEHOLDER
            // TODO: Use real data from somewhere out there
            dataPoints.add(new GraphSample("M", 80.0f, GraphSample.DataLabel.OK));
            dataPoints.add(new GraphSample("TU", 77.0f, GraphSample.DataLabel.BAD));
            dataPoints.add(new GraphSample("W", 88.0f, GraphSample.DataLabel.GOOD));
            dataPoints.add(new GraphSample("TH", 85.0f, GraphSample.DataLabel.BAD));
            dataPoints.add(new GraphSample("F", 73.0f, GraphSample.DataLabel.GOOD));
            dataPoints.add(new GraphSample("SA", 97.0f, GraphSample.DataLabel.GOOD));
            dataPoints.add(new GraphSample("SU", 92.0f, GraphSample.DataLabel.GOOD));
            return new TrendGraph(dataType, graphType, TrendGraph.TimePeriodType.DAY_OF_WEEK, dataPoints);

        } else if (graphType == TrendGraph.GraphType.TIME_SERIES_LINE && timePeriodType != TrendGraph.TimePeriodType.DAY_OF_WEEK) {
            final DateTime now = DateTime.now().withTimeAtStartOfDay();
            final int offset = -28800000;
            dataPoints.add(new GraphSample(now.getMillis(), 100.0f, offset, GraphSample.DataLabel.GOOD));
            dataPoints.add(new GraphSample(now.minusDays(1).getMillis(), 80.0f, offset, GraphSample.DataLabel.GOOD));
            dataPoints.add(new GraphSample(now.minusDays(2).getMillis(), 65.0f, offset, GraphSample.DataLabel.BAD));
            dataPoints.add(new GraphSample(now.minusDays(3).getMillis(), 88.0f, offset, GraphSample.DataLabel.GOOD));
            dataPoints.add(new GraphSample(now.minusDays(4).getMillis(), 72.0f, offset, GraphSample.DataLabel.BAD));
            dataPoints.add(new GraphSample(now.minusDays(5).getMillis(), 90.0f, offset, GraphSample.DataLabel.GOOD));
            return new TrendGraph(dataType, graphType, TrendGraph.TimePeriodType.OVER_TIME_ALL, TrendGraph.TimePeriodType.getTimeSeriesOptions(), dataPoints);
        }

        return new TrendGraph(dataType, graphType, timePeriodType, dataPoints);
    }
}
