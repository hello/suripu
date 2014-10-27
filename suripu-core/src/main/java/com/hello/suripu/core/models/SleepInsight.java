package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

/**
 * Created by kingshy on 10/24/14.
 */
public class SleepInsight {
    @JsonProperty("title")
    final public String title;

    @JsonProperty("message")
    final public String message;

    @JsonProperty("created_utc")
    public final DateTime created_utc;

    public SleepInsight(String title, String message, DateTime created_utc) {
        this.title = title;
        this.message = message;
        this.created_utc = created_utc;
    }
}
