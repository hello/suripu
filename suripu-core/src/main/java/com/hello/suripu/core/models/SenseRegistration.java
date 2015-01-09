package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SenseRegistration {

    public final String senseId;

    @JsonCreator
    public SenseRegistration(
        @JsonProperty("sense_id") final String senseId) {
        this.senseId = senseId;
    }
}
