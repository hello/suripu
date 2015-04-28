package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SenseRegistration {

    public final String senseId;
    public final String email;
    public final String timezone;

    @JsonCreator
    public SenseRegistration(@JsonProperty("sense_id") final String senseId,
                             @JsonProperty("email") final String email,
                             @JsonProperty("timezone") final String timezone) {
        this.senseId = senseId;
        this.email = email;
        this.timezone = timezone;
    }
}
