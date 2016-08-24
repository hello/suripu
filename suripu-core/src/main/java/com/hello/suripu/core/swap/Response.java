package com.hello.suripu.core.swap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Response {

    private final Status status;

    private Response(Status status) {
        this.status = status;
    }

    public static Response create(Status status) {
        return new Response(status);
    }

    @JsonProperty("status")
    public Status status() {
        return status;
    }
}
