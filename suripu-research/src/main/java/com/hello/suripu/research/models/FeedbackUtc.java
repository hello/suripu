package com.hello.suripu.research.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by benjo on 7/28/15.
 */
public class FeedbackUtc {

    @JsonProperty("event_type")
    public final String eventType;

    @JsonProperty("old_time_utc")
    public final Long feedbackOldTime;

    @JsonProperty("new_time_utc")
    public final Long feedbackNewTime;

    @JsonProperty("account_id")
    public final Long accountId;

    public FeedbackUtc(final String eventType,final Long feedbackOldTime,final Long feedbackNewTime,final Long accountId) {
        this.eventType = eventType;
        this.feedbackOldTime = feedbackOldTime;
        this.feedbackNewTime = feedbackNewTime;
        this.accountId = accountId;
    }




}
