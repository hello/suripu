package com.hello.suripu.coredw8.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.HttpClientConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by jakepiccolo on 2/23/16.
 */
public class MessejiHttpClientConfiguration {
    @Valid
    @NotNull
    @JsonProperty("http_client_config")
    private HttpClientConfiguration httpClientConfiguration;
    public HttpClientConfiguration getHttpClientConfiguration() { return httpClientConfiguration; }

    @Valid
    @NotNull
    @JsonProperty("endpoint")
    private String endpoint;
    public String getEndpoint() { return endpoint; }
}
