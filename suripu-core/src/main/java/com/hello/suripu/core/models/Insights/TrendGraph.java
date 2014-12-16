package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kingshy on 12/15/14.
 */
public class TrendGraph {
    public enum DataType {

        NONE("none"),
        SLEEP_SCORE("sleep_score") {
            public String toString() {return "SLEEP SCORE";}
        },
        SLEEP_DURATION("sleep_duration") {
            public String toString() {return "SLEEP DURATION";}
        },
        TIME_TO_BED("time_to_bed") {
            public String toString() {return "Time to Bed";}
        };

        private final String value;

        private DataType(final String value) {this.value = value;}

        private String getValue() {return this.value;}

        public static DataType fromString(final String text) {
            if (text != null) {
                for (DataType datatype : DataType.values()) {
                    if (text.equalsIgnoreCase(datatype.getValue())) {
                        return datatype;
                    }
                }
            }
            throw new IllegalArgumentException();
        }
    }

    public enum GraphType {
        HISTOGRAM, // day of week only
        TIME_SERIES_LINE;
    }

    public enum TimePeriodType {
        DAY_OF_WEEK("DOW"),
        OVER_TIME_1W("1W"),
        OVER_TIME_2W("2W"),
        OVER_TIME_1M("1M"),
        OVER_TIME_3M("3M"),
        OVER_TIME_ALL("ALL");

        private String value;
        private TimePeriodType(final String value) {this.value = value;}
        private String getValue() {return this.value;}

        public static TimePeriodType fromString(final String text) {
            if (text != null) {
                for (TimePeriodType periodType : TimePeriodType.values()) {
                    if (text.equalsIgnoreCase(periodType.getValue())) {
                        return periodType;
                    }
                }
            }
            throw new IllegalArgumentException();
        }

        public static List<String> getTimeSeriesOptions() {
            final List<String> options = new ArrayList<>();
            for (TimePeriodType periodType : TimePeriodType.values()) {
                if (periodType == DAY_OF_WEEK)
                    continue;
                options.add(periodType.getValue());
            }
            return options;
        }

        public String getTitle() {
            if (this.value.equals(DAY_OF_WEEK.getValue())) {
                return "BY DAY OF WEEK";
            }
            return "OVER TIME";
        }
    }

    @JsonProperty("title")
    public final String title;

    @JsonProperty("data_type")
    public final DataType dataType;

    @JsonProperty("graph_type")
    public final GraphType graphType;

    @JsonProperty("time_period")
    public final String timePeriod;

    @JsonProperty("options")
    public final List<String> options;

    @JsonProperty("data_points")
    public final List<GraphSample> dataPoints;

    /**
     * histogram graph, x = string label, y = number, no options
     */
    public TrendGraph(final DataType dataType, final GraphType graphType, final TimePeriodType timePeriod, final List<GraphSample> dataPoints) {
        this.dataType = dataType;
        this.graphType = graphType;
        this.timePeriod = timePeriod.getValue();
        this.dataPoints = dataPoints;
        this.options = Collections.emptyList();
        this.title = dataType.toString() + " " + timePeriod.getTitle();
    }

    /**
     * time-series graph, x = datetime, y = number, options
     */
    public TrendGraph(final DataType dataType, final GraphType graphType, final TimePeriodType timePeriod, final List<String> options, final List<GraphSample> dataPoints) {
        this.dataType = dataType;
        this.graphType = graphType;
        this.timePeriod = timePeriod.getValue();
        this.dataPoints = dataPoints;
        this.options = options;
        this.title = dataType.toString() + " " + timePeriod.getTitle();
    }
}
