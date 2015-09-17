package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AppUnreadStats {
    @JsonProperty("has_unread_insights")
    public final boolean hasUnreadInsights;

    @JsonCreator
    public AppUnreadStats(final @JsonProperty("has_unread_insights") boolean hasUnreadInsights) {
        this.hasUnreadInsights = hasUnreadInsights;
    }
}
