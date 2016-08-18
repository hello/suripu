package com.hello.suripu.core.swap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SwapRequest {

    final private String senseId;

    private SwapRequest(String senseId) {
        this.senseId = senseId;
    }

    @JsonCreator
    public static SwapRequest create(@JsonProperty("sense_id") final String senseId) {
        return new SwapRequest(senseId);
    }

    public String senseId() {
        return senseId;
    }
}
