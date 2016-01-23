package com.hello.suripu.core.trends.v2;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by kingshy on 1/21/16.
 */
public class TrendsResult {
    @JsonProperty("available_time_scales")
    public final List<TimeScale> timeScales;

    @JsonProperty("graphs")
    public final List<Graph> graphs;

    public TrendsResult(final List<TimeScale> timeScales, final List<Graph> graphs) {
        this.timeScales = timeScales;
        this.graphs = graphs;
    }
}
