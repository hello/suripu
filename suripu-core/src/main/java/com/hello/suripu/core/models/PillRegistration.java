package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PillRegistration {

    public final String pillId;
    public final String email;

    @JsonCreator
    public PillRegistration(@JsonProperty("pill_id") final String pillId,
                            @JsonProperty("email") final String email) {
        this.pillId = pillId;
        this.email = email;
    }
}
