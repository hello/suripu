package com.hello.suripu.core.trends.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.List;

/**
 * Created by ksg on 01/21/16
 */
public class GraphSection {

    public static final Float MISSING_VALUE = -1.0f;

    @JsonProperty("values")
    public final List<Float> values;

    @JsonProperty("titles")
    public final List<String> titles;

    @JsonProperty("highlighted_values")
    public final List<Integer> highlightedValues;

    @JsonProperty("highlighted_title")
    public final Optional<Integer> highlightedTitle;


    public GraphSection(final List<Float> values, final List<String> titles,
                        final List<Integer> highlightedValues, final Optional<Integer> highlightedTitle) {
        this.values = values;
        this.titles = titles;
        this.highlightedValues = highlightedValues;
        this.highlightedTitle = highlightedTitle;
    }

}
