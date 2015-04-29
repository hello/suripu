package com.hello.suripu.research.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by benjo on 4/29/15.
 */
public class MatchedFeedback {
    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("date")
    public final Long date;

    @JsonProperty("event_type")
    public final String eventType;

    @JsonProperty("delta")
    public final Integer eventDeltaInMillis;

    @JsonProperty("algorithm")
    public final String algorithm;

    @JsonProperty("version")
    public final String version;

    public MatchedFeedback(final Long accountId,final Long date,final String eventType,final Integer eventDeltaInMillis,final String algorithm,final String version) {
        this.accountId = accountId;
        this.date = date;
        this.eventType = eventType;
        this.eventDeltaInMillis = eventDeltaInMillis;
        this.algorithm = algorithm;
        this.version = version;
    }
}


