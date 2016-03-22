package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Shamelessy ripped off Jake's code, by benjo at some point in time
 */
public class NeuralNetServiceClientConfiguration {


    @Valid
    @NotNull
    @JsonProperty("endpoint")
    private String endpoint;
    public String getEndpoint() { return endpoint; }


    @Valid
    @NotNull
    @JsonProperty("sleep_net_id")
    private String sleepNetId;
    public String getSleepNetId() { return sleepNetId; }



}
