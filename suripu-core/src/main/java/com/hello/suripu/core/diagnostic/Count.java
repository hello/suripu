package com.hello.suripu.core.diagnostic;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class Count {

    @JsonProperty("ts")
    final public DateTime date;

    @JsonProperty("count")
    final public Integer count;

    public Count(final DateTime date, final Integer count) {
        this.date = date;
        this.count = count;
    }
}
