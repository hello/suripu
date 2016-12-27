package com.hello.suripu.coredropwizard.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.hello.suripu.core.util.AlgorithmType;
import io.dropwizard.client.HttpClientConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URL;
import java.util.Map;
/**
 * Created by jarredheinrich on 12/27/16.
 */
public class TaimurainConfiguration {

    @JsonProperty("http_client_config")
    private HttpClientConfiguration httpClientConfiguration;
    public HttpClientConfiguration getHttpClientConfiguration() {
        return httpClientConfiguration;
    }

    @Valid
    @NotNull
    @JsonProperty("endpoints")
    private Map<AlgorithmType, URL> endpoints = Maps.newHashMap();
    public Map<AlgorithmType, URL> getEndpoints() {
        return endpoints;
    }
}
