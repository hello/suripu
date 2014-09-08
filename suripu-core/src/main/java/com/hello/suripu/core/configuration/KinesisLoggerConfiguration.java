package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KinesisLoggerConfiguration {

    @JsonProperty("stream_name")
    private String streamName;

    public String getStreamName() {
        return streamName;
    }

    @JsonProperty("enabled")
    private Boolean enabled = false;

    public Boolean isEnabled() {
        return enabled;
    }

}
