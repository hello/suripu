package com.hello.suripu.core.trends.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.List;

/**
 * Created by kingshy on 1/21/16.
 */
public class Graph {

    public static final String TITLE_SLEEP_SCORE = "Sleep Score";
    public static final String TITLE_SLEEP_DEPTH = "Sleep Depth";
    public static final String TITLE_SLEEP_DURATION = "Sleep Duration";

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

    @JsonProperty("conditional_ranges")
    public final Optional<List<ConditionRange>> conditionRanges;

    @JsonProperty("annotations")
    public final Optional<List<Annotation>> annotations;

    public Graph(final TimeScale timeScale, final String title,
                 final DataType dataType, final GraphType graphType,
                 final float minValue, final float maxValue,
                 final List<GraphSection> sections,
                 final Optional<List<ConditionRange>> conditionRanges,
                 final Optional<List<Annotation>> annotations) {

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