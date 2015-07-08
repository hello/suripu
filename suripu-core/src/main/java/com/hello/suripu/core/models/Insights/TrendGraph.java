package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.translations.English;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 12/15/14.
 */
public class TrendGraph {
    public final static Map<TimePeriodType, Integer> PERIOD_TYPE_DAYS;
    static {
       final Map<TimePeriodType, Integer> tmpMap = new LinkedHashMap<>();
        tmpMap.put(TimePeriodType.OVER_TIME_1W, 7);
        tmpMap.put(TimePeriodType.OVER_TIME_2W, 14);
        tmpMap.put(TimePeriodType.OVER_TIME_1M, 30);
        tmpMap.put(TimePeriodType.OVER_TIME_3M, 90);
        tmpMap.put(TimePeriodType.OVER_TIME_ALL, 90); // force this to be 90 days
        PERIOD_TYPE_DAYS = ImmutableMap.copyOf(tmpMap);
    }

    public enum DataType {

        NONE("none"),
        SLEEP_SCORE("sleep_score") {
            public String toString() {return "SLEEP SCORE";}
        },
        SLEEP_DURATION("sleep_duration") {
            public String toString() {return "SLEEP DURATION";}
        };

        private final String value;

        private DataType(final String value) {this.value = value;}

        public String getValue() {return this.value;}

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
        public String getValue() {return this.value;}

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

        public static List<String> getTimeSeriesOptions(final int numDaysActive) {
            final List<String> options = new ArrayList<>();
            for (TimePeriodType periodType : PERIOD_TYPE_DAYS.keySet()) {
                // don't ever return ALL option
                if (periodType == DAY_OF_WEEK || PERIOD_TYPE_DAYS.get(periodType) > numDaysActive || periodType == OVER_TIME_ALL) {
                    continue;
                }
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

    public enum DayOfWeekLabel {
        MO(1), TU(2), WE(3), TH(4), FR(5), SA(6), SU(7);

        private int value;

        private DayOfWeekLabel(final int value) {this.value = value;}

        public static String fromInt(final int value) {
            if (value > 0) {
                for (final DayOfWeekLabel label : DayOfWeekLabel.values()) {
                    if (value == label.value) {
                        return label.toString();
                    }
                }
            }
            throw new IllegalArgumentException();
        }

    }
    public enum DataLabel {
        BAD(0),
        OK(1),
        GOOD(2);

        private final int value;

        private DataLabel(final int value) {this.value = value;}
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
        this.title = getTrendGraphTitle(dataType, timePeriod);
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
        this.title = getTrendGraphTitle(dataType, timePeriod);
    }

    public static DataLabel getDataLabel(final DataType dataType, final float value) {
        if (dataType == DataType.SLEEP_SCORE) {
            if (value > 80.0f) {
                return DataLabel.GOOD;
            } else if (value > 75.0f) {
                return DataLabel.OK;
            } else {
                return DataLabel.BAD;
            }
        } else if (dataType == DataType.SLEEP_DURATION) {
            // TODO: personalize later
            if (value >= 450.0f && value <= 510.0f) { // 7.5 to 8.5 hours
                return DataLabel.GOOD;
            } else if (value >= 390.0f && value <= 600.0f) { // 6.5 hours to 10 hours
                return DataLabel.OK;
            } else {
                return DataLabel.BAD; // less than 6.5 hours, or more than 10 hours
            }
        } else {
            return DataLabel.OK;
        }
    }

    static String getTrendGraphTitle(final DataType dataType, final TimePeriodType timePeriod) {
        if (dataType == DataType.SLEEP_SCORE) {
            if (timePeriod == TimePeriodType.DAY_OF_WEEK) {
                return English.TRENDS_SCORE_AVERAGE;
            } else {
                return English.TRENDS_SCORE_OVER_TIME;
            }
        } else if (dataType == DataType.SLEEP_DURATION) {
            if (timePeriod == TimePeriodType.DAY_OF_WEEK) {
                return English.TRENDS_DURATION_AVERAGE;
            } else {
                return English.TRENDS_DURATION_OVER_TIME;
            }
        }
        return "";
    }

    public static int getTimePeriodDays(final TimePeriodType timePeriodType) {
        if (PERIOD_TYPE_DAYS.containsKey(timePeriodType)) {
            return PERIOD_TYPE_DAYS.get(timePeriodType);
        }
        return 365;
    }
}
