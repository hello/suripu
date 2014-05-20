package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class KinesisConfiguration {

    @Valid
    @NotNull
    @JsonProperty
    private String endpoint;

    public String getEndpoint() {
        return endpoint;
    }


    @Valid
    @NotNull
    @JsonProperty
    private String streamName;

    public String getStreamName() {
        return streamName;
    }
}
