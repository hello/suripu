package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

/**
 * Created by kingshy on 10/24/14.
 */
public class SleepInsight {
    @JsonProperty("id")
    public final long id;

    @JsonProperty("account_id")
    public final Optional<Long> accountId;

    @JsonProperty("title")
    public final String title;

    @JsonProperty("message")
    public final String message;

    @JsonProperty("category")
    public final GenericInsightCards.Category category;

    @JsonProperty("local_datetime")
    public final DateTime localTimestampUTC; // local time in UTC

    @JsonProperty("timestamp")
    public final DateTime timestamp;

    @JsonIgnore
    public final int offsetMillis;

    public SleepInsight(final long id, final Long accountId, final String title, final String message, final GenericInsightCards.Category category,
                        final DateTime localTimestampUTC, final DateTime timestamp, final int offsetMillis) {
        this.id = id;
        this.accountId = Optional.fromNullable(accountId);
        this.title = title;
        this.message = message;
        this.category = category;
        this.localTimestampUTC = localTimestampUTC;
        this.timestamp = timestamp;
        this.offsetMillis = offsetMillis;
    }
}
