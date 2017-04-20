package com.hello.suripu.coredropwizard.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Created by jakepiccolo on 11/30/15.
 */
public class FirehoseConfiguration {
    @Valid
    @JsonProperty("debug")
    private Boolean debug = false;
    public Boolean debug() { return this.debug; }

    @Valid
    @NotNull
    @JsonProperty
    private String region;
    public String region() {
        return region;
    }

    @Valid
    @NotNull
    @JsonProperty("stream")
    private String stream;
    public String stream() { return stream; }

    @Valid
    @JsonProperty("max_buffer_size")
    @Min(1)
    @Max(100)
    private Integer maxBufferSize = 100;
    public Integer maxBufferSize() { return this.maxBufferSize; }
}
