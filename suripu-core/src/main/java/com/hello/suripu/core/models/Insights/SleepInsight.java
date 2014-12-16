package com.hello.suripu.core.models.Insights;

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

    @JsonProperty("created_utc")
    public final DateTime created_utc;

    public SleepInsight(final long id, final Optional<Long> accountId, final String title, final String message, final GenericInsightCards.Category category, final DateTime created_utc) {
        this.id = id;
        this.accountId = accountId;
        this.title = title;
        this.message = message;
        this.category = category;
        this.created_utc = created_utc;
    }
}
