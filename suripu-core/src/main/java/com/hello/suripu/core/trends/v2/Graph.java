package com.hello.suripu.core.trends.v2;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by ksg on 01/21/16
 */
public class Graph {

    @JsonProperty("time_scale")
    public final TimeScale timeScale;

    @JsonProperty("title")
    public final String title;

    @JsonProperty("data_type")
    public final DataType dataType;

    @JsonProperty("graph_type")
    public final GraphType graphType;

    @JsonProperty("min_value")
    public final float minValue;

    @JsonProperty("max_value")
    public final float maxValue;

    @JsonProperty("sections")
    public final List<GraphSection> sections;

    @JsonProperty("condition_ranges")
    public final List<ConditionRange> conditionRanges;

    @JsonProperty("annotations")
    public final List<Annotation> annotations;

    public Graph(final TimeScale timeScale, final String title,
                 final DataType dataType, final GraphType graphType,
                 final float minValue, final float maxValue,
                 final List<GraphSection> sections,
                 final List<ConditionRange> conditionRanges,
                 final List<Annotation> annotations) {

        this.timeScale = timeScale;
        this.title = title;
        this.dataType = dataType;
        this.graphType = graphType;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.sections = sections;
        this.conditionRanges = conditionRanges;
        this.annotations = annotations;
    }
}