package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by jyfan on 7/20/16.
 */
public class SumCountData {

    @JsonProperty("sum")
    public final int sum;

    @JsonProperty("count")
    public final int count;

    @JsonCreator
    public SumCountData(@JsonProperty("sum") final int sum,
                         @JsonProperty("count") final int count) {
        this.sum = sum;
        this.count = count;
    }
}
