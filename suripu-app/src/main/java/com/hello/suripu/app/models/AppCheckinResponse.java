package com.hello.suripu.app.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AppCheckinResponse {

    @JsonProperty("app_update_required")
    public final Boolean required;

    @JsonProperty("app_update_message")
    public final String message;

    @JsonProperty("app_new_version")
    public final String newVersion;

    public AppCheckinResponse(final Boolean required, final String message, final String newVersion) {
        this.required = required;
        this.message = message;
        this.newVersion = newVersion;
    }
}
