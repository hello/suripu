package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by kingshy on 12/15/14.
 */
public class GraphSample {
    public enum DataLabel {
        BAD(0),
        OK(1),
        GOOD(2);

        private final int value;

        private DataLabel(final int value) {this.value = value;}
    }

    @JsonProperty("datetime")
    public final long dateTime; // for time-series graph only

    @JsonProperty("y_value")
    public final float yValue;

    @JsonProperty("x_value")
    public final String xValue; // for histogram graph only

    @JsonProperty("offset_millis")
    public final int offsetMillis;

    @JsonProperty("data_label")
    public final DataLabel dataLabel;

    /**
     * for time-series data, x = datetime, y = number
     */
    public GraphSample(final long dateTime, final float value, final int offsetMillis, final DataLabel dataLabel) {
        this.dateTime = dateTime;
        this.yValue = value;
        this.offsetMillis = offsetMillis;
        this.xValue = "";
        this.dataLabel = dataLabel;
    }

    /**
     * for histogram data, x = label, y = number
a     */
    public GraphSample(final String xValue, final float value, final DataLabel dataLabel) {
        this.dateTime = 0;
        this.yValue = value;
        this.offsetMillis = 0;
        this.xValue = xValue;
        this.dataLabel = dataLabel;
    }

}