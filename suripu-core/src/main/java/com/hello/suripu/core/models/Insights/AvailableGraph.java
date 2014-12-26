package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by kingshy on 12/16/14.
 */
public class AvailableGraph {
    @JsonProperty("data_type")
    public final String dataType;

    @JsonProperty("time_period")
    public final String timePeriod;

    public AvailableGraph(final String dataType, final String timePeriod) {
        this.dataType = dataType;
        this.timePeriod = timePeriod;
    }
}
