package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AppUnreadStats {
    @JsonProperty("has_unread_insights")
    public final boolean hasUnreadInsights;

    @JsonProperty("has_unanswered_questions")
    public final boolean hasUnansweredQuestions;

    @JsonCreator
    public AppUnreadStats(final @JsonProperty("has_unread_insights") boolean hasUnreadInsights,
                          final @JsonProperty("has_unanswered_questions") boolean hasUnansweredQuestions) {
        this.hasUnreadInsights = hasUnreadInsights;
        this.hasUnansweredQuestions = hasUnansweredQuestions;
    }
}
