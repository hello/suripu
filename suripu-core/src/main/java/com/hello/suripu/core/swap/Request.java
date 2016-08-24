package com.hello.suripu.core.swap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Request {

    final private String senseId;

    private Request(String senseId) {
        this.senseId = senseId;
    }

    @JsonCreator
    public static Request create(@JsonProperty("sense_id") final String senseId) {
        return new Request(senseId);
    }

    public String senseId() {
        return senseId;
    }
}
