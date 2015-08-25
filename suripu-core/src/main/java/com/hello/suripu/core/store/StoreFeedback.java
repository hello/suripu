package com.hello.suripu.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class StoreFeedback {

    public final String like;
    public final Boolean review;
    public final Optional<Long> accountId;

    private StoreFeedback(final String like, final Boolean review) {
        this(like, review, null);
    }

    private StoreFeedback(final String like, final Boolean review, final Long accountId) {
        this.like = like;
        this.review = review;
        this.accountId = Optional.fromNullable(accountId);
    }

    @JsonCreator
    public static StoreFeedback create(@JsonProperty("like") final String like, @JsonProperty("review") final Boolean review) {
        return new StoreFeedback(like, review);
    }

    public static StoreFeedback forAccountId(final StoreFeedback storeFeedback, final Long accountId) {
        return new StoreFeedback(storeFeedback.like, storeFeedback.review, accountId);
    }
}
