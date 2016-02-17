package com.hello.suripu.core.trends.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

/**
 * Created by ksg on 01/21/16
 */
public class Annotation {

    public static final int ANNOTATION_ENABLED_THRESHOLD = 7; // accounts older than 7 days

    @JsonProperty("title")
    public final String title;

    @JsonProperty("value")
    public final float value;

    @JsonProperty("data_type")
    public final DataType dataType;

    @JsonProperty("condition")
    public final Optional<Condition> condition;

    public Annotation(final String title, final float value, final DataType dataType, final Optional<Condition> condition) {
        this.title = title;
        this.value = value;
        this.dataType = dataType;
        this.condition = condition;
    }
}
