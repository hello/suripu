package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

public class AppStats {
    @JsonProperty("insights_last_viewed")
    public final Optional<DateTime> insightsLastViewed;

    @JsonCreator
    public AppStats(final @JsonProperty("insights_last_viewed") Optional<DateTime> insightsLastViewed) {
        this.insightsLastViewed = insightsLastViewed;
    }
}
