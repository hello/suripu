package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import org.joda.time.DateTime;

public class AppStats {
    @JsonProperty(value = "insights_last_viewed", required = false)
    public final Optional<DateTime> insightsLastViewed;

    @JsonProperty(value = "questions_last_viewed", required = false)
    public final Optional<DateTime> questionsLastViewed;

    @JsonCreator
    public AppStats(final @JsonProperty("insights_last_viewed") Optional<DateTime> insightsLastViewed,
                    final @JsonProperty("questions_last_viewed") Optional<DateTime> questionsLastViewed) {
        this.insightsLastViewed = insightsLastViewed;
        this.questionsLastViewed = questionsLastViewed;
    }
}
