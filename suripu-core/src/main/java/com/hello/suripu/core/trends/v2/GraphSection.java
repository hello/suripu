package com.hello.suripu.core.trends.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 1/21/16.
 */
public class GraphSection {

    public static final Float MISSING_VALUE = -1.0f;

    @JsonProperty("values")
    public final List<Float> values;

    @JsonProperty("titles")
    public final Optional<List<String>> titles;

    @JsonProperty("highlighted_values")
    public final List<Integer> highlightedValues;

    @JsonProperty("highlighted_title")
    public final Integer highlightedTitle;


    public GraphSection(final List<Float> values, final Optional<List<String>> titles,
                        final List<Integer> highlightedValues, final Integer highlightedTitle) {
        this.values = values;
        this.titles = titles;
        this.highlightedValues = highlightedValues;
        this.highlightedTitle = highlightedTitle;
    }

    public static Map<String, Float> getDayOfWeekSectionTemplate() {
        final Map<String, Float> dowMap = Maps.newLinkedHashMap();
        dowMap.put("SUN", null);
        dowMap.put("MON", null);
        dowMap.put("TUE", null);
        dowMap.put("WED", null);
        dowMap.put("THU", null);
        dowMap.put("FRI", null);
        dowMap.put("SAT", null);
        return dowMap;
    }

    public static Map<String, Float> resetTemplate(final Map<String, Float> template) {
        Map<String, Float> newMap = Maps.newLinkedHashMap();
        for (final String key: template.keySet()) {
            newMap.put(key, null);
        }
        return newMap;
    }

}
