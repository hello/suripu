package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SleepFeedback {

    public final Optional<Long> accountId;
    public final String day;
    public final String hour;
    public final Boolean correct;

    @JsonCreator
    public SleepFeedback(
            @JsonProperty("day") final String day,
            @JsonProperty("hour") final String hour,
            @JsonProperty("good") final Boolean correct) {

        this(day, hour, correct, null);
    }

    private SleepFeedback(final String day, final String hour, final Boolean correct, final Long accountId) {
        this.day = day;
        this.hour = hour;
        this.correct = correct;
        this.accountId = Optional.fromNullable(accountId);
    }

    public static SleepFeedback forAccount(final SleepFeedback feedback, final Long accountId) {
        return new SleepFeedback(feedback.day, feedback.hour, feedback.correct, accountId);
    }
}
