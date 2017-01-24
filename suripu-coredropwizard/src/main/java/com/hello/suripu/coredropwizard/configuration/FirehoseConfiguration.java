package com.hello.suripu.coredropwizard.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by jakepiccolo on 11/30/15.
 */
public class FirehoseConfiguration {
    @Valid
    @NotNull
    @JsonProperty
    private String region;
    public String getRegion() {
        return region;
    }


    @Valid
    @NotNull
    @JsonProperty("stream")
    private String stream;
    public String getStream() { return stream; }
}
