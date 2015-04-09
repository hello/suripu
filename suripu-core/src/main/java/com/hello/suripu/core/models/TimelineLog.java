package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by benjo on 4/9/15.
 */
public class TimelineLog {

    @JsonProperty("algorithm")
    public final String algorithm;

    @JsonProperty("version")
    public final String version;

    @JsonProperty("created_date")
    public final Long createdDate;

    @JsonProperty("target_date")
    public final Long targetDate;


    @JsonCreator
    public static TimelineLog create(@JsonProperty("algorithm") final String score,
                                  @JsonProperty("version") final String message,
                                  @JsonProperty("created_date") final Long createdDate,
                                  @JsonProperty("target_date") final Long targetDate) {

        return new TimelineLog(score, message, createdDate, targetDate);

    }

    public TimelineLog(String algorithm,String version, Long createdDate, Long targetDate) {
        this.algorithm = algorithm;
        this.version = version;

        this.createdDate = createdDate;
        this.targetDate = targetDate;
    }

    static public TimelineLog createEmpty() {
        return new TimelineLog("","",0L,0L);
    }
}
