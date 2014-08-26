package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PillRegistration {

    public final String pillId;

    @JsonCreator
    public PillRegistration(
            @JsonProperty("pill_id") final String pillId) {
        this.pillId = pillId;
    }
}
