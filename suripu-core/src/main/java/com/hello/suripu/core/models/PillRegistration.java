package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PillRegistration {

    public final String pillId;
    public final Long accountId;

    @JsonCreator
    public PillRegistration(
            @JsonProperty("pill_id") final String pillId,
            @JsonProperty("account_id") final Long accountId) {
        this.pillId = pillId;
        this.accountId = accountId;
    }
}
