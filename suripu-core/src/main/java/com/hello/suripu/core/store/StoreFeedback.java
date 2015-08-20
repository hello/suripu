package com.hello.suripu.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class StoreFeedback {

    public final String question;
    public final String response;
    public final Optional<Long> accountId;

    private StoreFeedback(final String question, final String response) {
        this(question, response, null);
    }

    private StoreFeedback(final String question, final String response, final Long accountId) {
        this.question = question;
        this.response = response;
        this.accountId = Optional.fromNullable(accountId);
    }

    @JsonCreator
    public static StoreFeedback create(@JsonProperty("question") final String question, @JsonProperty("response") final String response) {
        return new StoreFeedback(question, response);
    }

    public static StoreFeedback forAccountId(final StoreFeedback storeFeedback, final Long accountId) {
        return new StoreFeedback(storeFeedback.question, storeFeedback.response, accountId);
    }
}
